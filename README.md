<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="100" height="100">
</p>

<h1 align="center">Iteration Launcher</h1>
<p align="center">
  <strong>iOS-inspired interactions, Android-native elegance, Material 3 (API 37) refined.</strong><br>
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.4+-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Design-Material_3-757575?logo=materialdesign&logoColor=white" alt="Material 3">
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License">
  <img src="https://img.shields.io/badge/Android-17_Ready-3DDC84?logo=android&logoColor=white" alt="Android">
</p>

---

**Iteration** is a high-performance, minimalist Android launcher built from the ground up using **Jetpack Compose**. It bridges the gap between fluid, intuitive UI patterns and the robust flexibility of the Android ecosystem. Optimized for the latest **Android 12 (API 31)** through **Android 17 (API 37)**.

---

## ✨ Key Features

### 🎨 Design & Interaction
*   **Immersive Edge-to-Edge**: Full transparency support that treats your wallpaper as the primary canvas.
*   **Edit Mode (Jiggle Mode)**: iOS-style desktop organization with iconic jiggle animations.
    *   **Quick Uninstall**: One-tap "X" badges on the top-left of app icons for instant uninstallation while editing.
*   **Liquid Glass Engine (Enhanced)**: High-performance real-time glassmorphism using the **Backdrop** library.
    *   **Applied Everywhere**: Dock, Home Folders, App Library Categories, Search Bars, and Widgets.
    *   **Customizable Effects**: Adjustable blur radius (0-50), physical refraction height, refraction amount, and chromatic aberration.
*   **Desktop Customization**:
    *   **Flexible Layouts**: Choose between 4x5, 4x6, 4x7, or "Auto (Adaptive)" based on screen aspect ratio.
    *   **Dock Styles**: Switch between **Modern (Floating)** round dock and **Classic (Full Width)** iOS-style dock that extends to the navigation bar.
    *   **Dynamic Search Pill**: Interactive "Search" capsule that elegantly transitions into page indicator dots during scrolling.
*   **Icon Customization & Styling**:
    *   **Iteration Styles**: Standard, Black, White, Glass, and **Custom** presets.
    *   **Ultimate Custom Style**: High-precision control over foreground/background colors using a full **HSV + Alpha color picker** with Hex support and real-time live preview.
    *   **Smart Original Hybrid**: Toggle between custom colors and original app icons/backgrounds for each layer.
    *   **Forced Theming**: High visual consistency by applying styles to all apps, bypassing traditional monochrome layer limitations.
    *   **Custom Exclusions**: Manually select specific apps to bypass styling and keep their original colorful look.
    *   **Third-Party Icon Packs**: Support for standard Android icon pack formats.
    *   **Individual Icon Edit**: Change specific app icons and labels with built-in cropping.

### 🖖 Advanced Gesture System
*   **Intelligent Priority**: Smart gesture handling that prioritizes widget scrolling (like Stack or Calendar) over home screen gestures.
*   **Rich Gesture Set**:
    *   **Single Finger**: Double Tap, Swipe Up, Swipe Down, Long Press.
    *   **Multi-Finger**: Two-finger Swipe Up, Two-finger Swipe Down.
*   **Versatile Actions**:
    *   **System Controls**: Lock Screen (via Accessibility), Open Notifications, Open System Settings.
    *   **Launcher Actions**: Global Search, Desktop Menu, Launcher Settings (Mapped to Two-finger Swipe Up by default).
*   **Accessibility Integration**: Optimized Accessibility Service for seamless "Double-tap to Lock" and notification management.

### 🧩 Widgets & Extensions (Minus One Page)
*   **Interactive Stack Stacker**: A vertical container for cycling through multiple widgets with simple swipes.
*   **Wide Calendar Widget**: Detailed event list view with prioritized scrolling.
*   **Real-time Music Widgets**: Live metadata and integrated controls for all active media sessions (Spotify, YouTube, etc.).
*   **Smart Productivity**: Minimalist Battery, Clock, and Standard Calendar monitoring widgets.

### 🔍 Discovery & Organization
*   **Responsive App Library**: Background-processed filtering and categorization for zero-lag interaction.
*   **Folder Management**: Full control over home screen and library folders (Add, delete, rename, reorder).
*   **Privacy & Security**: Secure sensitive applications with **Hide Apps** and a custom **Password Gate**.

---

## 🛠️ Technical Showcase

### High-Performance Rendering
*   **Collision-Free Gestures**: Custom-built `awaitPointerEventScope` logic with event consumption checks to ensure widgets and the Pager coexist perfectly.
*   **V10 Icon Cache Engine**: Robust multi-level caching (LruCache + Disk) that forces regeneration only when style parameters, custom colors, or exclusions change, ensuring instant theme swaps.
*   **Intelligent Compatibility**: 
    *   **API Level Guard**: Automatic detection for **Liquid Glass** (API 31+ required) with Material 3 warning dialogs for unsupported devices.
    *   **Precision Matrix**: Built-in compatibility info window explaining rendering levels from "Basic" (API 23) to "Perfect" (API 33+ monochrome support).
*   **Reactive UI Streams**: Leveraging `combine` and `StateFlow` to ensure heavy computations never block the UI thread.

### Reliability & Maintenance
*   **Config Backup/Restore**: Full layout and settings serialization to JSON.
*   **Modern Architecture**: Decoupled ViewModel design separating layout logic, configuration, and image processing.
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

## 📜 License
Licensed under the **MIT License**. Keep it light, keep it fast. Stay in control.

---

**Developed with ❤️ by Lifer_Lighdow**
