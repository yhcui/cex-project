# JPA实体对应的数据库表结构说明

## 概述
本文档详细说明了exchange-core模块中JPA实体类对应的数据库表结构。

## 关系型数据库表

### 1. exchange_order (交易订单表)
- **主键**: order_id (订单ID)
- **主要字段**:
  - member_id: 会员ID
  - symbol: 交易对符号 (如 BTC/USDT)
  - direction: 交易方向 (BUY/SELL)
  - type: 订单类型 (MARKET_PRICE/LIMIT_PRICE)
  - price: 委托价格
  - amount: 委托量
  - status: 订单状态 (TRADING/COMPLETED/CANCELED/OVERTIMED)
  - time: 挂单时间戳

### 2. exchange_coin (交易币种配置表)
- **主键**: symbol (交易对名称)
- **主要字段**:
  - coin_symbol: 交易币种符号
  - base_symbol: 结算币种符号
  - enable: 启用状态
  - fee: 交易手续费
  - min_sell_price: 卖单最低价格
  - max_buy_price: 最高买单价
  - 其他配置字段...

### 3. exchange_favor_symbol (用户自选交易对表)
- **主键**: id (自增ID)
- **唯一约束**: member_id + symbol
- **主要字段**:
  - member_id: 会员ID
  - symbol: 交易对符号
  - add_time: 添加时间

## MongoDB集合

### 4. exchange_order_detail (订单成交详情)
- **集合名**: exchange_order_detail
- **用途**: 存储每个订单的成交详情记录
- **关键字段**: orderId, price, amount, turnover, fee, time

### 5. exchange_trade_{symbol} (撮合成交记录)
- **集合名**: exchange_trade_BTC/USDT (按交易对分集合)
- **用途**: 存储实时撮合成交记录
- **关键字段**: symbol, price, amount, direction, buyOrderId, sellOrderId

### 6. order_detail_aggregation (订单统计聚合)
- **集合名**: order_detail_aggregation
- **用途**: 存储手续费等统计数据，用于报表分析
- **关键字段**: orderId, type, memberId, fee, amount, time

## 使用方法

### 1. 直接执行SQL文件
```bash
mysql -u username -p database_name < database_schema.sql
```

### 2. 在应用程序中使用
确保在application.properties中正确配置：
```properties
# MySQL配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password

# MongoDB配置  
spring.data.mongodb.uri=mongodb://localhost:27017/your_database
```

## 索引优化建议

已包含常用查询索引，可根据实际查询模式添加更多索引：
- 按用户ID和状态查询订单
- 按交易对和时间范围查询
- 按订单状态和时间排序

## 数据初始化

SQL文件中包含了常见的交易对初始化数据示例，可根据实际需求修改。

## 注意事项

1. **时间处理**: 所有时间字段使用Unix毫秒时间戳
2. **精度控制**: 金融数据使用DECIMAL类型避免浮点数精度问题
3. **分区策略**: MongoDB按交易对分集合存储提高查询效率
4. **数据清理**: 建议定期清理历史订单数据
5. **备份策略**: 重要交易数据需制定完善的备份方案