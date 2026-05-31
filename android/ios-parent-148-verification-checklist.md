# iOS Parent 148 Android Verification Checklist

Created: 2026-05-25

Purpose: validate the current Android implementation pass for the 8-bucket roadmap in `ios-parent-148-commit-parity-audit.md`.

Scope note: broad sidebar/composer/timeline visual parity was cancelled and is not a blocker for this checklist. Pet companion and payments remain separate non-roadmap verification items at the end.

## Validation Record

- Tester:
- Date:
- Android branch / commit:
- APK build variant:
- Android device / OS:
- Mac A name / OS:
- Mac B name / OS:
- Current bridge version:
- Old bridge version used for fallback, if any:

## 0. Automated Preflight

Rows covered: all roadmap rows.

- [x] From `android`, run `.\gradlew.bat :app:testDebugUnitTest`.
  - Expected: `BUILD SUCCESSFUL`.
  - Last known result: passed on 2026-05-25.
- [x] Install the debug build with `.\gradlew.bat :app:installDebug`.
  - Expected: install succeeds on the target device.
- [x] Launch the app from the device launcher.
  - Expected: app opens to `MainActivity`; logcat has no `FATAL EXCEPTION` for `com.remodex.mobile`.
  - Last known result: passed on 2026-05-25 with `Displayed com.remodex.mobile/.MainActivity`.
- [x] Keep `adb logcat` available for failures.
  - Suggested filter: `adb logcat -v time | Select-String -Pattern "FATAL EXCEPTION|AndroidRuntime|com.remodex.mobile|voice/transcribe|workspace/readFile|trusted|switch"`.

## 1. Multi-Mac State Isolation

Rows covered: #1, #2, #4, #24, #83, #139, #143, #144, and dependent merge rows #6, #9, #10, #11, #14, #25, #82, #116, #126, #137, #138.

- [x] Pair or restore two trusted Macs, Mac A and Mac B.
  - Expected: both appear in My Devices / Connections; sidebar switcher state is coherent.
- [x] On Mac A, open a known thread and let message history load.
  - Expected: messages are visible and tied to Mac A.
- [x] On Mac A, type an unsent normal composer draft in that thread.
  - Expected: draft remains visible on Mac A before switching.
- [x] On Mac A, select a runtime/model option if available.
  - Expected: selected runtime is reflected in the composer/runtime UI.
- [x] On Mac A, create or open a turn with AI changes/revert state if practical.
  - Expected: change-set/revert state is visible only for that Mac/thread.
- [x] Switch to Mac B.
  - Expected: Mac A messages, active thread, composer draft, runtime selection, worktree state, deleted/archived IDs, thread rename state, and AI change-set state do not appear under Mac B.
- [x] Create a distinct Mac B thread/draft/runtime state.
  - Expected: Mac B state is stored independently.
- [x] Switch back to Mac A.
  - Expected: Mac A active thread, cached messages, draft, runtime selection, thread metadata, and change-set state restore correctly.

Pass result:
- [x] PASS
- [ ] FAIL, notes:

## 2. Composer Draft Persistence

Rows covered: #99 plus Mac-scoped state rows #1/#2.

- [x] On Mac A, thread 1, type a normal composer draft and leave it unsent.
  - Expected: draft is saved without sending a turn.
- [x] Navigate to another thread on the same Mac.
  - Expected: thread 1 draft does not leak into thread 2.
- [x] Return to thread 1.
  - Expected: thread 1 draft is restored.
- [x] Force-stop or kill the app, then relaunch.
  - Expected: thread 1 draft still restores.
- [x] Switch to Mac B and open a thread with the same or similar name.
  - Expected: Mac A draft does not appear.
- [x] Send the draft successfully on Mac A.
  - Expected: draft clears after successful send and does not come back after relaunch.

Pass result:
- [x] PASS
- [ ] FAIL, notes:

## 3. Multi-Mac Reconnect, Recovery, And Teardown

Rows covered: #3, #5, #26, #141, #145.

- [x] Start a Mac A -> Mac B switch, then cancel it before completion.
  - Expected: selected/previous Mac state remains coherent; no partial relay/session state remains.
- [x] Simulate a failed Mac switch by stopping Mac B bridge or making Mac B unreachable, then try switching.
  - Expected: app restores the previous usable Mac state and surfaces a recoverable failure.
- [x] Restore Mac B bridge and switch again.
  - Expected: switch succeeds without requiring app data reset.
- [x] Start a running turn, background/reopen or reconnect, then tap Stop.
  - Expected: Stop remains visible and interruption works even if `turn/started` did not provide a usable turn id.
- [x] Reopen a previously running thread after reconnect.
  - Expected: running state is rehydrated and no duplicate "Thinking" rows are created.

Pass result:
- [x] PASS
- [ ] FAIL, notes:

## 4. Connectivity And Lifecycle Polish

Rows covered: #23, #27, #94, #106, #127, #141.

- [x] With the app connected and idle, background it for at least 30 seconds, then foreground it.
  - Expected: reconnect succeeds without stale session loss or noisy duplicate timeline messages.
