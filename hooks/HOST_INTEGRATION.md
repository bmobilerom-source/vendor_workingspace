# Working Space — host integration

Copy **`vendor/bmobile/workingspace/`** to your ROM, then apply these host hooks.

Verification checklist: **`hooks/VERIFICATION.md`**.

## 1. Product makefile

**Repo:** `vendor/lineage` (or your product vendor)

```makefile
$(call inherit-product-if-exists, vendor/bmobile/workingspace/config.mk)
```

`config.mk` registers:

- `PRODUCT_PACKAGES`: `WorkingSpace`, privapp whitelist
- `PRODUCT_SOONG_NAMESPACES`: app module + vendored AppIntro
- `SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS`: `vendor/bmobile/workingspace/sepolicy/private`

## 2. Framework — WorkingSpaceService

**Repo:** `frameworks/base`

Required pieces (see `hooks/frameworks/` patch series or upstream tree):

| Path | Purpose |
|------|---------|
| `core/java/com/android/internal/app/IWorkingSpace*.aidl` | Binder API |
| `services/core/java/com/android/server/wm/WorkingSpaceService.java` | `"working_space"` service |
| `services/core/java/com/android/server/wm/FocusListManager.java` | `workingspace_app_list` |
| `services/core/java/com/android/server/wm/SessionStateDispatcher.java` | Session secure settings |
| WM hooks in `WindowManagerService`, `DisplayContent`, `ActivityTaskSupervisor`, `KeyguardController` | Focus + lifecycle |
| `GameManagerService.isPackageGame()` | Listed apps get Game Mode API |

## 3. SystemUI

**Repo:** `frameworks/base/packages/SystemUI`

| Change | Purpose |
|--------|---------|
| `NotificationEntry.shouldSuppressVisualEffect()` | Suppress FSI when `workingspace_session_active` |
| `StatusBarNotificationActivityStarter.launchFullScreenIntent()` | Defense-in-depth FSI guard |
| `WorkingSpaceFocusTile` + `LineageModule.kt` bind | QS focus mode tile |
| `res/values/config.xml` | Add `workingspace_focus` to tile lists |
| `res/values/custom_strings.xml` + drawables | Tile labels/icons |

## 4. Settings (optional but recommended)

**Repo:** `packages/apps/Settings`

- `bmobile_settings.xml` preference + `WorkingSpacePreferenceController`
- Working Space ships its own **`IA_SETTINGS` activity-alias** → appears under System category

## 5. Privapp whitelist

Shipped with module: `privapp_whitelist_com.bmobile.workingspace.xml` via Soong `prebuilt_etc_xml`.

## 6. SELinux

Module ships minimal **system_ext** policy:

```
vendor/bmobile/workingspace/sepolicy/private/
├── service.te           # working_space_service type
├── service_contexts     # working_space → type
└── workingspace.te      # system_app find + lineage health HAL client
```

No `setenforce 0`. Audit AVCs before widening rules.

## 7. Secure / System settings keys

| Key | Namespace | Meaning |
|-----|-----------|---------|
| `workingspace_session_active` | Secure | Session overlay active (framework sets) |
| `workingspace_focus_mode_active` | Secure | Manual global focus mode |
| `workingspace_app_list` | System | JSON listed apps |
| `workingspace_gesture_lock` | Secure | Sidebar gesture lock |

## 8. Build smoke test

```bash
m WorkingSpace services SystemUI
adb shell pm path com.bmobile.workingspace
adb shell settings get secure workingspace_focus_mode_active
```

## 9. Attribution

Fork lineage: Axion GameSpace → crDroid → **BMobile Working Space** (`com.bmobile.workingspace`).  
AppIntro 6.3.1 vendored under `third_party/appintro/` (Apache-2.0).

See `docs/ROM_AI/working-space/04-attribution-and-licenses.md`.
