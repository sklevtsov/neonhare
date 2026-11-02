# APKs and releases / Выпуск APK

## For players / Игрокам

Use [GitHub Releases](https://github.com/sklevtsov/neonhare/releases), open a published version and download `neon-hare-<version>.apk` from **Assets**. “Source code” ZIP/TAR files are not installers. Pre-releases are test versions. You can subscribe to Releases through GitHub's Watch → Custom → Releases.

В [Releases](https://github.com/sklevtsov/neonhare/releases) выбери опубликованный выпуск и скачай APK из **Assets**. Архивы Source code не являются установщиками. Pre-release — тестовая версия. Подписка на выпуски: Watch → Custom → Releases.

Updates signed with the same official key install over the normal `com.neonhare.game` application and preserve its data. A different signing certificate cannot simply update an installed app. Existing developer/local installs may use another certificate: **do not uninstall them to resolve a mismatch before arranging a save migration**. Debug builds use a separate application ID and separate progress.

Публичные обновления должны сохранять один сертификат. Если ранее стояла локальная APK с другой подписью, установка поверх неё может быть невозможна. Не удаляй игру ради обхода ошибки до решения переноса сохранения. Отладочная игра устанавливается отдельно.

Each release includes `SHA256SUMS`. Download it beside the APK, then verify:

```sh
# Linux
sha256sum -c SHA256SUMS
# macOS
shasum -a 256 -c SHA256SUMS
```

Checksums detect changed/corrupted downloads; they are not an independent signature. A release's signing certificate is verified against the maintainer's pinned SHA-256 fingerprint during packaging. Users with Android build-tools can inspect it using `apksigner verify --print-certs neon-hare-<version>.apk`. Record the official certificate fingerprint in the first published release notes and preserve it in later releases.

## Channels

| Channel | Application ID | Distribution | Signing / purpose |
|---|---|---|---|
| Public release | `com.neonhare.game` | Published GitHub Release | Persistent dedicated release key; 5 starting lives, no debug shortcuts |
| Public pre-release | `com.neonhare.game` | Release marked Pre-release | Same release identity; testing candidate, versionCode still increases |
| Development | `com.neonhare.game.debug` | Actions → successful run → Artifacts | Ephemeral CI debug key; 99 lives, numeric shortcuts; separate progress |
| Local device build | `com.neonhare.game` | Private local file | Explicit existing developer key via `--local`; not a public stable release |

Actions artifacts are ZIP downloads, require GitHub sign-in and expire after 14 days in this project. Extract the APK before installation. A fresh CI debug key may prevent an update over another debug build; use an emulator/disposable development profile, preserving any needed data first. This is why ordinary players should use Releases.

## One-time maintainer setup

**Not provisioned automatically.** Keep the release identity outside Git. Use an existing dedicated key if one exists. Otherwise create it once, securely record the alias/passwords and make an encrypted offline backup; never regenerate it for another release.

Example key creation (interactive prompts; no password in command history):

```sh
mkdir -p "$HOME/.config/neonhare-signing"
chmod 700 "$HOME/.config/neonhare-signing"
keytool -genkeypair -keystore "$HOME/.config/neonhare-signing/neon-hare-release.jks" \
  -alias neonhare -keyalg RSA -keysize 4096 -validity 10000
keytool -list -v -keystore "$HOME/.config/neonhare-signing/neon-hare-release.jks" -alias neonhare
```

In repository **Settings → Environments**, configure `release`. Restrict deployment branches/tags to the intended version tags and, where available, require a maintainer reviewer. Add these environment secrets (repository Actions secrets are also supported if environment secrets are not used):

| Secret | Value |
|---|---|
| `NEONHARE_KEYSTORE_BASE64` | Base64-encoded keystore bytes; base64 is encoding, not encryption |
| `NEONHARE_RELEASE_ALIAS` | Alias of the dedicated release key |
| `NEONHARE_STORE_PASSWORD` | Keystore password |
| `NEONHARE_KEY_PASSWORD` | Private-key password |
| `NEONHARE_RELEASE_CERT_SHA256` | Expected SHA-256 certificate fingerprint, with or without colons |

Use a password manager/secure secret editor to transfer these values; do not paste them into issues, chats, source files or logs. Keep the expected fingerprint independently with the release records; changing both key and fingerprint must be a deliberate reviewed migration. Enable GitHub private vulnerability reporting under repository security settings. Consider main/tag rulesets and secret scanning appropriate to the public repository.

The signing job refuses missing secrets, a debug alias/certificate and a mismatched certificate. It deletes the temporary decoded keystore after signing. Signing credentials are never passed to pull-request checks. No stable signing identity has been generated or committed by this preparation.

## Publish a version

1. Increase `versionCode` monotonically and update `versionName` in `app/build.gradle`. Write bilingual `docs/releases/<version>.md`, including known limits and signing/migration information.
2. Run CI and manually check the candidate on Anbernic and Samsung: controls, cutouts, menu, checkpoint save/reload, death/retry and background/resume. Record results; an emulator pass alone is insufficient for a stable device release.
3. Commit, then tag that reviewed commit and push the tag. Example, when actually ready:

```sh
git tag -a v0.4.1 -m 'Neon Hare 0.4.1'
git push origin v0.4.1
```

4. `Signed APK release draft` runs the reusable build/gameplay checks, builds the tagged source afresh, signs with the pinned identity and creates a **draft** Release containing the APK and `SHA256SUMS`. The workflow can also be run manually for an existing version tag. Tags such as `v0.4.2-rc.1` create a pre-release draft.
5. Review the draft, install the exact attached APK on target devices, add the certificate fingerprint/device results and publish through GitHub's Release editor. Until publication, normal users cannot download the draft. A failed workflow does not create a successful release; fix the cause and rerun deliberately.

Do not force-move published version tags or overwrite a published APK. Ship a new version for corrections. Release notes remain editable for clarification. The initial public APK is pending the one-time key/secret setup and physical-device validation; a README download link alone is not a published release.

## Local commands

```sh
./scripts/package-apk.sh --local
# Public signing: supply the five NEONHARE_RELEASE_* / password environment values
# documented in scripts/package-apk.sh, then:
./scripts/package-apk.sh --release
```

The local signing command expects an existing Android debug keystore (`ANDROID_KEYSTORE` can select it). A new developer can create the usual debug identity by building `assembleDebug` first. Different developers' debug keystores are not interchangeable for updates.

## References

The reader-first README structure was informed by [Unciv](https://github.com/yairm210/Unciv/blob/master/README.md) and [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon/blob/master/README.md): a short game description, distribution links, then development guidance. Text and artwork are original to this repository. Distribution behaviour follows [GitHub Releases documentation](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases) and [artifact downloads](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts); Android update identity follows [Android app signing](https://developer.android.com/studio/publish/app-signing).
