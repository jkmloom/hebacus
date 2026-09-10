# Hebacus

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-23%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![APK Size](https://img.shields.io/badge/APK%20Size-1.8%20MB-brightgreen.svg)](https://github.com/jkmloom)

**Hebacus** is a minimal, typography-forward Android widget application designed to sit quietly on your home screen and read status at a glance. Built with a clean Bauhaus/Nordic design system, it delivers battery, device connectivity, and network information with zero clutter, zero bloat, and maximum performance.

Originally created and developed by **[Jatin Kumar Mehta](https://github.com/jkmloom)**.

---

## Highlights & Features

### 1. Battery Widget
- **Dynamic Percentage Display**: Big, tabular typography indicating charge level (`0%`–`100%`).
- **Real-Time Charging Status**:
  - Vibrant **Amber (`#FFB443`)** lightning bolt icon appears the moment the phone is plugged in.
  - Caption dynamically shifts to **`Charging`** in Amber text.
  - Vertical edge meter turns Amber and pulses softly during charging.
- **Low Battery Alert**: Shifts to warning **Red (`#FF5C5C`)** when capacity drops below 20%.
- **Normal State**: Renders a calm **Emerald Green (`#2FD673`)** or quiet ink edge meter when running on battery.

### 2. Bluetooth & Wired Devices Widget
- **Live Device Detection**: Reads active Bluetooth audio peripherals (e.g. `Pixel Buds Pro`) and connected wired headphones.
- **Signature Two-Node Thread**:
  - Two circular nodes joined by an indicator thread.
  - Glowing **Blue (`#4C8DFF`)** with a traveling pulse for Bluetooth pairings.
  - Glowing **Teal (`#2FC9B0`)** for wired headphones.
  - Muted ink with status `Not connected` when no device is paired.

### 3. Wi-Fi & Hotspot Widget
- **Wi-Fi Mode**:
  - Shows connected network SSID.
  - 4 rising signal bars dynamically colored in **Violet (`#8B6BFF`)** based on signal strength (`Weak`, `Fair`, `Good`, `Strong`).
- **Hotspot Mode**:
  - Displays radiating **Coral (`#FF7A59`)** concentric circles with a center broadcast dot showing active hotspot tethering.
- **Off / Disconnected Mode**:
  - Cleanly dims signal tracks to semi-transparent ink when Wi-Fi is disabled or disconnected.

### 4. Drag & Drop Floating Widgets
- **Place Anywhere on Your Screen**: In addition to standard home screen launcher widgets, you can launch a floating overlay directly from the app.
- **Touch & Drag**: Put your finger on the widget and drag it freely to place and drop it at any exact coordinate on your phone screen—above your wallpaper or over other applications.
- Tap the dismiss button (`×`) to close the floating widget whenever needed.

### 5. Dynamic Live Background Updates
- Utilizes Android's `NetworkCallback` and a dedicated, lightweight background monitor (`WidgetUpdateService`) to guarantee that home screen widgets update dynamically without needing to open the app.
- Immediately syncs when your charger is connected/disconnected, when you lock/unlock the phone, or when you switch networks.

### 6. Companion Workbench App
- Interactive live preview workbench matching the design in `index.html`.
- Toggle between **Live System Data** and **Custom Simulator Mode** (customize text, sliders, and states for testing).
- Seamless **Light & Dark Theme** switching.
- Dedicated **About Screen** with developer information and open-source licensing.

---

## Ultra-Lightweight Architecture

Hebacus is engineered natively using **Kotlin** and Android **RemoteViews**:

| Metric | Hebacus (Native Kotlin) | Flutter-Based Widgets |
| :--- | :--- | :--- |
| **Release APK Size** | **~1.8 MB** | ~25 MB – 45 MB |
| **RAM Usage** | **~12–18 MB** | ~45–80 MB baseline |
| **Startup Speed** | Instant | 400ms – 1200ms engine boot |
| **Old Phone Support** | Smooth on 1GB RAM Android 6.0+ devices | Stutter / background kill risk |

---

## Tech Stack & Requirements

- **Language**: Kotlin 1.9.23
- **Min SDK**: Android 6.0 (API 23)
- **Target SDK**: Android 14 (API 34)
- **Build System**: Gradle 8.7 & Android Gradle Plugin 8.4.0
- **JDK Requirement**: OpenJDK 17

---

## Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/jkmloom/hebacus.git
   cd hebacus
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Build the Optimized Release APK (R8 Minified)**:
   ```bash
   ./gradlew assembleRelease
   ```
   The APK will be generated at:
   `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Installation

### Via ADB (USB or Wireless Debugging):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Direct Install:
Transfer `app-debug.apk` to your Android device via USB or file sharing, and tap the APK to install.

---

## How to Add Widgets to Your Home Screen

1. Open the **Hebacus** app.
2. Select any widget (**Battery**, **Bluetooth**, or **Wi-Fi**) and tap **"Add to Home"**.
3. In the system dialog that appears:
   - **Touch and hold** the widget preview to drag and drop it onto any exact spot or page on your home screen grid.
   - Or tap **"Add automatically"** to let your launcher place it.
4. Alternatively, tap **"Float on Screen"** to pop out a movable overlay that stays pinned wherever you drag it.

---

## Author & Credits

- **Original Developer**: [Jatin Kumar Mehta](https://github.com/jkmloom)
- **GitHub**: [@jkmloom](https://github.com/jkmloom)

---

## License

This project is open-sourced under the **MIT License**. Anyone is free to use, study, modify, distribute, or build upon this project for any purpose. See the [LICENSE](LICENSE) file for complete terms.
