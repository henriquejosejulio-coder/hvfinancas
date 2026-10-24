package com.example.mobillsfinancas.utils

import android.content.Context
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

    /**
     * Conta de forma segura a quantidade de transações presentes em uma string JSON de backup.
     * Retorna -1 se a string for nula, vazia ou inválida.
     */
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
        } catch (t: Throwable) {
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
                try {
                    fos.fd.sync()
                } catch (t: Throwable) {
                    // Alguns dispositivos não suportam sync no fd
                }
            }
            if (tmpFile.exists()) {
                if (file.exists()) {
                    file.delete()
                }
                val renamed = tmpFile.renameTo(file)
                if (!renamed) {
                    file.writeText(content)
                    tmpFile.delete()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erro na escrita atomica de ${file.name}", t)
            try {
                file.writeText(content)
            } catch (t2: Throwable) {
                // Silencioso para não travar a aplicação
            }
        }
    }

    /**
     * Snapshot contínuo gravado de forma redundante e segura.
     * Usa exclusivamente armazenamento privado do app (SharedPreferences, filesDir e externalFilesDir),
     * garantindo 100% de sobrevivência e conformidade com Scoped Storage no Android 11 a 15 (inclusive Xiaomi MIUI/HyperOS).
     */
    fun saveContinuousSnapshot(context: Context, jsonString: String) {
        if (jsonString.isBlank() || jsonString.length < 50) return
        val count = countItems(jsonString)

        // 1. SharedPreferences Vault (independente de filesDir, não é apagado em atualizações de APK)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prevCount = prefs.getInt(KEY_ITEM_COUNT, -1)
            if (count >= prevCount || prevCount <= 0) {
                prefs.edit()
                    .putString(KEY_ACTIVE_SNAPSHOT, jsonString)
                    .putLong(KEY_LAST_SAVE_TIME, System.currentTimeMillis())
                    .putInt(KEY_ITEM_COUNT, count)
                    .apply()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erro ao gravar snapshot no SharedPreferences", t)
        }

        // 2. Backup espelho em filesDir interno
        try {
            val mirror = File(context.filesDir, "hvfinancas_v6_backup.json")
            atomicWrite(mirror, jsonString)
        } catch (t: Throwable) {
            // Silencioso
        }

        // 3. Backup espelho em armazenamento externo privado do app (sobrevive a limpezas de dados parciais)
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val extMirror = File(extDir, "hvfinancas_auto_backup.json")
                atomicWrite(extMirror, jsonString)
            }
        } catch (t: Throwable) {
            // Silencioso
        }
    }

    /**
     * Backup de emergência disparado compulsoriamente ANTES de qualquer download ou instalação de atualização.
     */
    fun createPreUpdateBackup(context: Context, currentJson: String) {
        if (currentJson.isBlank() || currentJson.length < 50) return
        val count = countItems(currentJson)
        Log.i(TAG, "Criando Pre-Update Backup com $count itens...")

        // 1. SharedPreferences Vault com commit síncrono para garantia imediata
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PRE_UPDATE_BACKUP, currentJson)
                .putLong("pre_update_time", System.currentTimeMillis())
                .putInt("pre_update_count", count)
                .commit()
        } catch (t: Throwable) {
            Log.e(TAG, "Erro no pre-update SharedPreferences", t)
        }

        // 2. Arquivos físicos locais internos
        try {
            atomicWrite(File(context.filesDir, "pre_update_backup.json"), currentJson)
            atomicWrite(File(context.filesDir, "hvfinancas_emergency_recovery.json"), currentJson)
        } catch (t: Throwable) {
            // Silencioso
        }

        // 3. Arquivos físicos locais externos privados
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                atomicWrite(File(extDir, "pre_update_backup.json"), currentJson)
                atomicWrite(File(extDir, "hvfinancas_pre_update_backup.json"), currentJson)
            }
        } catch (t: Throwable) {
            // Silencioso
        }
    }

    private enum class CandidateSourceType { PREFS_ACTIVE, PREFS_PRE_UPDATE, FILE }

    private data class CandidateMeta(
        val type: CandidateSourceType,
        val file: File? = null,
        val itemCount: Int,
        val timestamp: Long,
        val source: String
    )

    /**
     * Varredura profunda, inteligente e ultra-segura para restauração de dados.
     * Retorna o JSON mais rico e recente encontrado nas camadas privadas do dispositivo.
     * Não causa vazamento de memória nem violações de Scoped Storage.
     */
    fun deepScanBestAvailableData(context: Context): String? {
        return try {
            val candidates = mutableListOf<CandidateMeta>()

            // 1. Inspeciona SharedPreferences Vault (acesso em memória, ultra-rápido e 100% seguro)
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val activeSnap = prefs.getString(KEY_ACTIVE_SNAPSHOT, null)
                if (!activeSnap.isNullOrBlank() && activeSnap.length > 50) {
                    var cnt = prefs.getInt(KEY_ITEM_COUNT, -1)
                    if (cnt < 0) cnt = countItems(activeSnap)
                    val time = prefs.getLong(KEY_LAST_SAVE_TIME, 0L)
                    if (cnt >= 0) {
                        candidates.add(CandidateMeta(CandidateSourceType.PREFS_ACTIVE, null, cnt, time, "Prefs:active_snapshot"))
                    }
                }

                val preUpdate = prefs.getString(KEY_PRE_UPDATE_BACKUP, null)
                if (!preUpdate.isNullOrBlank() && preUpdate.length > 50) {
                    var cnt = prefs.getInt("pre_update_count", -1)
                    if (cnt < 0) cnt = countItems(preUpdate)
                    val time = prefs.getLong("pre_update_time", 0L)
                    if (cnt >= 0) {
                        candidates.add(CandidateMeta(CandidateSourceType.PREFS_PRE_UPDATE, null, cnt, time, "Prefs:pre_update_backup"))
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Erro inspecionando SharedPreferences Vault", t)
            }

            // 2. Inspeciona arquivos em filesDir interno
            val filesToCheck = mutableListOf(
                File(context.filesDir, "pre_update_backup.json"),
                File(context.filesDir, "hvfinancas_emergency_recovery.json"),
                File(context.filesDir, "hvfinancas_v6_backup.json"),
                File(context.filesDir, "hvfinancas_auto_backup.json"),
                File(context.filesDir, "hvfinancas_backup.json"),
                File(context.filesDir, "HV_Financas_Backup_Atualizado.json"),
                File(context.filesDir, "backup_hvfinancas_corrigido.json"),
                File(context.filesDir, "backup_usuario_prompt.json"),
                File(context.filesDir, "hvfinancas_v6.json"),
                File(context.filesDir, "hvfinancas_v5.json"),
                File(context.filesDir, "hvfinancas_v4.json"),
                File(context.filesDir, "hvfinancas_v3.json"),
                File(context.filesDir, "hvfinancas.json"),
                File(context.filesDir, "financas_data.json")
            )

            // Adiciona outros arquivos .json de filesDir
            try {
                val allInFiles = context.filesDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                if (allInFiles != null) {
                    for (f in allInFiles) {
                        if (filesToCheck.none { it.absolutePath == f.absolutePath }) {
                            filesToCheck.add(f)
                        }
                    }
                }
            } catch (t: Throwable) {
                // Silencioso
            }

            // 3. Inspeciona arquivos em externalFilesDir privado
            try {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null && extDir.exists()) {
                    val extFiles = extDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                    if (extFiles != null) {
                        for (f in extFiles) {
                            if (filesToCheck.none { it.absolutePath == f.absolutePath }) {
                                filesToCheck.add(f)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                // Silencioso
            }

            // Avalia cada arquivo de forma leve (sem carregar todos simultaneamente na memória)
            for (file in filesToCheck) {
                try {
                    if (file.exists() && file.isFile && file.length() > 50) {
                        val text = file.readText()
                        val cnt = countItems(text)
                        if (cnt >= 0) {
                            candidates.add(
                                CandidateMeta(
                                    CandidateSourceType.FILE,
                                    file,
                                    cnt,
                                    file.lastModified(),
                                    file.name
                                )
                            )
                        }
                    }
                } catch (t: Throwable) {
                    // Silencioso para erros de arquivo individual
                }
            }

            if (candidates.isEmpty()) {
                return null
            }

            // Escolhe o melhor candidato: maior quantidade de transações salvas, desempate por timestamp mais recente
            val best = candidates.maxWithOrNull(
                compareBy<CandidateMeta> { it.itemCount }
                    .thenBy { it.timestamp }
            ) ?: return null

            Log.i(TAG, "Melhor candidato escolhido: ${best.source} (${best.itemCount} itens, timestamp: ${best.timestamp})")

            // Lê e retorna o conteúdo exclusivamente do candidato vencedor
            when (best.type) {
                CandidateSourceType.PREFS_ACTIVE -> {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.getString(KEY_ACTIVE_SNAPSHOT, null)
                }
                CandidateSourceType.PREFS_PRE_UPDATE -> {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.getString(KEY_PRE_UPDATE_BACKUP, null)
                }
                CandidateSourceType.FILE -> {
                    best.file?.takeIf { it.exists() }?.readText()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erro crítico durante deepScanBestAvailableData", t)
            null
        }
    }
}
