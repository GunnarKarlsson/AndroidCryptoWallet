## Summary

<!-- What changed and why. Link issues with Fixes #N when applicable. -->

## Test plan

<!-- How you verified this. JVM unit tests, and manual steps if UI or chain behavior changed. -->

- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:lintDebug` passes (CI also assembles debug; no instrumented / emulator tests)

## Checklist

- [ ] No secrets in the diff (`local.properties`, `*.jks`, `*.keystore`, `keystore.properties`, `google-services.json`, mnemonics, API keys)
- [ ] UI changes include screenshots (or a short recording)
- [ ] Did not uninstall the app, clear app data, or run instrumented / `connected*AndroidTest` tasks against an emulator that holds wallets
- [ ] Room entity changes include an updated schema (and a migration when required)
