package com.urg.edge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DisasterNotificationListener : NotificationListenerService() {

    companion object {
        private val EMERGENCY_PACKAGES = setOf(
            "com.android.cellbroadcastreceiver",
            "com.google.android.cellbroadcastreceiver",
            "jp.co.nttdocomo.areamail",
            "jp.softbank.mb.areamail",
        )
        private val DISASTER_KEYWORDS = listOf(
            "緊急地震速報", "大津波警報", "津波警報", "特別警報", "弾道ミサイル"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg !in EMERGENCY_PACKAGES) return

        val extras = sbn.notification.extras
        val title  = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text   = extras.getString(Notification.EXTRA_TEXT)  ?: ""

        if (DISASTER_KEYWORDS.any { it in title || it in text }) {
            Log.d("DisasterListener", "Detected: $title")
            CoroutineScope(Dispatchers.IO).launch {
                DisasterModeManager(applicationContext).setDisasterMode(true)
            }
        }
    }
}