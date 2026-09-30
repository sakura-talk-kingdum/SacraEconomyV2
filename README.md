# SacraEconomyV2

Package:

`si.f5.sakura_tk.sacra.economyv2`

Target:

- Java 21
- Paper 1.21.1
- Vault
- MySQL 9.x
- Geyser

## Transaction purchase flow

1. Main Threadで購入候補の数量を確認
2. Main Threadでインベントリへの完全収容可能性を確認
3. Asyncで本番DB transaction開始
4. `SELECT ... FOR UPDATE`
5. 残高を再確認
6. 出品を `is_active = 0`
7. `ec_shop_transactions` に履歴INSERT
8. COMMIT
9. Main ThreadでItemStackを付与

購入者ごとの同時購入もロックする。

## Land

土地の重複確認とINSERTは同一DB transaction内で処理する。

## Build

```bash
mvn clean package
```

生成物:

`target/sacra-economy-v2-2.0.0.jar`

## 注意

Vaultの入出金はMySQL transactionには含められない外部処理。
そのため、完全な経済原子性を実現するには、使用するVault Economy実装側のtransaction/rollback仕様も確認する必要がある。
