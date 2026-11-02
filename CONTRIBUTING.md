# Developing Neon Hare

Contributions should improve a playable game, preserve existing progress and make future chapters easier to add. Discuss a large mechanic, chapter or story change in an issue before implementation. Small fixes can go directly to a focused pull request.

## Start here

Read [architecture](docs/architecture.md), [level authoring](docs/adding-levels.md), the [roadmap](docs/development-plan.md) and [security policy](SECURITY.md). Agent-specific requirements are in [AGENTS.md](AGENTS.md). The game currently uses Russian text; public README files must stay in sync in English and Russian.

## Code and responsibilities

- Java uses four spaces and the pinned AOSP google-java-format configuration. Run `./scripts/format-java.sh`; CI runs `checkJavaFormat`. Keep braces and meaningful names. Avoid dense multi-statement lines and unrelated formatting churn.
- `GameView` adapts Android events and surface lifecycle. `GameLoop` owns the simulation; `GameWorld` coordinates systems. UI threads enqueue copied input; they do not mutate the running world.
- Put mechanics in focused systems/rules, graphics in `GameRenderer`/`GameAssets`, and storage compatibility in the save/progression layer. Avoid moving a growing conditional block into a new class without improving its contract.
- Coordinate visual attachments and gameplay consistently. `PlayerPose` owns the sprite and muzzle attachment used by both drawing and shooting. Different artwork needs updated anchors and regression checks.
- Avoid allocations, resource decoding and synchronous file I/O inside the frame loop. Keep input queues and diagnostics bounded.
- Use logical coordinates and check screen cutouts, 4:3 handhelds and wide phone layouts when changing UI. Treat controller and touch input as supported controls.

## Add content, not chapter-number branches

Each chapter lives in `app/src/main/assets/levels/<stable-id>/` with `level.json` and `design.md`. The campaign catalogue controls ordering. Use `scripts/new-level.py` for a draft, then preview and review its route before adding it to the catalogue.

A proposal needs a distinct traversal idea, main/alternate route, checkpoint recovery, secret, reward, palette and story purpose. Document required skills and how a player learns them. New behaviour belongs in an objective rule or independent system, not `if (levelIndex == ...)`. Existing IDs and legacy save mappings are compatibility contracts; do not regenerate or renumber them.

## Verification

Use the JDK/SDK setup in the README. Start emulator-5554, then run:

```sh
./scripts/format-java.sh
./gradlew checkJavaFormat validateLevels lintDebug lintLocal lintRelease
./scripts/check-game.sh
./scripts/check-level-extension.sh
./scripts/check-signing.sh
```

Run checks appropriate to the change. Add meaningful regression coverage for a changed gameplay, lifecycle, content or save contract; avoid tests that merely repeat implementation constants. Run emulator/install suites sequentially, because they share one app and device. `check-level-extension.sh` temporarily adds a chapter and restores the catalogue even on failure.

For movement, combat or screen changes, also play the affected route using controller and touch, including death/retry, background/resume and checkpoint restore. Stable candidates need an Anbernic and Samsung check. Report precisely what passed and what remains untested; thousands of assertions are not thousands of independent scenarios or a stability guarantee.

## Save and release discipline

Never clear user progress to fix an update. Migrate by stable IDs, preserve the old save before migration and refuse an unsupported newer schema without overwriting it. Add recovery tests for storage changes.

Daily development goes to the emulator. Install on physical devices only when requested, after checks, with an explicit ADB serial. Never run `adb install`, `pm clear` or uninstall against an implicitly selected device. Debug and normal application IDs stay separate. Public APKs need the existing dedicated release identity; see [release instructions](docs/releases.md).

## Pull requests and assets

Explain the user-visible problem, resulting behaviour, validation and remaining limits. Keep the PR focused. Update the docs, Russian/English README or authoring guide when their contracts change. Include real game captures for visual changes and identify promotional artwork as such.

Do not commit keys, SDKs, builds, personal logs, reference screenshots or other games' files. Give new artwork provenance and explicit reuse terms; do not copy protected art from the games used for inspiration. Source contributions use the project MIT terms; artwork requires separate agreement under [asset terms](ASSETS-LICENSE.md).

Report vulnerabilities privately following `SECURITY.md`. Do not post raw device logs or exploit details in a public issue.

## Кратко по-русски

Сначала обсуждаем крупную механику и дизайн главы, затем добавляем её отдельной папкой со стабильными ID. Игровое состояние меняет один поток; ввод передаётся очередью. Изменения сохранений требуют миграции и проверки восстановления. Форматирование автоматическое, проверки запускаются на эмуляторе последовательно. На Anbernic и Samsung ставим проверенные сборки по явному запросу, не сбрасывая данные. В PR описываем результат, реальные проверки и ограничения. Референсы, ключи, SDK, APK и личные логи в Git не попадают.
