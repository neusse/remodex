# iOS Parent 148-Commit Android Parity Audit

Audit date: 2026-05-25

Parent source: `upstream/main` at `e63cf05c` (`Improve history tool call readability`)

Android assessment tree: `codex/ios-parity-assessment` with Android restored from `publish-main`

Scope: all 148 commits in `publish-main..upstream/main`, including first-parent commits and merged branch commits.

## Status Legend

- **Ported**: Android already has equivalent behavior, or the behavior is bridge/shared-runtime code already present in this assessment tree.
- **Partially ported**: Android has foundations or adjacent behavior, but important iOS behavior is missing.
- **Not ported**: Android has no meaningful equivalent yet.
- **Needs verification**: Static review cannot prove parity; validate on Android device/emulator, Windows host, or UX review before closing.
- **Out of scope**: Explicitly excluded from this Android porting pass; keep the row for audit history, but do not treat it as an active implementation or verification blocker.

## Current State Summary

1. **8-bucket Android roadmap**: **Implemented, needs manual/device verification**. Multi-Mac state isolation, composer draft persistence, reconnect/recovery, lifecycle polish, rootless draft-first chat, workspace text previews, and bridge-owned voice transcription now have Android implementation coverage.
2. **Automated verification already run**: `.\gradlew.bat :app:testDebugUnitTest` passes from `android`; debug install and startup smoke passed on device, with logcat showing `Displayed com.remodex.mobile/.MainActivity` and the app resumed.
3. **Manual validation still pending**: two-Mac/device switching, failed/cancelled switch recovery, foreground/background reconnect, rootless New Chat, durable composer drafts, workspace text previews, and voice transcription need live validation before moving rows to **Ported**.
4. **Validation checklist**: use `android/ios-parent-148-verification-checklist.md` as the incremental execution record.
5. **Non-roadmap verification still open**: pet companion and payments/subscription/offer-code rows remain **Needs verification** when those buckets are in scope.
6. **Canceled UI scope**: the broad refactor/sidebar/composer visual-parity bucket is intentionally **Out of scope** for this pass and should not block the 8-bucket roadmap.

## Partially Ported Commit Classification

After this implementation pass, the 8 roadmap buckets are no longer active Android implementation gaps; they are either **Needs verification** or dependent merge/maintenance rows. The removed broad refactor/sidebar visual-parity bucket is marked **Out of scope** instead of being tracked as an implementation blocker.

