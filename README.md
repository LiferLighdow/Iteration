# <img src="app/src/main/ic_launcher-playstore.png" width="48" valign="middle"> ⚡ Iteration Launcher

> **iOS-inspired interactions, Android-native elegance, Material 3 (API 37) refined.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material_3-757575?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-17_Ready-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)

**Iteration** is a high-performance, minimalist Android launcher built from the ground up using **Jetpack Compose**. It bridges the gap between fluid, intuitive UI patterns and the robust flexibility of the Android ecosystem. Optimized for the latest **Android 15 (API 35)** through **Android 17 (API 37)**.

---

## ✨ Key Features

### 🎨 Design & Interaction
*   **Immersive Edge-to-Edge**: Full transparency support that treats your wallpaper as the primary canvas.
*   **Liquid Glass Engine (Enhanced)**: High-performance real-time glassmorphism using the **Backdrop** library.
    *   **Applied Everywhere**: Dock, Home Folders, App Library Categories, Search Bars, and Widgets.
    *   **Customizable Effects**: Adjustable blur radius, physical refraction height, refraction amount, and chromatic aberration.
*   **Modern Wallpaper Management**: Unified selection system with direct system sync and privacy-first local caching.
*   **Icon Customization & Styling**:
    *   **Iteration Styles**: Standard, Black, White, and Glass presets with adaptive Material You blending.
    *   **Third-Party Icon Packs**: Support for Nova, ADW, and Go Launcher formats.
    *   **Individual Icon Edit**: Change specific app icons using your own images with a built-in cropping tool.
    *   **Custom Labels**: Rename any app on your home screen or library.
*   **Fluid Pagination & Jiggle Mode**: Smooth physics-based drag-and-drop with animated icon transitions.

### 🖖 Advanced Gesture System
*   **Rich Gesture Set**:
    *   **Single Finger**: Double Tap, Swipe Up, Swipe Down, Long Press.
    *   **Multi-Finger**: Two-finger Swipe Up, Two-finger Swipe Down.
*   **Versatile Actions**:
    *   **System Controls**: Lock Screen (via Accessibility), Open Notifications, Open System Settings.
    *   **Launcher Actions**: Global Search, Desktop Menu, Launcher Settings.
    *   **App Launching**: Map any gesture to launch a specific application.
*   **Accessibility Integration**: Optimized Accessibility Service for seamless "Double-tap to Lock" and notification management.

### 🧩 Widgets & Extensions (Minus One Page)
*   **Interactive Stack Stacker**: A vertical container for cycling through multiple widgets with simple swipes.
*   **Real-time Music Widgets**: Live metadata and integrated controls for all active media sessions (Spotify, YouTube, etc.).
*   **Smart Productivity**: Minimalist Battery, Clock, and Calendar (4x2) monitoring widgets.
*   **Interactive Photo Widgets**: Precision cropping and placement for personal images.

### 🔍 Discovery & Organization
*   **Responsive App Library**:
    *   **Reactive Filtering**: Search and categorization handled in the background for zero-lag interaction.
    *   **Folder Management**: Add, delete, rename, and reorder app library categories.
    *   **App Reassignment**: Manually assign apps to custom or default categories.
*   **Privacy & Security**:
    *   **Hide Apps**: Hide sensitive applications from the library and home screen.
    *   **Password Protection**: Secure your hidden apps list with a custom password gate.

---

## 🛠️ Technical Showcase

### High-Performance Rendering
*   **Advanced Gesture Engine**: Custom-built `awaitPointerEventScope` logic that distinguishes between single-finger and multi-finger vertical drags with pixel-perfect precision.
*   **Reactive UI Streams**: Leveraging `combine` and `StateFlow` to ensure heavy computations (like search or wallpaper blurring) never block the UI thread.
*   **Object Reuse**: Optimized `IconProcessor` using `ThreadLocal` storage to minimize GC pressure during real-time icon styling.

### Reliability & Maintenance
*   **Config Backup/Restore**: Full layout and settings serialization to JSON, allowing users to export/import their perfect setup.
*   **Modern Architecture**: Decoupled ViewModel design separating layout logic, configuration, and image processing for maximum stability.
*   **Android 15+ Optimized**: Full support for Enhanced Confirmation Mode and new Accessibility Tool declarations.

---

## 📦 Project Structure

```text
app/src/main/java/com/liferlighdow/iteration/
├── MainActivity.kt           # UI Entry, Gesture Engine, & Desktop Layout
├── MainViewModel.kt          # Core state orchestration & Persistent Settings
├── MainViewModelLayout.kt    # Folder logic and drag-and-drop orchestration
├── SettingsActivity.kt       # Multi-page settings hub (Gestures, Theme, Library)
├── IterationAccessibilityService.kt # Global actions (Lock screen, Notifications)
├── LiquidGlass.kt            # Backdrop-powered real-time glassmorphism engine
├── IconProcessor.kt          # Optimized icon masking & M3 tinting
├── ConfigSerializer.kt       # JSON Serialization for Backup/Restore
└── NotificationService.kt    # Media metadata & real-time badge counts
```

---

## 🔑 Permissions
*   **Accessibility Service**: Powering gestures like Lock Screen and Notifications.
*   **Query All Packages**: Listing and launching installed apps.
*   **Notification Listener**: Real-time notification badges and music metadata.
*   **Media Access**: Required for custom icons, photo widgets, and wallpaper selection.

---

## 📜 License
Licensed under the **MIT License**.

---

**Developed with ❤️ by Lifer_Lighdow**
