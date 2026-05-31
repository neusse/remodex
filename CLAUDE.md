# CLAUDE.md (Local-First)

Keep this file and `AGENTS.md` aligned.

This repo is local-first now. Do not reintroduce hosted-service assumptions, remote deployment runbooks, or hardcoded production domains.

## Agent behavioral guidelines

Behavioral guidelines to reduce common LLM coding mistakes. These bias toward caution over speed; for trivial tasks, use judgment.

### 1. Think Before Coding

Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — do not pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

For this repo: when the next verification step is safe and obvious (local code, protocol/schema, official sources), inspect first instead of asking — see Core guardrails.

### 2. Simplicity First

Minimum code that solves the problem. Nothing speculative.

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.
- Ask: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

Aligns with Core guardrails: avoid junk code, placeholder hacks, and noisy one-off workarounds.

### 3. Surgical Changes

Touch only what you must. Clean up only your own mess.

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — do not delete it.

When your changes create orphans:

- Remove imports/variables/functions that your changes made unused.
- Do not remove pre-existing dead code unless asked.

The test: every changed line should trace directly to the user's request.

For this repo: prefer targeted edits over drive-by refactors; put shared logic in services/coordinators instead of duplicating in views; do not add one-off report markdown unless the user asks — see Core guardrails and Build guardrails.

### 4. Goal-Driven Execution

Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

- "Add validation" → write tests for invalid inputs, then make them pass.
- "Fix the bug" → write a test that reproduces it, then make it pass.
- "Refactor X" → ensure tests pass before and after.

For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

For this repo: do not run Xcode tests unless the user explicitly asks. For small iOS/mobile fixes, prefer inspection and targeted edits over simulator runs — see Build guardrails.

These guidelines are working if: fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## Core guardrails

- Prefer local Mac runtime, local bridge, QR pairing, and daemon workflows.
- Be an intraprendente agent: proactively inspect local code, protocol/schema, and official sources to confirm facts before replying; do not repeatedly stop to ask for confirmation when the next verification step is safe and obvious.
- Keep repo isolation by thread/project metadata and local `cwd`.
- Do not reintroduce filtering by selected repo in sidebar/content.
- Keep cross-repo open/create flow with automatic local context switch.
- Preserve single responsibility: shared logic belongs in services/coordinators, not duplicated in views.
- Treat this repo as open source: avoid junk code, placeholder hacks, noisy one-off workarounds, and low-signal docs.
- If you touch docs, keep them local-only and remove stale hosted-service notes instead of adding compatibility layers.
- Do not create one-off report markdown files in the repo root (security reports, audit notes, scratch summaries, etc.) unless the user explicitly asks for a file. Keep ad-hoc analysis in the chat.
- For open-source/self-hosted safety, do not log live relay `sessionId` values or other bearer-like pairing identifiers in server logs; redact or hash them instead.
- Keep user-facing answers compact by default unless the user explicitly asks for more detail.

## iOS runtime + timeline guardrails

- `turn/started` may not include a usable `turnId`: keep the per-thread running fallback.
- If Stop is tapped and `activeTurnIdByThread` is missing, resolve via `thread/read` before interrupting.
- On reconnect/background recover, rehydrate active turn state so Stop remains visible.
- Suppress benign background disconnect noise (`NWError.posix(.ECONNABORTED)`) and retry on foreground.
- Keep assistant rows item-scoped to avoid timeline flattening/reordering.
- Merge late reasoning deltas into existing rows; do not spawn fake extra "Thinking..." rows.
- Ignore late turn-less activity events when the turn is already inactive.
- Preserve item-aware history reconciliation instead of falling back to `turnId`-only matching.

## Local connection guardrails

- Prefer saved relay pairing and local connection state as the source of truth.
- Avoid hardcoded remote domains; default to local values or explicit user config.
- Keep pairing/auth UX stable: do not clear saved relay info too early during reconnect flows.
- Preserve reconnect behavior across relaunch when the local host session is still valid.
- Preserve the QR/local-relay pairing path: do not regress the scanner -> saved pairing -> connect flow by letting onboarding/auto-reconnect race manual scan control.
- For local relay recovery, keep resumed desktop-thread live mirroring and rollout fallback logic intact so reopened/running threads still recover state even when the rollout file is older than the recent-candidate window.

## Build guardrails

- Do not run Xcode tests unless the user explicitly asks. Do not decide to run them on your own.
- Markdown files inside Xcode-synced groups can still produce harmless warnings.
- For small iOS/mobile fixes, prefer inspection and targeted edits over simulator runs by default.

## Local quick runbook

```bash
cd phodex-bridge
npm start
```
