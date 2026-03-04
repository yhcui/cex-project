# CEX 钱包系统 - 研发人员面试题库（含答案）

> 本文档涵盖中心化交易所钱包系统研发岗位的技术面试问题，包含完整参考答案。

---

## 📋 目录

1. [项目架构与基础](#第一部分项目架构与基础)
2. [Wallet 模块](#第二部分 wallet 模块)
3. [Signer 模块](#第三部分 signer 模块)
4. [Scan 扫描模块](#第四部分 scan 扫描模块)
5. [DB Gateway 模块](#第五部分 db-gateway 模块)
6. [Risk Control 风控模块](#第六部分 risk-control 风控模块)
7. [安全与加密](#第七部分安全与加密)
8. [区块链知识](#第八部分区块链知识)
9. [场景设计与系统设计](#第九部分场景设计与系统设计)

---

## 第一部分：项目架构与基础

### 🔰 初级

#### 1.1 请简述 CEX 钱包系统的整体架构

**参考答案**：

CEX 钱包系统采用微服务架构，包含 5 个核心模块：

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

**核心设计原则**：
1. **数据库隔离** - 所有模块只能通过 db_gateway 访问数据库，无直接连接
2. **双签名机制** - 敏感操作需要业务签名 + 风控签名
3. **链抽象** - EVM 和 Solana 有独立扫描器，但使用统一的余额表

---

#### 1.2 系统包含哪些模块？各自的端口号是什么？

**参考答案**：

| 模块 | 端口 | 功能 |
|------|------|------|
| **wallet** | 3000 | 用户钱包 API、余额查询、提现请求 |
| **signer** | 3001 | HD 钱包地址生成、交易签名 |
| **evm_scan** | 3002 | EVM 链扫描、存款检测、区块重组处理 |
| **db_gateway** | 3003 | 数据库访问网关、Ed25519 签名验证 |
| **risk_control** | 3004 | 风险评估、人工审核、风控签名 |

---

#### 1.3 为什么采用微服务架构而不是单体架构？

**参考答案**：

**优势**：
1. **安全隔离** - Signer 模块管理助记词，独立部署降低泄露风险
2. **独立扩展** - Scan 模块可以水平扩展应对高并发扫描
3. **故障隔离** - 单个模块故障不影响其他服务
4. **技术选型灵活** - 不同模块可使用不同技术栈
5. **部署灵活** - 可分别部署在不同安全级别的网络区域

**权衡**：
- 增加了运维复杂度
- 需要服务间认证机制（Ed25519 签名）
- 需要统一的数据访问控制（db_gateway）

---

#### 1.4 模块间如何通信？

**参考答案**：

**通信方式**：HTTP REST API

**认证机制**：
```typescript
// 1. 生成密钥对
POST /generate-keypair  // db_gateway

// 2. 配置公钥
WALLET_PUBLIC_KEY → db_gateway 的.env
SCAN_PUBLIC_KEY → db_gateway 的.env
RISK_PUBLIC_KEY → db_gateway 的.env

// 3. 配置私钥
WALLET_PRIVATE_KEY → wallet 模块.env
SCAN_PRIVATE_KEY → scan 模块.env
RISK_PRIVATE_KEY → risk_control 模块.env

// 4. 请求签名
signature = Ed25519.sign(payload, privateKey)

// 5. 验证签名
isValid = Ed25519.verify(payload, signature, publicKey)
```

**签名载荷**：
```json
{
  "operation_id": "uuid-v4",
  "operation_type": "sensitive",
  "table": "credits",
  "action": "insert",
  "data": {...},
  "conditions": {...},
  "timestamp": 1709555200000
}
```

---

### 🔶 中级

#### 1.5 系统启动顺序是什么？为什么需要这个顺序？

**参考答案**：

**启动顺序**：
```
db_gateway → risk_control → signer → wallet → scan
```

**原因分析**：

1. **db_gateway (3003)** - 最先启动
   - 提供数据库访问接口
   - 生成密钥对用于签名验证
   - 其他模块依赖它访问数据

2. **risk_control (3004)** - 第二个启动
   - 提供风控评估服务
   - 敏感操作需要风控签名
   - wallet 和 scan 依赖它

3. **signer (3001)** - 第三个启动
   - 提供地址生成和交易签名
   - 需要输入密码验证助记词
   - wallet 依赖它创建钱包

4. **wallet (3000)** - 第四个启动
   - 对外 API 服务
   - 依赖 signer 创建地址
   - 依赖 db_gateway 存储数据
   - 依赖 risk_control 风控评估

5. **scan (3002)** - 最后启动
   - 扫描链上存款
   - 依赖数据库记录监控地址
   - 依赖 db_gateway 写入数据

---

#### 1.6 请描述系统的核心设计模式有哪些？

**参考答案**：

| 设计模式 | 应用场景 | 好处 |
|----------|----------|------|
| **网关模式** | db_gateway | 统一数据库访问控制 |
| **策略模式** | WithdrawHandler | EVM/Solana 不同链处理 |
| **单例模式** | 各服务客户端 | 共享连接和配置 |
| **工厂模式** | WithdrawHandlerFactory | 根据链类型创建处理器 |
| **观察者模式** | ConfirmationManager | 交易状态变更通知 |
| **责任链模式** | 签名验证中间件 | 多步骤验证流程 |

---

#### 1.7 为什么所有模块不能直接访问数据库，必须通过 db_gateway？

**参考答案**：

**核心原因**：

1. **安全控制**
   - 统一的签名验证机制
   - 防止 SQL 注入攻击
   - 敏感操作需要双签名

2. **审计追踪**
   - 所有数据库操作可追溯
   - 记录操作人、时间、内容
   - 便于问题排查

3. **权限隔离**
   - 不同模块有不同的操作权限
   - 敏感表（credits）需要额外风控签名
   - 防止越权操作

4. **数据一致性**
   - 统一的事务管理
   - 防重放攻击（operation_id）
   - 时间窗口验证（5 分钟）

5. **便于扩展**
   - 数据库迁移对上层透明
   - 可以添加缓存层
   - 可以切换数据库类型

---

### 🔴 高级

#### 1.8 如果要添加一个新的区块链支持（如 BTC），需要修改哪些模块？

**参考答案**：

**需要修改的模块**：

1. **Signer 模块** ⭐⭐⭐
   ```typescript
   // 新增 btcSigner.ts
   - 实现 BTC HD 派生路径：m/84'/0'/0'/0/0
   - 实现 BTC 交易签名（PSBT 格式）
   - 新增 BTC 地址格式验证（Bech32）
   ```

2. **Scan 模块** ⭐⭐⭐
   ```typescript
   // 新增 btc_scan 服务
   - 连接 BTC 节点（RPC/WebSocket）
   - 扫描 UTXO 到账
   - 处理 BTC 重组（深度通常 6 块）
   ```

3. **Wallet 模块** ⭐⭐
   ```typescript
   // withdraw/handlerFactory.ts
   - 新增 BtcWithdrawHandler
   - 实现 BTC 费用估算（sat/vB）
   - 处理 BTC 提现地址格式验证
   ```

4. **数据库** ⭐⭐
   ```sql
   -- tokens 表支持 BTC 链
   INSERT INTO tokens (chain_id, chain_type, token_symbol)
   VALUES (1, 'btc', 'BTC');

   -- wallets 表支持 BTC 地址
   -- 现有设计已支持多链
   ```

5. **DB Gateway** ⭐
   - 配置 BTC 公钥
   - 无需代码修改

**工作量评估**：约 2-3 周

---

#### 1.9 如何保证模块间的调用安全？

**参考答案**：

**多层安全措施**：

1. **Ed25519 签名验证**
   ```typescript
   // 生成签名
   const signature = nacl.sign.detached(messageBytes, privateKey);

   // 验证签名
   const isValid = nacl.sign.detached.verify(messageBytes, signature, publicKey);
   ```

2. **防重放攻击**
   ```typescript
   // operation_id 唯一性
   const operationId = uuidv4();

   // db_gateway 记录已使用的 operation_id
   INSERT INTO used_operation_ids (operation_id, used_at)
   VALUES (?, ?);

   // 5 分钟过期清理
   DELETE FROM used_operation_ids
   WHERE used_at < datetime('now', '-5 minutes');
   ```

3. **时间窗口验证**
   ```typescript
   const timeDiff = Math.abs(Date.now() - timestamp);
   const FIVE_MINUTES = 5 * 60 * 1000;

   if (timeDiff > FIVE_MINUTES) {
     throw new Error('签名已过期');
   }
   ```

4. **CORS 限制**
   ```typescript
   cors({
     origin: ['http://localhost:3001', 'http://localhost:3002'],
     methods: ['GET', 'POST']
   });
   ```

5. **敏感操作双签名**
   ```typescript
   // credits 表操作需要
   - business_signature (wallet/scan)
   - risk_signature (risk_control)
   ```

---

#### 1.10 如果 wallet 模块频繁调用 db_gateway 导致性能瓶颈，如何优化？

**参考答案**：

**优化方案**（由简到繁）：

1. **连接池优化**
   ```typescript
   // db_gateway 使用连接池
   const db = sqlite3.Database(':memory:', {
     max: 10,
     idleTimeout: 30000
   });
   ```

2. **批量操作**
   ```typescript
   // 当前：逐笔调用
   await dbGateway.createCredit(credit1);
   await dbGateway.createCredit(credit2);

   // 优化：批量调用
   await dbGateway.createCreditsBatch([credit1, credit2]);
   ```

3. **本地缓存层**
   ```typescript
   // wallet 模块添加 Redis 缓存
   class BalanceCache {
     async getUserBalance(userId, tokenId) {
       const cached = await redis.get(`balance:${userId}:${tokenId}`);
       if (cached) return JSON.parse(cached);

       const balance = await dbGateway.query(...);
       await redis.setex(`balance:${userId}:${tokenId}`, 60, balance);
       return balance;
     }
   }
   ```

4. **读写分离**
   ```typescript
   // db_gateway 拆分读服务和写服务
   - 读操作：直接查询数据库（可多个副本）
   - 写操作：通过签名验证（主库）
   ```

5. **异步队列**
   ```typescript
   // 非实时写入使用消息队列
   await kafka.produce('credit-events', {
     userId, amount, type: 'deposit'
   });

   // 消费者批量写入数据库
   consumer.on('credit-events', async (events) => {
     await dbGateway.batchInsert(events);
   });
   ```

**推荐方案**：先实施 1+2，根据监控数据决定是否需要 3+4+5

---

## 第二部分：Wallet 模块

### 🔰 初级

#### 2.1 Wallet 模块的核心职责是什么？

**参考答案**：

**核心职责**：

1. **用户钱包管理**
   - 创建用户钱包地址（调用 Signer）
   - 查询用户钱包信息

2. **余额查询**
   - 查询用户总余额
   - 查询代币余额详情
   - 查询充值中余额

3. **提现处理**
   - 受理用户提现请求
   - 风控评估
   - 调用 Signer 签名
   - 发送链上交易

4. **提现记录查询**
   - 用户提现历史
   - 提现状态跟踪

**不提供**：
- 直接数据库访问（通过 db_gateway）
- 交易签名（由 Signer 负责）
- 链上扫描（由 Scan 负责）

---

#### 2.2 用户提现 API 的端点是什么？需要哪些参数？

**参考答案**：

**端点**：
```
POST /api/user/withdraw
```

**请求参数**：
```json
{
  "userId": 123,                    // 用户 ID（必填）
  "to": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb", // 提现地址（必填）
  "amount": "100.5",                // 提现金额（必填）
  "tokenSymbol": "USDT",            // 代币符号（必填）
  "chainType": "evm",               // 链类型：evm/solana（必填）
  "chainId": 1                      // 链 ID（必填）
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "withdrawId": 456,
    "txHash": "0x...",
    "status": "signing"
  }
}
```

**验证规则**：
- 地址格式验证（EVM: 0x 开头，Solana: Base58）
- 金额格式验证（正数，精度检查）
- 链类型验证（evm/solana）

---

#### 2.3 如何获取用户钱包地址？

**参考答案**：

**API 端点**：
```
GET /api/user/:id/address?chain_type=evm
```

**处理流程**：

```typescript
// routes/wallet.ts
async function getUserAddress(req, res) {
  const { id: userId } = req.params;
  const { chain_type } = req.query;

  // 1. 查询数据库是否已有地址
  const existingWallet = await dbGateway.query({
    table: 'wallets',
    action: 'select',
    conditions: { user_id: userId, chain_type }
  });

  if (existingWallet) {
    return res.json({ address: existingWallet.address });
  }

  // 2. 调用 Signer 创建新地址
  const newWallet = await signerClient.createWallet(chain_type);

  // 3. 保存到数据库
  await dbGateway.createWallet({
    user_id: userId,
    address: newWallet.address,
    chain_type,
    path: newWallet.path
  });

  // 4. Solana 需要为所有代币生成 ATA
  if (chain_type === 'solana') {
    await generateAtaForAllTokens(newWallet.address);
  }

  res.json({ address: newWallet.address });
}
```

---

### 🔶 中级

#### 2.4 请描述提现的完整业务流程

**参考答案**：

**完整流程图**：

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 用户请求                                                      │
│    POST /api/user/withdraw                                      │
│    {userId, to, amount, tokenSymbol, chainType}                │
├─────────────────────────────────────────────────────────────────┤
│ 2. 参数验证                                                      │
│    - 地址格式验证                                                │
│    - 金额格式验证                                                │
│    - 链类型验证                                                  │
├─────────────────────────────────────────────────────────────────┤
│ 3. 查询代币信息                                                  │
│    - decimals: 精度                                              │
│    - withdraw_fee: 提现手续费                                    │
│    - min_withdraw_amount: 最小提现金额                           │
├─────────────────────────────────────────────────────────────────┤
│ 4. 金额转换                                                      │
│    amount = parseUnits(amount, decimals) // 转为最小单位        │
│    fee = parseUnits(withdraw_fee, decimals)                    │
│    actualAmount = amount - fee                                 │
├─────────────────────────────────────────────────────────────────┤
│ 5. 余额检查                                                      │
│    if (userBalance < amount + fee) {                           │
│      throw new Error('余额不足');                               │
│    }                                                            │
├─────────────────────────────────────────────────────────────────┤
│ 6. 风控评估                                                      │
│    POST /api/withdraw-risk-assessment                           │
│    ↓                                                            │
│    - 自动批准 → 返回 risk_signature                             │
│    - 需要审核 → 返回 202，等待人工审批                           │
│    - 拒绝 → 返回 403                                            │
├─────────────────────────────────────────────────────────────────┤
│ 7. 选择热钱包                                                    │
│    - 查询余额足够的热钱包                                        │
│    - 检查 nonce 可用性                                           │
├─────────────────────────────────────────────────────────────────┤
│ 8. 构建交易参数                                                  │
│    - EVM: gas 估算，nonce                                        │
│    - Solana: 获取 blockhash                                      │
├─────────────────────────────────────────────────────────────────┤
│ 9. 双重签名                                                      │
│    - 已有 risk_signature                                         │
│    - 生成 wallet_signature                                       │
│    - 调用 Signer 签名                                             │
├─────────────────────────────────────────────────────────────────┤
│ 10. 发送交易                                                     │
│     sendTransaction(signedTx) → txHash                          │
├─────────────────────────────────────────────────────────────────┤
│ 11. 更新状态                                                     │
│     withdraws.status = 'pending'                                │
├─────────────────────────────────────────────────────────────────┤
│ 12. 创建 Credit 流水                                              │
│     - 用户支出：amount < 0                                      │
│     - 热钱包支出：fee < 0                                       │
├─────────────────────────────────────────────────────────────────┤
│ 13. 返回结果                                                     │
│     {withdrawId, txHash, status: 'pending'}                     │
└─────────────────────────────────────────────────────────────────┘
```

---

#### 2.5 BalanceService 是如何计算用户余额的？

**参考答案**：

**余额计算原理**：

基于 `credits` 表的 `amount` 字段聚合：

```sql
-- 用户余额查询
SELECT
  c.address,
  c.token_id,
  c.token_symbol,
  SUM(CAST(c.amount AS REAL)) as balance
FROM credits c
WHERE c.user_id = ?
  AND c.status IN ('confirmed', 'finalized')  -- 只计算已确认的
  AND c.credit_type != 'freeze'                -- 排除冻结
GROUP BY c.address, c.token_id, c.token_symbol;
```

**代码实现**：

```typescript
class BalanceService {
  async getUserBalances(userId: number, tokenId?: number) {
    const sql = `
      SELECT
        address,
        token_id,
        token_symbol,
        SUM(CAST(amount AS REAL)) as balance,
        COUNT(*) as credit_count
      FROM credits
      WHERE user_id = ?
        AND status IN ('confirmed', 'finalized')
        ${tokenId ? 'AND token_id = ?' : ''}
      GROUP BY address, token_id, token_symbol
    `;

    const balances = await database.all(sql, tokenId ? [userId, tokenId] : [userId]);

    return balances.map(b => ({
      address: b.address,
      tokenId: b.token_id,
      symbol: b.token_symbol,
      balance: b.balance,
      pendingCount: b.credit_count
    }));
  }
}
```

**金额方向**：
- `amount > 0`: 入账（存款、退款、奖励）
- `amount < 0`: 出账（提现、交易、手续费）
- `amount = 0`: 冻结/解冻

---

#### 2.6 什么是 Credits 表？它在系统中扮演什么角色？

**参考答案**：

**Credits 表结构**：

```sql
CREATE TABLE credits (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  address TEXT,              -- 关联的钱包地址
  token_id INTEGER NOT NULL, -- 代币 ID
  token_symbol TEXT NOT NULL,
  amount TEXT NOT NULL,      -- 金额（字符串避免精度丢失）
  credit_type TEXT NOT NULL, -- deposit/withdraw/trade/freeze/refund
  business_type TEXT NOT NULL, -- blockchain/system/manual
  reference_id TEXT,         -- 关联 ID（txHash_eventIndex）
  reference_type TEXT,       -- blockchain_tx/withdraw/trade
  chain_id INTEGER,
  chain_type TEXT,
  status TEXT NOT NULL,      -- confirmed/finalized/frozen/failed
  block_number INTEGER,      -- 区块号
  tx_hash TEXT,              -- 交易哈希
  event_index INTEGER,       -- 事件索引
  metadata TEXT,             -- JSON 元数据
  created_at DATETIME,
  updated_at DATETIME
);
```

**核心作用**：

1. **资金流水记录** - 所有资金变动的完整历史
2. **余额计算基础** - 通过聚合查询计算余额
3. **审计追踪** - 每笔资金变动可追溯
4. **双签名保护** - 敏感操作需要风控签名
5. **状态管理** - confirmed → finalized 状态流转

**信用类型**：

| credit_type | 说明 | 金额方向 |
|-------------|------|----------|
| deposit | 充值 | + |
| withdraw | 提现 | - |
| trade | 交易 | +/- |
| freeze | 冻结 | 0 |
| refund | 退款 | + |
| fee | 手续费 | - |

---

#### 2.7 提现处理器使用了什么设计模式？有什么好处？

**参考答案**：

**设计模式**：策略模式（Strategy Pattern）

**接口定义**：

```typescript
// types.ts
interface IWithdrawHandler {
  estimateGas(context: WithdrawContext, tokenInfo: any): Promise<bigint>;
  prepareTransactionParams(context: WithdrawContext): Promise<any>;
  buildSignRequest(context: WithdrawContext, params: any): Promise<any>;
  sendTransaction(signedTx: any, context: WithdrawContext): Promise<string>;
  afterSendTransaction(context: WithdrawContext, txHash: string): Promise<void>;
}
```

**具体实现**：

```typescript
// handlerFactory.ts
class WithdrawHandlerFactory {
  static getHandler(chainType: string): IWithdrawHandler {
    switch (chainType) {
      case 'evm':
        return new EvmWithdrawHandler();
      case 'solana':
        return new SolanaWithdrawHandler();
      default:
        throw new Error(`不支持的链类型：${chainType}`);
    }
  }
}
```

**使用方式**：

```typescript
// walletBusinessService.ts
async withdrawFunds(params) {
  // ...
  const handler = WithdrawHandlerFactory.getHandler(chainType);

  // 估算费用
  const gas = await handler.estimateGas(context, tokenInfo);

  // 准备参数
  const signParams = await handler.prepareTransactionParams(context);

  // 构建签名请求
  const signRequest = await handler.buildSignRequest(context, signParams);

  // 发送交易
  const txHash = await handler.sendTransaction(signedTx, context);
}
```

**好处**：
1. **易于扩展** - 新增链只需实现接口，不改现有代码
2. **职责清晰** - 每条链的逻辑独立封装
3. **便于测试** - 可以单独测试每个处理器
4. **代码复用** - 公共逻辑在基类或上下文中

---

### 🔴 高级

#### 2.8 双签名机制是如何实现的？请描述完整流程

**参考答案**：

**双签名场景**：`credits` 表的插入操作

**完整流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Wallet 模块发起操作                                            │
│    operation_type = 'sensitive'                                 │
│    table = 'credits'                                            │
│    action = 'insert'                                            │
├─────────────────────────────────────────────────────────────────┤
│ 2.生成 operation_id 和 timestamp                                  │
│    operation_id = uuidv4()                                      │
│    timestamp = Date.now()                                       │
├─────────────────────────────────────────────────────────────────┤
│ 3. 请求风控签名                                                   │
│    POST http://risk_control:3004/api/assess                     │
│    {                                                            │
│      operation_id, operation_type, table, action,               │
│      data, conditions, timestamp                                │
│    }                                                            │
│    ↓                                                            │
│    风控验证通过，生成 risk_signature                            │
├─────────────────────────────────────────────────────────────────┤
│ 4. 生成业务签名                                                   │
│    payload = {operation_id, ..., timestamp}                     │
│    business_signature = Ed25519.sign(payload, walletPrivateKey) │
├─────────────────────────────────────────────────────────────────┤
│ 5. 请求 db_gateway                                               │
│    POST http://db_gateway:3003/api/database/execute             │
│    {                                                            │
│      operation_id, operation_type, table, action,               │
│      data, conditions, timestamp,                               │
│      business_signature,                                        │
│      risk_signature                                             │
│    }                                                            │
├─────────────────────────────────────────────────────────────────┤
│ 6. db_gateway 验证                                                │
│    a) 验证 operation_id 未使用                                    │
│    b) 验证 timestamp 在 5 分钟内                                     │
│    c) 验证 business_signature → wallet 公钥                      │
│    d) 验证 risk_signature → 风控公钥                             │
│    ↓                                                            │
│    全部通过，执行数据库操作                                      │
├─────────────────────────────────────────────────────────────────┤
│ 7. 记录 operation_id                                             │
│    INSERT INTO used_operation_ids (operation_id, used_at)      │
│    VALUES (?, ?)                                                │
└─────────────────────────────────────────────────────────────────┘
```

**代码示例**：

```typescript
// wallet 模块
async createCredit(creditData) {
  const operationId = uuidv4();
  const timestamp = Date.now();

  // 1. 请求风控签名
  const riskResult = await riskControlClient.assess({
    operation_id: operationId,
    operation_type: 'sensitive',
    table: 'credits',
    action: 'insert',
    data: creditData,
    timestamp
  });

  // 2. 生成业务签名
  const payload = {
    operation_id: operationId,
    operation_type: 'sensitive',
    table: 'credits',
    action: 'insert',
    data: creditData,
    timestamp
  };
  const businessSignature = this.signer.sign(payload);

  // 3. 请求 db_gateway
  const result = await dbGatewayClient.executeOperation({
    ...payload,
    business_signature: businessSignature,
    risk_signature: riskResult.risk_signature
  });

  return result;
}
```

---

## 第三部分：Signer 模块

### 🔰 初级

#### 3.1 Signer 模块的核心职责是什么？

**参考答案**：

**核心职责**：

1. **HD 钱包管理**
   - 从助记词派生地址
   - 管理派生路径（BIP44）
   - 存储已生成的地址

2. **交易签名**
   - EVM 交易签名（viem）
   - Solana 交易签名（@solana/kit）
   - 双签名验证

3. **安全保护**
   - 助记词永不出模块
   - 密码保护（启动时输入）
   - 地址持久化存储

**不提供**：
- 数据库直接访问（通过 db_gateway）
- 业务逻辑判断
- 链上交互（只签名不发送）

---

#### 3.2 为什么 Signer 模块启动时需要输入密码？

**参考答案**：

**原因**：

1. **助记词加密**
   ```typescript
   // 助记词本身不加密，但派生种子需要密码
   const seed = mnemonicToSeedSync(mnemonic, password);
   //                    ↑
   //              密码作为盐值
   ```

2. **密码验证机制**
   ```typescript
   async validatePassword() {
     // 1. 获取第一个已保存的地址
     const firstAddress = await db.getFirstGeneratedAddress();

     // 2. 用当前密码重新派生
     const derivedAddress = deriveAddress(mnemonic, password, firstAddress.path);

     // 3. 比较地址是否匹配
     return derivedAddress === firstAddress.address;
   }
   ```

3. **安全意义**
   - 即使助记词泄露，没有密码也无法使用
   - 密码错误时无法派生正确的私钥
   - 防止未授权启动

4. **交互式输入**
   ```typescript
   // passwordInput.ts
   async function promptForPassword() {
     process.stdin.setRawMode(true);  // 隐藏输入
     // ...
   }
   ```

---

#### 3.3 Signer 模块支持哪些链的签名？

**参考答案**：

| 链类型 | 状态 | 实现文件 |
|--------|------|----------|
| **EVM** | ✅ 已实现 | `evmSigner.ts` |
| **Solana** | ✅ 已实现 | `solanaSigner.ts` |
| **BTC** | ❌ 未实现 | `btcSigner.ts` (返回错误) |

**EVM 支持**：
- 原生代币转账（ETH/BNB 等）
- ERC20 代币转账
- EIP-1559 交易类型

**Solana 支持**：
- SOL 原生转账
- SPL Token 转账
- ATA 地址自动计算

---

### 🔶 中级

#### 3.4 什么是 HD 钱包？派生路径是什么？

**参考答案**：

**HD 钱包**（Hierarchical Deterministic Wallet）：

从一个种子（seed）可以派生出多个子密钥的钱包系统。

**优势**：
1. **备份简单** - 只需备份一个助记词
2. **隐私保护** - 每个用户使用不同地址
3. **密钥管理** - 无需存储每个私钥
4. **离线派生** - 公钥可以离线派生

**派生路径标准**：

```
m / purpose' / coin_type' / account' / change / index
│       │          │           │         │        │
│       │          │           │         │        └─ 索引（从 0 开始）
│       │          │           │         └─ 0=外部，1=内部
│       │          │           └─ 账户索引
│       │          └─ 币种代码
│       └─ 固定为 44（BIP44）
└─ 主路径
```

**本系统路径**：

| 链 | 派生路径 | 说明 |
|----|----------|------|
| EVM | `m/44'/60'/0'/0/0` | 60=ETH 币种代码 |
| Solana | `m/44'/501'/0'/0'` | 501=SOL 币种代码 |
| BTC | `m/84'/0'/0'/0/0` | 84=BIP84 (Native SegWit) |

---

#### 3.5 EVM 和 Solana 的派生路径分别是什么？

**参考答案**：

**EVM 派生路径**：
```
m/44'/60'/0'/0/0
│  │   │  │ │ └─ index: 第 n 个地址
│  │   │  │ └── change: 0 (外部地址)
│  │   │  └─── account: 0
│  │   └────── coin_type: 60 (ETH)
│  └────────── purpose: 44 (BIP44)
└───────────── master
```

**Solana 派生路径**：
```
m/44'/501'/0'/0'
│  │   │  │ │
│  │   │  │ └─ index: 硬派生
│  │   │  └─── change: 0 (硬派生)
│  │   └────── account: 0
│  └────────── coin_type: 501 (SOL)
└───────────── purpose: 44
```

**关键区别**：
- EVM 使用 secp256k1 曲线
- Solana 使用 ed25519 曲线
- Solana 路径末端有 `'`（硬派生）

---

#### 3.6 请描述 Signer 模块的签名验证流程

**参考答案**：

**签名验证流程**：

```typescript
// index.ts - signTransaction 端点
async function signTransaction(req, res) {
  const {
    operation_id,
    timestamp,
    risk_signature,
    wallet_signature,
    // ...交易参数
  } = req.body;

  // 1. 时间戳验证（1 分钟窗口）
  const timeDiff = Math.abs(Date.now() - timestamp);
  if (timeDiff > 60000) {
    throw new Error('时间戳过期');
  }

  // 2. 验证风控签名
  const riskPayload = {
    operation_id, /*...*/, timestamp
  };
  const riskValid = SignatureValidator.verifyRiskSignature(
    riskPayload,
    risk_signature,
    RISK_PUBLIC_KEY
  );
  if (!riskValid) {
    throw new Error('风控签名验证失败');
  }

  // 3. 验证 Wallet 签名
  const walletValid = SignatureValidator.verifyWalletSignature(
    riskPayload,  // 相同载荷
    wallet_signature,
    WALLET_PUBLIC_KEY
  );
  if (!walletValid) {
    throw new Error('Wallet 签名验证失败');
  }

  // 4. 执行交易签名
  const signedTx = await signTransactionInternal(txParams);

  res.json({ signedTransaction: signedTx });
}
```

**验证要点**：
1. 时间戳窗口更短（1 分钟 vs 5 分钟）
2. 双重签名必须都通过
3. 使用相同的签名载荷

---

#### 3.7 什么是双重签名？为什么需要双重签名？

**参考答案**：

**双重签名**：

交易签名请求需要两个独立的 Ed25519 签名：
1. **风控签名**（risk_signature）- 来自 risk_control 模块
2. **业务签名**（wallet_signature）- 来自 wallet 或 scan 模块

**为什么需要**：

1. **职责分离**
   ```
   Wallet/Scan → 负责业务逻辑
   Risk Control → 负责风险评估
   Signer → 验证两者签名后才执行
   ```

2. **防止单点突破**
   - 即使 Wallet 私钥泄露，攻击者无法伪造风控签名
   - 即使风控私钥泄露，攻击者无法发起业务请求
   - 需要同时攻破两个模块

3. **风控拦截**
   ```typescript
   // 风控可以在这里拦截可疑交易
   async assessWithdraw(params) {
     if (isSuspicious(params)) {
       return { decision: 'reject' };  // 不生成签名
     }
     return {
       decision: 'approve',
       risk_signature: generateSignature()
     };
   }
   ```

4. **审计追踪**
   - 每次签名都有风控记录
   - 可追溯是谁批准的交易

---

### 🔴 高级

#### 3.8 助记词 + 密码是如何派生出私钥的？请描述技术细节

**参考答案**：

**完整派生流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 助记词生成（BIP39）                                           │
│    entropy (128-256 bits)                                       │
│      ↓ SHA-256 + checksum                                       │
│    mnemonic (12-24 words)                                       │
├─────────────────────────────────────────────────────────────────┤
│ 2. 种子生成（BIP39）                                             │
│    seed = PBKDF2-HMAC-SHA512(                                   │
│      password = mnemonic + password,                            │
│      salt = "mnemonic" + optional_passphrase,                   │
│      iterations = 2048,                                         │
│      dklen = 64                                                 │
│    )                                                            │
│                                                                 │
│    // 使用 scure-bip39                                          │
│    const seed = await mnemonicToSeed(mnemonic, password);       │
├─────────────────────────────────────────────────────────────────┤
│ 3. 主密钥生成（BIP32）                                           │
│    masterKey = HMAC-SHA512(                                     │
│      key = "Bitcoin seed",                                      │
│      data = seed                                                │
│    )                                                            │
│    masterPrivateKey = masterKey[0:32]                           │
│    masterChainCode = masterKey[32:64]                           │
│                                                                 │
│    // 使用 scure-bip32                                          │
│    const hdKey = HDKey.fromMasterSeed(seed);                   │
├─────────────────────────────────────────────────────────────────┤
│ 4. 派生子密钥                                                    │
│    childKey = derive(masterKey, path)                           │
│                                                                 │
│    // m/44'/60'/0'/0/0                                         │
│    const derived = hdKey.derive("m/44'/60'/0'/0/0");           │
│    const privateKey = derived.privateKey;                       │
├─────────────────────────────────────────────────────────────────┤
│ 5. EVM 地址生成                                                   │
│    publicKey = secp256k1.getPublicKey(privateKey)               │
│    address = keccak256(publicKey[1:])[12:20]                    │
│                                                                 │
│    // 使用 viem                                                 │
│    const account = privateKeyToAccount(`0x${privateKey}`);     │
│    const address = account.address;                             │
└─────────────────────────────────────────────────────────────────┘
```

**代码实现**：

```typescript
// evmSigner.ts
function deriveEvmAccountFromPath(mnemonic, password, path) {
  // 1. 助记词 → 种子
  const seed = mnemonicToSeedSync(mnemonic, password);

  // 2. 种子 → HD 密钥树
  const hdKey = HDKey.fromMasterSeed(seed);

  // 3. 派生路径 → 子密钥
  const derivedKey = hdKey.derive(path);

  // 4. 获取私钥
  const privateKeyHex = `0x${Buffer.from(derivedKey.privateKey).toString('hex')}`;

  // 5. viem account
  const account = privateKeyToAccount(privateKeyHex);

  return {
    address: account.address,
    privateKey: privateKeyHex
  };
}
```

---

## 第四部分：Scan 扫描模块

### 🔰 初级

#### 4.1 Scan 模块的核心职责是什么？

**参考答案**：

**核心职责**：

1. **区块扫描**
   - 定时扫描新区块
   - 批量处理提高效率
   - 支持历史区块补扫

2. **存款检测**
   - 监听用户地址入账
   - 支持 ETH 和 ERC20
   - Bloom 过滤器优化

3. **重组处理**
   - 检测区块哈希变化
   - 回滚孤立区块
   - 恢复正确数据

4. **确认管理**
   - 跟踪交易确认数
   - confirmed → safe → finalized
   - 支持网络终结性

5. **提现监控**
   - 跟踪提现交易状态
   - 链上确认处理
   - 失败处理

---

#### 4.2 什么是区块确认数（confirmation）？为什么需要等待确认？

**参考答案**：

**区块确认数**：

交易所在区块之后新增的区块数量。

```
区块高度：100    101    102    103
         │      │      │      │
         │ 交易  │      │      │
         │      │ +1   │ +2   │ +3
         │             │      │
         └─────────────┴──────┴──→ 确认数增加
```

**为什么需要等待**：

1. **防止重组**
   - 短重组：常见于网络延迟
   - 长重组：恶意攻击或网络分割

2. **确认阈值**：
   | 链 | 建议确认数 | 安全级别 |
   |----|-----------|----------|
   | ETH | 12-32 | 高安全 |
   | BSC | 15-50 | 中等 |
   | BTC | 6 | 标准 |

3. **本系统配置**：
   ```typescript
   confirmationBlocks = 32;  // 32 确认后 finalized
   ```

4. **状态流转**：
   ```
   confirmed (0 确认) → safe (16 确认) → finalized (32 确认)
   ```

---

#### 4.3 如何判断一笔交易是充值交易？

**参考答案**：

**判断条件**：

1. **接收地址是用户钱包**
   ```typescript
   const wallet = await walletDAO.getWalletByAddress(tx.to);
   if (!wallet) return false;  // 不是用户地址
   ```

2. **交易类型符合**
   ```typescript
   // ETH 转账
   if (tx.value > 0n && isUserAddress(tx.to)) {
     return true;
   }

   // ERC20 转账 - 解析 Transfer 事件
   const transfer = parseERC20Transfer(log);
   if (transfer && isUserAddress(transfer.to)) {
     return true;
   }
   ```

3. **代币在支持列表**
   ```typescript
   const tokenInfo = supportedTokens.get(tokenAddress);
   if (!tokenInfo) return false;  // 不支持的代币
   ```

**完整检测流程**：

```typescript
async function detectDeposit(blockNumber) {
  // 1. 获取区块内所有相关事件
  const transferData = await viemClient.getUserTransfersInBlocks(
    blockNumber, blockNumber,
    userAddressList,
    tokenAddressList
  );

  // 2. 逐个分析
  for (const log of transferData.erc20Logs) {
    const transfer = parseERC20Transfer(log);

    // 3. 检查接收方
    if (!isUserAddress(transfer.to)) continue;

    // 4. 查询用户
    const wallet = await getWalletByAddress(transfer.to);
    if (!wallet) continue;

    // 5. 检查代币
    const token = supportedTokens.get(log.address.toLowerCase());
    if (!token) continue;

    // 是充值交易
    return {
      txHash: log.transactionHash,
      userId: wallet.user_id,
      amount: transfer.value,
      tokenSymbol: token.token_symbol
    };
  }
}
```

---

### 🔶 中级

#### 4.4 请描述区块扫描的完整流程

**参考答案**：

**完整扫描流程**：

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 初始化                                                        │
│    - 连接数据库                                                  │
│    - 加载用户地址和代币列表                                       │
│    - 获取最后扫描区块                                             │
├─────────────────────────────────────────────────────────────────┤
│ 2. 获取扫描范围                                                  │
│    latestBlock = await viemClient.getLatestBlockNumber()        │
│    lastScannedBlock = await db.getLastScannedBlock()           │
│    startBlock = lastScannedBlock + 1                            │
│    endBlock = min(startBlock + batchSize, latestBlock)         │
├─────────────────────────────────────────────────────────────────┤
│ 3. 选择扫描策略                                                  │
│    if (endBlock <= finalizedBlock && batchSize > 3):           │
│      strategy = 'historical'  // 历史分析模式                   │
│    else:                                                        │
│      strategy = 'batch'       // 批量模式                       │
├─────────────────────────────────────────────────────────────────┤
│ 4. 批量扫描区块                                                  │
│    for blockNumber in [startBlock...endBlock]:                 │
│      a) 获取区块 → getBlock()                                   │
│      b) 检查重组 → checkAndHandleReorg()                        │
│      c) 分析交易 → analyzeBlock()                               │
│      d) 保存区块 → insertBlock()                                │
│      e) 保存存款 → processDeposit()                             │
├─────────────────────────────────────────────────────────────────┤
│ 5. 处理确认                                                      │
│    confirmationManager.processConfirmations()                   │
│    - 检查网络终结性或确认数                                       │
│    - 更新交易状态：confirmed → safe → finalized                │
├─────────────────────────────────────────────────────────────────┤
│ 6. 更新进度                                                      │
│    - blocks 表自动记录最后扫描区块                                │
├─────────────────────────────────────────────────────────────────┤
│ 7. 定时扫描                                                      │
│    setInterval(scanNewBlocks, scanInterval * 1000)             │
│    - 每 12 秒检查是否有新区块                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

#### 4.5 什么是批量扫描模式和历史分析模式？有什么区别？

**参考答案**：

**批量扫描模式**（Batch Mode）：

```typescript
async function scanBlockBatch(startBlock, endBlock) {
  // 1. 逐个获取区块（检查重组）
  for (let n = startBlock; n <= endBlock; n++) {
    const block = await viemClient.getBlock(n);
    await reorgHandler.checkAndHandleReorg(n, block.hash);
  }

  // 2. Bloom 过滤器分析交易
  const deposits = await txAnalyzer.analyzeBatchBlocksForDeposits(
    startBlock, endBlock
  );

  // 3. 批量保存
  await dbGateway.processBlocksAndDepositsInTransaction(
    blocks, deposits
  );
}
```

**特点**：
- 适用于未 finalized 的区块
- 逐区块检查重组
- 兼容性最好

---

**历史分析模式**（Historical Mode）：

```typescript
async function scanBlockBatchHistorical(startBlock, endBlock) {
  // 1. 批量获取区块（不检查重组，假设已 finalized）
  for (let n = startBlock; n <= endBlock; n++) {
    const block = await viemClient.getBlock(n);
    await dbGateway.insertBlock({...});  // 直接保存
  }

  // 2. 使用历史分析方法（一次性获取所有事件）
  await txAnalyzer.analyzeHistoricalBlocks(startBlock, endBlock);

  // 3. 处理确认
  await confirmationManager.processConfirmations();
}
```

**特点**：
- 仅适用于 finalized 区块
- 不检查重组（假设不可变）
- 效率最高（3-5 倍提升）

---

**对比**：

| 特性 | 批量模式 | 历史模式 |
|------|----------|----------|
| 适用区块 | 所有区块 | 仅 finalized |
| 重组检查 | 逐区块 | 跳过 |
| 性能 | 标准 | 高（3-5 倍） |
| 使用场景 | 实时扫描 | 历史补扫 |

---

#### 4.6 ConfirmationManager 是如何工作的？

**参考答案**：

**核心职责**：管理交易确认状态流转

**双策略支持**：

```typescript
async processConfirmations() {
  const pendingTxs = await getPendingTransactions();

  // 策略选择
  if (config.useNetworkFinality && networkSupportsFinality) {
    await processWithNetworkFinality(pendingTxs);
  } else {
    await processWithConfirmationCount(pendingTxs);
  }
}
```

**网络终结性策略**：

```typescript
async processWithNetworkFinality(txs) {
  const safeBlock = await getCachedSafeBlock();
  const finalizedBlock = await getCachedFinalizedBlock();

  for (const tx of txs) {
    // 检查 finalized
    if (tx.blockNo <= finalizedBlock.number && tx.status !== 'finalized') {
      await finalizeTransaction(tx, 'network_finality');
      continue;
    }

    // 检查 safe
    if (tx.blockNo <= safeBlock.number && tx.status === 'confirmed') {
      await safeTransaction(tx, 'network_finality');
    }
  }
}
```

**确认数策略**（回退）：

```typescript
async processWithConfirmationCount(txs) {
  const currentBlock = await viemClient.getLatestBlockNumber();
  const safeThreshold = 16;   // confirmationBlocks / 2
  const finalizedThreshold = 32;  // confirmationBlocks

  for (const tx of txs) {
    const confirmations = currentBlock - tx.blockNo;

    if (tx.status === 'confirmed' && confirmations >= safeThreshold) {
      await safeTransaction(tx, 'confirmation_count');
    }
    else if (tx.status === 'safe' && confirmations >= finalizedThreshold) {
      await finalizeTransaction(tx, 'confirmation_count');
    }

    // 更新确认数
    await updateTransactionConfirmation(tx.txHash, confirmations);
  }
}
```

**状态流转**：

```
pending (在 withdraws 表)
  ↓
confirmed (已打包，0 确认)
  ↓ (16 确认或 safe 区块)
safe (高度确定)
  ↓ (32 确认或 finalized 区块)
finalized (不可回滚)
```

---

### 🔴 高级

#### 4.8 什么是区块链重组（Reorg）？系统如何处理重组？

**参考答案**：

**什么是重组**：

区块链发生临时分叉，之前确认的区块被新链抛弃。

```
原链：A ─ B ─ C ─ D  (数据库记录)
          │
新链：A ─ B ─ C'─ D'─ E'  (实际链)
              ↑
          重组点
```

**重组原因**：
1. 网络延迟导致临时分叉
2. 算力/质押竞争
3. 恶意攻击（51% 攻击）

---

**系统处理流程**：

```typescript
// reorgHandler.ts
async function checkAndHandleReorg(blockNumber, chainHash) {
  // 1. 检测重组
  const reorgDetected = await detectReorg(blockNumber, chainHash);
  if (!reorgDetected) return null;

  // 2. 寻找共同祖先
  const commonAncestor = await findCommonAncestor(blockNumber);

  // 3. 回滚到共同祖先
  const reorgInfo = await rollbackToCommonAncestor(
    commonAncestor,
    blockNumber
  );

  return reorgInfo;
}
```

---

**详细步骤**：

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. detectReorg() - 检测重组                                      │
│    a) 比较区块哈希：db.hash vs chain.hash                       │
│    b) 不匹配 = 重组                                              │
│    c) 匹配时还需验证父区块连续性                                 │
├─────────────────────────────────────────────────────────────────┤
│ 2. validateParentChain() - 验证父链                              │
│    for i in 1..reorgCheckDepth:                                │
│      checkBlock = blockNumber - i                              │
│      if db[checkBlock].hash != chain[checkBlock].hash:         │
│        return true  // 检测到重组                               │
├─────────────────────────────────────────────────────────────────┤
│ 3. findCommonAncestor() - 找共同祖先                             │
│    for n = blockNumber down to startBlock:                     │
│      if db[n].hash == chain[n].hash:                           │
│        return n  // 找到共同祖先                                │
├─────────────────────────────────────────────────────────────────┤
│ 4. rollbackToCommonAncestor() - 回滚                             │
│    a) 删除 Credit 记录 (commonAncestor+1 → blockNumber)          │
│    b) 逐个回滚区块：                                             │
│       - 获取可回滚交易（排除 frozen 状态）                        │
│       - 删除交易记录                                            │
│       - 标记区块为 orphaned                                      │
│    c) 返回重组统计信息                                           │
├─────────────────────────────────────────────────────────────────┤
│ 5. 重新扫描                                                      │
│    从 commonAncestor+1 开始重新扫描新区块链                       │
└─────────────────────────────────────────────────────────────────┘
```

---

**回滚逻辑**：

```typescript
async function rollbackBlock(blockNumber) {
  const dbBlock = await getBlockByNumber(blockNumber);

  // 获取可回滚交易（排除 frozen 的 Credit）
  const transactions = await db.all(`
    SELECT DISTINCT t.*
    FROM transactions t
    LEFT JOIN credits c ON t.tx_hash = c.tx_hash
    WHERE t.block_hash = ?
      AND (c.status IS NULL OR c.status != 'frozen')
  `, [dbBlock.hash]);

  // 删除交易
  for (const tx of transactions) {
    await dbGateway.deleteTransaction(tx.tx_hash);
  }

  // 标记为孤儿块
  await dbGateway.updateBlockStatus(dbBlock.hash, 'orphaned');

  return {
    hash: dbBlock.hash,
    transactionCount: transactions.length
  };
}
```

---

#### 4.9 请描述重组检测和回滚的完整流程

**参考答案**：

这是上一个问题的详细版本，以下是完整的代码级流程：

**完整代码流程**：

```typescript
// 1. 区块扫描时检测
async function scanSingleBlock(blockNumber) {
  const block = await viemClient.getBlock(blockNumber);

  // 检查重组
  const reorgInfo = await reorgHandler.checkAndHandleReorg(
    blockNumber,
    block.hash
  );

  if (reorgInfo) {
    // 检测到重组，处理回滚
    logger.warn('检测到重组', {
      commonAncestor: reorgInfo.commonAncestor,
      blocksToRescan: reorgInfo.blocksToRescan
    });

    // 重新扫描
    for (let n = reorgInfo.commonAncestor + 1; n <= blockNumber; n++) {
      const chainBlock = await viemClient.getBlock(n);
      await processValidBlock(n, chainBlock);
    }
    return;
  }

  // 正常处理
  await processValidBlock(blockNumber, block);
}

// 2. 重组检测
async function detectReorg(blockNumber, chainHash) {
  const dbBlock = await blockDAO.getBlockByNumber(blockNumber);

  if (!dbBlock) return false;  // 新块，不是重组

  if (dbBlock.hash === chainHash) {
    // 哈希匹配，检查父链连续性
    return await validateParentChain(blockNumber, chainHash);
  }

  // 哈希不匹配，确定重组
  return true;
}

// 3. 验证父链
async function validateParentChain(blockNumber, blockHash) {
  const checkDepth = Math.min(config.reorgCheckDepth, blockNumber - 1);

  for (let i = 1; i <= checkDepth; i++) {
    const checkNum = blockNumber - i;
    const chainBlock = await viemClient.getBlock(checkNum);
    const dbBlock = await blockDAO.getBlockByNumber(checkNum);

    if (!chainBlock || !dbBlock) continue;

    // 哈希不匹配
    if (dbBlock.hash !== chainBlock.hash) {
      return true;
    }

    // 父子关系检查
    if (i === 1 && chainBlock.parentHash !== dbBlock.parent_hash) {
      return true;
    }
  }

  return false;  // 无重组
}

// 4. 寻找共同祖先
async function findCommonAncestor(startBlock) {
  for (let n = startBlock; n > 0; n--) {
    const dbBlock = await blockDAO.getBlockByNumber(n);
    const chainBlock = await viemClient.getBlock(n);

    if (dbBlock && chainBlock && dbBlock.hash === chainBlock.hash) {
      return n;  // 找到共同祖先
    }
  }

  return config.startBlock - 1;  // 回滚到起点
}

// 5. 回滚操作
async function rollbackToCommonAncestor(commonAncestor, currentBlock) {
  const orphanedBlocks = [];
  let revertedTransactions = 0;

  // 删除 Credit
  const deletedCredits = await dbGateway.deleteCreditsByBlockRange(
    commonAncestor + 1,
    currentBlock
  );

  // 逐区块回滚
  for (let n = commonAncestor + 1; n <= currentBlock; n++) {
    const result = await rollbackBlock(n);
    if (result) {
      orphanedBlocks.push(result.hash);
      revertedTransactions += result.transactionCount;
    }
  }

  return {
    detectedAt: currentBlock,
    commonAncestor,
    orphanedBlocks,
    revertedTransactions,
    blocksToRescan: currentBlock - commonAncestor
  };
}
```

---

## 第五部分：DB Gateway 模块

### 🔰 初级

#### 5.1 DB Gateway 的核心职责是什么？

**参考答案**：

**核心职责**：

1. **数据库访问网关**
   - 所有数据库操作的唯一入口
   - 其他模块不能直接访问数据库

2. **签名验证**
   - Ed25519 签名验证
   - 业务签名 + 风控签名（敏感操作）

3. **防重放攻击**
   - operation_id 唯一性检查
   - 时间窗口验证（5 分钟）

4. **操作审计**
   - 记录所有数据库操作
   - 可追溯操作人和时间

**核心端点**：
```
POST /api/database/execute      - 单操作
POST /api/database/batch        - 批量操作（事务）
POST /generate-keypair          - 生成密钥对（仅开发环境）
```

---

#### 5.2 为什么需要签名验证？

**参考答案**：

**原因**：

1. **身份认证**
   ```
   验证请求来自合法的模块
   ↓
   防止未授权访问
   ```

2. **数据完整性**
   ```
   签名包含完整操作数据
   ↓
   防止请求被篡改
   ```

3. **防抵赖**
   ```
   每个操作都有签名记录
   ↓
   操作不可否认
   ```

4. **权限控制**
   ```
   不同模块有不同的公钥
   ↓
   可以精细化控制权限
   ```

5. **敏感操作双签**
   ```
   credits 表操作需要：
   - business_signature (业务模块)
   - risk_signature (风控模块)
   ↓
   双重保障
   ```

---

#### 5.3 支持哪些数据库操作类型？

**参考答案**：

**操作类型**：

| 操作 | 表 | 操作类型 | 签名要求 |
|------|----|----------|----------|
| 查询 | 所有表 | read | 业务签名 |
| 插入 | wallets, tokens | write | 业务签名 |
| 插入 | transactions | write | 业务签名 |
| 插入 | credits | sensitive | 双签名 |
| 更新 | withdraws | sensitive | 双签名 |
| 更新 | credits | sensitive | 双签名 |
| 删除 | transactions | write | 业务签名 |
| 删除 | credits | sensitive | 双签名 |

**请求格式**：

```json
{
  "operation_id": "uuid-v4",
  "operation_type": "read|write|sensitive",
  "table": "credits",
  "action": "select|insert|update|delete",
  "data": {...},           // 插入/更新的数据
  "conditions": {...},     // WHERE 条件
  "business_signature": "...",
  "risk_signature": "...", // 仅 sensitive 操作
  "timestamp": 1709555200000
}
```

---

### 🔶 中级

#### 5.4 请描述请求验证的完整流程

**参考答案**：

**完整验证流程**：

```typescript
// middleware/signature.ts
async function validateRequest(req, res, next) {
  const {
    operation_id,
    operation_type,
    table,
    action,
    data,
    conditions,
    business_signature,
    risk_signature,
    timestamp
  } = req.body;

  try {
    // 1. 参数完整性检查
    if (!operation_id || !timestamp || !business_signature) {
      return res.status(400).json({ error: '缺少必需参数' });
    }

    // 2. 时间戳验证
    const timeDiff = Math.abs(Date.now() - timestamp);
    const FIVE_MINUTES = 5 * 60 * 1000;
    if (timeDiff > FIVE_MINUTES) {
      return res.status(400).json({ error: '签名已过期' });
    }

    // 3. 防重放检查
    const used = await db.get(
      'SELECT * FROM used_operation_ids WHERE operation_id = ?',
      [operation_id]
    );
    if (used) {
      return res.status(400).json({ error: 'operation_id 已使用' });
    }

    // 4. 构建验证载荷
    const payload = {
      operation_id,
      operation_type,
      table,
      action,
      data,
      conditions,
      timestamp
    };

    // 5. 验证业务签名
    const businessPublicKey = getPublicKeyForModule('wallet');
    const businessValid = Ed25519Verifier.verify(
      payload,
      business_signature,
      businessPublicKey
    );
    if (!businessValid) {
      return res.status(401).json({ error: '业务签名验证失败' });
    }

    // 6. 验证风控签名（敏感操作）
    if (operation_type === 'sensitive') {
      if (!risk_signature) {
        return res.status(400).json({ error: '缺少风控签名' });
      }
      const riskValid = Ed25519Verifier.verify(
        payload,
        risk_signature,
        RISK_PUBLIC_KEY
      );
      if (!riskValid) {
        return res.status(401).json({ error: '风控签名验证失败' });
      }
    }

    // 7. 记录 operation_id
    await db.run(
      'INSERT INTO used_operation_ids (operation_id, used_at) VALUES (?, ?)',
      [operation_id, new Date().toISOString()]
    );

    // 8. 清理过期记录
    await db.run(`
      DELETE FROM used_operation_ids
      WHERE used_at < datetime('now', '-5 minutes')
    `);

    // 验证通过，继续处理
    next();

  } catch (error) {
    logger.error('签名验证失败', { error });
    res.status(500).json({ error: '验证失败' });
  }
}
```

---

#### 5.5 什么是 Operation ID？它的作用是什么？

**参考答案**：

**Operation ID**：

UUID v4 格式的唯一标识符，用于标识每次数据库操作。

```typescript
const operation_id = uuidv4();
// 示例：550e8400-e29b-41d4-a716-446655440000
```

**作用**：

1. **防重放攻击**
   ```typescript
   // db_gateway 记录已使用的 operation_id
   async function validateOperationId(operationId) {
     const used = await db.get(
       'SELECT * FROM used_operation_ids WHERE operation_id = ?',
       [operationId]
     );

     if (used) {
       throw new Error('operation_id 已使用，可能是重放攻击');
     }

     // 记录为已使用
     await db.run(
       'INSERT INTO used_operation_ids (operation_id, used_at) VALUES (?, ?)',
       [operationId, new Date().toISOString()]
     );
   }
   ```

2. **幂等性保证**
   ```typescript
   // 如果请求失败重试，使用相同的 operation_id
   // db_gateway 会拒绝重复的 operation_id
   // 客户端需要生成新的 operation_id 重试
   ```

3. **审计追踪**
   ```sql
   -- 可以通过 operation_id 追踪完整操作链
   SELECT * FROM audit_log WHERE operation_id = 'xxx';
   ```

4. **过期清理**
   ```sql
   -- 5 分钟后自动清理
   DELETE FROM used_operation_ids
   WHERE used_at < datetime('now', '-5 minutes');
   ```

---

#### 5.6 签名验证的时间窗口是多久？为什么？

**参考答案**：

**时间窗口**：5 分钟

```typescript
const FIVE_MINUTES = 5 * 60 * 1000;  // 300000ms

async function validateTimestamp(timestamp) {
  const timeDiff = Math.abs(Date.now() - timestamp);

  if (timeDiff > FIVE_MINUTES) {
    throw new Error('签名已过期，请重新生成');
  }
}
```

**为什么是 5 分钟**：

| 考量 | 说明 |
|------|------|
| **安全性** | 窗口越短，重放攻击风险越低 |
| **可用性** | 窗口太短会导致正常请求失败 |
| **网络延迟** | 考虑模块间调用的网络耗时 |
| **时钟同步** | 考虑服务器间的时钟偏差 |
| **重试空间** | 给失败重试留出时间窗口 |

**权衡**：
- 1 分钟：太短，网络波动易导致失败
- 5 分钟：平衡安全和可用性（本系统选择）
- 30 分钟：太长，重放攻击风险增加

**Signer 模块更严格**：
```typescript
// Signer 验证交易签名请求
const ONE_MINUTE = 60 * 1000;
if (timeDiff > ONE_MINUTE) {
  throw new Error('时间戳过期');
}
```
原因：交易签名更敏感，需要更严格的时效控制。

---

## 第六部分：Risk Control 风控模块

### 🔰 初级

#### 6.1 风控模块的核心职责是什么？

**参考答案**：

**核心职责**：

1. **风险评估**
   - 提现风险评估
   - 敏感操作评估
   - 返回决策：approve/manual_review/reject

2. **规则引擎**
   - 配置风控规则
   - 规则匹配执行
   - 动态规则更新

3. **人工审核**
   - 待审核队列管理
   - 审核历史记录
   - 审核结果处理

4. **风控签名**
   - 为通过评估的操作生成签名
   - 双签名机制的一环

**核心端点**：
```
POST /api/assess                    - 通用风险评估
POST /api/withdraw-risk-assessment  - 提现风险评估
POST /api/manual-review             - 提交人工审核
GET  /api/pending-reviews           - 获取待审核列表
```

---

#### 6.2 风控决策有哪些类型？

**参考答案**：

**决策类型**：

| 决策 | HTTP 状态 | 说明 | 后续处理 |
|------|----------|------|----------|
| **approve** | 200 | 自动批准 | 返回 risk_signature，继续执行 |
| **manual_review** | 202 | 需要人工审核 | 等待审核，暂停执行 |
| **reject** | 403 | 拒绝 | 终止操作，返回错误 |
| **freeze** | 403 | 冻结 | 标记资金冻结，需要解冻 |

**决策流程**：

```typescript
async function assessWithdraw(params) {
  // 1. 规则匹配
  const ruleResult = await matchRules(params);

  // 2. 根据规则返回决策
  switch (ruleResult.action) {
    case 'auto_approve':
      return {
        decision: 'approve',
        risk_signature: generateSignature(params)
      };

    case 'manual_review':
      await createManualReview(params);
      return {
        decision: 'manual_review',
        review_id: reviewId,
        message: '需要人工审核'
      };

    case 'reject':
      return {
        decision: 'reject',
        message: ruleResult.reason
      };

    case 'freeze':
      await freezeFunds(params.userId, params.amount);
      return {
        decision: 'freeze',
        message: '资金已冻结'
      };
  }
}
```

---

#### 6.3 什么是人工审核？什么情况下触发？

**参考答案**：

**人工审核**：

需要管理员手动审批的敏感操作。

**触发条件**（示例）：

```typescript
async function assessWithdraw(params) {
  const rules = [
    // 1. 大额提现
    {
      condition: () => params.amount > 10000,
      action: 'manual_review',
      reason: '大额提现需要人工审核'
    },

    // 2. 新地址首次大额提现
    {
      condition: () => isNewAddress(params.to) && params.amount > 1000,
      action: 'manual_review',
      reason: '新地址大额提现'
    },

    // 3. 高频提现
    {
      condition: async () => {
        const count = await getWithdrawCountToday(params.userId);
        return count > 5;
      },
      action: 'manual_review',
      reason: '今日提现次数过多'
    },

    // 4. 黑名单匹配
    {
      condition: () => isBlacklisted(params.to),
      action: 'reject',
      reason: '地址在黑名单中'
    },

    // 5. 非常用 IP
    {
      condition: () => isUnusualIP(params.userId, req.ip),
      action: 'manual_review',
      reason: '非常用 IP 地址'
    }
  ];

  // 匹配规则
  for (const rule of rules) {
    if (await rule.condition()) {
      return { action: rule.action, reason: rule.reason };
    }
  }

  // 默认自动批准
  return { action: 'auto_approve' };
}
```

**审核流程**：

```
1. 风控创建审核记录 → status='pending'
2. 管理员查看待审核列表 → GET /api/pending-reviews
3. 管理员审批 → POST /api/manual-review
   - approve → 生成 risk_signature
   - reject → 拒绝操作
4. Wallet 查询审核结果
5. 继续执行或终止
```

---

## 第七部分：安全与加密

### 🔰 初级

#### 7.1 系统使用什么签名算法？

**参考答案**：

**签名算法**：Ed25519

**特点**：
- 公钥/私钥对
- 64 字节签名（128 字符十六进制）
- 确定性签名（相同消息=相同签名）
- 抗侧信道攻击

**库**：
```typescript
// 使用 tweetnacl
import nacl from 'tweetnacl';

// 签名
const signature = nacl.sign.detached(messageBytes, secretKey);

// 验证
const isValid = nacl.sign.detached.verify(messageBytes, signature, publicKey);
```

**密钥格式**：
```typescript
// 私钥：64 字节（32 字节种子 +32 字节公钥）
// 公钥：32 字节
// 签名：64 字节

// 十六进制字符串表示
const privateKeyHex = Buffer.from(privateKey).toString('hex');
// 128 字符
```

---

#### 7.2 私钥如何存储？

**参考答案**：

**存储方式**：

1. **环境变量**
   ```bash
   # .env 文件
   WALLET_PRIVATE_KEY=ed2c8...64 字符
   SCAN_PRIVATE_KEY=ab3f...64 字符
   RISK_PRIVATE_KEY=91cd...64 字符
   ```

2. **加载方式**
   ```typescript
   // dotenv 加载
   import dotenv from 'dotenv';
   dotenv.config();

   const privateKey = process.env.WALLET_PRIVATE_KEY;
   ```

3. **安全措施**
   - .env 文件加入.gitignore
   - 生产环境使用密钥管理服务（如 AWS Secrets Manager）
   - 定期轮换密钥

4. **公钥配置**
   ```bash
   # db_gateway 的.env
   WALLET_PUBLIC_KEY=92c8...
   SCAN_PUBLIC_KEY=8f3a...
   RISK_PUBLIC_KEY=7b2e...
   ```

---

#### 7.3 什么是 Ed25519？

**参考答案**：

**Ed25519**：

基于 EdDSA（Edwards-curve Digital Signature Algorithm）的签名算法，使用 Curve25519 曲线。

**特性**：

| 特性 | 说明 |
|------|------|
| **曲线** | Curve25519（扭曲爱德华曲线） |
| **密钥长度** | 私钥 32 字节，公钥 32 字节 |
| **签名长度** | 64 字节 |
| **安全性** | 128 位安全强度 |
| **性能** | 签名速度 75000 ops/s |
| **确定性** | 相同消息总是产生相同签名 |
| **抗攻击** | 抗侧信道攻击、抗定时攻击 |

**对比其他算法**：

| 算法 | 签名长度 | 安全性 | 速度 |
|------|---------|--------|------|
| Ed25519 | 64 字节 | 128 位 | 快 |
| ECDSA (secp256k1) | 64-72 字节 | 128 位 | 中 |
| RSA-2048 | 256 字节 | 112 位 | 慢 |

---

## 第八部分：区块链知识

### 🔰 初级

#### 8.1 EVM 兼容链有哪些？

**参考答案**：

**EVM 兼容链**：

| 链 | Chain ID | 特点 |
|----|----------|------|
| **Ethereum** | 1 | 原始 EVM 链 |
| **BSC** | 56 | 币安智能链，低 Gas |
| **Polygon** | 137 | 以太坊 Layer 2 |
| **Arbitrum** | 42161 | Optimistic Rollup |
| **Optimism** | 10 | Optimistic Rollup |
| **Avalanche C-Chain** | 43114 | 高 TPS |
| **Fantom** | 250 | 快速确认 |

**本系统配置**：
```typescript
// config/index.ts
ethRpcUrl: process.env.ETH_RPC_URL  // 可配置任意 EVM 链
chainId: await viemClient.getChainId()
```

---

#### 8.2 什么是 Gas？Gas Limit 和 Gas Price 有什么区别？

**参考答案**：

**Gas**：

EVM 网络执行操作的费用单位。

**概念**：

| 概念 | 说明 | 示例 |
|------|------|------|
| **Gas** | 计算单位 | 转账=21000 gas |
| **Gas Limit** | 愿意消耗的最大 Gas | 21000 |
| **Gas Price** | 每单位 Gas 的价格 | 20 gwei |
| **总费用** | Gas Used × Gas Price | 21000 × 20 = 420000 gwei |

**EIP-1559 后**：

```typescript
// 新费用结构
baseFee: 网络基础费用（被销毁）
priorityFee: 小费（给矿工）
maxFeePerGas: 愿意支付的最高单价

总费用 = GasUsed × (baseFee + priorityFee)
```

**代码示例**：

```typescript
// 估算 Gas
const gasEstimate = await publicClient.estimateGas({
  account,
  to: recipient,
  value: amount
});
// 返回：21000

// 获取 Gas 价格
const gasPrice = await publicClient.getGasPrice();
// 返回：20000000000n (20 gwei)

// EIP-1559
const feeHistory = await publicClient.getFeeHistory({...});
const maxFeePerGas = feeHistory.baseFeePerGas * 120n / 100n;
const maxPriorityFeePerGas = 1500000000n; // 1.5 gwei
```

---

#### 8.3 ERC20 代币的标准接口有哪些？

**参考答案**：

**ERC20 标准接口**：

```solidity
interface IERC20 {
  // 事件
  event Transfer(address indexed from, address indexed to, uint256 value);
  event Approval(address indexed owner, address indexed spender, uint256 value);

  // 查询方法
  function name() external view returns (string memory);
  function symbol() external view returns (string memory);
  function decimals() external view returns (uint8);
  function totalSupply() external view returns (uint256);
  function balanceOf(address account) external view returns (uint256);

  // 写入方法
  function transfer(address to, uint256 amount) external returns (bool);
  function approve(address spender, uint256 amount) external returns (bool);
  function transferFrom(address from, address to, uint256 amount) external returns (bool);
}
```

**本系统使用**：

```typescript
// 解析 Transfer 事件
function parseERC20Transfer(log) {
  // Transfer 事件签名
  const transferTopic = '0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef';

  if (log.topics[0] !== transferTopic) return null;

  // 解析参数
  const from = '0x' + log.topics[1].slice(26);
  const to = '0x' + log.topics[2].slice(26);
  const value = BigInt(log.data);

  return { from, to, value };
}
```

---

## 第九部分：场景设计与系统设计

### 🔶 中级

#### 9.1 设计一个定时任务，定期检查并更新用户余额

**参考答案**：

**设计方案**：

```typescript
// services/balanceSyncService.ts
class BalanceSyncService {
  private interval: NodeJS.Timeout | null = null;
  private readonly SYNC_INTERVAL = 60 * 1000; // 1 分钟

  async start() {
    logger.info('启动余额同步服务');

    // 立即执行一次
    await this.syncBalances();

    // 定时执行
    this.interval = setInterval(async () => {
      try {
        await this.syncBalances();
      } catch (error) {
        logger.error('余额同步失败', { error });
      }
    }, this.SYNC_INTERVAL);
  }

  async stop() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  private async syncBalances() {
    const startTime = Date.now();

    // 1. 获取所有有余额变动的用户
    const users = await database.all(`
      SELECT DISTINCT user_id
      FROM credits
      WHERE status = 'finalized'
        AND updated_at > datetime('now', '-1 minute')
    `);

    // 2. 重新计算每个用户的余额
    for (const user of users) {
      await this.recalculateUserBalance(user.user_id);
    }

    // 3. 更新缓存
    await this.invalidateCache(users.map(u => u.user_id));

    logger.info('余额同步完成', {
      userCount: users.length,
      duration: Date.now() - startTime
    });
  }

  private async recalculateUserBalance(userId: number) {
    // 聚合 credits 表
    const result = await database.all(`
      SELECT token_id, SUM(CAST(amount AS REAL)) as balance
      FROM credits
      WHERE user_id = ?
        AND status IN ('confirmed', 'finalized')
      GROUP BY token_id
    `, [userId]);

    // 更新余额汇总表（如有）
    for (const row of result) {
      await dbGateway.executeOperation({
        table: 'user_balances',
        action: 'upsert',
        data: {
          user_id: userId,
          token_id: row.token_id,
          balance: row.balance.toString()
        }
      });
    }
  }
}
```

---

#### 9.2 如何实现对特定地址的大额转账监控告警？

**参考答案**：

**设计方案**：

```typescript
// services/largeTransferMonitor.ts
class LargeTransferMonitor {
  private thresholds = new Map<string, bigint>();

  // 添加监控地址
  async addWatchAddress(address: string, threshold: string) {
    this.thresholds.set(address.toLowerCase(), parseUnits(threshold, 18));
    logger.info('添加监控地址', { address, threshold });
  }

  // 检查交易
  async checkTransaction(tx: any) {
    const toAddress = tx.to?.toLowerCase();
    const fromAddress = tx.from?.toLowerCase();

    // 检查是否是监控地址
    if (this.thresholds.has(toAddress) || this.thresholds.has(fromAddress)) {
      const amount = tx.value;
      const threshold = this.thresholds.get(toAddress) || this.thresholds.get(fromAddress);

      if (amount >= threshold) {
        await this.triggerAlert({
          address: toAddress || fromAddress,
          txHash: tx.hash,
          amount: formatEther(amount),
          direction: toAddress === address ? 'IN' : 'OUT',
          timestamp: new Date().toISOString()
        });
      }
    }
  }

  // 触发告警
  private async triggerAlert(alert: any) {
    logger.warn('大额转账告警', alert);

    // 发送通知
    await this.sendNotification(alert);

    // 记录告警历史
    await dbGateway.createAlert(alert);
  }

  // 集成到交易分析器
  // 在 txAnalyzer.analyzeBlock() 中调用
  async analyzeBlock(blockNumber: number) {
    const deposits = await this.detectDeposits(blockNumber);

    // 检查大额转账
    for (const tx of blockTransactions) {
      await this.checkTransaction(tx);
    }

    return deposits;
  }
}
```

---

### 🔴 高级

#### 9.4 如何设计一个支持高并发的充值处理系统？

**参考答案**：

**高并发设计方案**：

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 多实例扫描器                                                  │
│    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│    │ Scan-1       │  │ Scan-2       │  │ Scan-3       │        │
│    │ 区块 1-100   │  │ 区块 101-200 │  │ 区块 201-300 │        │
│    └──────────────┘  └──────────────┘  └──────────────┘        │
│                                                                 │
│    分片策略：按区块高度分片                                      │
│    协调服务：Redis 分布式锁                                      │
├─────────────────────────────────────────────────────────────────┤
│ 2. 消息队列缓冲                                                  │
│    ┌─────────┐     ┌─────────────┐     ┌─────────┐              │
│    │ Scan    │────>│   Kafka     │────>│ Worker  │              │
│    │         │     │  (Topic)    │     │         │              │
│    └─────────┘     └─────────────┘     └─────────┘              │
│                              │                                   │
│                              ├────> Consumer-1                   │
│                              ├────> Consumer-2                   │
│                              └────> Consumer-3                   │
├─────────────────────────────────────────────────────────────────┤
│ 3. 批量写入                                                       │
│    Worker 收集存款事件 → 每 100 条或每秒批量写入                    │
│    INSERT INTO credits (...) VALUES (...), (...), (...)         │
├─────────────────────────────────────────────────────────────────┤
│ 4. 幂等性保证                                                    │
│    reference_id = ${txHash}_${logIndex}                         │
│    INSERT OR IGNORE INTO credits (reference_id, ...)            │
├─────────────────────────────────────────────────────────────────┤
│ 5. 读写分离                                                      │
│    写操作 → 主库                                                  │
│    读操作 → 从库（余额查询）                                      │
├─────────────────────────────────────────────────────────────────┤
│ 6. 缓存层                                                        │
│    Redis 缓存用户余额                                             │
│    缓存更新：Credit 写入后异步刷新                                │
└─────────────────────────────────────────────────────────────────┘
```

**代码示例**：

```typescript
// 分片扫描
class ShardedScanner {
  async claimBlockRange() {
    const key = 'scan:shard:claim';
    const shardId = await redis.incr(key);
    const startBlock = (shardId - 1) * 100 + 1;
    const endBlock = shardId * 100;

    // 设置过期时间，防止实例崩溃后锁死
    await redis.expire(key, 60);

    return { startBlock, endBlock };
  }
}

// 批量写入
class BatchWriter {
  private buffer: DepositEvent[] = [];

  async addDeposit(event: DepositEvent) {
    this.buffer.push(event);

    if (this.buffer.length >= 100) {
      await this.flush();
    }
  }

  async flush() {
    if (this.buffer.length === 0) return;

    const deposits = this.buffer.splice(0, this.buffer.length);

    // 批量插入
    const values = deposits.map(d => `(${d.txHash}, ${d.amount}, ...)`).join(',');
    await db.run(`INSERT INTO credits (...) VALUES ${values}`);
  }
}
```

---

#### 9.6 设计一个冷热钱包分离的资金管理系统

**参考答案**：

**资金架构**：

```
┌─────────────────────────────────────────────────────────────────┐
│                      资金分配                                    │
│                                                                 │
│  冷钱包 (Cold Wallet)     温钱包 (Warm Wallet)    热钱包 (Hot Wallet)
│  ████████████████████     ████████                 ████
│  80%                     15%                       5%           │
│                                                                 │
│  离线存储                 在线，定期补充热钱包       在线，日常提现   │
│  多签管理                 自动归集目标              余额监控        │
└─────────────────────────────────────────────────────────────────┘
```

**热钱包管理**：

```typescript
class HotWalletManager {
  private readonly TARGET_BALANCE = parseEther('10');  // 目标余额 10 ETH
  private readonly MIN_BALANCE = parseEther('2');     // 最低余额 2 ETH

  // 提现时选择热钱包
  async selectWalletForWithdraw(amount: bigint) {
    const wallets = await this.getActiveHotWallets();

    for (const wallet of wallets) {
      const balance = await this.getBalance(wallet.address);
      if (balance >= amount + this.MIN_BALANCE) {
        return wallet;  // 找到足够余额的热钱包
      }
    }

    throw new Error('热钱包余额不足，需要从温钱包补充');
  }

  // 监控余额，自动归集
  async monitorAndSweep() {
    const wallets = await this.getHotWallets();

    for (const wallet of wallets) {
      const balance = await this.getBalance(wallet.address);

      // 余额过高，归集到温钱包
      if (balance > this.TARGET_BALANCE * 2n) {
        const sweepAmount = balance - this.TARGET_BALANCE;
        await this.sweepToWarm(wallet.address, sweepAmount);
      }

      // 余额过低，从温钱包补充
      if (balance < this.MIN_BALANCE) {
        await this.refillFromWarm(wallet.address, this.TARGET_BALANCE);
      }
    }
  }
}
```

**温钱包归集**：

```typescript
class WarmWalletManager {
  // 归集用户存款到温钱包
  async sweepUserDeposits() {
    const userWallets = await this.getUserWalletsWithBalance();

    for (const wallet of userWallets) {
      const balance = await this.getBalance(wallet.address);

      // 余额大于阈值才归集（避免 Gas 费过高）
      if (balance > parseEther('0.01')) {
        await this.transferToWarm(wallet.address, balance);
      }
    }
  }
}
```

**冷钱包提款流程**：

```
1. 热钱包余额不足
   ↓
2. 从温钱包提现到热钱包（自动）
   ↓
3. 温钱包余额不足
   ↓
4. 生成冷钱包提款请求
   ↓
5. 多签管理员审批（2/3 签名）
   ↓
6. 线下签名，线上广播
   ↓
7. 资金到达温钱包
```

**安全策略**：

| 钱包 | 存储方式 | 签名方式 | 限额 |
|------|----------|----------|------|
| 热钱包 | 在线 | 自动 | 每日 10 ETH |
| 温钱包 | 在线 | 单签 | 每日 100 ETH |
| 冷钱包 | 离线 | 多签 (2/3) | 无限制 |

---

## 📊 评分标准

### 初级工程师（0-2 年）
- 回答 60% 初级问题 ✓
- 理解基本概念
- 能完成简单功能开发

### 中级工程师（2-5 年）
- 回答 70% 初级 + 50% 中级问题 ✓
- 理解模块间交互
- 能独立负责模块开发

### 高级工程师（5 年以上）
- 回答 80% 初级 + 70% 中级 + 50% 高级问题 ✓
- 深入理解系统设计
- 能设计复杂功能和优化方案

---

## 📝 面试流程建议

### 第一轮：技术基础（45 分钟）
- 项目架构理解（10 分钟）
- 区块链基础知识（15 分钟）
- 加密安全知识（10 分钟）
- 编码能力测试（10 分钟）

### 第二轮：系统设计（60 分钟）
- 模块设计（20 分钟）
- 场景题（20 分钟）
- 代码审查（20 分钟）

### 第三轮：综合能力（45 分钟）
- 复杂场景处理（20 分钟）
- 团队协作与沟通（15 分钟）
- 学习与成长潜力（10 分钟）

---

*文档版本：1.0*
*最后更新：2026-03-04*
