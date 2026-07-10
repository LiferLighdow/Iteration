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

**Iteration** is a high-performance, minimalist Android launcher built from the ground up using **Jetpack Compose**. It bridges the gap between fluid, intuitive UI patterns and the robust flexibility of the Android ecosystem. Minimum <strong>Android 7 (API 24)</strong> supported. Optimized for the latest **Android 12 (API 31)** through **Android 17 (API 37)**.

---

## ✨ Key Features

### 🎨 Design & Interaction
*   **Immersive Edge-to-Edge**: Full transparency support that treats your wallpaper as the primary canvas.
*   **Bouncy Paging (Fluid Navigation)**: Custom spring physics engine (`DampingRatioLowBouncy`) applied to horizontal paging for an organic, responsive feel.
*   **Edit Mode (Jiggle Mode)**: iOS-style desktop organization with iconic jiggle animations.
    *   **Unified Quick Removal**: One-tap removal badges that intelligently distinguish between home screen removal and system uninstallation based on context.
*   **Liquid Glass Engine (Enhanced)**: High-performance real-time glassmorphism using the **Backdrop** library.
    *   **Applied Everywhere**: Dock, Home Folders, App Library Categories, Search Bars, and Widgets.
    *   **Customizable Effects**: Adjustable blur radius (0-50), physical refraction height, refraction amount, and chromatic aberration.
*   **Desktop Customization**:
    *   **Page Visibility Control**: Fully toggleable **Minus One (Widget) Page** and **App Library** via settings. Users can choose to hide these specialized pages for a cleaner desktop.
    *   **Flexible Layouts**: Choose between 4x5, 4x6, 4x7, or "Auto (Adaptive)" based on screen aspect ratio.
    *   **Dock Styles**: Switch between **Modern (Floating)** round dock, **Classic (Full Width)** iOS-style dock, and the nostalgic **Platform (3D Glass)** style.
    *   **3D Platform Dock (iOS 6 Inspired)**: A high-fidelity reconstruction of the classic 3D glass platform.
        *   **Trapezoidal Perspective**: Custom geometric shape providing a deep sense of desktop depth.
        *   **Dynamic Icon Reflections**: Real-time rendering of app icon reflections that perfectly match the user's selected icon shape (Round, Squircle, etc.).
        *   **Glass Highlight**: Subtle top-edge highlighting to simulate the thickness of a physical glass pane.
    *   **Gesture-Responsive Dock**: The Dock and Search Pill feature linear follow-through animations. They dynamically slide and fade based on the precise scroll progress.

### 🔍 Global Search & Intelligence (Powerhouse)
*   **Universal Discovery**: Search for installed **Apps** and **Local Contacts** in a single unified interface.
    *   **Direct Interaction**: Tap a contact to view details or use the quick-dial icon to call instantly.
*   **Advanced Scientific Calculator**: A built-in, lightning-fast recursive descent parser.
    *   **Rich Operations**: Supports `+`, `-`, `*`, `/`, `^` (Power), `%` (Modulo/Percentage).
    *   **Scientific Functions**: `sqrt`, `abs`, `sin`, `cos`, `tan`, `cot`, `sec`, `csc`, `log`, `ln`.
    *   **Constants & Symbols**: Full support for `pi`, `e`, and the `π` symbol.
    *   **Implicit Multiplication**: Intuitively handles `2π`, `2(3+4)`, and `sin(30)cos(30)`.
    *   **Degree-Based Trig**: Optimized for daily practical use (e.g., `sin(30)` = 0.5).
*   **Offline-Ready Unit Converter**: Instant conversion for **Length**, **Weight**, and **Temperature**—works with or without internet.
*   **Smart Equation Solver**: Detects equations (e.g., `x^2 - 4 = 0`) and provides a direct link to **WolframAlpha** for detailed steps and graphs.
*   **Smart Clipboard Integration**: Automatically detects recent clipboard content when the search bar is empty for one-tap filling.
*   **Real-time Language Translator**:
    *   **Natural Command Support**: Use `[text] to [lang]` (e.g., `Hello to ja`) for precise translations.
    *   **Smart System Default**: Type `tr [text]` to instantly translate into your device's current system language.
    *   **Lightning Fast**: Powered by high-performance asynchronous requests with intelligent debouncing.
*   **Customizable Search Engines**: Switch between Google, Bing, DuckDuckGo, Baidu, WolframAlpha, Perplexity, and more.

### 🌤️ Weather & Location (Privacy-First)
*   **Adaptive Weather Widget**: A native 4x2 widget providing real-time conditions and a 5-day forecast.
*   **Privacy-Friendly Geolocation**:
    *   **IP-Based Discovery**: Automatically determines your general location via IP address—**No GPS permissions required**.
    *   **Manual Search**: Integrated **Geocoding API** allows you to search and set any city worldwide.
