# CEX 钱包系统 - 完整部署指南

本文档记录完整的部署配置流程，用于换电脑或重新部署时参考。

## 一、系统要求

### 必需软件
```bash
# Node.js 版本要求
Node.js 18+ (推荐 v22.x)
npm 9+

# 区块链测试网络 (开发环境)
Foundry (anvil) - EVM 测试链
Solana CLI - Solana 测试网络
```

### 安装外部依赖

```bash
# 1. 安装 Foundry (EVM 测试网络)
curl -L https://foundry.paradigm.xyz | bash
foundryup

# 2. 安装 Solana CLI (Windows)
# 下载地址：https://github.com/anza-xyz/agave/releases
# 或使用 Scoop
scoop install solana-cli
```

---

## 二、项目结构

```
cex-wallet/
├── db_gateway/          # 数据库网关 (端口 3003)
├── risk_control/        # 风控服务 (端口 3004)
├── signer/              # 签名服务 (端口 3001)
├── wallet/              # 钱包 API (端口 3000)
├── scan/
│   ├── evm_scan/        # EVM 扫描器 (端口 3002)
│   └── solana_scan/     # Solana 扫描器
├── CLAUDE.md            # Claude Code 指导文档
├── LOCAL_SETUP_COMPLETE.md    # 当前环境配置摘要
└── SETUP.md             # 本文档 - 完整部署指南
```

---

## 三、部署步骤

### 步骤 1: 克隆项目
```bash
git clone <repository-url>
cd cex-wallet
```

### 步骤 2: 安装所有模块依赖

```bash
# Windows PowerShell 脚本
$modules = @("db_gateway", "risk_control", "signer", "wallet", "scan/evm_scan", "scan/solana_scan")
foreach ($module in $modules) {
    Write-Host "Installing dependencies for $module..."
    Set-Location $module
    npm install
    Set-Location ..
}
```

### 步骤 3: 生成和配置密钥对

#### 3.1 启动 db_gateway 生成主密钥对
```bash
cd db_gateway
npm run dev
# 保持运行，新开终端执行：
curl -X POST http://localhost:3003/generate-keypair
```

记录返回的公钥和私钥：
```json
{
  "publicKey": "xxxxx",
  "privateKey": "xxxxx"
}
```

#### 3.2 生成 Risk Control 密钥对
```bash
cd risk_control
npm run generate-keypair
```

#### 3.3 生成 Scan 模块密钥对（可选，不同链可用不同密钥）
```bash
# 为 EVM Scan 和 Solana Scan 分别生成
cd scan/evm_scan
# 使用 db_gateway 生成或复用主密钥

cd scan/solana_scan
```

### 步骤 4: 配置所有模块的 .env 文件

每个模块都需要创建 `.env` 文件。以下是配置模板：

#### db_gateway/.env
```ini
# 服务器配置
PORT=3003
NODE_ENV=development

# 数据库路径 (使用绝对路径，Windows 使用正斜杠或盘符格式)
WALLET_DB_PATH=E:/github/cex-project/cex-wallet/db_gateway/wallet.db

# Ed25519 公钥 (从步骤 3 获取)
WALLET_PUBLIC_KEY=<生成的公钥>
SCAN_PUBLIC_KEY=<生成的公钥，可为每个 Scan 模块配置不同的key>
RISK_PUBLIC_KEY=<risk_control 生成的公钥>

# 限流配置
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=1000

# 日志
LOG_LEVEL=info
```

#### risk_control/.env
```ini
# 服务器配置
PORT=3004
NODE_ENV=development
LOG_LEVEL=info

# 数据库路径
RISK_CONTROL_DB_PATH=E:/github/cex-project/cex-wallet/risk_control/risk_control.db

# 私钥 (从 npm run generate-keypair 获取)
RISK_PRIVATE_KEY=<生成的私钥>

# Wallet 服务地址
WALLET_SERVICE_URL=http://localhost:3000
```

#### signer/.env
```ini
# 服务器配置
PORT=3001
NODE_ENV=development
LOG_LEVEL=info

# 数据库路径
SIGNER_DB_PATH=E:/github/cex-project/cex-wallet/signer/signer.db

# 助记词 (重要！生产环境使用安全助记词)
MNEMONIC=your secure mnemonic phrase here

# 设备名称
SIGNER_DEVICE=signer_device1

# 签名验证公钥 (从 db_gateway 配置获取)
RISK_PUBLIC_KEY=<db_gateway 中的 WALLET_PUBLIC_KEY>
WALLET_PUBLIC_KEY=<db_gateway 中的 WALLET_PUBLIC_KEY>
```

