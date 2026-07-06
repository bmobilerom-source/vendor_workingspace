# Working Space — commit series index

All four commits below share the exact same title, `Workspace: build workspace
feature`, prefixed with their position in the series (`[N/4]`). Each commit
message cross-links the other three by repo path and Change-Id so the full
change can be reconstructed from any single repo.

| # | Repo | Commit | Change-Id | Role |
|---|------|--------|-----------|------|
| 1/4 | `vendor/bmobile/workingspace` | *(this commit)* | `Ib5dbc170db6ee1e4f078a69ff55af9b2d6aa7c56` | Main application module (app, sepolicy, docs) |
| 2/4 | `frameworks/base` | `df32b1a3c442` | `I07e315c2590a2d24222f16ed66e22e999b495ab1` | Platform service, AIDL, SystemUI QS tile |
| 3/4 | `packages/apps/Settings` | `b7aec73d557` | `I60efef5494bc85e40407004297405175562d35b6` | Settings hub entry point |
| 4/4 | `vendor/lineage` | `6bb595d8` | `Idb080535063374b72f3bb62a33cff81359683dc3` | Product makefile inherit |

All four commits depend on 1/4 (the app must exist for the framework hooks,
Settings entry point, and product package list to mean anything) and are
otherwise independent of each other — 2/4, 3/4, and 4/4 can be cherry-picked
in any order once 1/4 is present.

## Why four repos

Working Space is split across the AOSP/LineageOS tree the same way any
privileged system app with a platform-side service is:

- **`vendor/bmobile/workingspace`** — the app itself, self-contained
  (Soong module, sepolicy, docs). Nothing else in the tree strictly
  requires this repo to build, but Working Space has no effect without it.
- **`frameworks/base`** — code that must live in the platform because it
  needs privileged/system-only APIs: the `IWorkingSpaceService` Binder
  interface, `WorkingSpaceService` inside `system_server`, the
  `WorkingSpaceFocusTile` (native `QSTileImpl`s must live in SystemUI),
  and the two notification hooks that suppress full-screen intents during
  a session.
- **`packages/apps/Settings`** — a single preference + controller so users
  can find Working Space from Settings without needing a launcher icon.
- **`vendor/lineage`** — the one-line product makefile inherit that makes
  a `lunch`ed BMobile product actually include the package.

## Verifying the series is complete

```bash
# From the root of each repo:
git log --oneline -1  # should show "[N/4] Workspace: build workspace feature"
```

See `hooks/HOST_INTEGRATION.md` for the full per-file breakdown of what each
repo contributes, and `hooks/VERIFICATION.md` for the adb-based functional
test checklist (session overlay, Focus mode DND, QS tile, hidden-app
filtering) that was run against this series before it was committed.
