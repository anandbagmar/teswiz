# Breaking Changes
[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---


teswiz records notable changes and upgrades in this document as well as in release release notes.

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

