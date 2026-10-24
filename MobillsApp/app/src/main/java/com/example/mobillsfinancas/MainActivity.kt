package com.example.mobillsfinancas

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobillsfinancas.services.BankNotificationService
import com.example.mobillsfinancas.theme.MobillsFinancasTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Solicita permissão para exibir notificações no Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    // Reconecta o serviço de notificações caso o Android tenha desvinculado após atualização do APK
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val componentName = ComponentName(this, BankNotificationService::class.java)
            NotificationListenerService.requestRebind(componentName)
            Log.d("MainActivity", "requestRebind executado para BankNotificationService")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Solicita execução em segundo plano sem restrições de bateria
    if (!com.example.mobillsfinancas.utils.PermissionHelper.isIgnoringBatteryOptimizations(this)) {
        com.example.mobillsfinancas.utils.PermissionHelper.requestIgnoreBatteryOptimizations(this)
    }

    enableEdgeToEdge()
    setContent {
      MobillsFinancasTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
