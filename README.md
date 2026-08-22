# AndroidCryptoWallet

A non-custodial Android Bitcoin wallet. Keys stay on device.

## Keys, seed, and signing

Supported BIPs:

- **BIP-39** — 12-word English mnemonic, optional passphrase
- **BIP-32** — HD derivation (via BDK)
- **BIP-84** — Native SegWit (`bc1q` / `tb1q`) at account `m/84'/0'/0'` (mainnet) or `m/84'/1'/0'` (testnet4)

This is an HD wallet. The encrypted secret at rest is the **seed** (mnemonic + optional passphrase), not a per-address WIF or xprv. Public data (receive address, network, derivation index) lives in Room. Secrets never go in Room, DataStore, or logs.

At rest, the seed is stored in `EncryptedSharedPreferences` keyed by wallet id, with an Android Keystore `MasterKey` (AES-256). Backup is disabled (`allowBackup="false"`).

Signing does not persist private keys. On send, the app decrypts the seed, BDK rebuilds an in-memory BIP-84 wallet, derives the key for the receive address, signs the PSBT, and drops the seed from the call stack. Watch-only wallets have no seed and cannot send.

## Balance and UTXOs

The wallet is a **single-address** model: balance, history, and coins all belong to the BIP-84 receive address (external index 0). Change from a send is paid back to that same address, so it stays visible and spendable on the next send.

The amount shown on wallet details is the address chain balance from the RPC provider (confirmed received minus confirmed spent). Incoming or outgoing mempool amounts are shown separately as unconfirmed; they are **not** added to the spendable total.

Spending does not use that cached display figure. Right before building a transaction the app fetches UTXOs for the receive address and keeps only **confirmed** outputs (`status.confirmed == true`). Unconfirmed coins cannot be selected. Those confirmed UTXOs are the spendable balance: their satoshi values are what BDK coin-selects against the send amount plus fee. Fee is `sum(inputs) − sum(outputs)` at the chosen sat/vB rate; if confirmed UTXOs cannot cover amount plus fee, send fails with insufficient funds.

## RPC provider

Chain data comes from [mempool.space](https://mempool.space) (Esplora-compatible HTTP: address balance, history, UTXOs, and broadcast). Mainnet uses `https://mempool.space/api/`; testnet4 uses `https://mempool.space/testnet4/api/`. No API key is required for low request quantities.

## Setup

1. Copy `local.properties.example` to `local.properties` (Android Studio also creates this with `sdk.dir`).
2. Optionally set mock watch-only addresses (`MOCK_BITCOIN_WALLET_TESTNET4` / `MOCK_BITCOIN_WALLET_MAINNET`) to any bitcoin address you want to view in the wallet lists and tx lists.
3. Open the project in Android Studio and run the `debug` build (Bitcoin testnet4).

## License

MIT
