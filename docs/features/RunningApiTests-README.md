# API Test Execution

## Command: 

```bash
CONFIG=./configs/<apiConfig.properties> TAG=<ScenarioTag> PLATFORM=api ./gradlew run
```

To run with a specific API engine:

```bash
API_ENGINE=rest-assured CONFIG=./configs/api_local_config.properties TAG=@api PLATFORM=api ./gradlew run
API_ENGINE=playwright-java CONFIG=./configs/api_local_config.properties TAG=@api PLATFORM=api ./gradlew run
API_ENGINE=playwright-ts CONFIG=./configs/api_local_config.properties TAG=@api PLATFORM=api ./gradlew run
```

## Supported API Engines (`API_ENGINE`)

teswiz supports three API engines through the unified `ApiService` facade:

- **`rest-assured`** (default): RestAssured HTTP client execution.
- **`playwright-java`**: Playwright Java native `APIRequestContext` HTTP execution.
- **`playwright-ts`**: Playwright TypeScript worker protocol HTTP execution.

All engines support full payload serialization and data type parity:
- Data payloads: `Map<String, Object>`, `Collection`/`List`, `JSONObject`, `JSONArray`, `String`, numbers, booleans, and byte arrays.
- HTTP operations: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.
- Environment health filters: Automatic detection and `EnvironmentSetupException` throwing on `502`, `503`, and `504` status codes.
- Traffic logging: Automatic masked HTTP request/response logging under `api-traffic/`.

## API Config.properties File Params:

```properties
FRAMEWORK=cucumber
APP_NAME=<API SCENARIO APP NAME>
ENVIRONMENT_CONFIG_FILE=./src/test/resources/environments.json
IS_VISUAL=false
LOG_DIR=target
LOG_PROPERTIES_FILE=./src/test/resources/log4j2.properties
PARALLEL=<parallel count>
PLATFORM=api
API_ENGINE=rest-assured
PROXY_KEY=HTTP_PROXY
REPORT_PORTAL_FILE=src/test/resources/reportportal.properties
RUN_IN_CI=<true/false>
TARGET_ENVIRONMENT=<sit/eat/prod>
LAUNCH_NAME_SUFFIX= on <environment> Environment
TEST_DATA_FILE=./src/test/resources/testData.json
```

## Sample API scenario Example:

```
feature file: src/test/resources/com/znsio/teswiz/features/weatherAPI.feature
Step Definition File: src/test/java/com/znsio/teswiz/steps/WeatherAPISteps.java
BL File: src/test/java/com/znsio/teswiz/businessLayer/weatherAPI/WeatherAPIBL.java
```

## Sample API Workflow scenario Example:

```
feature file: src/test/resources/com/znsio/teswiz/features/workflow/weatherAPI.feature
Step Definition File: src/test/java/com/znsio/teswiz/steps/WeatherAPISteps.java
BL File: src/test/java/com/znsio/teswiz/businessLayer/weatherAPI/WeatherAPIBL.java
```

## Reportportal Execution report:

To get reportportal execution report, Update following params in `src/test/resources/reportportal.properties`:

```properties
rp.endpoint=http://127.0.0.1:8080
rp.uuid=<uuid from reportportal dashboard>
rp.launch=teswiz
rp.project=teswiz
rp.enable=false
```

## Migration Guide: Moving from RestAssured to Playwright Java (`API_ENGINE`)

### 1. Suites using `ApiService` Facade (Recommended)

If your test suite uses `ApiService` (`ApiService.get()`, `ApiService.post()`, etc.), **no code changes are required**.

To migrate execution:

Set `API_ENGINE=playwright-java` in your suite `.properties` file:

```properties
API_ENGINE=playwright-java
```

Or pass it as a command-line environment variable:

```bash
API_ENGINE=playwright-java CONFIG=./configs/api_local_config.properties TAG=@api PLATFORM=api ./gradlew run
```

### 2. Suites using Direct `RestAssured` Calls

If legacy code directly invokes `io.restassured.RestAssured`:

1. **Replace RestAssured calls with `ApiService`**:
   - `RestAssured.given().headers(...).get(url)` $\rightarrow$ `ApiService.get(url, queryParams, headers)`
   - `RestAssured.given().body(mapBody).post(url)` $\rightarrow$ `ApiService.post(url, mapBody, headers)`

2. **Update Response Assertions**:
   - Change `io.restassured.response.Response` to `com.znsio.teswiz.api.TeswizApiResponse`.
   - `response.getStatusCode()` $\rightarrow$ `response.getStatusCode()`
   - `response.getBody().asString()` $\rightarrow$ `response.getResponseText()` or `response.asJsonObject()`
   - `response.getTime()` $\rightarrow$ `response.getResponseTimeInMs()`

3. **Enable `API_ENGINE=playwright-java` in suite configuration.**