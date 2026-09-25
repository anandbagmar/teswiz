# API Test Implementation Example

This guide provides concrete examples of implementing API-level tests in Teswiz using the unified `ApiService` facade, supporting `API_ENGINE=rest-assured`, `API_ENGINE=playwright-java`, and `WEB_ENGINE=playwright-ts`.

---

## Configuration (`API_ENGINE`)

Engine selection is governed by the `API_ENGINE` property in your configuration properties file or environment variable:

```properties
# Supported values: rest-assured (default), playwright-java
API_ENGINE=playwright-java
```

- **`rest-assured`**: Default engine delegating to RestAssured.
- **`playwright-java`**: Playwright Java engine utilizing `APIRequestContext` for ultra-fast, native HTTP execution.
- **`playwright-ts`**: Playwright TypeScript web & API automation suite integration.

---

## Environment Issue Detection

Every API request in the suite — whether executed via `rest-assured` or `playwright-java` — is automatically checked for `502`, `503`, and `504` status codes by teswiz environment filters/interceptors. These status codes throw `EnvironmentSetupException` immediately before the response reaches your business layer or step definitions, preventing infrastructure outages from being misreported as test failures.

To bypass environment failure checks for intentional test scenarios:

```properties
DISABLE_ENVIRONMENT_ISSUE_FILTER=true
```

---

## API Traffic Logging

teswiz registers global traffic loggers so every API call in the suite is captured automatically — request method, URL, headers, query parameters, request body, response status, headers, and response body — as a single masked `.log` file per call under `api-traffic/` inside the scenario's report folder (e.g. `api-traffic/01-GET-https-jsonplaceholder-typicode-com-posts-1.log`). Masking applies `SensitiveDataMasker` configuration.

To turn off API traffic logging:

```properties
API_TRAFFIC_LOGGING=false
```

---

## 1. Feature File (`weather-api.feature`)

```gherkin
@api @pw-api
Feature: Weather Forecast Service

  # Execution commands:
  # API_ENGINE=rest-assured CONFIG=./configs/api_local_config.properties TAG=@api ./gradlew run
  # API_ENGINE=playwright-java CONFIG=./configs/api_local_config.properties TAG=@pw-api ./gradlew run

  Scenario: Get weather forecast for location coordinates
    Given I send GET request with location coordinates
    Then temperature of that location should be in range 10 and 40 C
```

---

## 2. Step Definition (`WeatherAPISteps.java`)

```java
package com.znsio.teswiz.steps;

import com.znsio.teswiz.businessLayer.weatherAPI.WeatherAPIBL;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.json.JSONObject;

public class WeatherAPISteps {
    private JSONObject jsonObject;

    @Given("I send GET request with location coordinates")
    public void sendGetRequest() {
        jsonObject = new WeatherAPIBL().getCurrentWeatherJSON();
    }

    @Then("temperature of that location should be in range {int} and {int} C")
    public void verifyTemperature(int lowerLimit, int upperLimit) {
        new WeatherAPIBL().verifyCurrentTemperature(jsonObject, lowerLimit, upperLimit);
    }
}
```

---

## 3. Engine-Agnostic Business Layer (`WeatherAPIBL.java`)

```java
package com.znsio.teswiz.businessLayer.weatherAPI;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.services.ApiService;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WeatherAPIBL {
    private final Map<String, Object> testData = Runner.getTestDataAsMap("Weather_API");
    private final String baseUrl = testData.get("url").toString();

    public JSONObject getCurrentWeatherJSON() {
        HashMap<String, Object> queryParams = new HashMap<>(){{
            put("latitude", testData.get("latitude").toString());
            put("longitude", testData.get("longitude").toString());
            put("current_weather", true);
        }};

        // Execute HTTP GET using the engine-independent ApiService facade
        TeswizApiResponse response = ApiService.get(baseUrl, queryParams);

        assertThat(response.getStatusCode())
                .as("Failed weather check API status code")
                .isEqualTo(200);

        // Verify response latency in milliseconds
        assertThat(response.getResponseTimeInMs())
                .as("API response latency check")
                .isLessThan(5000L);

        return response.asJsonObject().getJSONObject("current_weather");
    }

    public void verifyCurrentTemperature(JSONObject response, int minTemp, int maxTemp) {
        double currentTemp = response.getDouble("temperature");
        assertThat(currentTemp)
                .as("Current temperature value check")
                .isBetween((double) minTemp, (double) maxTemp);
    }
}
```
