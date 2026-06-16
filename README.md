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
    *   **Page Visibility Control**: Fully toggleable **Minus One (Widget) Page** and **App Library** via settings. Users can choose to hide these specialized pages for a cleaner, ultra-minimalist desktop.
    *   **Flexible Layouts**: Choose between 4x5, 4x6, 4x7, or "Auto (Adaptive)" based on screen aspect ratio.
    *   **Dock Styles**: Switch between **Modern (Floating)** round dock and **Classic (Full Width)** iOS-style dock that extends to the navigation bar.
    *   **Dynamic Search Pill**: Interactive "Search" capsule that elegantly transitions into page indicator dots during scrolling.

### 🎭 Icon Customization & Styling (The Ultimate System)
*   **Iteration Presets**: Instant application of **Standard**, **Black**, **White**, and **Glass** styles.
*   **Ultimate Custom Style**: High-precision control over foreground and background colors using a full **HSV + Alpha color picker** with Hex support and real-time live preview.
*   **Smart Original Hybrid**: Toggle between custom colors and original app icons/backgrounds for each layer independently.
*   **Forced Theming**: Achieve total visual consistency by applying styles to all apps, bypassing traditional monochrome layer limitations.
*   **Custom Exclusions**: Manually select specific apps to bypass styling and maintain their original colorful look.
*   **Icon Pack Support**: Full compatibility with third-party icon packs from the Play Store.
*   **Individual Icon Edit**: Change specific app icons (with built-in cropping) and labels directly from the desktop.

### 📂 Advanced Folder Management
*   **Context-Aware Menus**: Full context menu support (Long-press) inside folders even in non-edit mode. Users can Remove, Edit, or Uninstall apps directly from the folder overlay.
*   **Intelligent "Manage Apps"**: The folder management interface now **pre-checks all currently contained apps**. This allows for intuitive batch addition or removal with real-time synchronization and automatic duplicate prevention.
*   **Dynamic Resizing**: iOS-inspired folder animations that expand and collapse directly from the icon's desktop coordinates.

### 🖖 Advanced Gesture System
*   **Intelligent Priority**: Smart gesture handling that prioritizes widget scrolling (like Stack or Calendar) over home screen gestures.
*   **Search Lock**: Horizontal paging is automatically disabled when the App Library search is active or focused, preventing accidental page swaps during typing.
*   **Rich Gesture Set**:
    *   **Single Finger**: Double Tap, Swipe Up, Swipe Down, Long Press.
    *   **Multi-Finger**: Two-finger Swipe Up, Two-finger Swipe Down.
*   **Versatile Actions**:
    *   **System Controls**: Lock Screen (via Accessibility), Open Notifications, Open System Settings.
    *   **Launcher Actions**: Global Search, Desktop Menu, Launcher Settings (Mapped to Two-finger Swipe Up by default).

### 🧩 Widgets & Extensions (Minus One Page)
*   **Interactive Stack Stacker**: A vertical container for cycling through multiple widgets with simple swipes.
*   **Wide Calendar Widget**: Detailed event list view with prioritized scrolling.
*   **Real-time Music Widgets**: Live metadata and integrated controls for all active media sessions (Spotify, YouTube, etc.).
*   **Smart Monitoring**: Minimalist Battery, Clock, and Standard Calendar monitoring widgets.

### 🔍 Discovery & Organization
*   **Responsive App Library**: Background-processed filtering and categorization for zero-lag interaction.
*   **Privacy & Security**: Secure sensitive applications with **Hide Apps** and a custom **Password Gate**.
    *   **Advanced App Filtering**: Quickly manage hidden applications using live filters for **All**, **Hidden**, or **Visible** apps within the security settings.

---

## 🛠️ Technical Showcase

### 🏗️ Modern Decoupled Architecture
The project has undergone a massive structural refactoring to ensure high maintainability and developer productivity. The core logic is now distributed across specialized modules:
*   **Component-Based UI**: UI logic is strictly separated into atomic files (`Dock.kt`, `AppGrid.kt`, `FolderOverlay.kt`, `AppLibraryPage.kt`, etc.).
*   **State-Driven Overlays**: All overlays (Folders, Search, Menus) are managed via robust `StateFlow` orchestration, ensuring smooth transition animations and crash-free state handovers (e.g., during "Fade-out" cycles).
*   **Action Proxy Pattern**: System-level interactions are encapsulated in `LauncherActions.kt`, separating user intent from execution logic.

### 🚀 High-Performance Rendering
*   **Collision-Free Gestures**: Custom-built `awaitPointerEventScope` logic with event consumption checks to ensure widgets and the Pager coexist perfectly.
*   **V10 Icon Cache Engine**: Robust multi-level caching (LruCache + Disk) that forces regeneration only when style parameters, custom colors, or exclusions change.
*   **Intelligent Compatibility**: Built-in API Level Guard for **Liquid Glass** (API 31+) with Material 3 warning dialogs and fallback rendering for older devices.

---

## 📦 Project Structure (Modular)

```text
app/src/main/java/com/liferlighdow/iteration/
├── MainActivity.kt           # Pure entry point & Window configuration
├── LauncherScreen.kt        # Core Page Orchestrator & Navigation state
├── AppGrid.kt               # Desktop grid layout & Drag-and-drop engine
├── Dock.kt                  # Bottom bar variants & Search Pill logic
├── FolderOverlay.kt         # Immersive folder UI & context menu logic
├── GlobalSearchOverlay.kt   # High-performance search with math support
├── LauncherActions.kt       # System interaction & Gesture execution
├── LauncherMenu.kt          # Desktop long-press BottomSheet logic
├── LauncherDialogs.kt       # Pickers, App Editors, and confirmation dialogs
├── MainViewModel.kt          # Core state orchestration & Persistent Settings
├── MainViewModelLayout.kt    # Folder synchronization & layout serialization
├── LiquidGlass.kt            # Backdrop-powered glassmorphism engine
├── IconProcessor.kt          # Optimized icon masking & M3 tinting
├── ConfigSerializer.kt       # JSON Serialization for Backup/Restore
└── NotificationService.kt    # Media metadata & real-time badge counts
```

---

## 📜 License
Licensed under the **MIT License**. Keep it light, keep it fast. Stay in control.

---

**Developed with ❤️ by Lifer_Lighdow**
