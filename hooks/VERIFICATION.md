# Working Space — post-flash verification

Run the automated pass first:

```bash
vendor/bmobile/workingspace/hooks/adb_smoke_test.sh [device-serial]
```

It checks package install, the `working_space` system service, the app-side
binder service, session/focus/app-list settings keys, the focus-mode DND +
notification + QS tile cycle, SELinux denials, and crash logs — printing
PASS/FAIL per check and exiting non-zero on failure. Use the manual commands
below to drill into a specific failure.

## Install

- [ ] `adb shell pm path com.bmobile.workingspace` returns APK path
- [ ] App appears in launcher as **Working Space**
- [ ] Settings → System shows **Working Space** tile (IA_SETTINGS alias)

## Onboarding

- [ ] First launch shows AppIntro slides (4 screens)
- [ ] Skip or Done opens Workspace Hub
- [ ] Second launch skips intro

## Listed app session

- [ ] Add an app in hub library
- [ ] Launch listed app → overlay sidebar appears
- [ ] `adb shell settings get secure workingspace_session_active` → `1`
- [ ] Leave app → session ends → `0`

## Focus mode

- [ ] QS tile **Focus** toggles on
- [ ] `adb shell settings get secure workingspace_focus_mode_active` → `1`
- [ ] Persistent notification with **Stop** action
- [ ] Session tools apply on any foreground app
- [ ] Stop notification or QS tile → focus off

## SystemUI

- [ ] Incoming call FSI suppressed during active session (if configured)
- [ ] No unexpected heads-up during session (per app policy)

## SELinux

- [ ] `adb shell getenforce` → Enforcing
- [ ] No repeating AVCs for `com.bmobile.workingspace` in `logcat -b events`

## Regression

- [ ] Full ROM boot, no `system_server` crash from WorkingSpaceService
- [ ] `m WorkingSpace` clean on incremental build

## Manual ADB debug cheat sheet

Handy one-liners when the automated script fails a check and you need to dig
in. All assume a single device (`adb -s <serial>` otherwise).

**Service / process**

```bash
adb shell service check working_space
adb shell dumpsys activity services com.bmobile.workingspace
adb shell ps -A | grep com.bmobile.workingspace
adb logcat -s WorkingSpaceService:* WorkingSpaceBinderService:* FocusModeController:*
```

**Session mode (per-app)**

```bash
# Watch the session flag flip live while you foreground/background a listed app
adb shell settings get secure workingspace_session_active
watch -n1 'adb shell settings get secure workingspace_session_active'

# Inspect / seed the listed-app JSON
adb shell settings get system workingspace_app_list
adb shell settings put system workingspace_app_list '["com.example.app"]'

# Force a session start/stop without launching an app (binder service actions)
adb shell am startservice -a game_start --es package_name com.example.app \
    com.bmobile.workingspace/.gamebar.WorkingSpaceBinderService
adb shell am startservice -a game_stop \
    com.bmobile.workingspace/.gamebar.WorkingSpaceBinderService
```

**Focus mode (manual/global)**

```bash
# Toggle from the shell exactly like the QS tile does
adb shell settings put secure workingspace_focus_mode_active 1
adb shell settings get secure workingspace_focus_mode_active   # expect 1
adb shell settings get global zen_mode                          # expect 1 (priority)
adb shell dumpsys notification --noredact | grep -A5 'pkg=com.bmobile.workingspace'

adb shell settings put secure workingspace_focus_mode_active 0
adb shell settings get global zen_mode                          # expect 0 (off), if nothing else raised it

# Add the built-in tile to the active QS tile set (spec-based, not a
# TileService component — "cmd statusbar click-tile" does NOT apply here,
# that command only targets third-party CustomTile components).
adb shell settings get secure sysui_qs_tiles
adb shell settings put secure sysui_qs_tiles "$(adb shell settings get secure sysui_qs_tiles),workingspace_focus"
# Then physically tap the tile in Quick Settings, or drive it with:
adb shell input tap <x> <y>   # coordinates from `adb shell uiautomator dump`
```

**Onboarding**

```bash
# Reset first-run state to re-trigger AppIntro
adb shell run-as com.bmobile.workingspace rm -f \
    /data/data/com.bmobile.workingspace/shared_prefs/workingspace_intro.xml
adb shell am start -n com.bmobile.workingspace/.onboarding.WorkingSpaceIntroActivity
```

**SELinux**

```bash
adb shell getenforce
adb logcat -b events -d | grep -i 'avc:.*denied' | grep -i workingspace
adb shell dmesg 2>/dev/null | grep -i 'avc:.*denied.*workingspace'   # needs root/userdebug
```

**Crash / ANR triage**

```bash
adb logcat -d | grep -i 'FATAL EXCEPTION' -A 20 | grep -i -B5 com.bmobile.workingspace
adb bugreport ./bugreport_workingspace.zip   # heavier, use if the above is inconclusive
```
