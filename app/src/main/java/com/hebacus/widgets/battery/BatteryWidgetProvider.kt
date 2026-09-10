package com.hebacus.widgets.battery

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.hebacus.widgets.MainActivity
import com.hebacus.widgets.R
import com.hebacus.widgets.data.WidgetPreferences
import com.hebacus.widgets.service.WidgetUpdateService

class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateService.start(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateService.start(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BATTERY_CHANGED ||
            intent.action == Intent.ACTION_POWER_CONNECTED ||
            intent.action == Intent.ACTION_POWER_DISCONNECTED ||
            intent.action == ACTION_UPDATE_BATTERY) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_BATTERY = "com.hebacus.widgets.ACTION_UPDATE_BATTERY"

        fun updateAll(context: Context) {
            val intent = Intent(context, BatteryWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_BATTERY
            }
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_battery)

            // Read live battery state
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            var pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 82

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            var isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    plugged > 0

            // Apply overrides only if mock mode enabled
            if (WidgetPreferences.isMockEnabled(context)) {
                pct = WidgetPreferences.getBatteryPct(context, pct)
                isCharging = WidgetPreferences.isBatteryCharging(context, isCharging)
            }

            // Set percentage numeral
            views.setTextViewText(R.id.tv_battery_pct, "$pct")

            // Visual charging & caption state
            if (isCharging) {
                views.setTextViewText(R.id.tv_battery_caption, "Charging")
                views.setTextColor(R.id.tv_battery_caption, ContextCompat.getColor(context, R.color.accent_amber))
                views.setViewVisibility(R.id.iv_bolt, View.VISIBLE)
            } else {
                val caption = if (pct <= 20) "Low battery" else "On battery"
                views.setTextViewText(R.id.tv_battery_caption, caption)
                val captionColor = if (pct <= 20) R.color.accent_red else R.color.ink_soft
                views.setTextColor(R.id.tv_battery_caption, ContextCompat.getColor(context, captionColor))
                views.setViewVisibility(R.id.iv_bolt, View.GONE)
            }

            // Vertical battery meter bitmap
            val meterBitmap = createBatteryMeterBitmap(context, pct, isCharging)
            views.setImageViewBitmap(R.id.iv_battery_fill, meterBitmap)

            // Click to open MainActivity
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun createBatteryMeterBitmap(context: Context, pct: Int, isCharging: Boolean): Bitmap {
            val density = context.resources.displayMetrics.density
            val width = (6 * density).toInt().coerceAtLeast(12)
            val height = (140 * density).toInt().coerceAtLeast(200)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.widget_track)
                style = Paint.Style.FILL
            }

            val cornerRadius = width / 2f
            val trackRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

            // Fill color
            val fillColor = when {
                isCharging -> ContextCompat.getColor(context, R.color.accent_amber)
                pct <= 20 -> ContextCompat.getColor(context, R.color.accent_red)
                else -> ContextCompat.getColor(context, R.color.accent_green)
            }

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }

            val fillHeight = (height * (pct.coerceIn(0, 100) / 100f))
            val fillTop = height - fillHeight
            val fillRect = RectF(0f, fillTop, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)

            return bitmap
        }
    }
}