| Bucket | State | Covered commits | Android state | Missing / next decision |
|---|---|---|---|---|
| Multi-Mac/device state isolation | Implemented, needs verification | #1 `5b12f87d`, #2 `bf60c03b`, #24 `0b09031d`, #83 `af4622ce`, #144 `eebb5788` | Android now has trusted-device foundations, My Devices UI, trusted session resolve, switch/cancel flow, `MacScopedSessionStore`, Mac-scoped message history, Mac-scoped normal drafts, Mac-scoped AI change sets, and scoped worktree/runtime/active-thread reloads. | Validate Mac A -> Mac B switching on device to confirm no cached messages, drafts, active thread, runtime selection, worktree state, or change-set state leaks. |
| Multi-Mac merge/branch maintenance | Dependent; needs Multi-Mac verification | #6 `a7ec222c`, #9 `a20aca3f`, #10 `b396eda2`, #11 `fbf65c57`, #14 `4335a1c8`, #25 `ad00f7ab`, #82 `b2e95bc4`, #116 `67058278`, #126 `46bd43ec`, #137 `17cf5268`, #138 `e6021a40` | These are merge or branch-integration commits, not separate Android features. | Close them with the owning Multi-Mac validation pass; they should not drive separate Android tasks. |
| Multi-Mac reconnect, recovery, and teardown | Implemented, needs verification | #3 `1b2a9b5b`, #26 `33f05c3f`, #141 `10e89240` | Android has secure transport tests, trusted reconnect foundations, foreground/background reconnect paths, wake recovery plumbing, scoped reset during reconnect, and failed/cancelled switch restore paths. | Needs live two-device validation and teardown/recovery checks. Add more JVM tests only where logic can be exercised without emulator/device state. |
| Connectivity and lifecycle polish | Implemented, needs verification | #23 `cc9dce62`, #27 `e1b1f180`, #127 `4ce0dc1f` | Android has file-change grouping, reconnect/recovery paths, rootless chat, terminal route support, and scoped active-thread restore on foreground. | Device review should confirm foreground/background reconnect behavior, stale session handling, terminal pause/render lifecycle, and duplicate timeline-noise suppression. |
| Rootless and draft-first chat flow | Implemented, needs verification | #89 `b47ff435`, #90 `d1e82bcc`, #91 `db6005be`, #122 `91e63702` | Android supports `cwd = null` chats and `NewChatDraft` routing that defers thread creation until first send; older rows are normalized to the same verification bucket as #117/#118. | Validate draft-first rootless New Chat on device: no thread before send, exactly one first send, and `cwd = null` general chat. |
| Composer draft persistence | Implemented, needs verification | #99 `3ece8202` | Android now persists normal composer drafts per Mac/thread through repository/service APIs and clears storage after successful send/queue clear. | Validate restart persistence, send-clear, and no draft leakage between Macs or threads. |
| Workspace file previews | Implemented, needs verification | #30 `131f8bbd`, #130 `cf70af17`, #132 `73177efc`, #133 `0948c69d` | Android now supports `workspace/readFile` text previews from timeline markdown links, including metadata caching with `ifByteLength`/`ifMtimeMs`. | Validate text/code links, unchanged `notModified` reuse, too-large/binary/out-of-workspace errors, and rootless/relative paths on device. |
| Bridge-owned voice transcription | Implemented, needs verification | #134 `6fa0b9f5` | Android now calls `voice/transcribe` with `mimeType`, base64 WAV, `sampleRateHz=24000`, and `durationMs`, falling back to legacy direct upload only when the bridge reports the method unsupported. | Validate current bridge transcription and one old-bridge fallback pass. |


### Clear State

