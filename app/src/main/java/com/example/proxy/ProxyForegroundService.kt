package com.example.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R

class ProxyForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ProxyEngineManager.clearLogs()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_DISCONNECT -> {
                ProxyEngineManager.stopRelay(applicationContext)
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
                val config = ProxyEngineManager.getActiveConfig()
                val port = ProxyEngineManager.startRelayInService(config)
                val notification = buildNotification(config, port)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                return START_STICKY
            }
        }
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.proxy_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.proxy_service_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(config: ProxyConfig, port: Int): Notification {
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(this, 0, launchIntent, pendingFlags)

        val disconnectIntent = Intent(this, ProxyForegroundService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(this, 1, disconnectIntent, pendingFlags)

        val detail = when (config.mode) {
            ProxyMode.LOCAL_RELAY -> "127.0.0.1:$port • Privacy Relay"
            ProxyMode.UPSTREAM_HTTP -> "127.0.0.1:$port → HTTP ${config.upstreamHost}:${config.upstreamPort}"
            ProxyMode.UPSTREAM_SOCKS5 -> "127.0.0.1:$port → SOCKS5 ${config.upstreamHost}:${config.upstreamPort}"
            ProxyMode.DIRECT -> "Direct mode"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(getString(R.string.proxy_notification_title))
            .setContentText(detail)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launchPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "relayweb_proxy_channel"
        const val NOTIFICATION_ID = 4102
        const val ACTION_START = "com.example.proxy.ACTION_START"
        const val ACTION_STOP = "com.example.proxy.ACTION_STOP"
        const val ACTION_DISCONNECT = "com.example.proxy.ACTION_DISCONNECT"
    }
}
