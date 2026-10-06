[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---

# 🎬 Getting Started with teswiz

Follow this step-by-step guide to integrate **teswiz** into a new or existing Java-Gradle test automation project.

---

## 🛠️ Step 1: Install Prerequisites & Setup Project

1. Verify environment setup against [Prerequisites Guide](prerequisites.md) (JDK 17+, Node.js 18+).
2. Create a new Java-Gradle project in your IDE (e.g., IntelliJ IDEA).
3. Copy `build.gradle.sample` to your project root, rename it to `build.gradle`, and verify the `teswiz` dependency version:
   ```groovy
   dependencies {
       implementation 'com.github.anandbagmar:teswiz:1.0.41'
   }
   ```
4. Run `npm install` in your project root to install Node dependencies for Appium 2 and Playwright TS.

---

## 🎛️ Step 2: Configure Environment & Capabilities

### 📱 For Android Automation
Find your application package and activity name using `aapt`:
```bash
aapt dump badging temp/sampleApps/TheApp.apk | grep package
aapt dump badging temp/sampleApps/TheApp.apk | grep launchable-activity
```

### 🌐 For Web Automation
Add base URL keys in `environments.json` and reference them in `config.properties`:
```json
{
  "prod": {
    "THEAPP_BASE_URL": "https://the-internet.herokuapp.com"
  }
}
```
Set in `config.properties`:
```properties
BASE_URL_FOR_WEB=THEAPP_BASE_URL
WEB_ENGINE=selenium
```

---

## ✍️ Step 3: Implement Test Layers

Follow the mandatory Cucumber BDD pattern: **Feature -> Step -> Business Layer (BL) -> Screen Contract**:

1. **Feature File**: Define Gherkin scenarios under `src/test/resources/com/znsio/teswiz/features/*.feature`.
2. **Step Definitions**: Create step definitions under `src/test/java/com/znsio/teswiz/steps/*Steps.java`.
3. **Business Layer (BL)**: Implement business flows under `src/test/java/com/znsio/teswiz/businessLayer/*/*BL.java`.
4. **Screen Contracts & Concrete Screens**: Implement abstract screen contracts and platform concrete screens (`*ScreenWeb.java`, `*ScreenAndroid.java`).

---

## 🚀 Step 4: Run Tests

Execute your test suite via `./gradlew run`:

```bash
CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web ./gradlew run
```

> 📖 **Read more**: [Configuring Test Execution](configuring-test-execution.md) • [Writing Your First Test](writing-first-test.md) • [Sample Tests](sample-tests.md)
