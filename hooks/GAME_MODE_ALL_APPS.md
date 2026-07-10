# Working Space — Game Mode on All Apps

Follow-up series to `[N/4] Workspace: build workspace feature`. This change
makes Performance / Battery / Standard modes apply to **any app registered in
Working Space**, not only packages that declare `android:appCategory="game"`
or game-mode metadata in their manifest.

| # | Repo | Title | Role |
|---|------|-------|------|
| 1/2 | `vendor/bmobile/workingspace` | `[1/2] Workspace: game mode for all apps` | App-side DeviceConfig interventions + session apply |
| 2/2 | `frameworks/base` | `[2/2] Workspace: game mode for all apps` | GameManagerService availability + default config |

Settings (`packages/apps/Settings`) and product inherit (`vendor/lineage`) are
unchanged by this series — they already ship from the original `[N/4]` work.

---

## Problem

After the initial Working Space land, session overlay and Focus mode worked for
registered non-game apps (e.g. a music player), but **Performance / Battery
felt cosmetic**:

1. **Wrong config store.** `GameModeUtils.setIntervention()` wrote a single
   `Settings.Secure` key `"game_overlay"` as `"pkg;;config"`. Platform
   `GameManagerService` reads per-package strings from
   `DeviceConfig.NAMESPACE_GAME_OVERLAY` (`key = packageName`). The Secure
   write never reached GMS.
2. **Availability gate.** `SessionService.applyGameModeConfig()` only called
   `GameManager.setGameMode()` when `getAvailableGameModes()` contained the
   preferred mode. For apps without game config, GMS returned only
   `[STANDARD, CUSTOM]` — Performance (2) and Battery (3) were silently skipped.
3. **Incomplete registration path.** Only the legacy `AppListPreferences`
   path called `setIntervention()`. Compose hub / `SettingsViewModel.registerGame()`
   and unregister paths did not, so many apps never got an intervention string
   at all.
4. **No config → no interventions.** Even if `setGameMode()` succeeded,
   `updateInterventions()` early-returned when `getConfig()` was null, so
   downscale / FPS never applied.

`FocusListManager` already made listed packages count as games for
`isPackageGame()`, and `WorkingSpaceService` still sets
`persist.sys.power_mode_perf` for mode `2` — that path was never the bug.
The missing piece was **GameManager intervention config + mode apply**.

---

## Solution overview

### App (`vendor/bmobile/workingspace`) — Commit 1/2

| File | Change |
|------|--------|
| `utils/GameModeUtils.kt` | Write/delete interventions via `DeviceConfig.setProperty` / `deleteProperty` on `NAMESPACE_GAME_OVERLAY`. Add `syncInterventionsForRegisteredApps()` for boot-time repair of existing lists. |
| `gamebar/SessionService.kt` | Always `setGameMode(preferred)` for registered apps; ensure intervention config on session start (no `availableModes.contains` gate). |
| `ui/viewmodel/SettingsViewModel.kt` | Call `setIntervention(...)` on register / clear on unregister. |
| `ui/viewmodel/PerAppSettingsViewModel.kt` | Clear intervention on unregister. |
| `WorkingSpaceApp.kt` | On create, sync DeviceConfig for every entry in `workingspace_app_list`. |
| `AndroidManifest.xml` | Declare `WRITE_DEVICE_CONFIG` (app already has `sharedUserId=android.uid.system` + `READ_DEVICE_CONFIG`). |

Default intervention string (matches `GameConfig.ModeBuilder`):

```text
mode=2,downscaleFactor=0.7:mode=3,downscaleFactor=0.8
```

- Performance → 0.7× resolution scale (via CompatScale / WM downscale)
- Battery → 0.8× resolution scale

### Framework (`frameworks/base`) — Commit 2/2

| File | Change |
|------|--------|
| `GameManagerService.java` | If `FocusListManager.isListed(pkg)` and config is null, expose `[STANDARD, PERFORMANCE, BATTERY, CUSTOM]`. On `setGameMode`, `ensureListedAppInterventionConfig()` provisions the default DeviceConfig string when missing, then refreshes configs. |
| `GameManagerShellCommand.java` | Treat Working Space listed packages as games so `adb shell cmd game list-modes <pkg>` works for debugging. |

