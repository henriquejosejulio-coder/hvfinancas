package com.example.mobillsfinancas.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class CdiRateData(
    val dailyRatePercent: Double = DEFAULT_DAILY_RATE, // % ao dia (ex: 0.050788)
    val annualRatePercent: Double = DEFAULT_ANNUAL_RATE, // % ao ano (ex: 13.65)
    val dateStr: String = "Oficial BCB",
    val lastFetchTimestamp: Long = 0L,
    val isLiveFromBcb: Boolean = false
) {
    companion object {
        const val DEFAULT_DAILY_RATE = 0.050788 // Taxa oficial BCB recente (~13,65% a.a.)
        const val DEFAULT_ANNUAL_RATE = 13.65
        const val DEFAULT_TAX_RATE = 22.5 // 22,5% IR regressivo padrão de renda fixa
    }
}

object BcbCdiService {

    private const val TAG = "BcbCdiService"
    private const val PREFS_NAME = "bcb_cdi_prefs"
    private const val KEY_DAILY_RATE = "cdi_daily_rate"
    private const val KEY_ANNUAL_RATE = "cdi_annual_rate"
    private const val KEY_DATE_STR = "cdi_date_str"
    private const val KEY_FETCH_TIME = "cdi_fetch_timestamp"

    // Série 12 do SGS do Banco Central: Taxa CDI diária em % a.d.
    private const val BCB_SERIES_12_URL = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados/ultimos/1?formato=json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retorna a taxa CDI armazenada em cache localmente (instantâneo, seguro para UI thread).
     */
    fun getCachedCdiRate(context: Context): CdiRateData {
        val prefs = getPrefs(context)
        val daily = prefs.getFloat(KEY_DAILY_RATE, CdiRateData.DEFAULT_DAILY_RATE.toFloat()).toDouble()
        val annual = prefs.getFloat(KEY_ANNUAL_RATE, CdiRateData.DEFAULT_ANNUAL_RATE.toFloat()).toDouble()
        val dateStr = prefs.getString(KEY_DATE_STR, "Oficial BCB") ?: "Oficial BCB"
        val timestamp = prefs.getLong(KEY_FETCH_TIME, 0L)
        return CdiRateData(
            dailyRatePercent = if (daily > 0.001) daily else CdiRateData.DEFAULT_DAILY_RATE,
            annualRatePercent = if (annual > 0.1) annual else CdiRateData.DEFAULT_ANNUAL_RATE,
            dateStr = dateStr,
            lastFetchTimestamp = timestamp,
            isLiveFromBcb = timestamp > 0L
        )
    }

    /**
     * Verifica se é necessário atualizar a taxa pela internet hoje.
     */
    fun shouldFetchToday(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastTime = prefs.getLong(KEY_FETCH_TIME, 0L)
        if (lastTime == 0L) return true

        val calNow = Calendar.getInstance()
        val calLast = Calendar.getInstance().apply { timeInMillis = lastTime }

        // Se mudou o dia do ano ou se passaram mais de 12 horas
        val isDifferentDay = calNow.get(Calendar.YEAR) != calLast.get(Calendar.YEAR) ||
                calNow.get(Calendar.DAY_OF_YEAR) != calLast.get(Calendar.DAY_OF_YEAR)
        val isOver12Hours = (System.currentTimeMillis() - lastTime) > (12 * 60 * 60 * 1000L)

        return isDifferentDay || isOver12Hours
    }

    /**
     * Consulta a API do Banco Central em background (coroutine).
     */
    suspend fun fetchCdiRate(context: Context, force: Boolean = false): CdiRateData {
        return withContext(Dispatchers.IO) {
            fetchCdiRateSync(context, force)
        }
    }

    /**
     * Execução síncrona para chamadas já em thread de background (ex: IO Dispatcher ou WorkManager).
     */
    fun fetchCdiRateSync(context: Context, force: Boolean = false): CdiRateData {
        val cached = getCachedCdiRate(context)
        if (!force && !shouldFetchToday(context)) {
            return cached
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(BCB_SERIES_12_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; HVFinancas/1.0)")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = reader.use { it.readText() }

                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val rawVal = obj.getString("valor").replace(',', '.').trim()
                    val dateStr = obj.optString("data", "").trim()
                    val dailyVal = rawVal.toDoubleOrNull()

                    if (dailyVal != null && dailyVal in 0.001..1.0) {
                        // Converte taxa diária composta em taxa anual base 252 dias úteis
                        val dailyDecimal = dailyVal / 100.0
                        val annualVal = (Math.pow(1.0 + dailyDecimal, 252.0) - 1.0) * 100.0
                        val roundedAnnual = Math.round(annualVal * 100.0) / 100.0

                        getPrefs(context).edit()
                            .putFloat(KEY_DAILY_RATE, dailyVal.toFloat())
                            .putFloat(KEY_ANNUAL_RATE, roundedAnnual.toFloat())
                            .putString(KEY_DATE_STR, dateStr)
                            .putLong(KEY_FETCH_TIME, System.currentTimeMillis())
                            .apply()

                        Log.i(TAG, "Taxa CDI atualizada com sucesso do BCB: $dailyVal% a.d. (~$roundedAnnual% a.a.) - Data: $dateStr")

                        return CdiRateData(
                            dailyRatePercent = dailyVal,
                            annualRatePercent = roundedAnnual,
                            dateStr = dateStr,
                            lastFetchTimestamp = System.currentTimeMillis(),
                            isLiveFromBcb = true
                        )
                    }
                }
            } else {
                Log.w(TAG, "BCB API retornou código HTTP $responseCode")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao consultar API do Banco Central (usando cache local): ${t.message}")
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Throwable) {}
        }

        return cached
    }

    /**
     * Calcula o rendimento diário líquido oficial com retenção de IR (22,5%).
     */
    fun calculateDailyNetYield(
        balance: Double,
        cdiRatePercentage: Double,
        dailyCdiRatePercent: Double = CdiRateData.DEFAULT_DAILY_RATE,
        taxRatePercentage: Double = CdiRateData.DEFAULT_TAX_RATE,
        isBusinessDay: Boolean = true
    ): Double {
        if (balance <= 0.0 || cdiRatePercentage <= 0.0 || !isBusinessDay) return 0.0
        val dailyCdiDecimal = dailyCdiRatePercent / 100.0
        val dailyAccountRate = dailyCdiDecimal * (cdiRatePercentage / 100.0)
        val grossDaily = balance * dailyAccountRate
        val netDaily = grossDaily * (1.0 - (taxRatePercentage / 100.0))
        return Math.round(netDaily * 100.0) / 100.0
    }

    /**
     * Calcula o rendimento mensal líquido oficial estimado para 22 dias úteis.
     */
    fun calculateMonthlyNetYield(
        balance: Double,
        cdiRatePercentage: Double,
        dailyCdiRatePercent: Double = CdiRateData.DEFAULT_DAILY_RATE,
        taxRatePercentage: Double = CdiRateData.DEFAULT_TAX_RATE,
        businessDaysCount: Int = 22
    ): Double {
        if (balance <= 0.0 || cdiRatePercentage <= 0.0) return 0.0
        val dailyCdiDecimal = dailyCdiRatePercent / 100.0
        val dailyAccountRate = dailyCdiDecimal * (cdiRatePercentage / 100.0)
        val grossMonthly = balance * (Math.pow(1.0 + dailyAccountRate, businessDaysCount.toDouble()) - 1.0)
        val netMonthly = grossMonthly * (1.0 - (taxRatePercentage / 100.0))
        return Math.round(netMonthly * 100.0) / 100.0
    }
}
