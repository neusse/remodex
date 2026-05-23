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

Replace `0.1.7` and the text arrays with the new release content.

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
  '0.1.7',
  array[
    'Fixed Android saved-pairing reconnect behavior and transient error auto-reconnect',
    'Improved empty-thread UI with a centered No messages state and themed icon',
    'Fixed citation rendering',
    'Added collapsible Searches UI and searching shimmer for active searches',
    'Added real mobile turn steering support',
    'Added PR #133 Windows bridge optimizations',
    'Reworked the active-turn composer with Stop, Send, staged steering, edit/delete, and Steer actions'
  ],
  array[
    'Open Tester HQ',
    'Check the 0.1.7 wired missions',
    'Try steering during an active turn',
    'Expand a Searches section on a response that used search',
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
where app_version <> '0.1.7'
  and active_until is null;
```

## Add Missions For A Build

Mission points are displayed by the app, but awarded only by the Edge Function rules. Android never sends point values.

This focused `0.1.7` set emphasizes the newly introduced steering, Git, project picker, onboarding, settings, and plan rendering flows. Each mission has Android hooks plus backend event support.

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
  ('0.1.7-steering', '0.1.7', 'Try mobile steering', 'While a turn is running, stage and send a steering message, then report anything confusing in the new controls.', 35, 10, true),
  ('0.1.7-mobile-pr-draft', '0.1.7', 'Create mobile PR draft', 'Use Commit + Push + PR or Create PR and verify PR creation/opening from the phone.', 35, 20, true),
  ('0.1.7-repo-diff-review', '0.1.7', 'Review repository diff', 'Open the repository diff from the Git header, switch scopes, expand files, and copy a path.', 30, 30, true),
  ('0.1.7-project-thread', '0.1.7', 'Create a new project thread', 'Use the project picker flow to choose workspace/runtime context and create a thread.', 30, 40, true),
  ('0.1.7-user-bubble', '0.1.7', 'Customize user bubble color', 'Change the user bubble color in Settings and verify the timeline uses it.', 15, 50, true),
  ('0.1.7-onboarding', '0.1.7', 'Complete onboarding setup', 'Walk through onboarding, copy setup commands if needed, and continue to QR scanning.', 25, 60, true),
  ('0.1.7-plan-rendering', '0.1.7', 'Validate plan rendering', 'Open plan details and verify progress, statuses, and rendered markdown are readable.', 25, 70, true)
on conflict (id) do update set
  app_version = excluded.app_version,
  title = excluded.title,
  description = excluded.description,
  points = excluded.points,
  sort_order = excluded.sort_order,
  active = excluded.active;
```

## Route-Ready Missions

Do not enable route-ready catalog missions for `0.1.7` unless the matching Android hook has landed. They are supported by `/beta/mission-event`, but will stay pending on device until the app reliably sends the event.

```sql
-- Example: keep a route-ready mission hidden until Android sends the event.
update beta_missions
set active = false
where id = '0.1.7-design-mode-open';
```

## Disable A Mission

```sql
update beta_missions
set active = false
where id = '0.1.7-command-card';
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
  replace(id, '0.1.6', '0.1.7') as id,
  '0.1.7' as app_version,
  title,
  description,
  points,
  sort_order,
  active
from beta_missions
where app_version = '0.1.6'
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
where app_version = '0.1.7';

select id, title, description, points, sort_order, active
from beta_missions
where app_version = '0.1.7'
order by sort_order asc;
```

## Version Checklist

1. Bump Android `versionName`.
2. Build and upload the new AAB.
3. Insert or update the matching `beta_builds` row.
4. Add missions for the same `app_version`.
5. Close old active builds if needed.
6. Open Tester HQ on device and tap refresh.
