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
*   **Custom Category Reordering**: Fully control the layout of your App Library by sorting categories using simple Up/Down controls in Settings.
*   **Extended Search Integration**: Directly jump to **Web (Google)**, **Google Maps**, or **App Stores** (supporting F-Droid, Aurora Store, etc.).
*   **Enhanced Navigation**: Integrated search in App Library with intelligent focus management and `BackHandler` support.

### 🔒 Privacy & Personalization
*   **Secure Vault**: Hide sensitive applications behind a password-protected layer with an optional **Password Visibility Toggle** for ease of use.
*   **Instant Settings Sync**: All changes to icons, labels, and visibility apply **immediately** without requiring a launcher restart.
*   **Deep Customization**: 
    *   **Pro Icon Re-skinning**: High-resolution (512px) icon cropping with visual masking and precision `Matrix` transformations.
    *   **Alias Management**: Rename applications to fit your personal workflow.
*   **Orientation Locking**: Optimized for a stable, fixed **Portrait** experience.

---

## 🛠️ Technical Showcase

### High-Performance Rendering
To maintain a consistent **60/120 FPS** on modern displays:
*   **Adaptive Icon Synchronization**: Automatically scales foreground and background layers synchronously (1.35x default) to prevent "parallax breaking".
*   **Optimized Recomposition**: Leverages stable `keys` and `remember` blocks in Lazy lists to minimize overhead.
*   **Asynchronous Processing**: Offloads heavy bitmap decoding and package querying to `Dispatchers.IO` using Kotlin Coroutines.
*   **Hardware Acceleration**: Utilizes Compose's hardware-accelerated drawing for all complex UI components.

### Architecture (MVVM)
- **ViewModel + StateFlow**: Reactive UI updates driven by a single source of truth.
- **Repository Pattern**: Clean separation between Package Manager interactions and UI logic.
- **Data Persistence**: Efficient storage using `SharedPreferences` for flags and internal binary storage for custom assets.

---

## 📦 Project Structure

```text
app/src/main/java/com/liferlighdow/iteration/
├── MainActivity.kt        # Main Entry, Desktop, Library & Folder UI
├── MainViewModel.kt       # Core business logic & State management
├── AppRepository.kt      # App list & Metadata fetching
├── AppModel.kt           # Data definitions for Apps & Folders
└── SettingsActivity.kt    # Launcher configuration & Customization
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug | 2024.2.1 or newer.
- **JDK**: Java 17+ (Required for Gradle 8.x+).
- **Android Device**: API 23 (Android 6.0) or higher.

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
- **Vault**: Default access via App Library hidden section.

---

## 📜 License
Licensed under the **MIT License**.

---

**Developed with ❤️ by Lifer_Lighdow**
