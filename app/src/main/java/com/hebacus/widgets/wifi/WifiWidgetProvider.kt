package com.hebacus.widgets.wifi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.hebacus.widgets.MainActivity
import com.hebacus.widgets.R
import com.hebacus.widgets.data.WidgetPreferences

class WifiWidgetProvider : AppWidgetProvider() {

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
        if (intent.action == ACTION_UPDATE_WIFI ||
            intent.action == ConnectivityManager.CONNECTIVITY_ACTION ||
            intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION ||
            intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WifiWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIFI = "com.hebacus.widgets.ACTION_UPDATE_WIFI"

        fun updateAll(context: Context) {
            val intent = Intent(context, WifiWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIFI
            }
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_wifi)

            var mode = "wifi"
            var ssid = "Network"
            var strength = 3

            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            var isWifiConnected = false

            if (connManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = connManager.activeNetwork
                    val caps = connManager.getNetworkCapabilities(activeNetwork)
                    isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                } else {
                    @Suppress("DEPRECATION")
                    val netInfo = connManager.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    isWifiConnected = netInfo?.isConnected == true && netInfo.type == ConnectivityManager.TYPE_WIFI
                }
            }

            if (isWifiConnected) {
                mode = "wifi"
                try {
                    val info = wifiManager?.connectionInfo
                    val rawSsid = info?.ssid?.replace("\"", "")
                    if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                        ssid = rawSsid
                    } else {
                        ssid = "Wi-Fi Connected"
                    }
                    val rssi = info?.rssi ?: -65
                    strength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiManager != null) {
                        (wifiManager.calculateSignalLevel(rssi) + 1).coerceIn(1, 4)
                    } else {
                        @Suppress("DEPRECATION")
                        (WifiManager.calculateSignalLevel(rssi, 4) + 1).coerceIn(1, 4)
                    }
                } catch (e: Exception) {
                    ssid = "Wi-Fi Connected"
                    strength = 3
                }
            } else {
                val isWifiEnabled = wifiManager?.isWifiEnabled ?: false
                mode = if (isWifiEnabled) "wifi" else "off"
                ssid = if (isWifiEnabled) "Not connected" else "Network"
                strength = 0
            }

            // Apply overrides only if mock mode enabled
            if (WidgetPreferences.isMockEnabled(context)) {
                mode = WidgetPreferences.getWifiMode(context, mode)
                ssid = WidgetPreferences.getWifiSsid(context, ssid)
                strength = WidgetPreferences.getWifiStrength(context, strength)
            }

            // Update text labels
            views.setTextViewText(R.id.tv_wifi_ssid, ssid)
            val statusText = when (mode) {
                "hotspot" -> "Broadcasting"
                "off" -> "Wi-Fi off"
                else -> {
                    if (!isWifiConnected && !WidgetPreferences.isMockEnabled(context)) {
                        "Not connected"
                    } else {
                        when (strength) {
                            1 -> "Weak signal"
                            2 -> "Fair signal"
                            3 -> "Good signal"
                            4 -> "Strong signal"
                            else -> "Connected"
                        }
                    }
                }
            }
            views.setTextViewText(R.id.tv_wifi_status, statusText)

            // Render indicator bitmap
            val indicatorBitmap = createWifiIndicatorBitmap(context, mode, strength)
            views.setImageViewBitmap(R.id.iv_wifi_indicator, indicatorBitmap)

            // Click to open MainActivity
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 2, intent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun createWifiIndicatorBitmap(context: Context, mode: String, strength: Int): Bitmap {
            val density = context.resources.displayMetrics.density
            val width = (38 * density).toInt().coerceAtLeast(60)
            val height = (24 * density).toInt().coerceAtLeast(36)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            if (mode == "hotspot") {
                // Hotspot radiating rings
                val centerX = width / 2f
                val centerY = height / 2f
                val coralColor = ContextCompat.getColor(context, R.color.accent_coral)

                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = coralColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(centerX, centerY, 3f * density, dotPaint)

                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = coralColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.8f * density
                }
                canvas.drawCircle(centerX, centerY, 7f * density, ringPaint)
                canvas.drawCircle(centerX, centerY, 11f * density, ringPaint)
            } else {
                // Rising 4-bars
                val barCount = 4
                val barWidth = 4f * density
                val spacing = 3.5f * density
                val totalBarsWidth = (barCount * barWidth) + ((barCount - 1) * spacing)
                var startX = width - totalBarsWidth - (2 * density)

                val heightsPct = floatArrayOf(0.30f, 0.55f, 0.78f, 1.0f)
                val activeColor = ContextCompat.getColor(context, R.color.accent_violet)
                val trackColor = ContextCompat.getColor(context, R.color.widget_track)

                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

                for (i in 0 until barCount) {
                    val isActive = mode == "wifi" && (i + 1) <= strength
                    barPaint.color = if (isActive) activeColor else trackColor

                    val barH = (height * 0.9f) * heightsPct[i]
                    val top = height - barH
                    val rect = RectF(startX, top, startX + barWidth, height.toFloat())
                    canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint)

                    startX += barWidth + spacing
                }
            }

            return bitmap
        }
    }
}
