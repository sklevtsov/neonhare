# Repository instructions for development agents

Scope: the entire repository. Follow the user's current instructions and preserve unrelated work. Read `CONTRIBUTING.md`, `SECURITY.md`, `docs/architecture.md`, and the relevant chapter's `design.md` before changing their contracts.

## Work and communication

- Make requested changes concrete: implement, build and verify them. Describe the result and evidence, including unverified device limits. Keep the user informed during long builds.
- Discuss new chapter designs and major roadmap changes before implementing them. The approved next priorities are quality of chapters 1–3, then design of the Night Postal Hub. English game localisation is planned, not already implemented.
- Use focused changes and meaningful names. Run the pinned formatter, not an invented style. Do not hide failures, disable checks or rewrite golden fixtures to make a regression pass.
- Do not add agent credits, assistant signatures or co-author trailers. When asked to commit, use the configured repository author; do not invent another identity or expose a private email.

## Architecture contracts

- Android event callbacks enqueue copied events. A single `GameLoop` owner mutates the running simulation. Lifecycle changes must not create two world threads or lose input events silently.
- Keep Android adaptation in `GameView`, orchestration in `GameWorld`, rendering/assets in their own classes, and new mechanics in systems/objective rules.
- Sprite attachments belong to `PlayerPose`; shooting and rendering must use the same current pose and mirroring.
- Add chapters beside old ones: `app/src/main/assets/levels/<id>/level.json` and `design.md`, then the campaign catalogue. Use `scripts/new-level.py` and the preview validator. Do not introduce chapter-index switches for new content.
- Stable chapter/object IDs and `LegacySaveIds` are persistence contracts. Preserve old saves before migration, test reordering/extension and refuse unsupported future schemas without destroying data.
- Keep per-frame work bounded; avoid decoding, unbounded collections, synchronous diagnostics or repeated storage writes in the loop.

## Required checks

- Java changes: formatter and `checkJavaFormat`, compile, relevant gameplay regressions and Android lint.
- Content/catalogue changes: `python3 tools/level_content.py`, the gameplay suite and `scripts/check-level-extension.sh`; inspect the route preview and play the affected route.
- Gameplay/save/input/lifecycle bugs: add a regression that exercises the actual changed path; do not replace behaviour checks with copied implementation formulas.
- Run emulator/install suites sequentially on `emulator-5554`. Do not run extension tests concurrently with another build/capture using the same catalogue or app.
- Visual changes: inspect actual rendered frames. `scripts/capture-screenshots.sh` captures staged scenes with the real renderer and an isolated test save. Preserve artwork provenance and the approved Stu design.
- Formatting/docs-only edits need appropriate lightweight verification, not new tests that mirror text.

## Devices, data and releases

- Daily iterations target the emulator. Physical-device installs require a user request and an explicit device serial. Never clear/uninstall a user's app or reset progress unless explicitly requested.
- Preserve `com.neonhare.game.debug` isolation; debug rewards and test intents stay unavailable to local/release builds.
- Never commit credentials, signing files, local SDK/AVD state, build output, raw logs or user reference screenshots. Keep the local reference-game directory ignored. Check staged paths/content before publication.
- Public signing must use explicit `--release`, the persistent release keystore and its expected certificate fingerprint. Do not generate or substitute a release key automatically; never publish a development-key APK as a stable public release.
- CI for untrusted PRs gets no signing secrets or write token. Keep actions pinned to reviewed commit SHAs and Gradle dependency verification enabled. Release jobs create reviewable drafts containing APK and SHA256SUMS.
- Preserve the minimal offline permissions model. New network/SDK/exported-component requirements need explicit justification and review; use `SECURITY.md` for the full policy.

## Public documentation

Keep `README.md` (English) and `README.ru.md` (Russian) consistent. Distinguish current prototype features from plans and actual gameplay captures from promotional images. Link APK downloads to Releases; do not commit APK binaries. Code is MIT; images are excluded under `ASSETS-LICENSE.md`. Do not import another game's art, maps or code from the private research folder.
