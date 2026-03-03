package com.bizzan.bitrade.util;

import java.util.*;

/**
 * 简化版SQL生成器
 * 基于已知的实体类直接生成SQL，无需编译整个项目
 */
public class SimpleSqlGenerator {
    
    public static void main(String[] args) {
        System.out.println("=== JPA实体对应的建表SQL语句 ===\n");
        
        List<String> sqlStatements = generateSqlFromKnownEntities();
        
        for (String sql : sqlStatements) {
            System.out.println(sql);
        }
        
        System.out.println("总共生成了 " + sqlStatements.size() + " 个表的SQL语句");
    }
    
    public static List<String> generateSqlFromKnownEntities() {
        List<String> sqlList = new ArrayList<>();
        
        // 根据您项目中的实体类生成SQL
        sqlList.add(generateExchangeOrderSql());
        sqlList.add(generateExchangeCoinSql());
        sqlList.add(generateFavorSymbolSql());
        sqlList.add(generateExchangeOrderDetailSql());
        sqlList.add(generateExchangeTradeSql());
        sqlList.add(generateOrderDetailAggregationSql());
        
        return sqlList;
    }
    
    private static String generateExchangeOrderSql() {
        return "-- ExchangeOrder 表结构\n" +
               "CREATE TABLE IF NOT EXISTS `exchange_order` (\n" +
               "  `order_id` VARCHAR(255) NOT NULL COMMENT '订单ID',\n" +
               "  `member_id` BIGINT COMMENT '会员ID',\n" +
               "  `type` VARCHAR(50) COMMENT '订单类型(MARKET_PRICE/LIMIT_PRICE)',\n" +
               "  `amount` DECIMAL(18,8) DEFAULT 0 COMMENT '委托量',\n" +
               "  `symbol` VARCHAR(255) COMMENT '交易对符号',\n" +
               "  `traded_amount` DECIMAL(26,16) DEFAULT 0 COMMENT '成交量',\n" +
               "  `turnover` DECIMAL(26,16) DEFAULT 0 COMMENT '成交额',\n" +
               "  `coin_symbol` VARCHAR(255) COMMENT '交易币种',\n" +
               "  `base_symbol` VARCHAR(255) COMMENT '结算币种',\n" +
               "  `status` VARCHAR(50) COMMENT '订单状态(TRADING/COMPLETED/CANCELED/OVERTIMED)',\n" +
               "  `direction` VARCHAR(50) COMMENT '订单方向(BUY/SELL)',\n" +
               "  `price` DECIMAL(18,8) DEFAULT 0 COMMENT '委托价格',\n" +
               "  `time` BIGINT COMMENT '挂单时间戳',\n" +
               "  `completed_time` BIGINT COMMENT '完成时间戳',\n" +
               "  `canceled_time` BIGINT COMMENT '取消时间戳',\n" +
               "  `use_discount` VARCHAR(10) COMMENT '是否使用折扣',\n" +
               "  PRIMARY KEY (`order_id`),\n" +
               "  INDEX idx_member_symbol (`member_id`, `symbol`),\n" +
               "  INDEX idx_status_time (`status`, `time`),\n" +
               "  INDEX idx_symbol_time (`symbol`, `time`)\n" +
               ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易订单表';\n";
    }
    
    private static String generateExchangeCoinSql() {
        return "-- ExchangeCoin 表结构\n" +
               "CREATE TABLE IF NOT EXISTS `exchange_coin` (\n" +
               "  `symbol` VARCHAR(255) NOT NULL COMMENT '交易对名称(BTC/USDT)',\n" +
               "  `coin_symbol` VARCHAR(255) COMMENT '交易币种符号',\n" +
               "  `base_symbol` VARCHAR(255) COMMENT '结算币种符号',\n" +
               "  `enable` INT DEFAULT 1 COMMENT '状态(1:启用,2:禁用)',\n" +
               "  `fee` DECIMAL(8,4) COMMENT '交易手续费',\n" +
               "  `sort` INT COMMENT '排序',\n" +
               "  `coin_scale` INT COMMENT '交易币小数精度',\n" +
               "  `base_coin_scale` INT COMMENT '基币小数精度',\n" +
               "  `min_sell_price` DECIMAL(18,8) DEFAULT 0 COMMENT '卖单最低价格',\n" +
               "  `max_buy_price` DECIMAL(18,8) DEFAULT 0 COMMENT '最高买单价',\n" +
               "  `enable_market_sell` INT DEFAULT 1 COMMENT '是否启用市价卖(0:否,1:是)',\n" +
               "  `enable_market_buy` INT DEFAULT 1 COMMENT '是否启用市价买(0:否,1:是)',\n" +
               "  `max_trading_time` INT DEFAULT 0 COMMENT '最大交易时间(秒)',\n" +
               "  `max_trading_order` INT DEFAULT 0 COMMENT '最大交易中订单数',\n" +
               "  `robot_type` INT DEFAULT 0 COMMENT '机器人类型(0:一般,1:平价,2:控盘)',\n" +
               "  `flag` INT DEFAULT 0 COMMENT '标签位(0:普通,1:推荐)',\n" +
               "  `min_turnover` DECIMAL(18,8) DEFAULT 0 COMMENT '最小成交额',\n" +
               "  `zone` INT DEFAULT 0 COMMENT '交易区域',\n" +
               "  `min_volume` DECIMAL(18,8) DEFAULT 0 COMMENT '最小下单量',\n" +
               "  `max_volume` DECIMAL(18,8) DEFAULT 0 COMMENT '最大下单量',\n" +
               "  `publish_type` INT DEFAULT 1 COMMENT '发行类型(1:无活动,2:抢购,3:分摊)',\n" +
               "  `start_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT '活动开始时间',\n" +
               "  `end_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT '活动结束时间',\n" +
               "  `clear_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT '清盘时间',\n" +
               "  `publish_price` DECIMAL(18,8) DEFAULT 0 COMMENT '发行价格',\n" +
               "  `publish_amount` DECIMAL(18,8) DEFAULT 0 COMMENT '发行数量',\n" +
               "  `visible` INT DEFAULT 1 COMMENT '前台可见状态(1:可见,2:不可见)',\n" +
               "  `exchangeable` INT DEFAULT 1 COMMENT '是否可交易(1:可交易,2:不可交易)',\n" +
               "  PRIMARY KEY (`symbol`),\n" +
               "  INDEX idx_enable_sort (`enable`, `sort`),\n" +
               "  INDEX idx_base_symbol (`base_symbol`),\n" +
               "  INDEX idx_zone (`zone`)\n" +
               ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易币种配置表';\n";
    }
    
