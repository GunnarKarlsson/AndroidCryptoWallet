# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- GitHub Actions CI (`.github/workflows/ci.yml`) running JVM unit tests, Android Lint, and `assembleDebug` (no emulator instrumentation); README CI badge wired to that workflow
- Gradle Wrapper validation workflow (`.github/workflows/gradle-wrapper-validation.yml`)
- Dependabot `open-pull-requests-limit: 5` for Gradle updates
- Release R8 (`optimization { enable = true }`, resource shrinking, keep rules for BDK / Web3j / Retrofit / Kotlinx Serialization / Hilt); OkHttp logging interceptor is debug-only
- Network security config with cleartext disabled; Settings rejects `http://` provider URLs; backup/data-extraction rules name the mnemonic prefs files
- App `versionName` `0.1.0` (aligned with the tagged changelog until a 1.0 release)

### Changed

- README security model and Network security hosts; EVM class names (`JsonRpcEvmRemoteDataSource`, `ui/evm/**`)

### Fixed

- Lint `NewApi` on `BigInteger.TWO` (API 33) so `lintDebug` passes on minSdk 26
- BIP-39 parse errors no longer echo raw BDK messages that could contain recovery words

## [0.1.0] - 2026-09-15

First documented version of this in-progress wallet.

### Added

- Non-custodial Android wallet for Bitcoin (BIP-39 / BIP-32 / BIP-84, single receive address) and curated EVM families: Ethereum, BSC, Polygon, Arbitrum, Base, Optimism, and Avalanche
- On-device seed storage in `EncryptedSharedPreferences` with an Android Keystore `MasterKey`; signing rebuilds keys in memory and does not persist private keys
- Bitcoin chain data via mempool.space (mainnet and testnet4); EVM balance/send via JSON-RPC and tx history via Blockscout or Etherscan-compatible explorers
- Create, restore, watch-only, send, receive, and per-wallet details flows, plus a consolidated home list
- Settings for editable provider URLs and a mainnet / testnet switch for the consolidated lists
- Repository hygiene: security policy, contributing guide, code of conduct, changelog, GitHub issue/PR templates, and Dependabot for Gradle and GitHub Actions

[Unreleased]: https://github.com/GunnarKarlsson/AndroidCryptoWallet/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/GunnarKarlsson/AndroidCryptoWallet/releases/tag/v0.1.0
