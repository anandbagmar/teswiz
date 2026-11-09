# Breaking Changes
[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---


teswiz records notable changes and upgrades in this document as well as in release notes.

---

## 🔔 Run notifications and the failure breakdown (1.0.47 – 1.0.55)

These affect a project that implements a `RunNotificationComposer` or calls the `reporting.notify` helpers directly. See [Run Notifications](../subsystems/run-notifications.md) for the full guide.

| Release | Change | Needs action? |
| :--- | :--- | :--- |
| 1.0.47 | `HtmlReport.footer(env)` and `slackFooter(env)` now take a second argument, `footer(env, reproduceCommand)` and `slackFooter(env, reproduceCommand)`. `RunEnvironment.reproduceCommand()` was removed | **Yes, compile error.** Build the command with `HtmlReport.reproduceCommand(Map)` and pass it in |
| 1.0.50 | `JUnitResults.Scenario` gained `type` and `trace` | No. The 4-argument constructor is kept |
| 1.0.51 | The Slack failure breakdown text changed: a counted heading and an aligned code-block table, capped at the top 10 causes (`DEFAULT_MAX_CAUSES`) | Only if a composer or its tests match the old `*Why did things fail? (N)*` text. Pass `slack(breakdown, maxCauses)` for more than 10 |
| 1.0.53 | `FailureCause` gained a third component, `step`. The Slack and HTML breakdown are grouped by type and place with a nested row per failing step, and counts show as `×N`. The HTML table has four columns | Only if you build `FailureCause` or match the output. `new FailureCause(type, location)` still compiles, with a blank step |
| 1.0.55 | The default label for the exception type is now `HUMANIZED_TYPE`, so `FreeSpinNotAwardedException` shows as "Free spin not awarded". The raw class name moved under the location in the email and lost its parentheses in Slack | Only if you match the output. Pass `FailureBreakdownRenderer.SIMPLE_TYPE` as the label to keep the raw class name |

**Humanised labels.** A name made only of capitals, such as `SQLException`, has no word boundaries to split on, so it becomes one word ("Sql"). Override such types with your own `Function<String,String>`, composed over `HUMANIZED_TYPE`.

---

## 🔌 API engines send identical requests (1.0.49)

Switching `API_ENGINE` no longer changes the request that goes on the wire. Two behaviours changed on the way to that, and a suite that depended on the old behaviour needs a look. See [What goes on the wire](../engines/running-api-tests.md#what-goes-on-the-wire-same-for-every-engine).

- **Explicit `null` fields are now sent.** A `Map` or `Collection` body is written by one shared Jackson serialiser, so `{"amount":null}` is sent as written. The Playwright engine used to drop `null` fields and `null` list elements. If a test relied on a `null` field being omitted, remove the key from the map.
- **URLs are treated as already encoded.** On RestAssured a valid `%XX` escape is no longer encoded a second time (`%3A` used to go out as `%253A`). If a test double-encoded a value to work around that, encode it once.

---

## 🥒 Cucumber 8.0.3 Upgrade Notes

### ⚠️ Custom TestNG Runners (`AbstractTestNGCucumberTests`)

If a consumer project created its own custom subclass of `AbstractTestNGCucumberTests` and explicitly overrode the `scenarios()` method:

- **Cucumber 7.x**:
  ```java
  @Override
  @DataProvider(parallel = true)
  public Object[][] scenarios() {
      return super.scenarios();
  }
  ```
- **Cucumber 8.x**:
  ```java
  import org.testng.ITestContext;

  @Override
  @DataProvider(parallel = true)
  public Object[][] scenarios(ITestContext context) {
      return super.scenarios(context);
  }
  ```

Projects with custom runners overriding `scenarios()` will encounter a compilation error until they update the method signature to accept `org.testng.ITestContext context`.