- **Implemented, pending device validation**: Mac-scoped durable state (#1/#2/#24/#83/#144), composer draft persistence (#99), workspace text-file previews (#30/#130/#132/#133), bridge-owned voice transcription (#134), Multi-Mac switching/recovery (#3/#26/#141), reconnect/lifecycle polish (#23/#27/#127), and rootless draft-first chat (#89/#90/#91/#122).
- **Automated checks complete**: full Android JVM unit tests pass and the installed debug app opens successfully on device.
- **No remaining Android-only active implementation gap in these buckets**: remaining work is manual/device validation and any fixes that validation uncovers.
- **Not independent work items**: Multi-Mac merge commits (#6/#9/#10/#11/#14/#25/#82/#116/#126/#137/#138) close only through the owning Multi-Mac validation pass.
- **Out of scope**: the removed broad refactor/sidebar visual-parity bucket (#85/#86/#92) is intentionally not part of this roadmap.
- **Rootless older rows normalized**: #89/#90/#91/#122 now track the same draft-first/rootless validation pass as #117/#118 instead of separate stale implementation gaps.

## Commit Ledger

| # | Commit | Parent change | Android parity status | Tracking note |
|---:|---|---|---|---|
| 1 | `5b12f87d` | Add multi-Mac switching groundwork | Needs verification | Android has trusted-Mac registry, My Devices/switch UI, trusted resolve, and Mac-scoped local state; validate two-Mac switching on device. |
| 2 | `bf60c03b` | Fix multi-Mac switch state isolation | Needs verification | Message history, normal drafts, AI change sets, active thread, runtime selection, thread metadata, worktrees, and deleted/archived IDs are Mac-scoped; validate no cross-Mac leakage. |
| 3 | `1b2a9b5b` | Stabilize multi-Mac test coverage and service teardown | Needs verification | Android has secure transport tests and scoped switch/teardown paths; live two-device teardown validation remains. |
| 4 | `3be046ca` | Fix multi-Mac switching flow and My Macs UI | Needs verification | Android My Devices sheet, sidebar switcher, and switch lifecycle implemented; validate on device. |
| 5 | `8dc8ec23` | Add cancellable multi-Mac switch recovery | Needs verification | Android `cancelDeviceSwitch()` clears partial relay state and shows recovery notice; validate on device. |
| 6 | `a7ec222c` | Merge origin/main into feature/multi-mac | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 7 | `2a86bdcb` | Pin relay Node and npm versions for Zeabur | Needs verification | Hosted/deploy packaging item; decide whether Android tracking needs an equivalent local-only note. |
| 8 | `42a82f29` | Pin relay Node and npm versions for Zeabur | Needs verification | Duplicate packaging pin; not a direct Android feature. |
| 9 | `a20aca3f` | Merge github.com/main into feature/multi-mac | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 10 | `b396eda2` | Merge origin/main into feature/multi-mac | Needs verification | Empty/merge maintenance; closes with the owning Multi-Mac validation pass. |
| 11 | `fbf65c57` | Merge github.com/main into feature/multi-mac | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 12 | `37035d74` | Add unsigned IPA workflow | Needs verification | iOS CI artifact; no Android parity action unless Android release workflow is compared separately. |
| 13 | `2eea6eb4` | Extend relay watchdog and relaunch launch agent cleanly | Ported | Bridge/launch-agent behavior is present in shared `phodex-bridge`; Android consumes bridge state indirectly. |
| 14 | `4335a1c8` | Merge github.com/main into feature/multi-mac | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 15 | `0e838d80` | Compact older relay history before trimming newest turns | Ported | Shared bridge history compaction is present. |
| 16 | `8124de3d` | Run unsigned IPA build on main pushes | Needs verification | iOS CI workflow only; no Android parity behavior. |
| 17 | `82c09032` | Track assistant phases and workspace checkpoints | Ported | Android has workspace checkpoint and assistant revert services/tests. |
| 18 | `6f3f20a1` | Merge PR #79 unsigned IPA workflow | Needs verification | iOS packaging workflow only. |
| 19 | `445c5956` | Merge PR #91 git issues | Ported | Bridge/Android git issue fixes were covered by checkpoint/revert and action handling. |
| 20 | `1d2f232f` | Raise relay thread payload soft limit to 4 MB | Ported | Shared bridge relay payload behavior is present. |
| 21 | `c7843aa8` | Polish turn message rendering | Needs verification | Android has rich message rendering, but visual parity needs device review. |
| 22 | `7c82966a` | Add folder search to local project picker | Ported | Android project picker supports directory search through `ProjectFolderService`. |
| 23 | `cc9dce62` | Merge adjacent file changes and refine reconnect messaging | Needs verification | Android has file-change grouping and reconnect handling; copy/visual parity still needs device review. |
| 24 | `0b09031d` | Clean up multi-Mac merge fallout | Needs verification | Multi-Mac cleanup is represented by scoped state/recovery paths; validate against the main two-Mac scenario. |
| 25 | `ad00f7ab` | Merge github.com/main into feature/multi-mac | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 26 | `33f05c3f` | Fix trusted reconnect recovery | Needs verification | Android has trusted reconnect foundations and scoped recovery restore; multi-device recovery behavior still needs live validation. |
| 27 | `e1b1f180` | Merge PR #94 connectivity | Needs verification | Android has reconnect/recovery paths; foreground/background behavior needs device verification. |
| 28 | `e678be33` | Stabilize local usage and scroll state handling | Ported | Android has usage/context refresh and timeline scroll state handling. |
| 29 | `c605f8d9` | Return context section view explicitly | Ported | Android has context/usage sections in composer and status surfaces. |
| 30 | `131f8bbd` | Refine diff and image preview handling | Needs verification | Android has diff/image support plus `workspace/readFile` text preview from timeline links; validate linked text previews and bridge errors. |
| 31 | `6b922201` | Bridge + relay: iOS with Windows host | Ported | Shared bridge/relay Windows host compatibility is present; Android host validation still needed. |
| 32 | `38f2fa6a` | Add stacked git publish actions and progress toast | Ported | Android has git stacked actions/progress handling. |
| 33 | `98242063` | Add desktop IPC action following to bridge | Ported | Shared bridge IPC action follower is present. |
| 34 | `5eb42285` | Forward desktop file-read approvals | Ported | Shared bridge forwards file-read approvals; Android approval UI exists. |
| 35 | `73cb1f81` | Merge PR #96 proceed-with-idea | Ported | Desktop IPC/proceed behavior is bridge-side and present. |
| 36 | `993bd1eb` | Allow image reads from non-git workspace CWDs | Ported | Shared bridge and Android image preview service support workspace image reads. |
| 37 | `1c8744ba` | Merge PR #95 iOS Windows bridge relay | Ported | Shared bridge Windows compatibility is present. |
| 38 | `ff2196ce` | Fix desktop handoff RPC for Windows bridge | Ported | Bridge/Desktop handoff fixes are present; Android should still be tested against Windows host. |
| 39 | `4a033c93` | Harden Windows handoff and pairing output | Ported | Shared bridge/pairing behavior is present. |
| 40 | `d4af4c7e` | Update desktop handoff unsupported platform copy | Needs verification | User-facing copy parity needs Android UI review. |
| 41 | `d355a1c4` | Add slash compact command and keep file changes last | Ported | Android has slash command handling and file-change ordering logic. |
| 42 | `38280720` | Merge PR #97 windows bridge | Ported | Shared bridge Windows compatibility is present. |
| 43 | `ea0d45fd` | Refine turn header and diff markdown handling | Needs verification | Android has equivalent renderers; visual/markdown parity needs review. |
| 44 | `fd73ff49` | Add customizable user bubble colors | Ported | Android has `UserBubbleColor`, settings persistence, and palette tests. |
| 45 | `23284ad1` | Add local pet companion overlay and bridge handlers | Needs verification | Android pet store/service/overlay/settings implemented; bridge handlers shared; validate atlas rendering on device. |
| 46 | `8bc24a64` | Remove generated bubble color artifact | Ported | Artifact removal has no Android feature gap; user bubble colors remain ported. |
| 47 | `668400c4` | Merge PR #99 user bubble colors | Ported | Android user bubble colors are implemented. |
| 48 | `c80f1ff3` | Expand bubble colors and split settings cards | Ported | Android settings exposes user bubble preferences. |
| 49 | `84a19cb0` | Reject oversized pet spritesheets | Needs verification | Bridge validation shared; Android surfaces bridge error in pet settings/overlay; validate oversized pet rejection on device. |
| 50 | `4da9dd65` | Merge PR #100 pet companion overlay | Needs verification | Android pet companion bucket implemented; validate on device. |
| 51 | `9ea2c2b4` | Add live progress and success to git actions | Ported | Android git action progress/success UI is implemented. |
| 52 | `aea3b3b3` | Add manual pairing path to onboarding | Ported | Android onboarding supports QR/manual pairing paths. |
| 53 | `26405ad5` | Add paginated thread history loading | Ported | Android has thread history pagination support. |
| 54 | `296d6912` | Increase initial thread history page size | Ported | Android history sync consumes current bridge pagination behavior. |
| 55 | `0ceda7fb` | Pin Textual and add markdown rendering regression test | Needs verification | iOS dependency/test change; Android has separate markdown renderer/tests. |
| 56 | `c93a4739` | Defer thread baseline reads until needed | Ported | Android defers history/thread sync work in current service flow. |
| 57 | `67c28b0a` | Hydrate thread metadata from full list responses | Ported | Android thread-list sync hydrates metadata from list pages. |
| 58 | `8208d371` | Add local relay URL override | Ported | Android uses explicit/saved relay pairing and local-first relay configuration. |
| 59 | `6d0e7eb8` | Improve thread sync bootstrap and relay turns sanitization | Ported | Shared bridge sanitization and Android bootstrap sync are present. |
| 60 | `18553b72` | Merge PR #102 local relay URL option | Ported | Local relay option aligns with Android local-first pairing. |
| 61 | `1108f4b3` | Fix large turns list relay loading | Ported | Shared bridge adaptive history loading is present. |
| 62 | `89403cc9` | Adapt turns list relay pagination | Ported | Shared bridge pagination adaptation is present. |
| 63 | `436d68e7` | Merge PR #103 fix turns list timeout | Ported | Shared bridge turns-list timeout/pagination behavior is present. |
| 64 | `a251a94a` | Refine sidebar project icons and spacing | Needs verification | Android sidebar exists, but visual parity needs review. |
| 65 | `acf8a0de` | Handle unmaterialized thread history as empty state | Ported | Android handles missing/unmaterialized history as empty/recoverable state. |
| 66 | `bc250848` | Defer running-thread hydration and quiet recovery noise | Ported | Android has running-thread hydration and quiet recovery handling. |
| 67 | `d5241155` | Refine timeline hydration and skill autocomplete | Ported | Android has timeline hydration and skill autocomplete support. |
| 68 | `b50f016d` | Show completed plan items inline until turn resolves | Ported | Android has plan accessory/timeline handling. |
| 69 | `583e2241` | Set runtime defaults and quiet noisy turn UI | Ported | Android has runtime defaults and turn UI noise filtering. |
| 70 | `3cd7a561` | Reset runtime defaults and add file diff drilldown | Ported | Android has runtime defaults reset and repo diff/drilldown surfaces. |
| 71 | `84450c2d` | Add offer code redemption and relay health metrics | Needs verification | Android subscription/redeem abstraction and relay `/health` consumer added; bridge metrics shared; validate configured builds on device. |
| 72 | `74128c1c` | Add ordered batch reverts for fallback patches | Ported | Android assistant change-set revert service covers ordered revert behavior. |
| 73 | `d5c903ec` | Unify runtime error handling and fresh-thread recovery | Ported | Android has unified service error/recovery paths. |
| 74 | `e55e895e` | Keep runtime pill loading until selection hydrates | Ported | Android runtime selector/loading state exists. |
| 75 | `d74f22c2` | Sync inline git push progress with timeline rows | Ported | Android syncs git push progress into timeline/action UI. |
| 76 | `6c174737` | Update what is new release notes for v1.5 | Needs verification | iOS release-note content; Android release notes should be reviewed separately. |
| 77 | `4a9306dc` | Add Remodex CLI v1.5.0 to What is New | Needs verification | iOS release-note content; Android release notes should be reviewed separately. |
| 78 | `160d697a` | Add beta badge to Companion Pet toggle | Needs verification | Android settings pet toggle includes Beta badge; validate on device. |
| 79 | `207d5da4` | Show loading state for newly sent turns | Ported | Android has newly sent/running turn loading states. |
| 80 | `287f07e3` | Add dismissible error report card to turn timeline | Ported | Android has turn error report card handling. |
| 81 | `4bf799fd` | Include CLI version in feedback email | Needs verification | Feedback payload/copy should be compared separately. |
| 82 | `b2e95bc4` | Merge github.com/main into feature/multi-mac | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 83 | `af4622ce` | Merge PR #63 multi-Mac | Needs verification | Multi-Mac Android feature work is implemented through trusted-device switching and Mac-scoped state; validate end to end. |
| 84 | `e2cb8aab` | Preserve runtime selections and support mention-only turns | Ported | Android supports runtime selection preservation and mention payloads. |
| 85 | `3f865d22` | Huge refactor | Out of scope | Broad refactor/sidebar visual-parity bucket was cancelled for this pass; covered behavior should be judged by the concrete feature buckets. |
| 86 | `c6bc4d1a` | Merge PR #120 huge refactor | Out of scope | Same as huge-refactor bucket; not a single Android feature closure for this roadmap. |
| 87 | `72c45a5f` | Handle permission approvals and Spark summary fallback | Ported | Android approval UI and bridge Spark summary compatibility are present. |
| 88 | `60acb85b` | Refactor bridge status handling and model loading state | Ported | Shared bridge status publisher and Android runtime loading UI are present. |
| 89 | `b47ff435` | Support projectless Quick Chat threads | Needs verification | Android can start `cwd = null` chats through draft-first New Chat; validate no thread is created until first send. |
| 90 | `d1e82bcc` | Support projectless Quick Chat threads | Needs verification | Bridge/transport projectless support is present and Android New Chat is draft-first; validate rootless send-once flow. |
| 91 | `db6005be` | Merge PR #121 rootless chats | Needs verification | Rootless chat foundations and draft-first New Chat are present; validate alongside #117/#118. |
| 92 | `a7d993e6` | Add top sidebar action row and support relay subpaths | Out of scope | Relay subpaths are bridge-side; top sidebar visual parity is part of the cancelled UI bucket. |
| 93 | `a5d3b655` | Add native SSH terminal support | Ported | Android has SSH terminal route, profiles, SSHJ client, known-host verifier, and tests. |
| 94 | `f5ac30f5` | Keep iOS relay sockets alive while foregrounded | Needs verification | Android reconnect exists, but foreground socket lifecycle needs device validation. |
| 95 | `1f7f1d66` | Align local-first guardrails with repo conventions | Needs verification | iOS terminal UX polish/local-first guardrail alignment; Android terminal UX needs review. |
| 96 | `c46e5425` | Fix iOS test target compile errors | Needs verification | iOS test-only compile fix; no Android parity behavior. |
| 97 | `277404dc` | Merge PR #127 SSH terminal | Ported | Android terminal implementation exists. |
| 98 | `c86cd618` | Optimize timeline caching for large message text | Ported | Android has timeline render caches and large-message handling. |
| 99 | `3ece8202` | Persist composer drafts and improve terminal selection | Needs verification | Android persists normal composer drafts per Mac/thread via repository storage and clears after successful send; validate restart and Mac/thread isolation. |
| 100 | `c0498c21` | Preserve Windows compatibility for bridge and relay | Ported | Shared bridge Windows compatibility is present. |
| 101 | `0f8f8b39` | Mirror desktop assistant deltas over IPC | Ported | Shared bridge desktop IPC delta mirroring is present. |
| 102 | `75b57235` | Refine adaptive glass and composer view composition | Needs verification | Android UI differs; visual parity needs design/device review. |
| 103 | `9eb3dffa` | Refactor compact sidebar navigation | Needs verification | Android sidebar navigation exists but needs visual/flow parity review. |
| 104 | `a1dbef95` | Refine sidebar layout and expand project controls | Needs verification | Android project controls exist; sidebar visual parity needs review. |
| 105 | `92828d37` | Polish mobile sidebar, composer, and timeline | Needs verification | Android has corresponding surfaces; polish parity needs device review. |
| 106 | `482ce7a3` | Merge PR #124 iOS foreground websocket keepalive | Needs verification | Android reconnect exists; foreground lifecycle parity needs device validation. |
| 107 | `eba26446` | Restore old flash icons in composer runtime menus | Needs verification | Android runtime menu icon parity needs visual review. |
| 108 | `71c2964f` | Match composer icon sizes and restore SF Symbol flash | Needs verification | iOS icon/widget changes; Android icon sizing parity needs UI review. |
| 109 | `24ecd742` | Address Windows bridge review findings | Ported | Shared bridge review fixes are present. |
| 110 | `a8224910` | Use explicit bridge stop in IPC integration test | Ported | Shared bridge test helper/stop behavior is present. |
| 111 | `d4cd1467` | Smooth streaming timeline updates | Ported | Android has streaming reveal/shimmer/timeline update handling. |
| 112 | `603dfc52` | Migrate markdown rendering to RemodexTextKit | Needs verification | Android uses a different markdown renderer; output parity needs comparison. |
| 113 | `8dad8b41` | Use accent color for markdown links | Ported | Android markdown renderer supports link color/theming. |
| 114 | `436d0bc7` | Coalesce thread list fetches during sync and refresh | Ported | Android has thread-list sync pagination/coalescing behavior. |
| 115 | `04a20fd1` | Merge PR #125 iOS test target compile errors | Needs verification | iOS test-only merge. |
| 116 | `67058278` | Merge origin/main into dpcode/multiple-macos | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 117 | `20f21aba` | Add draft-first new chat flow in the sidebar | Needs verification | Android `NewChatDraft` route defers `thread/start` until first send; validate on device. |
| 118 | `9f565d04` | Add git and terminal actions to new chat drafts | Needs verification | Draft screen gates git actions on project path and exposes terminal-here cd hint; validate on device. |
| 119 | `ba5c5964` | Stabilize composer mentions and timeline anchoring | Ported | Android has mention chips/autocomplete and timeline anchoring behavior. |
| 120 | `a27ba90b` | Refine thread actions and pending state handling | Ported | Android has thread action/pending state handling. |
| 121 | `aa61d3e9` | Compact composer secondary controls and status indicators | Needs verification | Android composer controls exist; compact visual parity needs UI review. |
| 122 | `91e63702` | Make general chat rootless and tighten relay stale detection | Needs verification | Rootless general chat uses the draft-first New Chat flow and shared relay stale detection; validate rootless send-once behavior. |
| 123 | `ec08f635` | Merge PR #133 Windows bridge relay compatibility | Ported | Shared bridge compatibility is present. |
| 124 | `5ca0dd2e` | Bump bridge package to 1.5.4 | Ported | Shared bridge version bump is present in assessment tree. |
| 125 | `4d9a5158` | Add inline markdown rendering to user bubbles | Ported | Android renders user bubble markdown/links. |
| 126 | `46bd43ec` | Merge GitHub main into multiple mac support | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 127 | `4ce0dc1f` | Support rootless chats and pause terminal rendering | Needs verification | Android has rootless chat and terminal route; terminal pause/render lifecycle needs verification. |
| 128 | `ed052c3f` | Bump bridge package to 1.5.5 | Ported | Shared bridge version bump is present. |
| 129 | `ecd5b8d9` | Bump bridge package to 1.5.5 | Ported | Shared bridge version bump is present. |
| 130 | `cf70af17` | Add workspace file previews | Needs verification | Android supports image previews and text previews via `workspace/readFile`; validate text/code links, cache reuse, and errors. |
| 131 | `90e8389c` | Fix bridge IPC integration test cleanup | Ported | Shared bridge test cleanup/stop behavior is present. |
| 132 | `73177efc` | Merge main into file preview branch | Needs verification | File preview branch behavior is represented by Android text/image preview services; validate `workspace/readFile` on device. |
| 133 | `0948c69d` | Merge PR #138 workspace file previews | Needs verification | Image preview is present and text preview is implemented; validate timeline path-link preview behavior. |
| 134 | `6fa0b9f5` | Fix voice transcription fallback compatibility | Needs verification | Android calls bridge-owned `voice/transcribe` first and falls back to legacy direct upload only when unsupported; validate current and old bridge behavior. |
| 135 | `accb9316` | Add shimmer thinking indicator to streaming assistant rows | Ported | Android has shimmer and thinking row support. |
| 136 | `186c6e08` | Refine streaming assistant thinking indicator | Ported | Android has streaming/thinking presentation support. |
| 137 | `17cf5268` | Merge origin/main into multiple Mac support | Needs verification | Merge maintenance for multi-Mac branch; closes with the owning Multi-Mac validation pass. |
| 138 | `e6021a40` | Merge PR #137 multiple macOS | Needs verification | Multi-Mac branch merge; closes with the owning Multi-Mac validation pass. |
| 139 | `7977212d` | Restore device switching overlay in the sidebar | Needs verification | Android sidebar device switcher + switching overlay in My Devices sheet; validate on device. |
| 140 | `03386eef` | Update app icon assets | Needs verification | iOS app icon assets; Android branding assets should be compared separately. |
| 141 | `10e89240` | Improve foreground reconnect and wake recovery | Needs verification | Android has foreground reconnect/wake recovery foundations and scoped active-thread restore; device behavior needs validation. |
| 142 | `6c2e3567` | Honor REMODEX_ALLOWED_ROOTS in project quick locations | Ported | Shared bridge quick locations honor allowed roots; Android consumes `project/quickLocations`. |
| 143 | `08150d38` | Add device connections management and auto-hide switcher | Needs verification | Android Connections sheet with hide/show toggles and auto-hide switcher when only one useful choice; validate on device. |
| 144 | `eebb5788` | Merge PR #142 multiple Macs/devices | Needs verification | Secure registry, device switcher, and Mac-scoped app state are implemented; validate against the multi-device scenario. |
| 145 | `9c8a6936` | Restore selection after failed saved Mac switch | Needs verification | Android failed-switch path keeps selected Mac presentation and restores scoped state; validate on device. |
| 146 | `7797abea` | Defer runtime option refresh until thread hydration completes | Ported | Android preserves runtime/hydration sequencing in current service flow. |
| 147 | `bd3e5d2d` | Remove leftover typing indicator shimmer | Needs verification | Android shimmer/thinking indicators should be visually reviewed against final iOS behavior. |
| 148 | `e63cf05c` | Improve history tool call readability | Ported | Android has command/tool-call humanization and timeline render cache coverage. |

## Android Evidence Pointers

- Workspace checkpoints/reverts: `android/app/src/main/kotlin/com/remodex/mobile/services/WorkspaceCheckpointService.kt`, `android/app/src/main/kotlin/com/remodex/mobile/services/AiChangeSetRevertService.kt`
- Project picker/rootless chat grouping: `android/app/src/main/kotlin/com/remodex/mobile/ui/sidebar/SidebarScreen.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/sidebar/SidebarThreadGrouping.kt`
- Terminal: `android/app/src/main/kotlin/com/remodex/mobile/terminal/TerminalScreen.kt`, `android/app/src/main/kotlin/com/remodex/mobile/terminal/SshjTerminalClient.kt`, `android/app/src/main/kotlin/com/remodex/mobile/terminal/TerminalPersistence.kt`
- Workspace image/text previews: `android/app/src/main/kotlin/com/remodex/mobile/services/WorkspaceImageService.kt`, `android/app/src/main/kotlin/com/remodex/mobile/services/WorkspaceTextFileService.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/turn/TurnAttachmentViews.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/turn/WorkspaceTextFilePreviewDialog.kt`
- Voice: `android/app/src/main/kotlin/com/remodex/mobile/services/CodexServiceVoice.kt`, `android/app/src/main/kotlin/com/remodex/mobile/core/voice/GptVoiceTranscriptionClient.kt`
- Trusted Mac foundations: `android/app/src/main/kotlin/com/remodex/mobile/core/model/SecureTransportModels.kt`, `android/app/src/main/kotlin/com/remodex/mobile/services/CodexServiceSecureTransport.kt`
- My Devices / device switching: `android/app/src/main/kotlin/com/remodex/mobile/ui/mydevices/`, `android/app/src/main/kotlin/com/remodex/mobile/services/CodexServiceDeviceSwitch.kt`, `android/app/src/main/kotlin/com/remodex/mobile/services/CodexTrustedSessionResolveClient.kt`, `android/app/src/main/kotlin/com/remodex/mobile/core/persistence/MacScopedSessionStore.kt`
- Pet companion: `android/app/src/main/kotlin/com/remodex/mobile/core/model/PetCompanionModels.kt`, `android/app/src/main/kotlin/com/remodex/mobile/data/PetCompanionStore.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/pet/PetCompanionOverlay.kt`
- Offer code + relay health: `android/app/src/main/kotlin/com/remodex/mobile/services/SubscriptionService.kt`, `android/app/src/main/kotlin/com/remodex/mobile/services/RelayHealthClient.kt`, `android/app/src/main/kotlin/com/remodex/mobile/core/model/RelayHealthModels.kt`
- Draft-first New Chat: `android/app/src/main/kotlin/com/remodex/mobile/ui/draft/NewChatDraftScreen.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/draft/NewChatDraftModels.kt`
- Composer draft persistence: `android/app/src/main/kotlin/com/remodex/mobile/ui/turn/TurnConversationPane.kt`, `android/app/src/main/kotlin/com/remodex/mobile/core/persistence/MacScopedSessionStore.kt`
- Markdown/timeline: `android/app/src/main/kotlin/com/remodex/mobile/ui/turn/TurnMarkdownBody.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/turn/TurnMessageRow.kt`, `android/app/src/main/kotlin/com/remodex/mobile/ui/turn/TurnTimelineRenderCaches.kt`

## Maintenance Rules

- When `upstream/main` advances, append new commits to this ledger before porting.
- Do not mark **Ported** from API shape alone when the feature is visible UI; use **Needs verification** until Android device review confirms behavior and visual parity.
- Keep bridge-only commits marked **Ported** only when the current Android assessment tree uses the updated shared `phodex-bridge`.
- For multi-Mac, pet companion, payments, draft-first New Chat, workspace text preview, and bridge-owned voice transcription, move **Needs verification** rows to **Ported** only after the linked checklist item passes.