- [x] Repeat while a turn is running.
  - Expected: timeline resumes streaming/recovery cleanly; Stop remains correct.
- [x] Lock/unlock the Android device or allow the Mac to sleep/wake if available.
  - Expected: app recovers connection state without clearing the current thread incorrectly.
- [ ] Open terminal route, produce output, background the app, then foreground it.
  - Expected: terminal rendering resumes cleanly and does not duplicate or freeze output.
- [x] Run or inspect a turn with adjacent file changes.
  - Expected: adjacent file changes are grouped once and do not create duplicate timeline noise after reconnect.

Pass result:
- [ ] PASS
- [ ] FAIL, notes:

## 5. Rootless And Draft-First New Chat

Rows covered: #89, #90, #91, #117, #118, #122, #127.

- [ ] Tap New Chat for a general/rootless chat.
  - Expected: app shows a draft composer and does not create a thread before first send.
- [ ] Send the first message.
  - Expected: exactly one thread is created and exactly one first user message is sent.
- [ ] Confirm the rootless chat uses `cwd = null`.
  - Expected: no project binding is required and the chat remains usable.
- [ ] Start a project-backed draft chat.
  - Expected: project-backed draft still creates the correct project thread on first send.
- [ ] Use draft git/terminal actions where a project path exists.
  - Expected: git actions are gated by project path and terminal-here uses the expected working directory hint.

Pass result:
- [ ] PASS
- [ ] FAIL, notes:

## 6. Workspace Text-File Previews

Rows covered: #30, #130, #132, #133.

- [ ] In a project-backed thread, tap a markdown/timeline link to a small text or code file.
  - Expected: read-only text preview opens with file content.
- [ ] Tap the same unchanged file again.
  - Expected: preview reuses cached metadata/content cleanly; `notModified` behavior does not produce an empty view.
- [ ] Modify the file on disk, then open the preview again.
  - Expected: updated content appears.
- [ ] Tap a binary file path.
  - Expected: clear non-text/binary error, no crash.
- [ ] Tap a too-large file path.
  - Expected: clear too-large error, no crash.
- [ ] Tap an out-of-workspace path or invalid relative path.
  - Expected: bridge error surfaces cleanly, no crash, no unsafe path read.
- [ ] Repeat from a rootless thread if a file link appears there.
  - Expected: missing `cwd` or unavailable workspace errors are clean and understandable.

Pass result:
- [ ] PASS
- [ ] FAIL, notes:

## 7. Bridge-Owned Voice Transcription

Rows covered: #134.

- [ ] Use a current bridge that supports `voice/transcribe`.
  - Expected: Android sends `mimeType="audio/wav"`, base64 WAV audio, `sampleRateHz=24000`, and `durationMs`; transcript returns successfully.
- [ ] Check Android logs during current-bridge transcription.
  - Expected: Android does not expose ChatGPT/API auth tokens and does not need `voice/resolveAuth`.
- [ ] Test microphone permission denied and retry after permission grant.
  - Expected: denial is handled cleanly and retry can succeed.
- [ ] Test an older bridge where `voice/transcribe` is unsupported.
  - Expected: app falls back to the legacy `voice/resolveAuth` + direct upload path and transcription still works.
- [ ] Test a short failed/noisy recording.
  - Expected: failure surfaces as a recoverable error, no crash.

Pass result:
- [ ] PASS
- [ ] FAIL, notes:

## 8. Non-Roadmap Verification Still In Audit

These are not blockers for closing the 8-bucket roadmap, but they remain **Needs verification** in the full 148-commit audit.

### Pet Companion

Rows covered: #45, #49, #50, #78.

- [ ] Enable the pet companion setting.
  - Expected: overlay appears and animates/render spritesheets correctly.
- [ ] Validate `pet/list` and `pet/read` bridge-backed loading.
  - Expected: installed pets list and selected pet load without UI lockups.
- [ ] Try an oversized/invalid pet spritesheet.
  - Expected: bridge rejection surfaces cleanly.
- [ ] Confirm Beta badge presentation in settings.
  - Expected: badge is visible and does not break layout.

### Payments, Offer Code, And Relay Health

Rows covered: #71.

- [ ] Run a build/configuration with subscription environment available.
  - Expected: subscription abstraction initializes cleanly.
- [ ] Launch redeem-code flow.
  - Expected: external flow opens or surfaces an actionable unavailable state.
- [ ] Confirm relay `/health` metrics consumer behavior.
  - Expected: health data loads or errors cleanly without blocking core local use.

## Closeout Rules

- [ ] Keep failed items open with notes and logcat snippets.
- [ ] Fix only functional defects found by validation; do not reopen the cancelled broad UI bucket.
- [ ] After a section passes, update only the affected rows in `ios-parent-148-commit-parity-audit.md`.
- [ ] Move **Needs verification** rows to **Ported** only after the relevant checklist section passes.
- [ ] Merge/maintenance rows close together with their owning Multi-Mac validation pass.
