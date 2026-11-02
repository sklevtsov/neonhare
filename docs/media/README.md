# Public media

## Gameplay captures

`green-rift.png`, `lev-arden.png`, `floating-gardens.png` and `crystal-memory.png` are **960×720 Android emulator screenshots**, captured through `UiAutomation.takeScreenshot()` after presenting the real `GameRenderer` to its Surface. They are not generated illustrations or edited mockups.

Scenes are staged by `app/src/androidTest/java/com/neonhare/game/ScreenshotCapture.java`: chosen chapter/camera, pose, one shot, normal starting lives, isolated test save, hidden developer annotations. These are illustrative frames, not evidence of completing the campaign or measuring frame rate. The standalone instrumentation is only in the test APK; no production screenshot/debug-intent entry point is added.

Recreate them with the emulator running, after other install/check suites finish:

```sh
./scripts/capture-screenshots.sh
```

This installs only `com.neonhare.game.debug` and its test APK on **emulator-5554**. The test changes only the test profile, not a normal player's save. Captures must be visually inspected before replacing the public images. The helper installs a different instrumentation runner; rerun `scripts/check-game.sh` afterwards to restore the normal gameplay test runner.

## Promotional wordmark

`neon-hare-banner.png` is a separately generated **1774×887** promotional illustration/wordmark, not a screenshot. Tool mode: built-in image generation with the approved `nova_green.png` protagonist supplied as a local image reference, opaque background. Generated once for this repository, copied into this directory and visually reviewed. It is suitable as a README hero image; the repository's Social preview must be uploaded separately in GitHub repository settings if desired.

Final generation prompt:

> Create a polished wide GitHub repository banner and game wordmark for NEON HARE. Landscape 2:1 composition, about 1536x768. This is promotional key art, not a gameplay screenshot. Use the attached game's approved protagonist Stu faithfully: appealing green rabbit, long swept-back ears pink inside, cream muzzle, red short futuristic jacket, black shorts, red white sneakers and a compact cyan glowing blaster. Preserve his character design and charming confident expression. Place Stu on the right third, full or three-quarter dynamic running pose pointing his blaster toward the right. On left and center put large beautifully drawn highly legible exact text NEON HARE in bold custom videogame logo lettering, ivory white with lime green and cyan dimensional edges, no other text. Background is an atmospheric dark midnight-teal alien forest with distant floating stone islands and a subtle warm sunset opening, strong depth but uncluttered. Crisp richly shaded 1990s inspired pixel-art illustration, tasteful restrained neon accents, sharp dark silhouette around rabbit, professional indie action-platformer branding. Keep all essential artwork and lettering at least 8 percent inside the edges for GitHub social crop. No real-world brands, no other games' characters, no badges, no watermark. The text must be exactly NEON HARE.

All images here follow [the separate artwork terms](../../ASSETS-LICENSE.md); the project's MIT code licence does not grant image reuse rights.
