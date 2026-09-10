package com.hebacus.widgets.data

import android.content.Context
import android.content.SharedPreferences

object WidgetPreferences {
    private const val PREFS_NAME = "hebacus_widget_prefs"

    // Theme: "system", "light", "dark"
    private const val KEY_WIDGET_THEME = "widget_theme"

    // Battery overrides (if mock mode enabled)
    private const val KEY_USE_MOCK = "use_mock"
    private const val KEY_MOCK_BATTERY_PCT = "mock_battery_pct"
    private const val KEY_MOCK_BATTERY_CHARGING = "mock_battery_charging"

    // Bluetooth overrides
    private const val KEY_MOCK_BT_CONNECTED = "mock_bt_connected"
    private const val KEY_MOCK_BT_WIRED = "mock_bt_wired"
    private const val KEY_MOCK_BT_NAME = "mock_bt_name"

    // Wi-Fi overrides
    private const val KEY_MOCK_WIFI_MODE = "mock_wifi_mode" // "wifi", "hotspot", "off"
    private const val KEY_MOCK_WIFI_SSID = "mock_wifi_ssid"
    private const val KEY_MOCK_WIFI_STRENGTH = "mock_wifi_strength" // 1..4
    private const val KEY_MOCK_HOTSPOT_DEVICES = "mock_hotspot_devices"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWidgetTheme(context: Context): String =
        getPrefs(context).getString(KEY_WIDGET_THEME, "system") ?: "system"

    fun setWidgetTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString(KEY_WIDGET_THEME, theme).apply()
    }

    fun isMockEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_USE_MOCK, false)

    fun setMockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_MOCK, enabled).apply()
    }

    fun getBatteryPct(context: Context, fallback: Int): Int =
        if (isMockEnabled(context)) getPrefs(context).getInt(KEY_MOCK_BATTERY_PCT, fallback) else fallback

    fun setMockBatteryPct(context: Context, pct: Int) {
        getPrefs(context).edit().putInt(KEY_MOCK_BATTERY_PCT, pct).apply()
    }

    fun isBatteryCharging(context: Context, fallback: Boolean): Boolean =
        if (isMockEnabled(context)) getPrefs(context).getBoolean(KEY_MOCK_BATTERY_CHARGING, fallback) else fallback

    fun setMockBatteryCharging(context: Context, charging: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MOCK_BATTERY_CHARGING, charging).apply()
    }

    fun getBtDeviceName(context: Context, fallback: String): String =
        if (isMockEnabled(context)) getPrefs(context).getString(KEY_MOCK_BT_NAME, fallback) ?: fallback else fallback

    fun setMockBtName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_MOCK_BT_NAME, name).apply()
    }

    fun isBtConnected(context: Context, fallback: Boolean): Boolean =
        if (isMockEnabled(context)) getPrefs(context).getBoolean(KEY_MOCK_BT_CONNECTED, fallback) else fallback

    fun setMockBtConnected(context: Context, connected: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MOCK_BT_CONNECTED, connected).apply()
    }

    fun isBtWired(context: Context, fallback: Boolean): Boolean =
        if (isMockEnabled(context)) getPrefs(context).getBoolean(KEY_MOCK_BT_WIRED, fallback) else fallback

    fun setMockBtWired(context: Context, wired: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MOCK_BT_WIRED, wired).apply()
    }

    fun getWifiMode(context: Context, fallback: String): String =
        if (isMockEnabled(context)) getPrefs(context).getString(KEY_MOCK_WIFI_MODE, fallback) ?: fallback else fallback

    fun setMockWifiMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_MOCK_WIFI_MODE, mode).apply()
    }

    fun getWifiSsid(context: Context, fallback: String): String =
        if (isMockEnabled(context)) getPrefs(context).getString(KEY_MOCK_WIFI_SSID, fallback) ?: fallback else fallback

    fun setMockWifiSsid(context: Context, ssid: String) {
        getPrefs(context).edit().putString(KEY_MOCK_WIFI_SSID, ssid).apply()
    }

    fun getWifiStrength(context: Context, fallback: Int): Int =
        if (isMockEnabled(context)) getPrefs(context).getInt(KEY_MOCK_WIFI_STRENGTH, fallback) else fallback

    fun setMockWifiStrength(context: Context, strength: Int) {
        getPrefs(context).edit().putInt(KEY_MOCK_WIFI_STRENGTH, strength).apply()
    }
}
