# Windows Compatibility Changes for Bridge and Relay

These notes describe the Windows compatibility changes applied after importing the latest upstream `phodex-bridge/` and `relay/` folders from `upstream/main`.

The goal for a future PR is to keep the upstream bridge/relay behavior while preserving support for running and testing the local-first bridge on Windows.

## Source Changes

### `phodex-bridge/src/codex-transport.js`

Restored injectable platform handling in `createCodexTransport`.

Why:
- Tests and helper code need to simulate macOS launch behavior while running on Windows.
- Without passing `platform` through to `createCodexLaunchPlans`, Windows always uses the `cmd.exe /d /c codex app-server` launch plan, even in tests that intentionally request `platform: "darwin"`.

Change:
- Added `platform = process.platform` to `createCodexTransport`.
- Passed `platform` through to `createSpawnTransport`.
- Passed `platform` through to `createCodexLaunchPlans`.

### `phodex-bridge/src/macos-launch-agent.js`

Restored injectable platform handling in `runMacOSBridgeService`.

Why:
- The macOS service runner has tests that simulate macOS behavior from Windows.
- Calling `assertDarwinPlatform()` without the injected platform makes these tests fail on Windows before they can exercise daemon-state error handling.

Change:
- Added `platform = process.platform` to `runMacOSBridgeService`.
- Passed the injected platform to `assertDarwinPlatform(platform)`.

### `phodex-bridge/src/workspace-handler.js`

Restored native Windows image preview resizing.

Why:
- Upstream preview resizing was `sips`-only, which works on macOS but not Windows.
- Windows users need bounded image previews without relying on macOS tooling.

Change:
- Replaced `usesSipsImagePreview()` with `usesNativeImagePreview()`.
- Treated `win32` / `windows` as preview-capable platforms.
- Added `downsampleImageNative()`.
- Added Windows PowerShell image resizing via `System.Drawing`.
- Kept `sips` for macOS.
- Added Windows command shim resolution so fake `sips.js` test helpers can run on Windows.

### `phodex-bridge/src/rollout-watch.js`

Restored deterministic rollout file sorting when mtimes tie.

Why:
- Windows filesystem timestamp granularity can make recently written rollout files have identical `mtimeMs`.
- Sorting only by `mtimeMs` can choose an older rollout nondeterministically.

Change:
- Sort rollout candidates by:
  1. descending `mtimeMs`
  2. descending basename
  3. descending full path

This keeps newest timestamped rollout filenames preferred even when mtimes tie.

## Test Harness Changes

### `relay/push-service.test.js`

Made relay push persistence tests portable.

Why:
- Windows does not enforce POSIX `0600` file mode semantics.
- Hardcoded `/tmp/...` path assertions fail because Node normalizes paths to Windows separators.

Change:
- Skip the `0o600` mode assertion on `win32`.
- Compare durable state paths with `path.join(...)` instead of a hardcoded POSIX string.

### `phodex-bridge/test/bridge-desktop-ipc-integration.test.js`

Added a cross-platform IPC socket helper.

Why:
- Windows cannot listen on Unix socket paths like `C:\...\ipc.sock`.
- Node IPC on Windows requires named pipe paths.

Change:
- Added `createIpcTestSocket(prefix)`.
- Uses `\\.\pipe\<temp-name>-ipc` on Windows.
- Uses `<tempDir>/ipc.sock` on other platforms.

### `phodex-bridge/test/desktop-ipc-action-follower.test.js`

Added the same cross-platform IPC socket helper across all desktop IPC follower tests.

Why:
- Multiple tests were creating `ipc.sock` paths under `%TEMP%`, which fail with `EACCES` on Windows.

Change:
- Replaced each direct `path.join(tempDir, "ipc.sock")` with `createIpcTestSocket(prefix)`.
- Uses Windows named pipes on `win32`, Unix sockets elsewhere.

### `phodex-bridge/test/codex-cli-bootstrap.test.js`

Restored explicit macOS simulation for CLI bootstrap tests.

Why:
- These tests validate macOS bootstrap/install behavior.
- Running them on Windows without `platform: "darwin"` makes the code take Windows paths and fail unrelated assertions.

Change:
- Passed `platform: "darwin"` into `ensureCodexCLI(...)` test calls.

### `phodex-bridge/test/codex-transport.test.js`

Restored explicit macOS simulation for Codex launch-plan tests.

Why:
- The fallback bundled Codex binary behavior is macOS-specific.
- On Windows, `createCodexTransport` otherwise selects `cmd.exe /d /c codex app-server`.

Change:
- Passed `platform: "darwin"` into transport tests that expect macOS launch behavior.

### `phodex-bridge/test/desktop-handler.test.js`

Made Windows deep-link executable path assertions platform-aware.

Why:
- `path.join("C:\\Windows", "System32", "rundll32.exe")` produces the actual Windows-style path.
- The upstream assertion expected mixed separators: `C:\Windows/System32/rundll32.exe`.

Change:
- Reintroduced `node:path`.
- Asserted against `path.join(...)`.

### `phodex-bridge/test/git-handler.test.js`

Disabled automatic CRLF conversion in temporary git repositories.

Why:
- Windows Git can convert LF fixtures to CRLF, making exact file-content assertions fail.

Change:
- Added `git config core.autocrlf false` inside `makeTempRepo()`.

### `phodex-bridge/test/macos-launch-agent.test.js`

Restored fake UID handling for macOS launchd tests running on Windows.

Why:
- Windows Node does not expose `process.getuid()`.
- macOS launchd tests still need deterministic `gui/<uid>` assertions.

Change:
- Added `TEST_UID = typeof process.getuid === "function" ? process.getuid() : 501`.
- Passed `UID: String(TEST_UID)` into simulated macOS envs.
- Replaced direct `process.getuid()` expectations with `TEST_UID`.
- Passed `platform: "darwin"` to `runMacOSBridgeService` test coverage.

### `phodex-bridge/test/workspace-image.test.js`

Restored Windows-compatible fake `sips` test helper and preview assertions.

Why:
- Windows cannot execute a Unix-style `sips` script directly.
- The Windows preview path can return a resized PNG, so tests should assert bounded preview size rather than byte-for-byte equality with the source image.

Change:
- Added `IS_WINDOWS_HOST`.
- Added `writeFakeSips(...)`.
- On Windows, writes:
  - `sips.js`
  - `sips.cmd`
- Updated fake `sips` tests to use the helper.
- Changed the non-mac preview assertion to check that preview bytes are non-empty and at most 2 MiB.

## Verification

Both suites pass on Windows after these changes:

```powershell
cd phodex-bridge
npm test
```

Result:
- 305 passing
- 1 skipped
- 0 failing

```powershell
cd relay
npm test
```

Result:
- 39 passing
- 0 failing

## Suggested PR Scope

For a clean PR, start from current upstream `main` and apply only the compatibility changes listed above.

Recommended title:

`Preserve Windows compatibility for local bridge and relay tests`

Recommended summary:

- Restore platform injection for Codex transport and macOS service simulations.
- Add Windows-native image preview resizing and test shims.
- Use Windows named pipes for desktop IPC tests.
- Make relay path/mode assertions cross-platform.
- Stabilize rollout selection and git fixture line endings on Windows.
