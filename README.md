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
*   **Fluid Pagination**: Smooth `HorizontalPager` transitions with a physics-based drag-and-drop system.
*   **Paged Folders**: Organized **3x3 grid folders** with horizontal paging, allowing you to manage large collections without clutter.
*   **Dynamic Dock**: A floating, customizable utility bar for your mission-critical applications.
*   **Jiggle Mode**: Long-press to enter edit mode with intuitive "jiggling" icons, making reordering and folder creation a breeze.
*   **Quick Actions**: Long-press any app in normal mode to access quick menus for **Uninstalling** or **Removing from Home Screen**.

### 🔍 Discovery & Organization
*   **Global Search Hub**: A unified search interface (swipe-down) indexing apps, web results, and system shortcuts.
*   **Extended Search Integration**: Directly jump to **Web**, **Google Maps**, or **App Stores** (supporting F-Droid, Aurora, etc.) from the search bar.
*   **Smart Categorization**: Automatic grouping of applications using Android's native metadata in the App Library.
*   **Enhanced App Library**: Integrated search with improved navigation and focus management.

### 🔒 Privacy & Personalization
*   **Secure Vault**: Hide sensitive applications behind a password-protected layer in the App Library.
*   **Material You Integration**: Respects system-wide theming and dynamic color palettes.
*   **Pro Icon Re-skinning**: High-resolution icon cropping with visual masking and precision `Matrix` transformations.
*   **Alias Management**: Rename applications to fit your personal workflow.

---

## 🛠️ Technical Showcase

### High-Performance Rendering
*   **Adaptive Icon Synchronization**: Automatically scales foreground and background layers synchronously to prevent "parallax breaking".
*   **Optimized Recomposition**: Leverages stable `keys` and `remember` blocks to minimize overhead during complex animations.
*   **Asynchronous Processing**: Offloads heavy bitmap decoding and package querying to background threads using Kotlin Coroutines.

### Architecture (MVVM)
- **ViewModel + StateFlow**: Reactive UI updates driven by a single source of truth.
- **Repository Pattern**: Clean separation between Package Manager interactions and UI logic.
- **Data Persistence**: Efficient storage using `SharedPreferences` and internal binary storage for custom assets.

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
- **JDK**: Java 17+.
- **Android Device**: API 26 (Android 8.0) or higher.

### Installation
1.  Clone the repository: `git clone https://github.com/LiferLighdow/Iteration.git`
2.  Open in Android Studio and Sync Gradle.
3.  Run the `app` module on your device.
4.  Set as Default: **Settings > Apps > Default Apps > Home app > Iteration**.

---

## 📖 Usage Tips
- **Swipe Down**: Open Global Search from anywhere on the home screen.
- **Swipe Left (Last Page)**: Access the Smart App Library.
- **Long Press App**: 
    - *Normal Mode*: Quick Actions (Delete/Uninstall).
    - *Edit Mode*: Drag to reorder or drop onto another app to create a folder.
- **Folder Rename**: Tap the folder title while it's open to rename.

---

## 📜 License
Licensed under the **MIT License**.

---

**Developed with ❤️ by LiferLighdow**
