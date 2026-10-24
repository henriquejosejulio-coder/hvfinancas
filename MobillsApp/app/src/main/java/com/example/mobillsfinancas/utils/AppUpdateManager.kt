package com.example.mobillsfinancas.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.mobillsfinancas.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersionName: String,
    val changelog: String,
    val downloadUrl: String
)

object AppUpdateManager {

    private const val GITHUB_REPO = "henriquejosejulio-coder/hvfinancas"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASES_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "HVFinancas-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode != 200) {
                return@withContext null
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = JSONObject(jsonString)

            val tagName = releaseObj.optString("tag_name", "").replace("v", "").trim()
            val body = releaseObj.optString("body", "Melhorias gerais e correções.")
            val assetsArray = releaseObj.optJSONArray("assets")

            var downloadUrl = ""
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (downloadUrl.isBlank() || tagName.isBlank()) {
                return@withContext null
            }

            val currentVersion = BuildConfig.VERSION_NAME.replace("v", "").trim()
            if (isVersionNewer(tagName, currentVersion)) {
                return@withContext UpdateInfo(
                    latestVersionName = tagName,
                    changelog = body,
                    downloadUrl = downloadUrl
                )
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remote != current
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // CRÍTICO: Executa backup compulsório de segurança ANTES de qualquer download ou instalação de APK!
            try {
                val currentData = com.example.mobillsfinancas.data.FinanceRepository.getInstance(context).exportBackupJson()
                if (currentData.isNotBlank() && currentData.length > 50) {
                    BackupManager.createPreUpdateBackup(context, currentData)
                    android.util.Log.i("AppUpdateManager", "Backup pré-atualização criado com sucesso antes de iniciar o download.")
                }
            } catch (e: Exception) {
                android.util.Log.e("AppUpdateManager", "Erro ao criar backup pré-atualização", e)
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "HVFinancas_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // Suporta redirecionamento automático (GitHub redireciona assets para AWS S3)
            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "HVFinancas-Android")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    currentUrl = connection.getHeaderField("Location") ?: break
                    redirects++
                    if (redirects > 5) break
                } else {
                    break
                }
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Erro ao baixar atualização.")
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