*   **Dual Data Sources**: Choose between **MET Norway** (Authority) and **Open-Meteo** (Open Source) directly from the widget menu.
*   **Intelligent Caching**: Automatically saves the last known weather data for offline viewing.
*   **Network Guard**: Fully integrated with system-level network toggles; respects your data and privacy settings.

### 📚 Knowledge & Support
*   **In-App User Manual**: A dedicated documentation hub built directly into the settings.
    *   **Discoverable Power**: Step-by-step guides for Global Search, hidden gestures, and advanced tools.
    *   **Modular Design**: Expanding library of "manual books" to help you master every corner of the launcher.

### 🌍 Globalization & Localization (V3.0 Major Update)
*   **Complete Resource Migration**: 100% of UI strings, Toasts, menus, and descriptions have been migrated from hardcoded code to `strings.xml`.
*   **Multi-Language Support**:
    *   **English**: Default global language.
    *   **Traditional Chinese (繁體中文)**: Fully translated and native-ready.
*   **Dedicated Language Settings**: A new, independent configuration page to switch between **System Default**, **English**, and **Traditional Chinese**.
*   **Dynamic Locale Switching**: Real-time interface updates using `AppCompatDelegate` and `LocaleListCompat` without requiring a full device restart.

### 🎭 Industrial-Grade App & Profile Management
*   **Full Multi-User & Work Profile Support**: Native compatibility with **Work Profiles**, **Parallel Spaces** (Xiaomi Dual Apps, Samsung Secure Folder), and multiple guest profiles.
*   **Special App Icon Clones (Multi-Entry)**: Advanced support for apps with multiple entry points (`activity-alias`). Handles independent icons and labels for sub-apps within a single package (e.g., `vUtils`, `vCalc`, `vPulse`).
*   **Zero-Loss App Updates**: Smart persistent ID mechanism ensures app updates don't trigger "New App" desktop placement logic or lose custom label/visibility metadata.
*   **Auto-Sorted Folders**: Desktop folders are automatically maintained in alphabetical order (A-Z) for consistent organization.
*   **Icon Customization (The Ultimate System)**:
    *   **Iteration Presets**: Instant application of **Standard**, **Black**, **White**, and **Glass** styles.
    *   **Ultimate Custom Style**: High-precision control using a full **HSV + Alpha color picker**.
    *   **Smart Original Hybrid**: Toggle between custom colors and original app icons/backgrounds independently.
    *   **IPS (Icon Pack Studio) Advanced Parsing**: Custom XML & binary stream parser for full compatibility with modern dynamic icon packs.

### 📂 Advanced Folder & Library
*   **Context-Aware Menus**: Full shortcut and management support inside folders even in non-edit mode.
*   **Intelligent "Manage Apps"**: Folder management pre-checks contained apps for intuitive batch addition/removal with automatic duplicate prevention.
*   **Optimized App Library**:
    *   **2x2 Suggestions**: Intelligent recommendation grid (max 4 icons) for a cleaner, unified look.
    *   **Fast Categorization**: Zero-lag filtering and background-processed group management.
*   **Privacy & Security**: **Hide Apps** with a secure **Password Gate**.

### 🖖 Gesture System & Permissions
*   **Intelligent Priority**: Smart gesture handling that prioritizes widget scrolling over home screen gestures.
*   **Search Lock**: Horizontal paging is automatically disabled when the App Library search is focused.
*   **Centralized Permission Center**: A dedicated "Permissions" page for **Contacts**, **Notifications**, and **Accessibility**.
*   **Local Privacy Control**: Fully configurable network access; your personal data processing remains on-device by default.

---

## 🛠️ Technical Showcase

### 🏗️ Modern Decoupled Architecture
*   **Component-Based UI**: UI logic is strictly separated into atomic files (`Dock.kt`, `AppGrid.kt`, `FolderOverlay.kt`, etc.).
*   **State-Driven Overlays**: All overlays are managed via robust `StateFlow` orchestration for smooth transitions and crash-free state handovers.
*   **Action Proxy Pattern**: System-level interactions are encapsulated in `LauncherActions.kt`, separating user intent from execution logic.
*   **Milestone 3.0 Stability**: Major bug fixes including folder creation logic, delete confirmation reliability, and Settings navigation state management.

