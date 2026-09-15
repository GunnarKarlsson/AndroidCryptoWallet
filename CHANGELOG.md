# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- GitHub Actions CI (`.github/workflows/ci.yml`) running `./gradlew :app:testDebugUnitTest`; README CI badge wired to that workflow

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
