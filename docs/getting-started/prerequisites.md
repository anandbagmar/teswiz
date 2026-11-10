[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---

# 🛠️ System Prerequisites & Setup Guide

Ensure your development environment meets the required dependencies before running **teswiz** test suites.

---

## 📋 Environment Requirements Matrix

| Requirement | Minimum Version | Required Setup & Environment Variables | Installation Link |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | JDK 17+ | Set `JAVA_HOME` pointing to JDK 17 installation directory | [Eclipse Adoptium (JDK 17)](https://adoptium.net/) |
| **Node.js & npm** | Node.js 20.19+, 22.12+ or 24+, with npm 10+ (Node.js 22.12+ or 24+ for `WEB_ENGINE=playwright-ts`, whose worker runs with `--experimental-strip-types`) | Required for Playwright TS worker, Appium 3, and CLI dependencies | [Node.js Official Download](https://nodejs.org) |
| **Android SDK** | Android SDK 33+ | Set `ANDROID_HOME` pointing to Android SDK path | [Android Studio / Command-Line Tools](https://developer.android.com/studio#command-tools) |
| **Appium 3** | Appium 3.x | Installed automatically via project `package.json` | Running `npm install` |

> [!NOTE]
> The Node.js minimum comes from Appium 3 and its drivers, which require `^20.19.0 || ^22.12.0 || >=24.0.0` and npm 10+, and from Playwright, which requires Node.js 20+. Node.js 18 is not supported. On the 22.x line, 22.12+ is needed (Appium), which also covers the 22.6+ the `playwright-ts` worker needs.

---

## 🚀 ⚡ Quick Setup Instructions

### 1. Install Node.js Dependencies & Appium 3

Run `npm install` in the project root to install Appium 3, required Appium drivers, and Playwright dependencies:

```bash
npm install
```

> [!NOTE]
> `npm install` installs Appium 3 and the required drivers into `./node_modules`. The drivers stay on the releases that support Appium 3 (`appium-uiautomator2-driver` 8.x, `appium-xcuitest-driver` 12.x); their newer majors require Appium 4.

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

> 📖 **Reference**: [Appium Capability Guidelines](https://appium.io/docs/en/latest/guides/caps/)
