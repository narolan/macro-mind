# Contributing

- **Test first.** Write a failing test, make it pass, then tidy. Aim for 80% line coverage; `./gradlew check` enforces it.
- **Run both suites before pushing:** `./gradlew check` (backend, also fails on compiler warnings) and
  `cd overlay && npm test`.
- **Rules** live in `src/main/java/com/macromind/domain/service`. A new rule is a method in `ThreatRules`,
  `CompositionRules` or `CoreItemRules`, plus a test. Shared thresholds go in `AdviceThresholds`.
- **Advice output is pinned.** `AdvisorGoldenMasterTest` and `CatalogueGoldenMasterTest` fail when what players are told
  changes. If the change is intended, update the expectation in the same commit and say why.
- **Commit messages:** `<type>: <description>` with type one of feat, fix, refactor, docs, test, chore, perf, ci.
- **Keep dependency bumps and features in separate commits.**
- **Never commit `mise.local.toml`** or anything that may hold credentials; stage files by explicit path.