Only these two files are part of Commit 2/2. Other dirty files in
`frameworks/base` are unrelated WIP and must stay uncommitted.

---

## Data flow (after fix)

```text
User registers app in Working Space hub
        │
        ▼
Settings.System workingspace_app_list  (pkg=mode;...)
        │
        ├─► FocusListManager (system_server)  → isPackageGame() == true
        │
        └─► GameModeUtils.setIntervention()
                DeviceConfig game_overlay / <pkg> = mode=2,...:mode=3,...
                        │
                        ▼
                GameManagerService DeviceConfigListener
                        → updateConfigsForUser()
                        → GamePackageConfiguration active

Session starts (app foreground)
        │
        ▼
SessionService.applyGameModeConfig()
        → setIntervention (idempotent)
        → GameManager.setGameMode(pkg, preferred)
                → ensureListedAppInterventionConfig (framework safety net)
                → updateInterventions (FPS / ANGLE / scaling)
```

---

## Attribution

Working Space (and this follow-up) builds on prior open-source work. Credit
where due; this is **not** a claim of affiliation with those projects.

| Source | What we reused / adapted | License |
|--------|--------------------------|---------|
| **AxionAOSP GameSpace** | Session overlay architecture, game-bar tiles, per-app mode UX patterns, original GameSpace port baseline | Apache-2.0 (as shipped in Axion trees) |
| **Chaldeaprjkt / LibreMobileOS GameSpace lineage** | Early GameSpace / Game Mode intervention helpers (`GameModeUtils`, `GameConfig`, preference-era app list) | Apache-2.0 |
| **crDroid Android Project** | Historical GameSpace / session service contributions reflected in file headers | Apache-2.0 |
| **AOSP `GameManagerService`** | Platform Game Mode API, DeviceConfig `game_overlay` namespace, intervention / CompatScale pipeline | Apache-2.0 |
| **AppIntro** (vendored under `third_party/appintro`) | Onboarding pager used by Working Space intro | Apache-2.0 |
| **LineageOS / BMobile** | Product integration, SELinux wiring, Settings hub entry, QS tile host | Apache-2.0 |

**BMobile / LineageOS-specific work in this series** (not upstream Axion):

- Rebrand GameSpace → Working Space; Focus mode + QS tile; hidden-app filter
- Fix intervention path to DeviceConfig; apply modes to all listed apps
- Framework `FocusListManager` bridge + shell-command parity
- Docs under `hooks/` (`COMMIT_SERIES.md`, `HOST_INTEGRATION.md`,
  `VERIFICATION.md`, this file)

File-level copyright headers in the app tree retain historical authors where
the file originated; new/edited platform hooks use LineageOS Project years
as appropriate.

---

## Validation (after rebuild)

```bash
# Register a non-game app, set Performance (mode=2), then:
adb shell settings get system workingspace_app_list
adb shell device_config get game_overlay <package.name>
adb shell cmd game list-modes <package.name>
# Expect: standard,performance,battery,custom (and current mode)

# Launch the app:
adb shell settings get system workingspace_session_active   # → 1
# Optional: confirm power boost for mode 2
adb shell getprop persist.sys.power_mode_perf
```

Also re-run `hooks/adb_smoke_test.sh` and the checklist in `hooks/VERIFICATION.md`.

---

## Rollback

- App-only: revert Commit 1/2 in `vendor/bmobile/workingspace` and rebuild /
  push the APK. Modes may again skip for non-games; sessions/Focus still work.
- Framework: revert Commit 2/2 in `frameworks/base` (two files only) and
  rebuild `services`. Prefer `git revert` of the series commits; do not
  `git reset --hard` / `git clean` the tree.
- Clear leftover DeviceConfig keys if needed:
  `adb shell device_config delete game_overlay <package.name>`

---

## Related docs

- `hooks/COMMIT_SERIES.md` — original `[N/4]` feature series index
- `hooks/HOST_INTEGRATION.md` — per-repo file map for the feature
- `hooks/VERIFICATION.md` — adb functional checklist
- `hooks/COMMIT_SERIES_GAME_MODE.md` — this follow-up’s commit cross-links
