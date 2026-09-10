package com.hebacus.widgets.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import com.hebacus.widgets.R
import com.hebacus.widgets.battery.BatteryWidgetProvider
import com.hebacus.widgets.bluetooth.BluetoothWidgetProvider
import com.hebacus.widgets.databinding.WidgetBatteryBinding
import com.hebacus.widgets.databinding.WidgetBluetoothBinding
import com.hebacus.widgets.databinding.WidgetWifiBinding
import com.hebacus.widgets.wifi.WifiWidgetProvider

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val widgetType = intent?.getStringExtra(EXTRA_WIDGET_TYPE) ?: "battery"
        showFloatingWidget(widgetType)
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun showFloatingWidget(widgetType: String) {
        removeFloatingWidget()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_widget, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 180
        }

        val contentContainer = floatingView!!.findViewById<FrameLayout>(R.id.floating_content)
        val closeBtn = floatingView!!.findViewById<ImageButton>(R.id.btn_close_floating)

        // Inflate selected widget inside the floating card
        when (widgetType) {
            "bluetooth" -> {
                val btBinding = WidgetBluetoothBinding.inflate(inflater, contentContainer, true)
                btBinding.tvBtName.text = "Pixel Buds Pro"
                btBinding.tvBtStatus.text = "Connected over Bluetooth"
                btBinding.ivBtIcon.setImageResource(R.drawable.ic_bt_connected)
                btBinding.ivBtThread.setImageBitmap(
                    BluetoothWidgetProvider.createBluetoothThreadBitmap(this, true, false)
                )
            }
            "wifi" -> {
                val wifiBinding = WidgetWifiBinding.inflate(inflater, contentContainer, true)
                wifiBinding.tvWifiSsid.text = "Home Fibre 5G"
                wifiBinding.tvWifiStatus.text = "Good signal"
                wifiBinding.ivWifiIndicator.setImageBitmap(
                    WifiWidgetProvider.createWifiIndicatorBitmap(this, "wifi", 3)
                )
            }
            else -> { // battery
                val batteryBinding = WidgetBatteryBinding.inflate(inflater, contentContainer, true)
                batteryBinding.tvBatteryPct.text = "82"
                batteryBinding.tvBatteryCaption.text = "On battery"
                batteryBinding.ivBolt.visibility = View.GONE
                batteryBinding.ivBatteryFill.setImageBitmap(
                    BatteryWidgetProvider.createBatteryMeterBitmap(this, 82, false)
                )
            }
        }

        // Close button listener
        closeBtn.setOnClickListener {
            stopSelf()
        }

        // Drag and drop touch movement anywhere on screen
        val card = floatingView!!.findViewById<View>(R.id.floating_card)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        card.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(floatingView, params)
    }

    private fun removeFloatingWidget() {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // Ignored
            }
            floatingView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWidget()
    }

    companion object {
        const val EXTRA_WIDGET_TYPE = "extra_widget_type"

        fun start(context: Context, widgetType: String) {
            val intent = Intent(context, FloatingWidgetService::class.java).apply {
                putExtra(EXTRA_WIDGET_TYPE, widgetType)
            }
            context.startService(intent)
        }
    }
}