    private static String generateFavorSymbolSql() {
        return "-- FavorSymbol 表结构\n" +
               "CREATE TABLE IF NOT EXISTS `exchange_favor_symbol` (\n" +
               "  `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',\n" +
               "  `symbol` VARCHAR(255) COMMENT '交易对符号',\n" +
               "  `member_id` BIGINT COMMENT '会员ID',\n" +
               "  `add_time` VARCHAR(30) COMMENT '添加时间',\n" +
               "  PRIMARY KEY (`id`),\n" +
               "  UNIQUE KEY uk_member_symbol (`member_id`, `symbol`),\n" +
               "  INDEX idx_member_id (`member_id`)\n" +
               ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自选交易对表';\n";
    }
    
    private static String generateExchangeOrderDetailSql() {
        return "-- ExchangeOrderDetail 表结构 (MongoDB集合)\n" +
               "-- 注意：这是MongoDB文档，不是关系型数据库表\n" +
               "-- 集合名：exchange_order_detail\n" +
               "-- 文档结构示例：\n" +
               "-- {\n" +
               "--   \"orderId\": \"订单ID\",\n" +
               "--   \"price\": \"价格\",\n" +
               "--   \"amount\": \"数量\",\n" +
               "--   \"turnover\": \"成交额\",\n" +
               "--   \"fee\": \"手续费\",\n" +
               "--   \"time\": \"成交时间戳\"\n" +
               "-- }\n\n";
    }
    
    private static String generateExchangeTradeSql() {
        return "-- ExchangeTrade 表结构 (MongoDB集合)\n" +
               "-- 注意：这是MongoDB文档，不是关系型数据库表\n" +
               "-- 集合名：exchange_trade_{symbol} (每个交易对一个集合)\n" +
               "-- 文档结构示例：\n" +
               "-- {\n" +
               "--   \"symbol\": \"交易对\",\n" +
               "--   \"price\": \"成交价格\",\n" +
               "--   \"amount\": \"成交量\",\n" +
               "--   \"buyTurnover\": \"买方成交额\",\n" +
               "--   \"sellTurnover\": \"卖方成交额\",\n" +
               "--   \"direction\": \"交易方向\",\n" +
               "--   \"buyOrderId\": \"买单ID\",\n" +
               "--   \"sellOrderId\": \"卖单ID\",\n" +
               "--   \"time\": \"成交时间戳\"\n" +
               "-- }\n\n";
    }
    
    private static String generateOrderDetailAggregationSql() {
        return "-- OrderDetailAggregation 表结构 (MongoDB集合)\n" +
               "-- 注意：这是MongoDB文档，不是关系型数据库表\n" +
               "-- 集合名：order_detail_aggregation\n" +
               "-- 文档结构示例：\n" +
               "-- {\n" +
               "--   \"orderId\": \"订单ID\",\n" +
               "--   \"type\": \"订单类型(EXCHANGE/OTC)\",\n" +
               "--   \"username\": \"用户名\",\n" +
               "--   \"realName\": \"真实姓名\",\n" +
               "--   \"memberId\": \"会员ID\",\n" +
               "--   \"time\": \"时间戳\",\n" +
               "--   \"fee\": \"手续费\",\n" +
               "--   \"amount\": \"数量\",\n" +
               "--   \"unit\": \"币种单位\",\n" +
               "--   \"direction\": \"交易方向\",\n" +
               "--   \"customerId\": \"交易对象ID\",\n" +
               "--   \"customerName\": \"交易对象用户名\",\n" +
               "--   \"customerRealName\": \"交易对象真实姓名\"\n" +
               "-- }\n\n";
    }
}