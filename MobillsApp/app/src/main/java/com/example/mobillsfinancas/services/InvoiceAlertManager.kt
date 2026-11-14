package com.example.mobillsfinancas.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mobillsfinancas.MainActivity
import com.example.mobillsfinancas.data.CreditCardItem
import com.example.mobillsfinancas.data.FinanceRepository
import com.example.mobillsfinancas.utils.Formatters
import com.example.mobillsfinancas.utils.MonthHelper
import java.util.Calendar

object InvoiceAlertManager {

    const val CHANNEL_ID = "invoice_due_alerts"
    private const val PREFS_NAME = "invoice_alerts_prefs"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Faturas de Cartão"
            val desc = "Avisos de fechamento e vencimento de faturas de cartão de crédito"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifyInvoices(context: Context) {
        try {
            createNotificationChannel(context)
            val repository = FinanceRepository.getInstance(context)
            val cards = repository.cards.value
            if (cards.isEmpty()) return

            val nowCal = Calendar.getInstance()
            val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
            val currentMonth = MonthHelper.getCurrentMonthYear()

            val tomorrowCal = (nowCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowDay = tomorrowCal.get(Calendar.DAY_OF_MONTH)
            val tomorrowMonth = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d",
                tomorrowDay, tomorrowCal.get(Calendar.MONTH) + 1, tomorrowCal.get(Calendar.YEAR))
            val dueTomorrowTargetMonth = MonthHelper.getMonthYearFromDate(tomorrowMonth) ?: currentMonth

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            for (card in cards) {
                // Cartões sem crédito ou sem dias configurados são ignorados
                if (!card.cardType.allowsCredit || card.closingDay <= 0 || card.dueDay <= 0) {
                    continue
                }

                // 1. DIA DO FECHAMENTO (closingDay)
                if (currentDay == card.closingDay) {
                    val targetMonth = if (card.closingDay < card.dueDay) {
                        currentMonth
                    } else {
                        MonthHelper.addMonths(currentMonth, 1)
                    }
                    val amount = repository.getInvoiceForCard(card, targetMonth)
                    val isPaid = repository.isInvoicePaidForCard(card, targetMonth)

                    // Fatura zerada ou já paga NÃO notifica
                    if (amount > 0.0 && !isPaid) {
                        val alertKey = "alert_closing_${card.id}_${currentDay}_$targetMonth"
                        if (!prefs.getBoolean(alertKey, false)) {
                            sendClosingNotification(context, card, amount, targetMonth)
                            prefs.edit().putBoolean(alertKey, true).apply()
                            Log.d("InvoiceAlertManager", "Notificação de fechamento enviada para ${card.name}: R$ $amount ($targetMonth)")
                        }
                    }
                }

                // 2. UM DIA ANTES DO VENCIMENTO (dueDay - 1)
                if (tomorrowDay == card.dueDay) {
                    val targetMonth = dueTomorrowTargetMonth
                    val amount = repository.getInvoiceForCard(card, targetMonth)
                    val isPaid = repository.isInvoicePaidForCard(card, targetMonth)

                    // Fatura zerada ou já paga NÃO notifica
                    if (amount > 0.0 && !isPaid) {
                        val alertKey = "alert_due_tomorrow_${card.id}_${currentDay}_$targetMonth"
                        if (!prefs.getBoolean(alertKey, false)) {
                            sendDueTomorrowNotification(context, card, amount, targetMonth)
                            prefs.edit().putBoolean(alertKey, true).apply()
                            Log.d("InvoiceAlertManager", "Notificação de véspera de vencimento enviada para ${card.name}: R$ $amount ($targetMonth)")
                        }
                    }
                }

                // 3. NO DIA DO VENCIMENTO (dueDay)
                if (currentDay == card.dueDay) {
                    val targetMonth = currentMonth
                    val amount = repository.getInvoiceForCard(card, targetMonth)
                    val isPaid = repository.isInvoicePaidForCard(card, targetMonth)

                    // Fatura zerada ou já paga NÃO notifica
                    if (amount > 0.0 && !isPaid) {
                        val alertKey = "alert_due_today_${card.id}_${currentDay}_$targetMonth"
                        if (!prefs.getBoolean(alertKey, false)) {
                            sendDueTodayNotification(context, card, amount, targetMonth, isReminder = false)
                            prefs.edit().putBoolean(alertKey, true).apply()
                            Log.d("InvoiceAlertManager", "Notificação de vencimento HOJE enviada para ${card.name}: R$ $amount ($targetMonth)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("InvoiceAlertManager", "Erro ao verificar faturas de cartão", e)
        }
    }

    private fun sendClosingNotification(context: Context, card: CreditCardItem, amount: Double, targetMonth: String) {
        val notifId = (card.id.hashCode() and 0x7FFFFFFF) % 100000 + 100000
        val title = "📅 Fatura Fecha Hoje! • ${card.name}"
        val text = "A fatura de $targetMonth fecha hoje no valor de ${Formatters.formatMoney(amount, false)}. Não deixe de conferir seus lançamentos!"

        val openAppPendingIntent = createOpenCardsPendingIntent(context, notifId)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    private fun sendDueTomorrowNotification(context: Context, card: CreditCardItem, amount: Double, targetMonth: String) {
        val notifId = (card.id.hashCode() and 0x7FFFFFFF) % 100000 + 200000
        val title = "⏰ Fatura Vence Amanhã! • ${card.name}"
        val text = "A fatura de $targetMonth no valor de ${Formatters.formatMoney(amount, false)} vence amanhã (dia ${card.dueDay}). Prepare seu pagamento!"

        val openAppPendingIntent = createOpenCardsPendingIntent(context, notifId)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    fun sendDueTodayNotification(
        context: Context,
        card: CreditCardItem,
        amount: Double,
        targetMonth: String,
        isReminder: Boolean
    ) {
        val notifId = (card.id.hashCode() and 0x7FFFFFFF) % 100000 + 300000
        val title = if (isReminder) "🚨 Lembrete: Fatura Vence HOJE! • ${card.name}" else "🚨 Fatura Vence HOJE! • ${card.name}"
        val text = "Atenção: A fatura de $targetMonth no valor de ${Formatters.formatMoney(amount, false)} vence hoje (dia ${card.dueDay}). Evite juros e pague sua fatura!"

        val openAppPendingIntent = createOpenCardsPendingIntent(context, notifId)

        // Ação: Lembrar mais tarde (2 horas)
        val snoozeIntent = Intent(context, InvoiceAlertReceiver::class.java).apply {
            action = InvoiceAlertReceiver.ACTION_SNOOZE
            putExtra("EXTRA_CARD_ID", card.id)
            putExtra("EXTRA_CARD_NAME", card.name)
            putExtra("EXTRA_TARGET_MONTH", targetMonth)
            putExtra("EXTRA_NOTIF_ID", notifId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 500,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_popup_reminder, "⏰ Lembrar mais tarde (2h)", snoozePendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    private fun createOpenCardsPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_TAB", 2) // Aba Cartões
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleDailyAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, InvoiceAlertReceiver::class.java).apply {
                action = InvoiceAlertReceiver.ACTION_CHECK_DAILY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
            Log.d("InvoiceAlertManager", "Alarme diário de faturas agendado para ${cal.time}")
        } catch (e: Exception) {
            Log.e("InvoiceAlertManager", "Erro ao agendar alarme diário de faturas", e)
        }
    }

    fun scheduleSnooze(
        context: Context,
        cardId: String,
        cardName: String,
        targetMonth: String,
        notifId: Int,
        delayMinutes: Int = 120
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, InvoiceAlertReceiver::class.java).apply {
                action = InvoiceAlertReceiver.ACTION_SNOOZE_FIRED
                putExtra("EXTRA_CARD_ID", cardId)
                putExtra("EXTRA_CARD_NAME", cardName)
                putExtra("EXTRA_TARGET_MONTH", targetMonth)
                putExtra("EXTRA_NOTIF_ID", notifId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 700,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("InvoiceAlertManager", "Snooze de fatura agendado para daqui a $delayMinutes minutos (card=$cardName)")
        } catch (e: Exception) {
            Log.e("InvoiceAlertManager", "Erro ao agendar snooze de fatura", e)
        }
    }
}
