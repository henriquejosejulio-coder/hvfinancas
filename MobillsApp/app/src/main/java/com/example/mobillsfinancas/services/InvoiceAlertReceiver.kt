package com.example.mobillsfinancas.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.mobillsfinancas.data.FinanceRepository

class InvoiceAlertReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_DAILY = "com.example.mobillsfinancas.ACTION_CHECK_DAILY_INVOICES"
        const val ACTION_SNOOZE = "com.example.mobillsfinancas.ACTION_SNOOZE_INVOICE"
        const val ACTION_SNOOZE_FIRED = "com.example.mobillsfinancas.ACTION_SNOOZE_FIRED"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("InvoiceAlertReceiver", "Recebido broadcast: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, ACTION_CHECK_DAILY -> {
                InvoiceAlertManager.checkAndNotifyInvoices(context)
                InvoiceAlertManager.scheduleDailyAlarm(context)
            }

            ACTION_SNOOZE -> {
                val cardId = intent.getStringExtra("EXTRA_CARD_ID") ?: ""
                val cardName = intent.getStringExtra("EXTRA_CARD_NAME") ?: "Cartão"
                val targetMonth = intent.getStringExtra("EXTRA_TARGET_MONTH") ?: ""
                val notifId = intent.getIntExtra("EXTRA_NOTIF_ID", 0)

                // 1. Cancela a notificação atual
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notifId != 0) {
                    manager.cancel(notifId)
                }

                // 2. Agenda o lembrete para 2 horas depois
                InvoiceAlertManager.scheduleSnooze(context, cardId, cardName, targetMonth, notifId, delayMinutes = 120)

                // 3. Feedback visual para o usuário
                Toast.makeText(context, "⏰ Lembrete de fatura adiado por 2 horas", Toast.LENGTH_SHORT).show()
                Log.d("InvoiceAlertReceiver", "Snooze acionado para $cardName (2h)")
            }

            ACTION_SNOOZE_FIRED -> {
                val cardId = intent.getStringExtra("EXTRA_CARD_ID") ?: return
                val targetMonth = intent.getStringExtra("EXTRA_TARGET_MONTH") ?: return

                val repository = FinanceRepository.getInstance(context)
                val card = repository.cards.value.find { it.id == cardId }
                if (card != null) {
                    val amount = repository.getInvoiceForCard(card, targetMonth)
                    val isPaid = repository.isInvoicePaidForCard(card, targetMonth)

                    // Só exibe novamente se a fatura ainda estiver maior que zero e não paga
                    if (amount > 0.0 && !isPaid) {
                        InvoiceAlertManager.sendDueTodayNotification(context, card, amount, targetMonth, isReminder = true)
                        Log.d("InvoiceAlertReceiver", "Lembrete de vencimento (Snooze) reexibido para ${card.name}: R$ $amount")
                    } else {
                        Log.d("InvoiceAlertReceiver", "Fatura de ${card.name} já foi paga ou zerada. Notificação não reexibida.")
                    }
                }
            }
        }
    }
}
