# Contributing

Thank you for your interest in contributing to AndroidCryptoWallet. This document explains how to contribute to the project.

## Good first contributions

These areas are a solid place to start:

- **Tests for pure helpers** — formatting, parsing, domain mapping, and ViewModel state under `app/src/test/`.
- **UI polish** — clearer empty/error/loading states, copy consistency, and layout fixes that do not change signing or storage.
- **Docs alignment** — README and comments that match the current screens and supported chains.

If you are unsure whether an idea fits, open an issue first and describe the intended user-facing change.

## Discuss internals before large work

Changes that touch core internals should be discussed with maintainers before you invest significant time. Open an issue or draft PR with the problem and proposed approach so scope and design can be agreed early.

That includes, at least:

- Seed storage, Android Keystore, `EncryptedSharedPreferences`, logging, and backup flags
- Bitcoin or EVM signing, derivation paths, and coin selection
- Room schema / migrations
- New chain families or a change to how RPC / explorer URLs are configured
- Gradle, Hilt, or navigation architecture

Adding another **curated EVM family** (same screens, new `EvmFamily` / `EvmNetwork`) is documented in the [README](README.md); still open an issue if the explorer API is a new kind.

## Development setup

See the [README](README.md) for product context. To build:

1. Install [Android Studio](https://developer.android.com/studio) (JDK 17; `sourceCompatibility` / `targetCompatibility` are Java 17).
2. Copy `local.properties.example` to `local.properties` and set `sdk.dir` (Android Studio also writes this file).
3. Optionally set `MOCK_BITCOIN_WALLET_TESTNET4` / `MOCK_BITCOIN_WALLET_MAINNET` for watch-only list fixtures.
4. Open the project and run the `debug` build, or from the repo root:

```bash
./gradlew :app:assembleDebug
```

Do not commit `local.properties`, keystores, or mnemonics.

### Emulator data

Treat a debug install as a real wallet. Do **not** uninstall the app, run `adb shell pm clear`, `./gradlew uninstallDebug` / `installDebug`, or instrumented (`connectedAndroidTest`) tasks against an emulator that already has wallets. Those flows wipe Room and the encrypted mnemonic store.

## Before you open a pull request

### Keep your branch current with `main`

Always rebase or merge the latest `main` into your branch before opening or updating a PR:

```bash
git fetch origin
git merge origin/main
# or: git rebase origin/main
```

Resolve any conflicts locally, re-run the checks below, then push.

### Run the JVM unit tests

Run tests with:

```bash
./gradlew :app:testDebugUnitTest
```

That is the check contributors should pass before review. Do **not** run `connectedAndroidTest`, `connectedDebugAndroidTest`, or `connectedCheck` unless a maintainer has explicitly accepted that it will wipe emulator wallet data.

Do not submit with failing unit tests.

## Pull request guidelines

- Prefer small, focused PRs that are easy to review. Small PRs are usually reviewed within a few days; larger ones may take longer.
- Describe **what** changed and **why** in the PR body. Link related issues when applicable.
- Match existing code style and folder boundaries (`domain/`, `data/`, `ui/`, `di/`).
- Do not commit secrets — for example `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`, `google-services.json`, mnemonics, or API keys.
- Include screenshots or a short recording for UI changes.
- If you change Room entities, include the exported schema under `app/schemas/` and a migration when required.
- Fill in the pull request template checklist.

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE) that covers this project.
