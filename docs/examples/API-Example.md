# API Test Implementation Example

This guide provides a concrete example of implementing an API-level test in Teswiz using the integrated `RestAssuredService`.

---

## Environment Issue Detection

Every RestAssured request in the suite — not just calls made through `RestAssuredService`, any RestAssured client — is automatically checked for `502`, `503`, and `504` responses by `EnvironmentIssueFilter`, which teswiz registers globally during setup. These status codes are never a valid test expectation — they mean the target service is unavailable — so the filter throws `EnvironmentSetupException` immediately, before the response reaches your business layer or step definitions. This keeps environment outages from being misreported as test/assertion failures.

If a specific run genuinely needs to bypass this (e.g. a test that intentionally exercises a mocked gateway returning one of these codes), disable it via an environment variable or system property:

    DISABLE_ENVIRONMENT_ISSUE_FILTER=true

---

## API Traffic Logging

teswiz also registers a global `ApiTrafficLoggingFilter` so every RestAssured call in the suite is captured automatically — request and response, headers and body — as a single masked `.log` file per call, written to `api-traffic/` inside the current scenario's report folder (e.g. `api-traffic/03-POST-api-v1-wallet-authenticate.log`). Calls are numbered sequentially per scenario starting at `01`. Masking reuses the same `SensitiveDataMasker` config as the rest of teswiz — `SHOW_SENSITIVE_DATA`, `MASK_ADDITIONAL_KEYS`, and `MASK_KEYS_OVERRIDE` all apply, no separate setup needed.

The feature is on by default. To turn it off:

    API_TRAFFIC_LOGGING=false

A call is still recorded even when `EnvironmentIssueFilter` throws on it — those are exactly the calls worth having a record of.

---

## 1. Feature File (`weather-api.feature`)
```gherkin
@api
Feature: Weather Forecast Service

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

## 3. Business Layer (`WeatherAPIBL.java`)
```java
package com.znsio.teswiz.businessLayer.weatherAPI;

import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.services.RestAssuredService;
import io.restassured.response.Response;
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
        
        // Execute HTTP GET using the built-in RestAssuredService wrapper
        Response jsonResponse = RestAssuredService.getHttpResponseWithQueryMap(baseUrl, queryParams);
        
        assertThat(jsonResponse.getStatusCode())
                .as("Failed weather check API status code")
                .isEqualTo(200);
                
        return new JSONObject(jsonResponse.getBody().asString()).getJSONObject("current_weather");
    }

    public void verifyCurrentTemperature(JSONObject response, int minTemp, int maxTemp) {
        double currentTemp = response.getDouble("temperature");
        assertThat(currentTemp)
                .as("Current temperature value check")
                .isBetween((double) minTemp, (double) maxTemp);
    }
}
```