#### wallet/.env
```ini
# 服务器配置
PORT=3000
NODE_ENV=development
LOG_LEVEL=info

# 依赖服务地址
SIGNER_BASE_URL=http://localhost:3001
DB_GATEWAY_URL=http://localhost:3003
RISK_CONTROL_URL=http://localhost:3004

# 私钥 (用于 db_gateway 签名)
WALLET_PRIVATE_KEY=<db_gateway 生成的私钥>
```

#### scan/evm_scan/.env
```ini
# Ethereum RPC 配置
ETH_RPC_URL=http://localhost:8545
ETH_RPC_URL_BACKUP=

# 数据库路径
WALLET_DB_PATH=E:/github/cex-project/cex-wallet/db_gateway/wallet.db

# 扫描配置
START_BLOCK=1
CONFIRMATION_BLOCKS=32
SCAN_BATCH_SIZE=10
SCAN_INTERVAL=12
REORG_CHECK_DEPTH=64
USE_NETWORK_FINALITY=true

# DB Gateway
DB_GATEWAY_URL=http://localhost:3003
SCAN_PRIVATE_KEY=<db_gateway 生成的私钥或复用WALLET_PRIVATE_KEY>

# Risk Control
RISK_CONTROL_URL=http://localhost:3004

# 日志
LOG_LEVEL=info
```

#### scan/solana_scan/.env
```ini
# Solana RPC 配置
SOLANA_RPC_URL=http://localhost:8899
SOLANA_RPC_URL_BACKUP=

# 数据库路径
WALLET_DB_PATH=E:/github/cex-project/cex-wallet/db_gateway/wallet.db

# 扫描配置
START_SLOT=0
CONFIRMATION_THRESHOLD=32
SCAN_INTERVAL=0.4

# 服务配置
LOG_LEVEL=info
AUTO_START=true

# DB Gateway
DB_GATEWAY_URL=http://localhost:3003
DB_GATEWAY_SECRET=your-secret-key-here
SCAN_PRIVATE_KEY=<db_gateway 生成的私钥>

# Risk Control
RISK_CONTROL_URL=http://localhost:3004
```

---

## 四、启动服务

### 启动顺序 (必须按此顺序)

```bash
# 1. 启动 DB Gateway (基础服务)
cd db_gateway
npm run dev &

# 2. 启动 Risk Control
cd risk_control
npm run dev &

# 3. 启动 Signer (需要输入密码)
cd signer
echo "12345678" | npm run dev &
# 或直接运行后手动输入密码

# 4. 启动 Wallet API
cd wallet
npm run dev &

# 5. 启动 Scan 服务 (可选，需要先创建钱包地址)
cd scan/evm_scan
npm run dev &

cd scan/solana_scan
npm run dev &
```

### 验证服务状态

```bash
# 健康检查
curl http://localhost:3000/health  # Wallet
curl http://localhost:3001/health  # Signer
curl http://localhost:3002/api/scan/status  # EVM Scan (可选)
curl http://localhost:3003/health  # DB Gateway
curl http://localhost:3004/health  # Risk Control
```

---

## 五、启动区块链测试网络 (开发环境)

### EVM 测试网络
```bash
# 启动 anvil (Foundry)
anvil --block-time 1

# 验证
curl http://localhost:8545
```

### Solana 测试网络
```bash
# 启动 solana-test-validator
solana-test-validator

# 验证
solana cluster-version
```

---

## 六、初始化测试数据

### 6.1 创建用户钱包
```bash
# 创建 EVM 钱包地址
curl -X POST "http://localhost:3000/api/user/1/address?chain_type=evm"

# 创建 Solana 钱包地址
curl -X POST "http://localhost:3000/api/user/1/address?chain_type=solana"
```

### 6.2 部署测试代币 (需要测试网络运行中)
```bash
cd wallet

# 部署 ERC20 代币合约
npm run deploy:erc20:tokens

# 部署 SPL Token (Solana)
npm run deploy:solana:tokens
```

### 6.3 初始化测试钱包和代币数据
```bash
cd wallet
npm run mock:init
```

### 6.4 模拟交易测试
```bash
cd wallet

# EVM 存款模拟
npm run mock:evm:transfer

# Solana 存款模拟
npm run mock:solana:transfer

# EVM 提现模拟
npm run mock:withdraw:evm

# Solana 提现模拟
npm run mock:withdraw:solana:sol

# 审核提现
npm run mock:approveReview
```

---

## 七、常见问题

