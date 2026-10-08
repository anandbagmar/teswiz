# teswiz Socket-Based Communication Testing — Plan & TODO

> Status: **Proposed — awaiting clarifications before implementation.**
> Enable testing of socket-based communication between a front-end and back-end, from the
> browser, Android, and iOS apps.

## Goal

Support testing socket-based communication between the front-end and the back-end, driven
from the browser / Android / iOS app.

## Disambiguation first (changes everything downstream)

"Socket" must be pinned down before design — the library and framing differ a lot:

- **WebSocket** (`ws://` / `wss://`) — most common for web front-ends.
- **Socket.IO** — sits on top of WebSocket/polling, has its own framing/handshake.
- **Raw TCP sockets** — unusual from a browser; more likely in native mobile apps.

## Current state (verified in codebase)

- **No socket-based communication testing code exists** anywhere in teswiz today.
- Playwright is already integrated (Java + TS worker) and exposes page/context access,
  which is the natural home for web-side WebSocket observation.
- Multi-user execution already exists via the persona model (`UserPersonaDetails`,
  `Drivers.createDriverFor(...)`), which a "socket persona" could plug into.

## Two testing styles (choose one or both)

1. **Passive observation** — assert on the frames the app actually exchanges.
2. **Active participation** — the test opens its own socket connection to the backend and
   verifies round-trips / multi-user scenarios. Platform-agnostic; fits teswiz's multi-user
   persona model (a persona could be a socket client rather than a UI driver).

## Mechanism per platform

| Platform | Passive: observe app's own socket traffic | Active: test-owned socket client |
|---|---|---|
| Web (PW-Java / PW-TS) | Playwright `page.on('websocket')` → inspect frames sent/received (cleanest, native) | Open a Java/Node WebSocket client in the test as a second participant and assert |
| Web (Selenium) | No native WS inspection; CDP `Network.webSocketFrameSent/Received` (**Chromium only**) or a proxy | Same test-owned-client approach |
| Android / iOS | No frame inspection via Appium; needs a WS-aware **proxy** or app-level hooks/logs | A test-side WebSocket/Socket.IO client against the same backend — works regardless of platform |

## Likely teswiz-shaped design

- A small **socket-client abstraction** (connect, send, await-message-matching-predicate,
  close) exposed to the Business Layer.
- For web: optional Playwright WebSocket **observation** surfaced through
  `PlaywrightJavaScreenContext` / the TS worker.
- Captured frames written to the scenario folder (`target/reports/<scenario>/`) and
  published via `ScenarioArtifactReporter`, consistent with other capture features.
- Optional integration with the multi-user persona model as a "socket persona".

## Pre-requisites

- Backend WebSocket/Socket.IO endpoint URL + auth/handshake details.
- Socket client library choice:
  - Java-side for active participation (e.g. Java-WebSocket, Socket.IO-client-java).
  - Node-side if driving through the PW-TS worker.
- For passive **mobile** observation: a proxy (Appium cannot see frames).
- Any new config property follows the canonical-template-first rule + example configs +
  `./gradlew validateConfigurationTemplates`.

## Clarifications needed (blocking)

1. Which **protocol** — raw WebSocket, Socket.IO, or raw TCP? (Changes library and framing.)
2. **Goal** — observe the app's real socket traffic and assert on it, or have the test act
   as its own socket client to drive/verify scenarios (**or both**)?
3. For **web**, is Playwright-only acceptable for passive observation, with Selenium needing
   CDP (Chromium-only)?
4. For **mobile**, is passive frame observation required (needs a proxy), or is a test-owned
   socket client against the same backend sufficient?
5. Should this integrate with the existing **multi-user persona model** (a "socket persona"),
   or be a standalone helper?

## Acceptance criteria (draft — finalize after clarifications)

- A test can establish and/or observe socket communication for the chosen protocol and
  assert on messages exchanged with the backend.
- Captured frames (if observation is in scope) are attached to the scenario report.
- `./gradlew validateConfigurationTemplates` and `./gradlew test` remain green.

## Cross-cutting note

Shares the same artifact-publishing path and (optionally) a common per-session capture layer
with the Session Video Recording and Web API Traffic Capture TODOs. Decide whether to build a
shared capture layer or keep the three features independent.
