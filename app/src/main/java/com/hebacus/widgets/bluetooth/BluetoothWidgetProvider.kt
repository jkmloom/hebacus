package com.hebacus.widgets.bluetooth

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.hebacus.widgets.MainActivity
import com.hebacus.widgets.R
import com.hebacus.widgets.data.WidgetPreferences

class BluetoothWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        com.hebacus.widgets.service.WidgetUpdateService.start(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        com.hebacus.widgets.service.WidgetUpdateService.start(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_BT ||
            intent.action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED ||
            intent.action == AudioManager.ACTION_HEADSET_PLUG) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BluetoothWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_BT = "com.hebacus.widgets.ACTION_UPDATE_BT"

        fun updateAll(context: Context) {
            val intent = Intent(context, BluetoothWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_BT
            }
            context.sendBroadcast(intent)
        }

        @SuppressLint("MissingPermission")
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_bluetooth)

            var isWired = false
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    for (dev in devices) {
                        if (dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            dev.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                            isWired = true
                            break
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    isWired = audioManager.isWiredHeadsetOn
                }
            }

            var isConnected = isWired
            var isWiredConnection = isWired
            var deviceName = if (isWired) "Wired Headphones" else "No device"

            if (!isWired) {
                try {
                    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    @Suppress("DEPRECATION")
                    val btAdapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

                    if (btAdapter != null && btAdapter.isEnabled) {
                        val a2dp = btAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                        val headset = btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                        if (a2dp || headset) {
                            isConnected = true
                            isWiredConnection = false
                            deviceName = "Bluetooth Audio"

                            // Try to get paired device name if possible
                            val bonded = btAdapter.bondedDevices
                            for (dev in bonded) {
                                val name = dev.name
                                if (!name.isNullOrBlank()) {
                                    deviceName = name
                                    break
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    // Permission not granted yet
                }
            }

            // Apply overrides only if mock mode enabled
            if (WidgetPreferences.isMockEnabled(context)) {
                isConnected = WidgetPreferences.isBtConnected(context, isConnected)
                isWiredConnection = WidgetPreferences.isBtWired(context, isWiredConnection)
                deviceName = WidgetPreferences.getBtDeviceName(context, deviceName)
            }

            // Update Text and Icon
            if (isConnected) {
                views.setTextViewText(R.id.tv_bt_name, deviceName)
                views.setTextViewText(
                    R.id.tv_bt_status,
                    if (isWiredConnection) "Connected over the wire" else "Connected over Bluetooth"
                )
                views.setImageViewResource(
                    R.id.iv_bt_icon,
                    if (isWiredConnection) R.drawable.ic_wired_connected else R.drawable.ic_bt_connected
                )
            } else {
                views.setTextViewText(R.id.tv_bt_name, "No device")
                views.setTextViewText(R.id.tv_bt_status, "Not connected")
                views.setImageViewResource(R.id.iv_bt_icon, R.drawable.ic_bt_connected)
            }

            // Render bottom two-node thread bitmap
            val threadBitmap = createBluetoothThreadBitmap(context, isConnected, isWiredConnection)
            views.setImageViewBitmap(R.id.iv_bt_thread, threadBitmap)

            // Click to open MainActivity
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 1, intent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun createBluetoothThreadBitmap(context: Context, isConnected: Boolean, isWired: Boolean): Bitmap {
            val density = context.resources.displayMetrics.density
            val width = (220 * density).toInt().coerceAtLeast(300)
            val height = (14 * density).toInt().coerceAtLeast(24)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val centerY = height / 2f
            val nodeRadius = (3.5f * density).coerceAtLeast(6f)
            val trackHeight = (2f * density).coerceAtLeast(3f)

            // 1. Draw horizontal track line
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.widget_track)
                style = Paint.Style.FILL
            }
            val leftX = nodeRadius + (8 * density)
            val rightX = width - nodeRadius - (8 * density)
            val trackRect = RectF(leftX, centerY - trackHeight / 2f, rightX, centerY + trackHeight / 2f)
            canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackPaint)

            // 2. Node & Pulse color
            val activeColor = when {
                !isConnected -> ContextCompat.getColor(context, R.color.ink_faint)
                isWired -> ContextCompat.getColor(context, R.color.accent_teal)
                else -> ContextCompat.getColor(context, R.color.accent_blue)
            }

            val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = activeColor
                style = Paint.Style.FILL
            }

            // Draw Node A (Left) and Node B (Right)
            canvas.drawCircle(leftX, centerY, nodeRadius, nodePaint)
            canvas.drawCircle(rightX, centerY, nodeRadius, nodePaint)

            // If connected, draw traveling pulse in the middle
            if (isConnected) {
                val midX = (leftX + rightX) / 2f
                val pulseRadius = nodeRadius * 0.9f
                canvas.drawCircle(midX, centerY, pulseRadius, nodePaint)
            }

            return bitmap
        }
    }
}
