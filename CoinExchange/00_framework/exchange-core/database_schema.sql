-- ==========================================
-- JPA实体对应的数据库表结构SQL脚本
-- ==========================================

-- 1. ExchangeOrder 表结构 (交易订单表)
CREATE TABLE IF NOT EXISTS `exchange_order` (
  `order_id` VARCHAR(255) NOT NULL COMMENT '订单ID',
  `member_id` BIGINT COMMENT '会员ID',
  `type` VARCHAR(50) COMMENT '订单类型(MARKET_PRICE/LIMIT_PRICE)',
  `amount` DECIMAL(18,8) DEFAULT 0 COMMENT '委托量',
  `symbol` VARCHAR(255) COMMENT '交易对符号',
  `traded_amount` DECIMAL(26,16) DEFAULT 0 COMMENT '成交量',
  `turnover` DECIMAL(26,16) DEFAULT 0 COMMENT '成交额',
  `coin_symbol` VARCHAR(255) COMMENT '交易币种',
  `base_symbol` VARCHAR(255) COMMENT '结算币种',
  `status` VARCHAR(50) COMMENT '订单状态(TRADING/COMPLETED/CANCELED/OVERTIMED)',
  `direction` VARCHAR(50) COMMENT '订单方向(BUY/SELL)',
  `price` DECIMAL(18,8) DEFAULT 0 COMMENT '委托价格',
  `time` BIGINT COMMENT '挂单时间戳',
  `completed_time` BIGINT COMMENT '完成时间戳',
  `canceled_time` BIGINT COMMENT '取消时间戳',
  `use_discount` VARCHAR(10) COMMENT '是否使用折扣',
  PRIMARY KEY (`order_id`),
  INDEX idx_member_symbol (`member_id`, `symbol`),
  INDEX idx_status_time (`status`, `time`),
  INDEX idx_symbol_time (`symbol`, `time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易订单表';

-- 2. ExchangeCoin 表结构 (交易币种配置表)
CREATE TABLE IF NOT EXISTS `exchange_coin` (
  `symbol` VARCHAR(255) NOT NULL COMMENT '交易对名称(BTC/USDT)',
  `coin_symbol` VARCHAR(255) COMMENT '交易币种符号',
  `base_symbol` VARCHAR(255) COMMENT '结算币种符号',
  `enable` INT DEFAULT 1 COMMENT '状态(1:启用,2:禁用)',
  `fee` DECIMAL(8,4) COMMENT '交易手续费',
  `sort` INT COMMENT '排序',
  `coin_scale` INT COMMENT '交易币小数精度',
  `base_coin_scale` INT COMMENT '基币小数精度',
  `min_sell_price` DECIMAL(18,8) DEFAULT 0 COMMENT '卖单最低价格',
  `max_buy_price` DECIMAL(18,8) DEFAULT 0 COMMENT '最高买单价',
  `enable_market_sell` INT DEFAULT 1 COMMENT '是否启用市价卖(0:否,1:是)',
  `enable_market_buy` INT DEFAULT 1 COMMENT '是否启用市价买(0:否,1:是)',
  `max_trading_time` INT DEFAULT 0 COMMENT '最大交易时间(秒)',
  `max_trading_order` INT DEFAULT 0 COMMENT '最大交易中订单数',
  `robot_type` INT DEFAULT 0 COMMENT '机器人类型(0:一般,1:平价,2:控盘)',
  `flag` INT DEFAULT 0 COMMENT '标签位(0:普通,1:推荐)',
  `min_turnover` DECIMAL(18,8) DEFAULT 0 COMMENT '最小成交额',
  `zone` INT DEFAULT 0 COMMENT '交易区域',
  `min_volume` DECIMAL(18,8) DEFAULT 0 COMMENT '最小下单量',
  `max_volume` DECIMAL(18,8) DEFAULT 0 COMMENT '最大下单量',
  `publish_type` INT DEFAULT 1 COMMENT '发行类型(1:无活动,2:抢购,3:分摊)',
  `start_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT '活动开始时间',
  `end_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT '活动结束时间',
  `clear_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT '清盘时间',
  `publish_price` DECIMAL(18,8) DEFAULT 0 COMMENT '发行价格',
  `publish_amount` DECIMAL(18,8) DEFAULT 0 COMMENT '发行数量',
  `visible` INT DEFAULT 1 COMMENT '前台可见状态(1:可见,2:不可见)',
  `exchangeable` INT DEFAULT 1 COMMENT '是否可交易(1:可交易,2:不可交易)',
  PRIMARY KEY (`symbol`),
  INDEX idx_enable_sort (`enable`, `sort`),
  INDEX idx_base_symbol (`base_symbol`),
  INDEX idx_zone (`zone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易币种配置表';

-- 3. FavorSymbol 表结构 (用户自选交易对表)
CREATE TABLE IF NOT EXISTS `exchange_favor_symbol` (
  `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
  `symbol` VARCHAR(255) COMMENT '交易对符号',
  `member_id` BIGINT COMMENT '会员ID',
  `add_time` VARCHAR(30) COMMENT '添加时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_member_symbol (`member_id`, `symbol`),
  INDEX idx_member_id (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自选交易对表';

-- ==========================================
-- MongoDB集合结构说明
-- ==========================================

-- 4. ExchangeOrderDetail 集合结构
-- 集合名: exchange_order_detail
-- 存储订单成交详情，每个订单的每次成交都会产生一条记录

-- 5. ExchangeTrade 集合结构  
-- 集合名: exchange_trade_{symbol} (每个交易对一个独立集合)
-- 存储撮合成交记录，按交易对分集合存储以提高查询性能

-- 6. OrderDetailAggregation 集合结构
-- 集合名: order_detail_aggregation
-- 存储订单手续费聚合统计信息，用于报表和分析

-- ==========================================
-- 常用查询索引建议
-- ==========================================

-- 为提高查询性能，建议添加以下索引:

-- ExchangeOrder表额外索引
ALTER TABLE `exchange_order` ADD INDEX idx_member_status (`member_id`, `status`);
ALTER TABLE `exchange_order` ADD INDEX idx_direction_status (`direction`, `status`);

-- ExchangeCoin表额外索引  
ALTER TABLE `exchange_coin` ADD INDEX idx_visible_enable (`visible`, `enable`);
ALTER TABLE `exchange_coin` ADD INDEX idx_publish_type (`publish_type`);

-- ==========================================
-- 初始化数据示例
-- ==========================================

-- 插入常见交易对示例
INSERT INTO `exchange_coin` VALUES 
('BTC/USDT', 'BTC', 'USDT', 1, 0.0010, 1, 8, 2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, '2000-01-01 01:00:00', '2000-01-01 01:00:00', '2000-01-01 01:00:00', 0, 0, 1, 1),
('ETH/USDT', 'ETH', 'USDT', 1, 0.0010, 2, 8, 2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, '2000-01-01 01:00:00', '2000-01-01 01:00:00', '2000-01-01 01:00:00', 0, 0, 1, 1),
('BNB/USDT', 'BNB', 'USDT', 1, 0.0010, 3, 8, 2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, '2000-01-01 01:00:00', '2000-01-01 01:00:00', '2000-01-01 01:00:00', 0, 0, 1, 1);

-- ==========================================
-- 表关系说明
-- ==========================================

-- 1. exchange_order 与 exchange_order_detail 关系
--    - 一对多关系：一个订单可以有多条成交详情
--    - 通过 orderId 字段关联

-- 2. exchange_order 与 exchange_coin 关系  
--    - 多对一关系：多个订单对应一个交易对配置
--    - 通过 symbol 字段关联

-- 3. exchange_order 与 exchange_favor_symbol 关系
--    - 多对多关系：用户可以收藏多个交易对，交易对可以被多个用户收藏
--    - 通过 member_id 和 symbol 字段关联

-- ==========================================
-- 注意事项
-- ==========================================

-- 1. 时间字段统一使用毫秒时间戳格式
-- 2. 金额字段使用 DECIMAL 类型避免精度丢失
-- 3. MongoDB集合需要单独创建，不能通过SQL创建
-- 4. 建议定期清理历史订单数据以优化性能
-- 5. 根据实际业务需求调整索引策略