# Working Space — Game Mode follow-up commit series

All commits below share the title **`Workspace: game mode for all apps`**,
prefixed with `[N/2]`. Each message cross-links the other by repo path and
Change-Id. Full technical write-up and attribution:
`hooks/GAME_MODE_ALL_APPS.md`.

| # | Repo | Commit | Change-Id | Role |
|---|------|--------|-----------|------|
| 1/2 | `vendor/bmobile/workingspace` | *(this commit)* | `I9c4e2a1b8f7d6e5c4b3a29180706050403020100` | DeviceConfig interventions, session apply, register/unregister hooks, docs |
| 2/2 | `frameworks/base` | `22d4ebf66d8c` | `I8b3d1a0c7e6f5d4c3b2a180706050403020100ff` | GameManagerService listed-app modes + default config; shell command parity |

This series **depends on** the original feature land:

| Prior | Repo | Title |
|-------|------|-------|
| 1/4 | `vendor/bmobile/workingspace` | `[1/4] Workspace: build workspace feature` |
| 2/4 | `frameworks/base` | `[2/4] Workspace: build workspace feature` |
| 3/4 | `packages/apps/Settings` | `[3/4] Workspace: build workspace feature` |
| 4/4 | `vendor/lineage` | `[4/4] Workspace: build workspace feature` |

No Settings or `vendor/lineage` changes in this follow-up.

## Finding everything from one commit

From either commit message, follow:

1. Sibling commit in the other repo (same title, other `[N/2]`).
2. Parent series `[N/4]` for the rest of Working Space (service, QS tile,
   Settings entry, product inherit).
3. `vendor/bmobile/workingspace/hooks/GAME_MODE_ALL_APPS.md` for root cause,
   data flow, attribution, and validation.
4. `vendor/bmobile/workingspace/hooks/COMMIT_SERIES.md` for the original
   four-repo index.

## Verify

```bash
# vendor/bmobile/workingspace
git log --oneline -1   # [1/2] Workspace: game mode for all apps

# frameworks/base
git log --oneline -1 --grep='game mode for all apps'   # [2/2] ...
```
