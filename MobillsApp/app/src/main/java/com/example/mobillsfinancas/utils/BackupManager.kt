package com.example.mobillsfinancas.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "hvfinancas_vault"
    private const val KEY_ACTIVE_SNAPSHOT = "active_db_snapshot"
    private const val KEY_PRE_UPDATE_BACKUP = "pre_update_backup"
    private const val KEY_LAST_SAVE_TIME = "last_save_time"
    private const val KEY_ITEM_COUNT = "item_count"

    fun countItems(jsonString: String?): Int {
        if (jsonString.isNullOrBlank() || jsonString.length < 50) return -1
        return try {
            val trimmed = jsonString.trim()
            val monthsArr = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("months") ?: return 0
            }
            var count = 0
            for (i in 0 until monthsArr.length()) {
                val mObj = monthsArr.optJSONObject(i) ?: continue
                val itemsArr = mObj.optJSONArray("itens") ?: mObj.optJSONArray("items") ?: continue
                count += itemsArr.length()
            }
            count
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Gravação atômica segura com fsync (impede corrupção de arquivo em desligamentos ou fechamentos bruscos).
     */
    fun atomicWrite(file: File, content: String) {
        try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tmpFile = File(parent ?: file.parentFile, "${file.name}.tmp")
            FileOutputStream(tmpFile).use { fos ->
                val bytes = content.toByteArray(Charsets.UTF_8)
                fos.write(bytes)
                fos.flush()
                fos.fd.sync()
            }
            if (tmpFile.exists()) {
                if (file.exists()) {
                    file.delete()
                }
                tmpFile.renameTo(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na escrita atomica de ${file.name}", e)
            try {
                file.writeText(content)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Snapshot contínuo gravado a cada alteração em memória, SharedPreferences e arquivos redundantes.
     */
    fun saveContinuousSnapshot(context: Context, jsonString: String) {
        if (jsonString.isBlank() || jsonString.length < 50) return
        val count = countItems(jsonString)

        // 1. SharedPreferences Vault (independente de filesDir, não é apagado em atualizações de APK)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prevCount = prefs.getInt(KEY_ITEM_COUNT, -1)
            // Só sobrescreve se o novo snapshot for válido e não tiver menos itens por acidente
            if (count >= prevCount || prevCount <= 0) {
                prefs.edit()
                    .putString(KEY_ACTIVE_SNAPSHOT, jsonString)
                    .putLong(KEY_LAST_SAVE_TIME, System.currentTimeMillis())
                    .putInt(KEY_ITEM_COUNT, count)
                    .apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gravar snapshot no SharedPreferences", e)
        }

        // 2. Backup espelho em filesDir
        try {
            val mirror = File(context.filesDir, "hvfinancas_v6_backup.json")
            atomicWrite(mirror, jsonString)
        } catch (e: Exception) {
            // silencioso
        }

        // 3. Backup espelho em armazenamento externo privado do app (sobrevive a limpezas parciais)
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val extMirror = File(extDir, "hvfinancas_auto_backup.json")
                atomicWrite(extMirror, jsonString)
            }
        } catch (e: Exception) {
            // silencioso
        }

        // 4. Backup espelho na pasta pública Downloads (se acessível)
        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir != null && dlDir.exists()) {
                val dlFile = File(dlDir, "hvfinancas_backup.json")
                atomicWrite(dlFile, jsonString)
            }
        } catch (e: Exception) {
            // silencioso
        }
    }

    /**
     * Backup de emergência disparado compulsoriamente ANTES de qualquer download ou instalação de atualização.
     */
    fun createPreUpdateBackup(context: Context, currentJson: String) {
        if (currentJson.isBlank() || currentJson.length < 50) return
        val count = countItems(currentJson)
        Log.i(TAG, "Criando Pre-Update Backup com $count itens...")

        // 1. SharedPreferences Vault
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PRE_UPDATE_BACKUP, currentJson)
                .putLong("pre_update_time", System.currentTimeMillis())
                .putInt("pre_update_count", count)
                .commit() // commit síncrono para garantir escrita imediata
        } catch (e: Exception) {
            Log.e(TAG, "Erro no pre-update SharedPreferences", e)
        }

        // 2. Arquivos físicos locais e externos
        val targets = mutableListOf<File>(
            File(context.filesDir, "pre_update_backup.json"),
            File(context.filesDir, "hvfinancas_emergency_recovery.json")
        )
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                targets.add(File(extDir, "pre_update_backup.json"))
                targets.add(File(extDir, "hvfinancas_pre_update_backup.json"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir != null && dlDir.exists()) {
                targets.add(File(dlDir, "HVFinancas_PreUpdate_Backup.json"))
                targets.add(File(dlDir, "hvfinancas_pre_update_backup.json"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (target in targets) {
            try {
                atomicWrite(target, currentJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Varredura profunda e inteligente de restauração de dados.
     * Retorna o JSON mais rico e recente encontrado em qualquer camada do dispositivo.
     */
    fun deepScanBestAvailableData(context: Context): String? {
        data class Candidate(val content: String, val itemCount: Int, val timestamp: Long, val source: String)
        val candidates = mutableListOf<Candidate>()

        // 1. Inspeciona SharedPreferences Vault
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val activeSnap = prefs.getString(KEY_ACTIVE_SNAPSHOT, null)
            if (!activeSnap.isNullOrBlank()) {
                val cnt = countItems(activeSnap)
                val time = prefs.getLong(KEY_LAST_SAVE_TIME, 0L)
                if (cnt >= 0) candidates.add(Candidate(activeSnap, cnt, time, "Prefs:active_db_snapshot"))
            }

            val preUpdate = prefs.getString(KEY_PRE_UPDATE_BACKUP, null)
            if (!preUpdate.isNullOrBlank()) {
                val cnt = countItems(preUpdate)
                val time = prefs.getLong("pre_update_time", 0L)
                if (cnt >= 0) candidates.add(Candidate(preUpdate, cnt, time, "Prefs:pre_update_backup"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Inspeciona arquivos em filesDir
        val filesToCheck = mutableListOf(
            File(context.filesDir, "hvfinancas_v6.json"),
            File(context.filesDir, "pre_update_backup.json"),
            File(context.filesDir, "hvfinancas_emergency_recovery.json"),
            File(context.filesDir, "hvfinancas_v6_backup.json"),
            File(context.filesDir, "hvfinancas_auto_backup.json"),
            File(context.filesDir, "hvfinancas_backup.json"),
            File(context.filesDir, "HV_Financas_Backup_Atualizado.json"),
            File(context.filesDir, "backup_hvfinancas_corrigido.json"),
            File(context.filesDir, "backup_usuario_prompt.json"),
            File(context.filesDir, "hvfinancas_v5.json"),
            File(context.filesDir, "hvfinancas_v4.json"),
            File(context.filesDir, "hvfinancas_v3.json"),
            File(context.filesDir, "hvfinancas.json"),
            File(context.filesDir, "financas_data.json")
        )

        // Adiciona qualquer outro .json existente em filesDir
        try {
            val allInFiles = context.filesDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            if (allInFiles != null) {
                for (f in allInFiles) {
                    if (filesToCheck.none { it.absolutePath == f.absolutePath }) {
                        filesToCheck.add(f)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Adiciona arquivos em externalFilesDir
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null && extDir.exists()) {
                val extFiles = extDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                if (extFiles != null) {
                    filesToCheck.addAll(extFiles)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Adiciona arquivos em Downloads
        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir != null && dlDir.exists()) {
                val dlCandidates = listOf(
                    File(dlDir, "HVFinancas_PreUpdate_Backup.json"),
                    File(dlDir, "hvfinancas_pre_update_backup.json"),
                    File(dlDir, "hvfinancas_backup.json"),
                    File(dlDir, "HV_Financas_Backup_Atualizado.json"),
                    File(dlDir, "backup_hvfinancas_corrigido.json"),
                    File(dlDir, "hvfinancas_v6_backup.json"),
                    File(dlDir, "hvfinancas_v6.json")
                )
                for (f in dlCandidates) {
                    if (f.exists() && f.isFile) filesToCheck.add(f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Lê e avalia cada arquivo
        for (file in filesToCheck) {
            try {
                if (file.exists() && file.isFile && file.length() > 50) {
                    val text = file.readText()
                    val cnt = countItems(text)
                    if (cnt >= 0) {
                        candidates.add(Candidate(text, cnt, file.lastModified(), file.name))
                    }
                }
            } catch (e: Exception) {
                // ignora erros de leitura de arquivos individuais
            }
        }

        if (candidates.isEmpty()) {
            return null
        }

        // Ordena primeiro pelo MAIOR número de itens de transação, depois pelo timestamp mais recente
        val best = candidates.maxWithOrNull(
            compareBy<Candidate> { it.itemCount }
                .thenBy { it.timestamp }
        )

        Log.i(TAG, "Melhor candidato escolhido: ${best?.source} com ${best?.itemCount} itens (Timestamp: ${best?.timestamp})")
        return best?.content
    }
}
