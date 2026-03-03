# CEX 钱包系统 - 本地开发环境配置完成

> **重要**: 换电脑或重新部署时，请参阅 **SETUP.md** 获取完整部署指南。
> 本文档记录当前环境的实际配置状态。

## 服务启动状态

以下服务已成功启动并运行：

| 服务 | 端口 | 状态 | 健康检查 |
|------|------|------|---------|
| **DB Gateway** | 3003 | 运行中 | http://localhost:3003/health |
| **Risk Control** | 3004 | 运行中 | http://localhost:3004/health |
| **Signer** | 3001 | 运行中 | http://localhost:3001/health |
| **Wallet** | 3000 | 运行中 | http://localhost:3000/health |
| **EVM Scan** | 3002 | 已启动（无监控地址） | http://localhost:3002/api/scan/status |
| **Solana Scan** | - | 已启动（无监控地址） | - |

## 环境变量配置

### db_gateway/.env
- WALLET_DB_PATH: `E:/github/cex-project/cex-wallet/db_gateway/wallet.db`
- WALLET_PUBLIC_KEY: `92c8c75013be3207f92b7450999d20947539c2285a0299214559b2e6b836c61b`
- SCAN_PUBLIC_KEY: `92c8c75013be3207f92b7450999d20947539c2285a0299214559b2e6b836c61b`
- RISK_PUBLIC_KEY: `8c097825fa6837b20d53062260ce9ca86cd1f41d67f91c967bb01b3cd5fd0351`

### risk_control/.env
- RISK_CONTROL_DB_PATH: `E:/github/cex-project/cex-wallet/risk_control/risk_control.db`
- RISK_PRIVATE_KEY: `d21605cca5098efdc2eabdef42e21ab4f4bea007c7775c1349851bfb1070d6708c097825fa6837b20d53062260ce9ca86cd1f41d67f91c967bb01b3cd5fd0351`

### signer/.env
- SIGNER_DB_PATH: `E:/github/cex-project/cex-wallet/signer/signer.db`
- MNEMONIC: `test test test test test test test test test test test junk` (测试助记词)
- SIGNER_DEVICE: `signer_device1`
- RISK_PUBLIC_KEY / WALLET_PUBLIC_KEY: 已配置

### wallet/.env
- WALLET_PRIVATE_KEY: `f5706fed7cb45b1493859f570158cf5730be921cc8c2f257d89d9614524b942292c8c75013be3207f92b7450999d20947539c2285a0299214559b2e6b836c61b`
- SIGNER_BASE_URL: `http://localhost:3001`
- DB_GATEWAY_URL: `http://localhost:3003`
- RISK_CONTROL_URL: `http://localhost:3004`

### scan/evm_scan/.env
- ETH_RPC_URL: `http://localhost:8545` (需要运行 anvil)
- SCAN_PRIVATE_KEY: 已配置

### scan/solana_scan/.env
- SOLANA_RPC_URL: `http://localhost:8899` (需要运行 solana-test-validator)
- SCAN_PRIVATE_KEY: 已配置

## 下一步操作

### 1. 创建用户钱包地址
使用 Wallet API 创建用户钱包：
```bash
# 创建 EVM 钱包
curl -X POST "http://localhost:3000/api/user/1/address?chain_type=evm"

# 创建 Solana 钱包
curl -X POST "http://localhost:3000/api/user/1/address?chain_type=solana"
```

### 2. 启动区块链测试网络
```bash
# 启动 EVM 测试网络
anvil --block-time 1

# 启动 Solana 测试网络
solana-test-validator
```

### 3. 初始化测试数据
```bash
cd wallet
npm run mock:init                    # 初始化测试钱包和代币
npm run mock:evm:transfer            # 模拟 EVM 存款
npm run mock:withdraw:evm            # 模拟 EVM 提现
```

## 注意事项

1. **Scan 服务**: 当前 Scan 服务因为没有监控地址而未完全启动。需要先创建钱包地址。
2. **外部依赖**:
   - EVM 扫描需要运行 `anvil` 本地测试网络
   - Solana 扫描需要运行 `solana-test-validator`
3. **助记词**: 当前使用测试助记词，生产环境请替换为安全的助记词
4. **数据库**: SQLite 数据库文件位于 `db_gateway/wallet.db`

## 重启服务

如果需要重启服务：
```bash
# 停止所有服务 (Ctrl+C 或 kill 进程)
# 然后按顺序启动：
cd db_gateway && npm run dev &
cd risk_control && npm run dev &
cd signer && echo "12345678" | npm run dev &
cd wallet && npm run dev &
cd scan/evm_scan && npm run dev &
cd scan/solana_scan && npm run dev &
```
