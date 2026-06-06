# <img src="app/src/main/ic_launcher-playstore.png" width="48" valign="middle"> ⚡ Iteration Launcher

> **iOS-inspired interactions, Android-native elegance, Material 3 refined.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material_3-757575?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)

**Iteration** is a high-performance, minimalist Android launcher built from the ground up using **Jetpack Compose**. It bridges the gap between fluid, intuitive UI patterns and the robust flexibility of the Android ecosystem. Designed for power users who value speed, aesthetics, and privacy.

---

## ✨ Key Features

### 🎨 Design & Interaction
*   **Immersive Edge-to-Edge**: Full transparency support that treats your wallpaper as the primary canvas.
*   **Themed Icons (Material You)**: Standard-compliant dynamic icon tinting. Supports Android 13+ **Monochrome layers** and provides high-quality fallback for older versions.
*   **Notification Badges**: Real-time unread counts displayed as elegant badges on app icons (Requires Notification Access).
*   **Fluid Pagination**: Smooth `HorizontalPager` transitions with a physics-based drag-and-drop system.
*   **Dynamic Preview Pages**: Drag apps to the edge in Edit Mode to intuitively "slide out" and create new desktop pages.
*   **Refined Motion**: UI elements like the Dock feature **Spring-physics** animations and smooth alpha transitions for a premium tactile feel.
*   **Paged Folders (3x3 Grid)**: Organized folders with horizontal paging and a refined, transparent UI.
*   **Jiggle Mode**: Long-press to enter edit mode with "jiggling" icons, optimized for reordering and smart folder creation.
*   **Visual Polish**: System-wide icon corner radius refined to **105% (0.238f)** for a softer, more modern aesthetic.
*   **Material You Integration**: Automatic UI color adaptation using official Android 12+ Dynamic Color APIs.

### 🧩 Widgets & Extensions (Minus One Page)
*   **Dynamic Theme Extraction**: Automatically extracts the dominant color from your wallpaper using **QuantizerCelebi** and **Score** algorithms to generate a harmonious theme.
*   **Smart Battery Widget**: Real-time battery level monitoring with a circular progress indicator and charging status.
*   **Analog Clock Widget**: A minimalist, high-performance clock with synchronized second-hand animations and dynamic M3 coloring.
*   **Grid-Sized Calendar**: A 2x2 widget showcasing the current date, month, and day of the week at a glance.
*   **Interactive Photo Widgets**: 
    *   Supports **2x2 (Standard)** and **4x2 (Wide)** formats.
    *   **Built-in Cropping Tool**: Precision gesture-based (pinch-to-zoom, drag) image cropping with a visual guide to ensure a perfect fit for your desktop.

### 🔍 Discovery & Organization
*   **Usage-based Suggestions**: A dedicated section at the top of the App Library that intelligently learns and displays your **4 most frequent apps**.
*   **Global Search Hub**: A unified search interface (swipe-down) indexing apps, web results, and system shortcuts.
*   **Smart Categorization**: Automatic grouping of applications using Android's native metadata.
*   **Custom Category Management**: Create, delete, and reorder categories to fit your workflow.
*   **App Re-categorization**: Manually assign any app to your custom categories or restore it to its system default.
*   **Extended Search Integration**: Directly jump to **Web (Google)**, **Google Maps**, or **App Stores** (supporting F-Droid, Aurora Store, etc.).
*   **Enhanced Navigation**: Integrated search in App Library with intelligent focus management and `BackHandler` support.

### 🔒 Privacy & Personalization
*   **Secure Vault**: Hide sensitive applications behind a password-protected layer with an optional **Password Visibility Toggle**.
*   **Deep Customization**: 
    *   **Pro Icon Re-skinning**: High-resolution (512px) icon cropping with visual masking and precision `Matrix` transformations.
    *   **Alias Management**: Rename applications to fit your personal aesthetic.
*   **Backup & Restore**: Export your entire launcher configuration (layout, custom icons, labels, categories) to a single JSON file and restore it instantly on any device.
*   **Orientation Locking**: Optimized for a stable, fixed **Portrait** experience.

---

## 🛠️ Technical Showcase

### High-Performance Rendering
To maintain a consistent **60/120 FPS** on modern displays:
*   **Multi-Layer Icon Processing**: Decouples icon fetching, processing (tinting/masking), and caching.
*   **Two-Level Caching**: 
    *   **Memory (LruCache)**: Instant access to recently used `ImageBitmaps`.
    *   **Disk (Internal Storage)**: Persistent cache for processed icons to avoid re-computation on reboot.
*   **Optimized Recomposition**: Leverages stable `keys` in Lazy lists and `remembered` lambdas to minimize UI overhead.
*   **Asynchronous Processing**: Uses `CoroutineScope` with `Dispatchers.Default` and `awaitAll()` for massive parallel package processing.

### Architecture (MVVM)
- **ViewModel + StateFlow**: Reactive UI updates driven by a single source of truth.
- **Repository Pattern**: Clean separation between Package Manager interactions and UI logic.
- **Data Persistence**: 
    *   `SharedPreferences`: Lightweight settings and layout serialization.
    *   `Internal Files`: Binary storage for custom cropped icons and widget photos.
- **JSON Serialization**: Custom logic to convert complex folder/widget structures into portable JSON formats.

---

## 📦 Project Structure

```text
app/src/main/java/com/liferlighdow/iteration/
├── MainActivity.kt        # Entry point, Desktop (Pager), Dock, & App Library
├── MainViewModel.kt       # State machine, layout logic, and data orchestration
├── AppRepository.kt      # System package querying and metadata extraction
├── AppModel.kt           # Domain models for Apps, Folders, and Widgets
├── IconProcessor.kt      # Complex bitmap manipulation & M3 tinting logic
├── SettingsActivity.kt    # Preferences, Customization UI, & Backup Tools
└── NotificationService.kt # Background listener for real-time app badges
```

---

## 🔑 Permissions
To provide a full launcher experience, **Iteration** requires the following permissions:
- **Query All Packages**: Necessary to list and launch installed applications.
- **Notification Listener**: Required to display unread badges on app icons.
- **Read External Storage**: Required for selecting custom icons and photo widget images.
- **Read Calendar**: Required for the Calendar widget to display upcoming events (Optional).

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug | 2024.2.1 or newer.
- **JDK**: Java 17+ (Required for Gradle 8.x+).
- **Android Device**: API 26 (Android 8.0) or higher (API 31+ recommended for full Material You support).

### Installation & Build
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/LiferLighdow/Iteration.git
    ```
2.  **Compile via Gradle**:
    ```bash
    ./gradlew assembleDebug
    ```
3.  **Run/Install**: Open in Android Studio and run the `app` module, or use `./gradlew installDebug`.
4.  **Set as Default**: **Settings > Apps > Default Apps > Home app > Iteration**.

---

## 📖 Usage Tips
- **Swipe Down**: Open Global Search from anywhere on the home screen.
- **Swipe Left (Last Page)**: Access the Smart App Library.
- **Long Press App**: 
    - *Normal Mode*: Quick Actions (Uninstall / Remove from Home).
    - *Edit Mode*: Drag to reorder or drop onto another app to create a folder.
- **Folder Management**: 
    - Tap the folder title while it's open to **Rename**.
    - Swipe horizontally inside a folder to navigate between pages.
- **Backup**: Always keep a copy of your exported JSON in a safe place when switching devices.

---

## 📜 License
Licensed under the **MIT License**.

---

**Developed with ❤️ by Lifer_Lighdow**
