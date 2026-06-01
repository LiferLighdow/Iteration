# <img src="app/src/main/ic_launcher-playstore.png" width="48" valign="middle"> ⚡ Iteration Launcher

> **iOS-inspired interactions, Android-native elegance, Material 3 refined.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
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
*   **Dynamic Dock**: A floating, customizable utility bar for your mission-critical applications.
*   **Material You Integration**: Respects system-wide theming and dynamic color palettes.

### 🔍 Discovery & Organization
*   **Global Search Hub**: A unified search interface (swipe-down) indexing apps, web results, and system shortcuts.
*   **Smart Categorization**: Automatic grouping of applications (Games, Productivity, Social) using Android's native `ApplicationInfo` metadata.
*   **App Library**: A searchable, alphabetically sorted repository of every installed tool.

### 🔒 Privacy & Personalization
*   **Secure Vault**: Hide sensitive applications behind a configurable password-protected layer.
*   **Deep Customization**: 
    *   **Icon Re-skinning**: Support for custom image icons per application.
    *   **Alias Management**: Rename applications to fit your personal workflow.
    *   **Grid Control**: Adjustable page sizes and layout density.

---

## 🛠️ Technical Showcase

### High-Performance Rendering
To maintain a consistent **60/120 FPS** on modern displays, Iteration utilizes a custom pre-rendering pipeline for icons:
- **Adaptive Icon Processing**: Automatically extracts and scales foreground/background layers from `AdaptiveIconDrawable`.
- **ImageBitmap Caching**: Icons are pre-processed into `ImageBitmap` formats on a background thread to prevent UI jank during scroll events.
- **Hardware Acceleration**: Leverages Compose's hardware-accelerated drawing for all UI components.

### Architecture (MVVM)
- **ViewModel + StateFlow**: Reactive UI updates driven by a single source of truth.
- **Coroutines & Flow**: Asynchronous app loading and icon processing to keep the Main Thread free for user interactions.
- **Repository Pattern**: Clean separation between data fetching (Package Manager) and UI logic.

---

## 📦 Project Structure

```text
app/src/main/java/com/liferlighdow/iteration/
├── MainActivity.kt        # Entry point & Compose Scaffold
├── SettingsActivity.kt    # User preferences & customization
├── MainViewModel.kt       # State management & business logic
├── AppRepository.kt      # Interaction with Android Package Manager
└── AppModel.kt           # Unified data model for applications
```

---

## 🚀 Getting Started & Compilation

### Prerequisites
- **Android Studio**: Ladybug | 2024.2.1 or newer.
- **JDK**: Java 17 or higher (Required for Gradle 8.x+).
- **Android SDK**: API 34 (Target) / API 26 (Minimum).

### Build Instructions
You can build the project directly from Android Studio or via the command line.

#### 1. Clone the repository
```bash
git clone https://github.com/LiferLighdow/Iteration.git
cd Iteration
```

#### 2. Compile via Gradle
To generate a debug APK:
```bash
./gradlew assembleDebug
```
To generate a release App Bundle (AAB):
```bash
./gradlew bundleRelease
```

#### 3. Installation
Install the debug APK to your connected device:
```bash
./gradlew installDebug
```

### Setting as Default Launcher
After installation, press the Home button and select **Iteration**. Alternatively, go to:
**Settings > Apps > Default Apps > Home app** and choose **Iteration**.

---

## 📖 Usage Guide
- **Quick Search**: Swipe down anywhere on the home screen.
- **Organize**: Long-press an icon to enter "Jiggle Mode" (Move/Remove).
- **Library to Home**: Drag an app from the library and drop it onto any home screen page.
- **Hidden Apps**: Open the App Library and navigate to the Vault section. Default PIN is `1234`.

---

## 📜 License
**MIT License**. Keep it light, keep it fast. Stay in control.

---

**Developed with ❤️ by LiferLighdow**
