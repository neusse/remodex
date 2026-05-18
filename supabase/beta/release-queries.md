# Beta Release Queries

Use these SQL snippets in the Supabase SQL editor when publishing a new Android beta build or updating Tester HQ missions.

## Current Model

`beta_builds` does not use an `is_current` column. The Edge Function chooses the build like this:

1. If the app sends `app_version`, it loads the matching `beta_builds.app_version`.
2. If there is no exact match, it falls back to the most recent active build where:
   - `active_from <= now()`
   - `active_until is null` or `active_until >= now()`

For a new Play release, always create a matching `beta_builds` row for the new `versionName`.

## Add A New Beta Build

Replace `0.1.5` and the text arrays with the new release content.

```sql
insert into beta_builds (
  app_version,
  changelog,
  today_test,
  known_issues,
  reward_copy,
  active_from,
  active_until
)
values (
  '0.1.5',
  array[
    'Added Tailscale support for easier local-first connectivity outside the LAN',
    'Added the first Android terminal add-on flow with native SSH support, saved profiles, trusted hosts, multiple sessions, setup help, input handling, and terminal UI wiring',
    'Upgraded remodex-host to 0.3.0',
    'Added a host diagnostics panel',
    'Added a new animated pet asset set with improved customization/setup assets',
    'Improved setup customization and bundled host configuration',
    'Improved Windows bridge compatibility, plan UI behavior, and Android/host runtime validation'
  ],
  array[
    'Open Tester HQ',
    'Check the 0.1.5 wired missions',
    'Test one local connection or conversation flow',
    'Send one useful feedback message'
  ],
  array[
    'Beta leaderboard may update after refresh'
  ],
  'Top 30 useful beta contributors will receive 1 free month after public release. Points help us track participation, but final selection also considers useful feedback, confirmed bugs, and testing quality. Reviews and ratings are never required or rewarded.',
  now(),
  null
)
on conflict (app_version) do update set
  changelog = excluded.changelog,
  today_test = excluded.today_test,
  known_issues = excluded.known_issues,
  reward_copy = excluded.reward_copy,
  active_from = excluded.active_from,
  active_until = excluded.active_until;
```

## Close Older Active Builds

Use this if you want fallback `/beta/hq` calls to resolve only to the latest build.

```sql
update beta_builds
set active_until = now()
where app_version <> '0.1.5'
  and active_until is null;
```

## Add Missions For A Build

Mission points are displayed by the app, but awarded only by the Edge Function rules. Android never sends point values.

This focused `0.1.5` set uses no more than five missions and emphasizes the newly introduced Terminal add-on. Each mission is either `auto-core` or has an Android hook plus backend event support.

```sql
insert into beta_missions (
  id,
  app_version,
  title,
  description,
  points,
  sort_order,
  active
)
values
  ('0.1.5-open', '0.1.5', 'Open latest beta', 'Open the newest Android beta build and visit Tester HQ.', 15, 10, true),
  ('0.1.5-terminal-open', '0.1.5', 'Open Terminal add-on', 'Open the Android Terminal add-on from the sidebar and verify the profile list or empty state is understandable.', 20, 20, true),
  ('0.1.5-terminal-profile', '0.1.5', 'Save a terminal profile', 'Create or edit a terminal SSH profile and report confusing labels, key validation, or setup issues.', 35, 30, true),
  ('0.1.5-terminal-connect', '0.1.5', 'Try terminal SSH connect', 'Start an SSH connection from a saved terminal profile and report host trust, passphrase, auth, or network issues.', 40, 40, true),
  ('0.1.5-feedback', '0.1.5', 'Send beta feedback', 'Send one useful feedback message from Tester HQ after checking the 0.1.5 Terminal add-on.', 40, 50, true)
on conflict (id) do update set
  app_version = excluded.app_version,
  title = excluded.title,
  description = excluded.description,
  points = excluded.points,
  sort_order = excluded.sort_order,
  active = excluded.active;
```

## Route-Ready Missions

Do not enable route-ready catalog missions for `0.1.5` unless the matching Android hook has landed. They are supported by `/beta/mission-event`, but will stay pending on device until the app reliably sends the event.

```sql
-- Example: keep a route-ready mission hidden until Android sends the event.
update beta_missions
set active = false
where id = '0.1.5-design-mode-open';
```

## Disable A Mission

```sql
update beta_missions
set active = false
where id = '0.1.5-command-card';
```

## Copy Missions From Previous Build

Use this when a new build keeps the same mission structure.

```sql
insert into beta_missions (
  id,
  app_version,
  title,
  description,
  points,
  sort_order,
  active
)
select
  replace(id, '0.1.4', '0.1.5') as id,
  '0.1.5' as app_version,
  title,
  description,
  points,
  sort_order,
  active
from beta_missions
where app_version = '0.1.4'
on conflict (id) do update set
  app_version = excluded.app_version,
  title = excluded.title,
  description = excluded.description,
  points = excluded.points,
  sort_order = excluded.sort_order,
  active = excluded.active;
```

## Check What Tester HQ Will Show

```sql
select *
from beta_builds
where app_version = '0.1.5';

select id, title, description, points, sort_order, active
from beta_missions
where app_version = '0.1.5'
order by sort_order asc;
```

## Version Checklist

1. Bump Android `versionName`.
2. Build and upload the new AAB.
3. Insert or update the matching `beta_builds` row.
4. Add missions for the same `app_version`.
5. Close old active builds if needed.
6. Open Tester HQ on device and tap refresh.