### 🚀 High-Performance Rendering
*   **Linear Animation Engine**: Real-time interpolation of UI elements based on Pager scroll fractions, replacing binary visibility states with fluid transitions.
*   **Collision-Free Gestures**: Custom-built `awaitPointerEventScope` logic ensuring widgets and the Pager coexist perfectly.
*   **Enhanced V13 Icon Engine**: High-fidelity caching system (LruCache + Disk) utilizing a `packageName/activity@userId` composite key. Features advanced "timestamp-stripping" to synchronize icons across the App Library and Desktop.
*   **Intelligent Self-Healing Cache**: Automatically detects app updates and configuration changes to regenerate outdated cache files using latest rendering algorithms.
*   **Full Configuration Backup**: JSON-based serialization including Desktop Layout, Liquid Glass parameters, Gesture mappings, and search engine choices.

---

## 📦 Project Structure (Modular)

```text
app/src/main/java/com/liferlighdow/iteration/
│
├── data/                 # Data & Configuration Layer
│   ├── AppModel.kt       # Unified Multi-User Model
│   ├── AppRepository.kt  # Multi-Entry Activity Resolver
│   ├── ConfigSerializer.kt # Version-Agnostic Coder
│   ├── ContactModel.kt
│   ├── CurrencyRepository.kt
│   ├── LauncherConfig.kt # Data Classes for Backup
│   ├── WeatherRepository.kt
│   └── WidgetModel.kt
│
├── ui/                   # UI Components & Screens
│   ├── settings/         # Modular Settings Pages
│   │   ├── SettingsAppScreens.kt      # Hide, Rename, Library Settings
│   │   ├── SettingsCommon.kt          # Shared UI Components (Item, Group)
│   │   ├── SettingsInteractionScreens.kt # Desktop & Gestures
│   │   ├── SettingsLiquidGlassScreen.kt # Real-time Glass Workshop
│   │   ├── SettingsMainScreen.kt      # Unified Search & Navigation
│   │   ├── SettingsOtherScreens.kt    # Permissions, Language, Search
│   │   └── SettingsStyleScreens.kt    # Icon Theme, Pack, & Styles
│   ├── widgets/          # Individual Widget Components
│   │   ├── BatteryWidget.kt
│   │   ├── CalendarWidget.kt
│   │   ├── ClockWidget.kt
│   │   ├── MusicWidget.kt
│   │   ├── NoteWidget.kt
│   │   ├── PhotoWidget.kt
│   │   ├── StackWidget.kt
│   │   └── WeatherWidget.kt
│   ├── dialogs/          # Specialized Dialogs
│   │   └── WidgetDialogs.kt  # Stack Picker, Photo Crop, Location Search, etc.
│   ├── AppGrid.kt        # Optimized Slot-Based Layout
│   ├── AppItem.kt
│   ├── AppLibraryPage.kt
│   ├── Dock.kt           # Linear Animation Container
│   ├── FolderOverlay.kt
│   ├── GlobalSearchOverlay.kt
│   ├── LauncherBottomBar.kt
│   ├── LauncherDialogs.kt
│   ├── LauncherScreen.kt # Core Pager Engine
│   ├── LiquidGlass.kt    # Real-time Refraction
│   ├── ManualScreens.kt  # Documentation System
│   └── MinusOnePage.kt
│
├── viewmodel/            # Architecture & State Management
│   ├── MainViewModel.kt          # Core State & Initialization
│   ├── MainViewModelApps.kt      # Icon & App Logic Extension
│   ├── MainViewModelConfig.kt    # Backup/Import Extension
│   ├── MainViewModelExternal.kt  # Weather/Currency/Contacts Extension
│   ├── MainViewModelLayout.kt    # Desktop & Folder Logic Extension
│   ├── MainViewModelSettings.kt  # User Preferences Setter Extension
│   ├── MainViewModelWallpaper.kt # Blur & Wallpaper Logic Extension
│   └── MainViewModelWidgets.kt   # Stacker & Widget Logic Extension
│
├── service/              # System Services & Receivers
│   ├── IterationAccessibilityService.kt
│   └── NotificationService.kt
│
├── utils/                # Utility Classes (Icons, Wallpaper, Actions)
│   ├── ActionMode.kt
│   ├── GestureAction.kt
│   ├── IconPackManager.kt # Advanced XML & IPS Parser
│   ├── IconProcessor.kt   # PorterDuff-Masked Renderer
│   ├── IconShape.kt
│   ├── IconStyle.kt
│   ├── LauncherActions.kt
│   └── WallpaperProcessor.kt
│
├── MainActivity.kt      
└── SettingsActivity.kt
```

---

## 📜 License
Licensed under the **MIT License**. Keep it light, keep it fast. Stay in control.

---

**Developed with ❤️ by Lifer_Lighdow**
