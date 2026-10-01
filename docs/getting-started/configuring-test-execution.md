[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---

# 🎛️ Configuring Test Execution

**teswiz** test execution is highly configurable via configuration property files, system properties, and environment variables.

> 📖 **Reference**: See complete [Configuration Parameters Reference](../configuration/configuration-parameters.md).

---

## 🧪 Framework Selection (`FRAMEWORK`)

Choose between Cucumber BDD and plain TestNG `@Test` execution:

```properties
FRAMEWORK=cucumber
```

| Value | Description | Best Used For |
| :--- | :--- | :--- |
| `cucumber` (default) | Executes Gherkin `.feature` files via step definitions | BDD collaboration & Gherkin scenarios |
| `testng` | Executes plain TestNG `@Test` classes calling BL & Screen contracts directly | Code-first automation without Gherkin step defs |

```bash
# Execute TestNG mode with tag group filtering
CONFIG=configs/cli_local_config.properties FRAMEWORK=testng TAG=@calculator ./gradlew run
```

> [!NOTE]
> In TestNG mode, tags map to TestNG groups (e.g. `TAG=@calculator` selects tests declared with `@Test(groups = "calculator")`).
>
> 📖 **Read more**: [Cucumber to TestNG Migration Guide](../architecture-and-internals/cucumber-to-testng-migration-guide.md)

---

## 📱 Mobile Execution

### 🤖 Android Execution

Execute Android test suites locally or on cloud device farms:

```bash
PLATFORM=android ./gradlew run
```

#### Local Android Parallel Execution
teswiz automatically detects connected Android devices and emulators, distributing test scenarios in parallel.

#### pCloudy Cloud Execution
Set the following environment variables to run against pCloudy:

```bash
PLATFORM=android RUN_IN_CI=true CLOUD_USERNAME=myusername CLOUD_KEY=abcd1234abcd ./gradlew run
```

#### TestMu AI (formerly LambdaTest) Execution
Run test scenarios on TestMu AI using provided capability templates:

```bash
CONFIG=configs/theapp/theapp_lambdatest_android_config.properties PLATFORM=android ./gradlew run
```

> [!TIP]
> - Set `CLOUD_UPLOAD_APP=true` to automatically upload local APKs to TestMu AI.
> - If `CLOUD_UPLOAD_APP=false`, set `APP_PATH=lt://...` to reference an existing remote app ID.

### 🍎 iOS Execution

```bash
PLATFORM=iOS ./gradlew run
```

---

## 🌐 Web Execution

Execute web scenarios across Selenium, Playwright-Java, or Playwright-TS engines:

```bash
# Run using default Selenium engine
PLATFORM=web ./gradlew run

# Override web engine to Playwright TypeScript
PLATFORM=web WEB_ENGINE=playwright-ts ./gradlew run

# Override web engine to Playwright Java
PLATFORM=web WEB_ENGINE=playwright-java ./gradlew run
```

---

## 🔌 API Execution

Run API suites using RestAssured or Playwright engines:

```bash
# Run using default RestAssured engine
PLATFORM=api ./gradlew run

# Override API engine to Playwright Java APIRequestContext
PLATFORM=api API_ENGINE=playwright-java ./gradlew run

# Override API engine to Playwright TypeScript Worker
PLATFORM=api API_ENGINE=playwright-ts ./gradlew run
```

---

## 🖥️ Desktop & Web-Adjacent Execution

```bash
# Run Windows Application tests
PLATFORM=windows ./gradlew run

# Run Electron Desktop Application tests
PLATFORM=electron ./gradlew run
```

---

## 👥 Multi-Persona & Multi-User Simulations

teswiz scenarios can orchestrate interactions between multiple personas and platforms within a single scenario (e.g., Buyer on Web, Seller on Android):

```bash
# Run multi-user scenario across Android & Web
TAG=@multiuser-android-web ./gradlew run

# Run multi-user scenario across multiple Web browsers
TAG=@multiuser-web-web ./gradlew run
```

---

## 🏷️ Filtering & Running Subsets of Tests

Filter scenario execution using tags:

```bash
# Run scenarios tagged with @schedule
PLATFORM=android TAG=@schedule ./gradlew run

# Run scenarios matching AND condition
PLATFORM=android TAG="@schedule and @signup" ./gradlew run

# Run scenarios matching OR condition
PLATFORM=android TAG="@schedule or @signup" ./gradlew run
```

---

## ⚙️ Overriding Runtime Defaults

Override default timeouts, retry counts, or driver settings via `TESWIZ_RUNTIME_CONFIG_FILE`:

```bash
TESWIZ_RUNTIME_CONFIG_FILE=./configs/teswiz/runtime.properties ./gradlew run
```

> [!NOTE]
> Values set in system properties or environment variables take highest precedence over property files.
