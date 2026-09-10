package com.hebacus.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.hebacus.widgets.battery.BatteryWidgetProvider
import com.hebacus.widgets.bluetooth.BluetoothWidgetProvider
import com.hebacus.widgets.data.WidgetPreferences
import com.hebacus.widgets.databinding.ActivityMainBinding
import com.hebacus.widgets.service.FloatingWidgetService
import com.hebacus.widgets.wifi.WifiWidgetProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
        syncAllLiveData()
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!WidgetPreferences.isMockEnabled(context)) {
                syncAllLiveData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupThemeToggle()
        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        setupModeSwitch()
        setupPermissions()
        setupBatterySection()
        setupBluetoothSection()
        setupWifiSection()
        setupActionButtons()

        com.hebacus.widgets.service.WidgetUpdateService.start(this)
        syncAllLiveData()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        registerReceiver(systemReceiver, filter)
        checkPermissions()
        syncAllLiveData()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(systemReceiver)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun setupThemeToggle() {
        binding.btnThemeToggle.setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun setupModeSwitch() {
        val isMock = WidgetPreferences.isMockEnabled(this)
        binding.switchLiveMode.isChecked = !isMock
        updateModeUI(!isMock)

        binding.switchLiveMode.setOnCheckedChangeListener { _, isLiveChecked ->
            WidgetPreferences.setMockEnabled(this, !isLiveChecked)
            updateModeUI(isLiveChecked)
            if (isLiveChecked) {
                syncAllLiveData()
            } else {
                updateAllFromControls()
            }
            BatteryWidgetProvider.updateAll(this)
            BluetoothWidgetProvider.updateAll(this)
            WifiWidgetProvider.updateAll(this)
        }
    }

    private fun updateModeUI(isLive: Boolean) {
        binding.tvModeDesc.text = if (isLive) {
            "Widgets show real battery, Wi-Fi, and Bluetooth state"
        } else {
            "Custom Simulator active. Widgets use your custom sliders & text below"
        }
        binding.layoutBatteryControls.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.layoutBtControls.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.layoutWifiControls.visibility = if (isLive) View.GONE else View.VISIBLE
    }

    private fun setupPermissions() {
        binding.btnGrantPermissions.setOnClickListener {
            val permissionsToRequest = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun checkPermissions() {
        var missing = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missing = true
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing = true
        }
        binding.btnGrantPermissions.visibility = if (missing && binding.switchLiveMode.isChecked) View.VISIBLE else View.GONE
    }

    private fun syncAllLiveData() {
        if (!WidgetPreferences.isMockEnabled(this)) {
            syncRealBattery()
            syncRealBluetooth()
            syncRealWifi()
        }
    }

    // ----------------- Battery -----------------
    private fun setupBatterySection() {
        binding.sliderBattery.addOnChangeListener { _, value, fromUser ->
            if (fromUser && WidgetPreferences.isMockEnabled(this)) {
                WidgetPreferences.setMockBatteryPct(this, value.toInt())
                updateBatteryPreview(value.toInt(), binding.switchBatteryCharging.isChecked)
                BatteryWidgetProvider.updateAll(this)
            }
        }

        binding.switchBatteryCharging.setOnCheckedChangeListener { _, isChecked ->
            if (WidgetPreferences.isMockEnabled(this)) {
                WidgetPreferences.setMockBatteryCharging(this, isChecked)
                updateBatteryPreview(binding.sliderBattery.value.toInt(), isChecked)
                BatteryWidgetProvider.updateAll(this)
            }
        }
    }

    private fun syncRealBattery() {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 82

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        binding.sliderBattery.value = pct.toFloat().coerceIn(0f, 100f)
        binding.switchBatteryCharging.isChecked = isCharging
        updateBatteryPreview(pct, isCharging)
    }

    private fun updateBatteryPreview(pct: Int, isCharging: Boolean) {
        binding.tvCtrlBatteryVal.text = "Charge Level: $pct%"
        val p = binding.previewBattery
        p.tvBatteryPct.text = "$pct"
        p.tvBatteryCaption.text = when {
            isCharging -> "Charging"
            pct <= 20 -> "Low battery"
            else -> "On battery"
        }
        p.ivBolt.visibility = if (isCharging) View.VISIBLE else View.GONE
        p.ivBatteryFill.setImageBitmap(
            BatteryWidgetProvider.createBatteryMeterBitmap(this, pct, isCharging)
        )
    }

    // ----------------- Bluetooth -----------------
    private fun setupBluetoothSection() {
        binding.switchBtConnected.setOnCheckedChangeListener { _, isChecked ->
            if (WidgetPreferences.isMockEnabled(this)) {
                WidgetPreferences.setMockBtConnected(this, isChecked)
                updateBtPreviewFromControls()
                BluetoothWidgetProvider.updateAll(this)
            }
        }

        binding.groupBtType.check(R.id.btn_type_bt)
        binding.groupBtType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && WidgetPreferences.isMockEnabled(this)) {
                WidgetPreferences.setMockBtWired(this, checkedId == R.id.btn_type_wired)
                updateBtPreviewFromControls()
                BluetoothWidgetProvider.updateAll(this)
            }
        }

        binding.etBtName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (WidgetPreferences.isMockEnabled(this@MainActivity)) {
                    val name = s?.toString() ?: "Pixel Buds Pro"
                    WidgetPreferences.setMockBtName(this@MainActivity, name)
                    updateBtPreviewFromControls()
                    BluetoothWidgetProvider.updateAll(this@MainActivity)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })
    }

    @SuppressLint("MissingPermission")
    private fun syncRealBluetooth() {
        var isWired = false
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
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
                val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                @Suppress("DEPRECATION")
                val btAdapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

                if (btAdapter != null && btAdapter.isEnabled) {
                    val a2dp = btAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                    val headset = btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                    if (a2dp || headset) {
                        isConnected = true
                        isWiredConnection = false
                        deviceName = "Bluetooth Audio"

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
                // Ignore missing permission
            }
        }

        renderBtPreview(isConnected, isWiredConnection, deviceName)
    }

    private fun updateBtPreviewFromControls() {
        val connected = binding.switchBtConnected.isChecked
        val isWired = binding.groupBtType.checkedButtonId == R.id.btn_type_wired
        val name = binding.etBtName.text?.toString()?.ifBlank { "No device" } ?: "No device"
        renderBtPreview(connected, isWired, name)
    }

    private fun renderBtPreview(connected: Boolean, isWired: Boolean, name: String) {
        val p = binding.previewBluetooth
        if (connected) {
            p.tvBtName.text = name
            p.tvBtStatus.text = if (isWired) "Connected over the wire" else "Connected over Bluetooth"
            p.ivBtIcon.setImageResource(if (isWired) R.drawable.ic_wired_connected else R.drawable.ic_bt_connected)
        } else {
            p.tvBtName.text = "No device"
            p.tvBtStatus.text = "Not connected"
            p.ivBtIcon.setImageResource(R.drawable.ic_bt_connected)
        }
        p.ivBtThread.setImageBitmap(
            BluetoothWidgetProvider.createBluetoothThreadBitmap(this, connected, isWired)
        )
    }

    // ----------------- Wi-Fi -----------------
    private fun setupWifiSection() {
        binding.groupWifiMode.check(R.id.btn_wifi_mode_wifi)
        binding.groupWifiMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && WidgetPreferences.isMockEnabled(this)) {
                val mode = when (checkedId) {
                    R.id.btn_wifi_mode_off -> "off"
                    R.id.btn_wifi_mode_hotspot -> "hotspot"
                    else -> "wifi"
                }
                WidgetPreferences.setMockWifiMode(this, mode)
                updateWifiPreviewFromControls()
                WifiWidgetProvider.updateAll(this)
            }
        }

        binding.etWifiSsid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (WidgetPreferences.isMockEnabled(this@MainActivity)) {
                    val ssid = s?.toString() ?: "Home Fibre 5G"
                    WidgetPreferences.setMockWifiSsid(this@MainActivity, ssid)
                    updateWifiPreviewFromControls()
                    WifiWidgetProvider.updateAll(this@MainActivity)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        binding.sliderWifi.addOnChangeListener { _, value, fromUser ->
            if (fromUser && WidgetPreferences.isMockEnabled(this)) {
                WidgetPreferences.setMockWifiStrength(this, value.toInt())
                updateWifiPreviewFromControls()
                WifiWidgetProvider.updateAll(this)
            }
        }
    }

    private fun syncRealWifi() {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

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

        var mode = "wifi"
        var ssid = "Network"
        var strength = 3

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
            val isEnabled = wifiManager?.isWifiEnabled ?: false
            mode = if (isEnabled) "wifi" else "off"
            ssid = if (isEnabled) "Not connected" else "Network"
            strength = 0
        }

        renderWifiPreview(mode, ssid, strength)
    }

    private fun updateWifiPreviewFromControls() {
        val mode = when (binding.groupWifiMode.checkedButtonId) {
            R.id.btn_wifi_mode_off -> "off"
            R.id.btn_wifi_mode_hotspot -> "hotspot"
            else -> "wifi"
        }
        val ssid = binding.etWifiSsid.text?.toString()?.ifBlank { "Network" } ?: "Network"
        val strength = binding.sliderWifi.value.toInt()
        renderWifiPreview(mode, ssid, strength)
    }

    private fun renderWifiPreview(mode: String, ssid: String, strength: Int) {
        binding.tvCtrlWifiVal.text = "Signal Strength: $strength of 4"
        val p = binding.previewWifi
        p.tvWifiSsid.text = if (mode == "hotspot") "Hotspot" else ssid
        p.tvWifiStatus.text = when (mode) {
            "hotspot" -> "Broadcasting"
            "off" -> "Wi-Fi off"
            else -> {
                if (strength == 0) "Not connected" else when (strength) {
                    1 -> "Weak signal"
                    2 -> "Fair signal"
                    3 -> "Good signal"
                    4 -> "Strong signal"
                    else -> "Connected"
                }
            }
        }
        p.ivWifiIndicator.setImageBitmap(
            WifiWidgetProvider.createWifiIndicatorBitmap(this, mode, strength)
        )
    }

    private fun updateAllFromControls() {
        updateBatteryPreview(binding.sliderBattery.value.toInt(), binding.switchBatteryCharging.isChecked)
        updateBtPreviewFromControls()
        updateWifiPreviewFromControls()
    }

    // ----------------- Pin & Floating Drag & Drop -----------------
    private fun setupActionButtons() {
        // Pin to Home Screen
        binding.btnPinBattery.setOnClickListener {
            pinWidgetWithTip(BatteryWidgetProvider::class.java, "Battery")
        }
        binding.btnPinBluetooth.setOnClickListener {
            pinWidgetWithTip(BluetoothWidgetProvider::class.java, "Bluetooth")
        }
        binding.btnPinWifi.setOnClickListener {
            pinWidgetWithTip(WifiWidgetProvider::class.java, "Wi-Fi")
        }

        // Float on Screen (Drag and drop anywhere)
        binding.btnFloatBattery.setOnClickListener {
            startFloatingWidget("battery")
        }
        binding.btnFloatBluetooth.setOnClickListener {
            startFloatingWidget("bluetooth")
        }
        binding.btnFloatWifi.setOnClickListener {
            startFloatingWidget("wifi")
        }
    }

    private fun pinWidgetWithTip(providerClass: Class<*>, widgetTitle: String) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, providerClass)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                AlertDialog.Builder(this)
                    .setTitle("Add $widgetTitle Widget")
                    .setMessage("Tap 'Add' to open the launcher placement prompt.\n\nTip: You can TOUCH AND HOLD the preview in the prompt to drag and drop it directly onto any spot or page of your home screen!")
                    .setPositiveButton("Add") { _, _ ->
                        val successCallback = PendingIntent.getBroadcast(
                            this,
                            0,
                            Intent(this, providerClass),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        appWidgetManager.requestPinAppWidget(provider, null, successCallback)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }
        }
        Toast.makeText(this, "Please drag widget from your home screen widget menu", Toast.LENGTH_LONG).show()
    }

    private fun startFloatingWidget(widgetType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Display Over Other Apps")
                .setMessage("To drag and drop this widget anywhere on your phone screen, Hebacus needs permission to display over other apps.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        FloatingWidgetService.start(this, widgetType)
        Toast.makeText(this, "Floating widget opened! Touch and drag it anywhere on your screen.", Toast.LENGTH_SHORT).show()
    }
}
