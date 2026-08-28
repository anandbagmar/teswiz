# teswiz Claude Instructions

When working in this repository:

- Keep the repo instructions aligned with the Codex and Antigravity entry points:
  - `.codex/skills/teswiz-project/SKILL.md`
  - `CLAUDE.md`
  - `ANTIGRAVITY.md`
- When you change code or documentation, include a concise suggested commit message in the final response.
- Prefer a short imperative commit message that reflects the main change clearly.
- Keep changes focused and test-first when the task is a refactor or cleanup.
- Prefer small, meaningfully named methods, variables, and classes.
- When authoring TestNG-mode tests (`com.znsio.teswiz.testng`), always use fluent method chaining between business-layer calls wherever the BL API returns a chainable type (e.g. `AuthBL.signIn()` returning `LandingBL`, or a method returning `this`), instead of constructing a new BL instance per call. **Before reusing an already-constructed BL instance instead of re-constructing it, check whether that BL's constructor has side effects** (several call `Runner.setCurrentDriverForUser(...)`, which sets a single thread-scoped "current persona" pointer that screen resolution reads from) — in a multi-persona test, reusing a stale instance after constructing a later persona's BL silently resolves screens against the wrong persona/platform. In that case, reconstruct the BL immediately before each interaction instead (matching what the equivalent Cucumber step-defs already do) rather than holding a reference across other personas' calls. See `docs/internals/Cucumber-To-TestNG-Migration-Guide.md` for worked examples.
- Keep encapsulation tight; do not widen visibility unless there is a clear framework-facing need.
- For `playwright-ts`, keep test-owned `.ts` screen modules under `src/test/resources/playwright/screens`.
- Prefer shared app-path resolution, version detection, and download handling under `com.znsio.teswiz.config.app` instead of growing `runner`.
- Prefer shared capability lookup and capability-file persistence under `com.znsio.teswiz.config.capability` instead of growing `runner`.
- Prefer internal mobile device-session state under `com.znsio.teswiz.mobile.session` instead of growing `runner`.
- Prefer local mobile device and simulator setup under `com.znsio.teswiz.mobile.device` instead of growing `runner`.
- Prefer internal Appium server lifecycle code under `com.znsio.teswiz.mobile.server` instead of growing `runner`.
- Prefer mobile cloud setup and cleanup routing under `com.znsio.teswiz.mobile.provider` instead of growing `runner`.
- For CI flows that install Node dependencies, run `actions/setup-node` first and prefer `npm ci` over `npm install`.
- Whenever `package.json` dependencies or `overrides` change, keep the matching `package-lock.json` update in the same change.
- In CI, install Playwright browsers only in workflows that actually execute Playwright.
- Keep only the latest artifact set per workflow per user-created branch, and do not retain artifacts for `dependabot/*` or `renovate/*` branches.
- For stricter screen-contract audits, use `./gradlew verifyScreenContracts -PincludeMissingScreenTargets=true`.
- Treat `configs/teswiz/teswiz_config.properties.template` as the canonical execution-config
  contract. Keep every `configs/**/*.properties` file aligned with it and run
  `./gradlew validateConfigurationTemplates` after config changes.
- Prefer serial focused Gradle verification runs on the same checkout; parallel independent Gradle invocations can produce misleading failures because they share build outputs and intermediates.
