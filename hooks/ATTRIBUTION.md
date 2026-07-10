# Working Space — Attribution

This document records provenance for the Working Space module and related
platform hooks. It is informational for developers and auditors; it does
**not** assert affiliation with AxionOS, GrapheneOS, or any other project
beyond what the licenses and file headers already state.

## Primary lineage

Working Space started as a port of **AxionAOSP GameSpace**, itself descended
from community GameSpace / game-mode tooling used across LibreMobileOS /
Chaldeaprjkt-era trees and later crDroid contributions. BMobile / LineageOS
maintainers then:

- Stripped Axion-specific product dependencies
- Rebranded user-facing “game” terminology to “Working Space”
- Added Focus mode, SystemUI QS tile, Settings hub entry, SELinux, and
  framework Binder service (`WorkingSpaceService` / `FocusListManager`)
- Fixed Game Mode interventions so Performance / Battery apply to **all
  registered apps** (see `hooks/GAME_MODE_ALL_APPS.md`)

## Third-party / upstream components

| Component | Location / use | License |
|-----------|----------------|---------|
| AppIntro | `third_party/appintro` — onboarding | Apache-2.0 |
| AOSP GameManagerService | Platform Game Mode + `game_overlay` DeviceConfig | Apache-2.0 |
| AOSP / LineageOS SystemUI | Host for `WorkingSpaceFocusTile` | Apache-2.0 |

## Copyright headers

Individual source files keep historical `Copyright (C)` lines for authors
who originally contributed that file (e.g. Chaldeaprjkt, crDroid, AxionOS,
LineageOS Project). New BMobile-specific files typically use
`Copyright (C) YYYY The LineageOS Project` under Apache-2.0.

## Documentation map

| Doc | Purpose |
|-----|---------|
| `hooks/COMMIT_SERIES.md` | Original `[N/4]` multi-repo commit index |
| `hooks/COMMIT_SERIES_GAME_MODE.md` | Follow-up `[N/2]` game-mode commit index |
| `hooks/HOST_INTEGRATION.md` | Per-repo integration file map |
| `hooks/VERIFICATION.md` | adb smoke / functional checklist |
| `hooks/GAME_MODE_ALL_APPS.md` | Game-mode-on-all-apps design + attribution detail |
| `hooks/ATTRIBUTION.md` | This file |

## Contact / product

Working Space ships as part of the BMobile LineageOS product tree under
`vendor/bmobile/workingspace`, inherited via `vendor/lineage` product
makefiles. See in-app / Settings strings for end-user facing product name.