### 问题 1: 数据库无法打开
```
错误：SQLITE_CANTOPEN: unable to open database file
```
**解决**: 确保 `.env` 中的路径使用 Windows 格式：
- 正确：`E:/github/cex-project/cex-wallet/db_gateway/wallet.db`
- 或：`E:\github\cex-project\cex-wallet\db_gateway\wallet.db`

### 问题 2: 签名验证失败
```
错误：签名验证配置缺失
```
**解决**: 检查 `signer/.env` 是否配置了 `RISK_PUBLIC_KEY` 和 `WALLET_PUBLIC_KEY`

### 问题 3: Scan 服务启动失败
```
错误：数据库中没有活跃的钱包地址需要监控
```
**解决**: 这是正常的。先启动其他服务，创建钱包地址后再启动 Scan

### 问题 4: 端口被占用
```
错误：EADDRINUSE
```
**解决**: 查找并停止占用端口的进程
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <进程 ID> /F
```

### 问题 5: Signer 密码验证失败
```
错误：密码验证失败，地址不匹配
```
**解决**: 如果更换了助记词或密码，需要删除 `signer/signer.db` 重新初始化

---

## 八、数据库重置

如需完全重置数据库：
```bash
cd cex-wallet

# 删除所有数据库文件
rm db_gateway/wallet.db*
rm risk_control/risk_control.db*
rm signer/signer.db*

# 删除日志
rm -rf db_gateway/logs
rm -rf risk_control/logs
rm -rf scan/*/logs

# 重新启动服务
# (按第四节顺序)
```

---

## 九、密钥管理记录

部署时请记录以下密钥（生产环境妥善保管）：

### DB Gateway 生成的密钥
- 公钥：`_________________________________`
- 私钥：`_________________________________`

### Risk Control 密钥
- 公钥：`_________________________________`
- 私钥：`_________________________________`

### Signer 配置
- 助记词：`_________________________________`
- 设备名：`_________________________________`

---

## 十、快速启动脚本

创建 `start_all.bat` (Windows) 或 `start_all.sh` (Linux/Mac)：

```batch
@echo off
echo Starting CEX Wallet Services...

cd db_gateway
start "DB Gateway" cmd /k "npm run dev"
timeout /t 3

cd ..\risk_control
start "Risk Control" cmd /k "npm run dev"
timeout /t 2

cd ..\signer
start "Signer" cmd /k "echo 12345678 | npm run dev"
timeout /t 3

cd ..\wallet
start "Wallet" cmd /k "npm run dev"
timeout /t 3

cd ..\scan\evm_scan
start "EVM Scan" cmd /k "npm run dev"

cd ..\solana_scan
start "Solana Scan" cmd /k "npm run dev"

echo All services started!
```

---

## 十一、换电脑部署清单

换电脑时需要：

1. [ ] 安装 Node.js 18+
2. [ ] 安装 Foundry (anvil)
3. [ ] 安装 Solana CLI
4. [ ] 克隆代码
5. [ ] 执行 `npm install` 所有模块
6. [ ] 重新生成密钥对（或从安全位置恢复）
7. [ ] 配置所有 `.env` 文件
8. [ ] 启动服务
9. [ ] 验证健康检查
10. [ ] (可选) 启动测试网络
11. [ ] (可选) 初始化测试数据

---

## 十二、API 快速参考

完整 API 文档见 `API_USAGE.md`

### 创建钱包
```bash
POST http://localhost:3000/api/user/{user_id}/address?chain_type=evm
```

### 获取余额
```bash
GET http://localhost:3000/api/user/{user_id}/balance/total
GET http://localhost:3000/api/user/{user_id}/balance/pending
GET http://localhost:3000/api/user/{user_id}/balance/token/{SYMBOL}
```

### 提现
```bash
POST http://localhost:3000/api/user/withdraw
{
  "userId": 123,
  "to": "0x...",
  "amount": "1.5",
  "tokenSymbol": "USDT",
  "chainId": 1,
  "chainType": "evm"
}
```

### 获取提现记录
```bash
GET http://localhost:3000/api/user/{user_id}/withdraws
GET http://localhost:3000/api/withdraws/pending
```

---

## 十三、服务端口汇总

| 服务 | 端口 | 说明 |
|------|------|------|
| Wallet | 3000 | 主 API 服务 |
| Signer | 3001 | 地址生成/签名 |
| EVM Scan | 3002 | EVM 链扫描 |
| DB Gateway | 3003 | 数据库网关 |
| Risk Control | 3004 | 风控服务 |
| Anvil | 8545 | EVM 测试网络 |
| Solana | 8899 | Solana 测试网络 |
