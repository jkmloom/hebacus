package com.hebacus.widgets.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.hebacus.widgets.MainActivity
import com.hebacus.widgets.R
import com.hebacus.widgets.battery.BatteryWidgetProvider
import com.hebacus.widgets.bluetooth.BluetoothWidgetProvider
import com.hebacus.widgets.wifi.WifiWidgetProvider

class WidgetUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val periodicRunnable = object : Runnable {
        override fun run() {
            updateAllWidgets()
            handler.postDelayed(this, 30000L) // Refresh every 30 seconds
        }
    }

    private val dynamicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> {
                    BatteryWidgetProvider.updateAll(context)
                }
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                AudioManager.ACTION_HEADSET_PLUG -> {
                    BluetoothWidgetProvider.updateAll(context)
                }
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    updateAllWidgets()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        registerDynamicReceivers()
        registerNetworkCallback()
        handler.post(periodicRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateAllWidgets()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "hebacus_live_service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hebacus Live Widget Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your home screen widgets updated in real time"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle("Hebacus Live Monitor")
            .setContentText("Keeping home screen widgets updated live")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun registerDynamicReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(dynamicReceiver, filter)
    }

    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    WifiWidgetProvider.updateAll(this@WidgetUpdateService)
                }

                override fun onLost(network: Network) {
                    WifiWidgetProvider.updateAll(this@WidgetUpdateService)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    WifiWidgetProvider.updateAll(this@WidgetUpdateService)
                }
            }
            try {
                connectivityManager?.registerNetworkCallback(request, networkCallback!!)
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    private fun updateAllWidgets() {
        BatteryWidgetProvider.updateAll(this)
        BluetoothWidgetProvider.updateAll(this)
        WifiWidgetProvider.updateAll(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(periodicRunnable)
        try {
            unregisterReceiver(dynamicReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
