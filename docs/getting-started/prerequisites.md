[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---

# 🛠️ System Prerequisites & Setup Guide

Ensure your development environment meets the required dependencies before running **teswiz** test suites.

---

## 📋 Environment Requirements Matrix

| Requirement | Minimum Version | Required Setup & Environment Variables | Installation Link |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | JDK 17+ | Set `JAVA_HOME` pointing to JDK 17 installation directory | [Eclipse Adoptium (JDK 17)](https://adoptium.net/) |
| **Node.js & npm** | Node.js 18+ | Required for Playwright TS worker, Appium 2, and CLI dependencies | [Node.js Official Download](https://nodejs.org) |
| **Android SDK** | Android SDK 33+ | Set `ANDROID_HOME` pointing to Android SDK path | [Android Studio / Command-Line Tools](https://developer.android.com/studio#command-tools) |
| **Appium 2** | Appium 2.x | Installed automatically via project `package.json` | Running `npm install` |

---

## 🚀 ⚡ Quick Setup Instructions

### 1. Install Node.js Dependencies & Appium 2

Run `npm install` in the project root to install Appium 2, required Appium drivers, and Playwright dependencies:

```bash
npm install
```

> [!NOTE]
> `npm install` installs Appium 2 and the required drivers into `./node_modules`.

---

## 🧪 Verifying Installation

Verify installed Appium components by running `npx appium doctor` or inspecting `npm list`:

### 🔌 Installed Drivers & Plugins

| Category | Package Name | Purpose |
| :--- | :--- | :--- |
| **iOS Driver** | `appium-xcuitest-driver` | Automation driver for iOS devices and simulators |
| **Android Driver** | `appium-uiautomator2-driver` | Automation driver for Android devices and emulators |
| **Device Farm Plugin** | `appium-device-farm` | Multi-device farm parallel execution plugin |
| **Dashboard Plugin** | `appium-dashboard` | Real-time device execution dashboard |
| **Relaxed Caps Plugin** | `@appium/relaxed-caps-plugin` | Relaxes vendor capability prefix constraints |

### 🛠️ Diagnostics

Run Appium Doctor to check system setup:

```bash
npx appium-doctor
```

> [!TIP]
> Ensure all mandatory checks pass cleanly with no reported errors.

---

## 🎛️ Appium Capabilities Configuration

Add `appiumServerPath` to your `capabilities.json` file:

```json
{
  "appiumServerPath": "./node_modules/appium/build/lib/main.js",
  "android": {
    "appiumServerLogLevel": "info"
  }
}
```

---

## 🚀 📱 Appium Inspector Setup

To inspect element locators for Android and iOS apps, install the latest [Appium Inspector](https://github.com/appium/appium-inspector/releases).

> 📖 **Reference**: [Appium Capability Guidelines](https://appium.io/docs/en/2.0/guides/caps/)
