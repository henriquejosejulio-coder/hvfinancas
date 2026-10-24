package com.example.mobillsfinancas.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.mobillsfinancas.data.FinanceRepository
import java.util.Locale

class InstallmentActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        val notifId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == "com.example.mobillsfinancas.ACTION_SET_INSTALLMENTS") {
            val transactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID") ?: return
            val installments = intent.getIntExtra("EXTRA_INSTALLMENTS", 1)
            val baseAmount = intent.getDoubleExtra("EXTRA_BASE_AMOUNT", 0.0)
            val desc = intent.getStringExtra("EXTRA_DESC") ?: "Compra"

            val repo = FinanceRepository.getInstance(context)
            repo.updateInstallmentsForTransaction(transactionId, installments)

            val parcelVal = if (installments > 1) baseAmount / installments else baseAmount
            val msg = if (installments > 1) {
                String.format(Locale.getDefault(), "✓ '%s' parcelado em %dx de R$ %.2f!", desc, installments, parcelVal)
            } else {
                String.format(Locale.getDefault(), "✓ '%s' confirmado à vista (1x de R$ %.2f)!", desc, baseAmount)
            }

            // Exibe Toast na UI principal
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show()
            }

            // Atualiza notificação avisando que foi concluído
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("mobills_auto_capture", "Lançamentos Automáticos", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            val updatedNotif = NotificationCompat.Builder(context, "mobills_auto_capture")
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("✅ HV Finanças: Parcelas Definidas!")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(8000)
                .build()

            notificationManager.notify(notifId, updatedNotif)
        } else if (action == "com.example.mobillsfinancas.ACTION_DISMISS_NOTIFICATION") {
            notificationManager.cancel(notifId)
        }
    }
}
