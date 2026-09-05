package io.github.immaghzbad.aetherst.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.immaghzbad.aetherst.core.AutoConnectManager
import io.github.immaghzbad.aetherst.shared.data.LogRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            LogRepository.i("[BootReceiver] Boot completed, checking auto-connect")
            AutoConnectManager.handleBootCompleted(context)
        }
    }
}
