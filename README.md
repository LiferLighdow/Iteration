# <img src="app/src/main/ic_launcher-playstore.png" width="48" valign="middle"> ⚡ Iteration Launcher

> **iOS-inspired interactions, Android-native elegance, Material 3 (API 37) refined.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material_3-757575?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-17_Ready-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)

**Iteration** is a high-performance, minimalist Android launcher built from the ground up using **Jetpack Compose**. It bridges the gap between fluid, intuitive UI patterns and the robust flexibility of the Android ecosystem. Optimized for the latest **Android 17 (API 37)** and **AGP 9.2.1** standards.

---

## ✨ Key Features

### 🎨 Design & Interaction
*   **Immersive Edge-to-Edge**: Full transparency support that treats your wallpaper as the primary canvas.
*   **Liquid Glass Engine (Enhanced)**: High-performance real-time glassmorphism using the **Backdrop** library.
    *   **Physical Refraction (Lens)**: Real-time optical displacement that "bends" the background.
    *   **Dynamic Vibrancy**: Adaptive saturation and contrast for a crystal-clear liquid aesthetic.
    *   **Optimized Sampling**: Intelligent wallpaper processing for maximum smoothness.
    *   **3D Bevel Border**: Multi-layered stroke simulating environment light reflection and shadow.
*   **Modern Wallpaper Management**: Unified selection system.
    *   **Long-Press Selection**: Set your launcher wallpaper directly from the home screen.
    *   **Direct Sync**: Changes in Iteration automatically update your system wallpaper.
    *   **Privacy First**: Fully compliant with Android 13+ privacy restrictions (No system-level wallpaper reading needed).
*   **Icon Pack Support**: Comprehensive support for Nova, ADW, and Go Launcher formats.
    *   **High-Speed Parsing**: Optimized XML mapping for near-instant icon loading.
    *   **Smart Fallback**: Seamlessly blends icon packs with Iteration's native styles.
*   **Icon Styles**: 4 distinct presets with Material You (M3) integration:
    *   **Standard / Black / White / Glass**: Premium frosting effects with adaptive M3 color blending.
*   **Themed Icons**: Standard-compliant dynamic icon tinting with high-quality fallback for older Android versions.
*   **Fluid Pagination**: Smooth transitions using `HorizontalPager` with physics-based drag-and-drop.
*   **Jiggle Mode**: Intuitively edit your layout with animated jiggling icons.
*   **Refined Motion**: UI elements like the Dock feature **Spring-physics** animations.

### 🧩 Widgets & Extensions (Minus One Page)
*   **Interactive Stack Stacker**: A vertical container for cycling through multiple widgets with simple swipes.
*   **Real-time Music Widgets**: Live metadata and integrated controls for all active media sessions (Spotify, YouTube, etc.).
*   **Smart Battery & Clock**: Minimalist monitoring widgets with dynamic M3 coloring.
*   **Detailed Calendar (4x2)**: Detailed list of upcoming system events.
*   **Interactive Photo Widgets**: Precision cropping tool with built-in `Matrix` transformations.

### 🔍 Discovery & Organization
*   **Responsive App Library (Optimized)**:
    *   **ViewModel Filtering**: Search, filtering, and sorting are now handled reactively in the background for zero-lag UI.
    *   **Smart Categorization**: Automatic grouping based on system metadata.
    *   **Custom Management**: Create, rename, and reorder categories to fit your workflow.
*   **Global Search Hub**: Unified interface (swipe-down) for local apps and web results.
*   **Usage-based Suggestions**: Learns and displays your 4 most frequent apps.

---

## 🛠️ Technical Showcase

### High-Performance Rendering & Memory
*   **Reactive UI Filtering**: Leveraging `combine` and `StateFlow` to move heavy computation out of the Composition loop, drastically reducing frame drops during search.
*   **Object Reuse (IconProcessor)**: Uses `ThreadLocal` for `Paint`, `Path`, and `Matrix` arrays to eliminate memory churn and GC pressure during batch icon processing.
*   **Advanced Caching**: Triple-layer caching strategy (LruCache, Disk, and Metadata) with color-aware invalidation.
*   **ProGuard/R8 Optimized**: Custom rules ensuring a crash-free **Release APK** experience.

### Modern Architecture (Refactored)
*   **Modular ViewModel**: De-bloated `MainViewModel` through strategic logic separation:
    *   `ConfigSerializer.kt`: Dedicated JSON serialization and Backup/Restore logic.
    *   `MainViewModelLayout.kt`: Extension-based layout and drag-and-drop orchestration.
    *   `WallpaperProcessor.kt`: High-efficiency bitmap manipulation and sampling.
*   **User-Level Compliance**: Converted from a "System App" requirement to a standard "User App," ensuring full compatibility with Google Play.
*   **Android 17 & AGP 9.2.1 Ready**: Utilizing the latest Gradle features and Kotlin 2.4.0 standards.

---

## 📦 Project Structure

```text
app/src/main/java/com/liferlighdow/iteration/
├── MainActivity.kt           # UI Entry, Desktop, Dock, & Reactive App Library
├── MainViewModel.kt          # Core state orchestration & responsive streams
├── MainViewModelLayout.kt    # Desktop layout, folders, and drag-and-drop logic
├── WallpaperProcessor.kt     # High-speed wallpaper sampling & Liquid Glass source
├── ConfigSerializer.kt       # JSON Serialization for Layouts & Backup/Restore
├── LiquidGlass.kt            # Backdrop-powered real-time glassmorphism engine
├── IconProcessor.kt          # Optimized icon masking & M3 tinting (Object Reuse)
├── AppRepository.kt         # Efficient system package querying
├── IconPackManager.kt       # Third-party icon pack parsing
└── NotificationService.kt    # Media sessions & real-time notification badges
```

---

## 🔑 Permissions & Requirements
*   **Standard User Permission**: Does not require `READ_WALLPAPER_INTERNAL`.
*   **Query All Packages**: List and launch apps.
*   **Notification Listener**: Real-time badges and music widget data.
*   **Set Wallpaper**: Unified sync between Iteration and Android System.
*   **Read Media Images**: Required for custom icons and photo widgets.

---

## 📜 License
Licensed under the **MIT License**.

---

**Developed with ❤️ by Lifer_Lighdow**
