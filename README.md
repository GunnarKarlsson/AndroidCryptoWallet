# AndroidCryptoWallet

A non-custodial Android wallet for Bitcoin and EVM chains (Ethereum, BSC, Polygon, Arbitrum, Base, Optimism, Avalanche). Keys stay on device.

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

To add another chain client later:

1. Implement every method on `BitcoinRemoteDataSource` (balance, transactions, UTXOs, transaction hex, broadcast, tip height). Do not ship a partial client.
2. Add a `@Binds` (or `@Provides`) of that type in Hilt and swap the mempool.space bind in `DataBindsModule`.
3. Put provider-specific URLs and keys in `local.properties` → BuildConfig, never in git.
4. Reuse the existing `MsApiFactory` pattern (per-network Retrofit cache) if the API is HTTP.

## EVM chains

Adding another EVM chain is easy. All families share the same wallet screens and backend (JSON-RPC, signing, tx history). BSC is the reference implementation.

Each **family** (Ethereum, BSC, …) is a chain-select entry. Users pick a **network** inside that family (Sepolia, BSC Testnet, …) from the dropdown. You do not add new screen packages or repositories.

### What you get for free

- JSON-RPC balance/send and EIP-1559 signing (`JsonRpcEthereumRemoteDataSource`, `Web3jEthereumKeyEngine`)
- BIP-44 coin type `60'` (MetaMask-compatible addresses)
- Room `ethereum_wallet` / tx cache and encrypted `ethereum_mnemonic` prefs (names unchanged)
- Shared screens under `app/src/main/java/.../ui/ethereum/**`, filtered by `EvmFamily`
- Receive QR: EIP-681 `ethereum:address@chainId`
- Amount labels from `network.nativeSymbol`

### Checklist

#### 1. Domain — `EvmFamily` + `EvmNetwork`

| Task | File |
|------|------|
| Add enum value | `app/src/main/java/network/bahn/androidcryptowallet/domain/model/EvmFamily.kt` |
| Add networks (testnet + mainnet typical) | `app/src/main/java/network/bahn/androidcryptowallet/domain/model/EvmNetwork.kt` |

Each `EvmNetwork` needs: `family`, `label` (dropdown), `chainId`, `nativeSymbol`. `EvmNetwork.networksFor(family)` filters automatically.

Wallets store `network` as the enum name (`SEPOLIA`, `BSC_TESTNET`, …). New constants are **additive only** — no migration. Do **not** rename existing enum names.

Tests: extend `app/src/test/java/.../domain/model/EvmNetworkTest.kt`.

#### 2. Catalog — RPC + explorer

| Task | File |
|------|------|
| RPC URL per network | `app/src/main/java/network/bahn/androidcryptowallet/di/AppModule.kt` → `provideEvmChainCatalog()` |
| Explorer URL + kind per network | same |

```kotlin
rpcUrls = mapOf(EvmNetwork.NEW_TESTNET to "https://…", …)
explorerEndpoints = mapOf(
    EvmNetwork.NEW_TESTNET to EvmExplorerEndpoint(
        baseUrl = "https://…",
        kind = EvmExplorerKind.BLOCKSCOUT, // or ETHERSCAN
    ),
)
```

Tx history adapters (picked automatically by `RoutingEthereumTransactionRemoteDataSource`):

| Kind | API | Class |
|------|-----|-------|
| `BLOCKSCOUT` | Blockscout REST v2 | `BlockscoutEthereumTransactionRemoteDataSource` |
| `ETHERSCAN` | Etherscan-compatible `txlist` | `EtherscanEthereumTransactionRemoteDataSource` |

If the explorer is neither format, add a new `EvmExplorerKind`, adapter, and routing branch.

Tests: extend `app/src/test/java/.../data/remote/evm/EvmChainCatalogTest.kt`.

#### 3. Chain select — label, icon, strings

| Task | File |
|------|------|
| Chain-select enum + order | `app/src/main/java/.../ui/chain/SupportedChain.kt` |
| Label + icon on chain select | `app/src/main/java/.../ui/chain/ChainSelectScreen.kt` |
| List title, default wallet name, clipboard label | `app/src/main/java/.../ui/chain/EvmFamilyUi.kt` |
| `SupportedChain` → `EvmFamily` | `EvmFamilyUi.kt` → `toEvmFamily()` |

Add to `app/src/main/res/values/strings.xml` (mirror Ethereum/BSC):

| Resource | Example (BSC) |
|----------|-----------------|
| `chain_*` | `chain_bsc` |
| `*_wallets_title` | `bsc_wallets_title` |
| `*_wallet_list_item_label` | `bsc_wallet_list_item_label` |
| `receive_clipboard_label_*` | `receive_clipboard_label_bsc` |

Add `app/src/main/res/drawable/ic_chain_<family>.xml`.

#### 4. Navigation

In `app/src/main/java/.../ui/navigation/WalletNavHost.kt`:

```kotlin
SupportedChain.NEW_FAMILY ->
    navController.navigate(EvmWalletListRoute(EvmFamily.NEW_FAMILY))
```

Create/restore/details/send/receive/edit reuse the existing EVM graph — no new route types.

#### 5. Default network

In `app/src/main/java/.../data/local/prefs/SelectedEvmNetworkDataStore.kt` → `defaultNetwork()`: set the family’s testnet (e.g. ETH → `SEPOLIA`, BSC → `BSC_TESTNET`).

#### 6. Verify

```bash
./gradlew :app:testDebugUnitTest
```

Manual smoke (do not clear emulator app data):

1. Chain select → new family → dropdown shows only that family’s networks.
2. Create wallet on testnet → receive QR ends with `@<chainId>`.
3. Send native on testnet.
4. Details → tx history loads (or empty, no crash).
5. Other families unchanged.

### Do not

- Add new screen packages — reuse `ui/ethereum/**`
- Merge EVM with the Bitcoin stack
- Rename/drop `ethereum_*` tables or `ethereum_mnemonic` prefs
- Run destructive migrations or reinstall/clear app data to test
- Add ERC-20 / BEP-20 tokens in the same change

### BSC reference — files touched

| Area | Files |
|------|-------|
| Domain | `EvmFamily.kt`, `EvmNetwork.kt` |
| Catalog | `AppModule.kt` |
| Tx history | `EtherscanTx.kt`, `EtherscanEthereumTransactionRemoteDataSource.kt`, `RoutingEthereumTransactionRemoteDataSource.kt` |
| Chain select | `SupportedChain.kt`, `ChainSelectScreen.kt`, `EvmFamilyUi.kt`, `strings.xml`, `ic_chain_bsc.xml` |
| Nav | `WalletNavHost.kt` |
| Defaults | `SelectedEvmNetworkDataStore.kt` |
| Tests | `EvmNetworkTest.kt`, `EvmChainCatalogTest.kt`, `EtherscanTxMappingTest.kt` |

## Setup

1. Copy `local.properties.example` to `local.properties` (Android Studio also creates this with `sdk.dir`).
2. Optionally set mock watch-only addresses (`MOCK_BITCOIN_WALLET_TESTNET4` / `MOCK_BITCOIN_WALLET_MAINNET`) to any bitcoin address you want to view in the wallet lists and tx lists.
3. Open the project in Android Studio and run the `debug` build (Bitcoin testnet4).

## License

MIT
