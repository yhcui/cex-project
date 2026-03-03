# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 在此代码仓库中工作提供指导。

## 项目概述

中心化交易所 (CEX) 钱包系统。多模块微服务架构，提供钱包管理、区块链扫描、风控控制和安全的数据库访问。

## 架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Wallet    │────>│   Signer    │     │  DB Gateway │
│  (端口 3000) │     │  (端口 3001)│     │  (端口 3003)│
└─────────────┘     └─────────────┘     └─────────────┘
       │                                         │
       v                                         v
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│Risk Control │     │  EVM Scan   │     │  Solana     │
│  (端口 3004)│     │  (端口 3002)│     │   Scan      │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 模块说明

| 模块 | 端口 | 功能 |
|------|------|------|
| **wallet** | 3000 | 主钱包 API - 用户地址、余额、提现 |
| **signer** | 3001 | 从助记词生成地址 (使用 viem，支持 EVM) |
| **scan/evm_scan** | 3002 | EVM 链扫描器，含区块重组处理 |
| **scan/solana_scan** | - | Solana 扫描器 (基于槽位，ATA 映射) |
| **db_gateway** | 3003 | 安全数据库网关，Ed25519 签名验证 |
| **risk_control** | 3004 | 敏感操作的风险评估和人工审核 |

### 核心设计模式

1. **数据库隔离**: 所有模块只能通过 `db_gateway` 访问 `wallet.db`，无直接数据库连接
2. **双签名机制**: 敏感操作 (credits 表) 需要业务签名 + 风控签名
3. **Operation ID**: 基于 UUID 的防重放攻击机制，记录在 `used_operation_ids` 表
4. **链抽象**: EVM 和 Solana 有独立扫描器，但使用统一的余额/流水表

## 常用命令

### 开发环境设置

```bash
# 安装依赖 (每个模块)
npm install

# 编译 TypeScript
npm run build

# 开发模式 (热重载)
npm run dev
```

### 启动顺序

```bash
db_gateway → risk_control → signer → wallet → scan
```

### 测试

```bash
# 运行测试
npm test

# Wallet 模块模拟数据脚本
npm run mock:init                    # 初始化测试钱包
npm run mock:evm:transfer            # 模拟 EVM 存款
npm run mock:solana:transfer         # 模拟 Solana 存款
npm run mock:withdraw:evm            # 请求 EVM 提现
npm run mock:approveReview           # 批准提现审核
```

## 数据库结构

`wallet.db` 核心表:
- `users` - 用户账户 (normal/sys_hot_wallet/sys_multisig)
- `wallets` - 所有钱包地址 (user/hot/multisig/cold)
- `tokens` - 代币元数据 (多链，精度标准化)
- `credits` - 资金流水 (deposit/withdraw/trade/freeze)
- `withdraws` - 提现记录，含风控状态
- `transactions` - EVM 链上交易
- `solana_*` - Solana 专用表 (槽位/代币账户)

## API 认证

模块通过 Ed25519 签名认证:
1. 生成密钥对：`POST /generate-keypair` (db_gateway)
2. 公钥 → db_gateway 的 `.env`
3. 私钥 → 请求模块的 `.env`
4. 签名数据：JSON 字符串化的操作数据 + 时间戳

## 风控流程

```
用户提现 → risk_reviewing → [auto_approve | manual_review | deny]
                             ↓
                       signing → pending → processing → confirmed
```

## 环境变量

每个模块都有 `.env.example` - 复制到 `.env` 并配置:
- RPC URLs (ETH_RPC_URL, SOLANA_RPC_URL)
- 数据库路径 (WALLET_DB_PATH)
- 端口 (PORT)
- 签名验证的公钥/私钥

## 安全考虑

- 助记词永不离开 `signer` 模块
- 签名私钥仅存储在环境变量中
- 5 分钟时间窗口防重放攻击
- 敏感表 (credits) 需要双签名验证

## 外部依赖

- **Node.js 18+** - 所有模块的运行环境
- **Foundry (anvil)** - 本地 EVM 测试链
- **Solana CLI** - 本地 Solana 测试网
- **SQLite3** - 数据库 (自动创建)

## 部署文档

- **SETUP.md** - 完整部署指南，包含换电脑部署清单
- **LOCAL_SETUP_COMPLETE.md** - 当前环境配置摘要

### 2026-03-03 部署记录

本次部署生成的密钥对：

**DB Gateway 公钥** (Wallet/Scan 共用):
```
WALLET_PUBLIC_KEY=92c8c75013be3207f92b7450999d20947539c2285a0299214559b2e6b836c61b
```

**Risk Control 公钥**:
```
RISK_PUBLIC_KEY=8c097825fa6837b20d53062260ce9ca86cd1f41d67f91c967bb01b3cd5fd0351
```

**Signer 配置**:
- 助记词：`test test test test test test test test test test test junk` (测试用)
- 密码：`12345678`

换电脑后需要：
1. 重新运行 `npm install` 安装所有依赖
2. 重新生成密钥对并配置 `.env` 文件
3. 按启动顺序重启服务

详细步骤见 `SETUP.md`
