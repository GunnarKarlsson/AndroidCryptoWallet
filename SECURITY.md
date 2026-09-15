# Security Policy

## Supported versions

Security fixes are applied to the latest code on `main`. If tagged releases exist, the most recent release is also considered supported.

## Reporting a vulnerability

This repository is a **non-custodial cryptocurrency wallet**. Please **do not** open a public GitHub issue for security vulnerabilities.

Report them privately using [GitHub Security Advisories](https://github.com/GunnarKarlsson/AndroidCryptoWallet/security/advisories/new):

1. Go to the repository’s **Security** tab.
2. Choose **Advisories** → **New draft security advisory** (or use the link above).
3. Include a clear description, steps to reproduce, affected versions if known, impact (for example seed leakage, unauthorized signing, or fund loss), and any suggested fix.

Do not include a real mnemonic, private key, keystore password, or funded mainnet address in the report. Use throwaway testnet material only.

We aim to acknowledge reports promptly, typically within a few days. After triage, we will work with you on a fix and coordinated disclosure when appropriate.

## Scope

In scope:

- Seed, mnemonic, passphrase, or private-key leakage (logs, crash reports, screenshots, backups, Room, DataStore, or the network)
- Weak or broken at-rest encryption, Android Keystore usage, or backup settings that expose secrets
- Signing or derivation bugs that could spend funds the user did not confirm, sign for the wrong chain, or reuse keys unsafely
- Secrets, keystores, or credentials committed to this repository
- Supply-chain issues in this repo’s Gradle dependencies that affect the built APK
- Compromised or unexpected behavior in this repo’s CI workflows (when present)

Out of scope:

- Loss of funds after the user exports or types their mnemonic into another app or device
- A compromised, rooted, or malware-infected device
- Availability or correctness of third-party RPC / explorer APIs (for example mempool.space, public EVM RPCs, CoinGecko)
- Social-engineering against GitHub or maintainer accounts
- Issues that exist only in unmodified third-party libraries, unless this app uses them unsafely

If you are unsure whether a finding belongs here, report it privately through the advisory form above and we will route it.
