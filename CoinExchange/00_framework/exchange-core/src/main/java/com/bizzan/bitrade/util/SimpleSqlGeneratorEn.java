package com.bizzan.bitrade.util;

import java.util.*;

/**
 * Simple SQL Generator for JPA Entities
 * Generates SQL statements based on known entity classes
 */
public class SimpleSqlGeneratorEn {
    
    public static void main(String[] args) {
        System.out.println("=== JPA Entity Corresponding SQL Statements ===\n");
        
        List<String> sqlStatements = generateSqlFromKnownEntities();
        
        for (String sql : sqlStatements) {
            System.out.println(sql);
        }
        
        System.out.println("Total generated SQL statements for " + sqlStatements.size() + " tables");
    }
    
    public static List<String> generateSqlFromKnownEntities() {
        List<String> sqlList = new ArrayList<>();
        
        // Generate SQL based on entities in your project
        sqlList.add(generateExchangeOrderSql());
        sqlList.add(generateExchangeCoinSql());
        sqlList.add(generateFavorSymbolSql());
        sqlList.add(generateExchangeOrderDetailSql());
        sqlList.add(generateExchangeTradeSql());
        sqlList.add(generateOrderDetailAggregationSql());
        
        return sqlList;
    }
    
    private static String generateExchangeOrderSql() {
        return "-- ExchangeOrder Table Structure\n" +
               "CREATE TABLE IF NOT EXISTS `exchange_order` (\n" +
               "  `order_id` VARCHAR(255) NOT NULL COMMENT 'Order ID',\n" +
               "  `member_id` BIGINT COMMENT 'Member ID',\n" +
               "  `type` VARCHAR(50) COMMENT 'Order Type(MARKET_PRICE/LIMIT_PRICE)',\n" +
               "  `amount` DECIMAL(18,8) DEFAULT 0 COMMENT 'Order Amount',\n" +
               "  `symbol` VARCHAR(255) COMMENT 'Trading Pair Symbol',\n" +
               "  `traded_amount` DECIMAL(26,16) DEFAULT 0 COMMENT 'Traded Amount',\n" +
               "  `turnover` DECIMAL(26,16) DEFAULT 0 COMMENT 'Turnover Amount',\n" +
               "  `coin_symbol` VARCHAR(255) COMMENT 'Trading Coin Symbol',\n" +
               "  `base_symbol` VARCHAR(255) COMMENT 'Base Coin Symbol',\n" +
               "  `status` VARCHAR(50) COMMENT 'Order Status(TRADING/COMPLETED/CANCELED/OVERTIMED)',\n" +
               "  `direction` VARCHAR(50) COMMENT 'Order Direction(BUY/SELL)',\n" +
               "  `price` DECIMAL(18,8) DEFAULT 0 COMMENT 'Order Price',\n" +
               "  `time` BIGINT COMMENT 'Order Time Timestamp',\n" +
               "  `completed_time` BIGINT COMMENT 'Completion Time Timestamp',\n" +
               "  `canceled_time` BIGINT COMMENT 'Cancellation Time Timestamp',\n" +
               "  `use_discount` VARCHAR(10) COMMENT 'Use Discount Flag',\n" +
               "  PRIMARY KEY (`order_id`),\n" +
               "  INDEX idx_member_symbol (`member_id`, `symbol`),\n" +
               "  INDEX idx_status_time (`status`, `time`),\n" +
               "  INDEX idx_symbol_time (`symbol`, `time`)\n" +
               ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Trading Order Table';\n";
    }
    
    private static String generateExchangeCoinSql() {
        return "-- ExchangeCoin Table Structure\n" +
               "CREATE TABLE IF NOT EXISTS `exchange_coin` (\n" +
               "  `symbol` VARCHAR(255) NOT NULL COMMENT 'Trading Pair Name(BTC/USDT)',\n" +
               "  `coin_symbol` VARCHAR(255) COMMENT 'Trading Coin Symbol',\n" +
               "  `base_symbol` VARCHAR(255) COMMENT 'Base Coin Symbol',\n" +
               "  `enable` INT DEFAULT 1 COMMENT 'Status(1:Enable,2:Disable)',\n" +
               "  `fee` DECIMAL(8,4) COMMENT 'Trading Fee',\n" +
               "  `sort` INT COMMENT 'Sort Order',\n" +
               "  `coin_scale` INT COMMENT 'Trading Coin Decimal Scale',\n" +
               "  `base_coin_scale` INT COMMENT 'Base Coin Decimal Scale',\n" +
               "  `min_sell_price` DECIMAL(18,8) DEFAULT 0 COMMENT 'Minimum Sell Price',\n" +
               "  `max_buy_price` DECIMAL(18,8) DEFAULT 0 COMMENT 'Maximum Buy Price',\n" +
               "  `enable_market_sell` INT DEFAULT 1 COMMENT 'Enable Market Sell(0:No,1:Yes)',\n" +
               "  `enable_market_buy` INT DEFAULT 1 COMMENT 'Enable Market Buy(0:No,1:Yes)',\n" +
               "  `max_trading_time` INT DEFAULT 0 COMMENT 'Maximum Trading Time(seconds)',\n" +
               "  `max_trading_order` INT DEFAULT 0 COMMENT 'Maximum Trading Orders',\n" +
               "  `robot_type` INT DEFAULT 0 COMMENT 'Robot Type(0:Normal,1:Flat,2:Control)',\n" +
               "  `flag` INT DEFAULT 0 COMMENT 'Flag(0:Normal,1:Recommended)',\n" +
               "  `min_turnover` DECIMAL(18,8) DEFAULT 0 COMMENT 'Minimum Turnover',\n" +
               "  `zone` INT DEFAULT 0 COMMENT 'Trading Zone',\n" +
               "  `min_volume` DECIMAL(18,8) DEFAULT 0 COMMENT 'Minimum Volume',\n" +
               "  `max_volume` DECIMAL(18,8) DEFAULT 0 COMMENT 'Maximum Volume',\n" +
               "  `publish_type` INT DEFAULT 1 COMMENT 'Publish Type(1:None,2:Flash Sale,3:Distribution)',\n" +
               "  `start_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT 'Activity Start Time',\n" +
               "  `end_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT 'Activity End Time',\n" +
               "  `clear_time` VARCHAR(30) DEFAULT '2000-01-01 01:00:00' COMMENT 'Clear Time',\n" +
               "  `publish_price` DECIMAL(18,8) DEFAULT 0 COMMENT 'Publish Price',\n" +
               "  `publish_amount` DECIMAL(18,8) DEFAULT 0 COMMENT 'Publish Amount',\n" +
               "  `visible` INT DEFAULT 1 COMMENT 'Frontend Visible Status(1:Visible,2:Invisible)',\n" +
               "  `exchangeable` INT DEFAULT 1 COMMENT 'Exchangeable Status(1:Exchangeable,2:Not Exchangeable)',\n" +
               "  PRIMARY KEY (`symbol`),\n" +
               "  INDEX idx_enable_sort (`enable`, `sort`),\n" +
               "  INDEX idx_base_symbol (`base_symbol`),\n" +
               "  INDEX idx_zone (`zone`)\n" +
               ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Trading Coin Configuration Table';\n";
    }
    
    private static String generateFavorSymbolSql() {
        return "-- FavorSymbol Table Structure\n" +
               "CREATE TABLE IF NOT EXISTS `exchange_favor_symbol` (\n" +
               "  `id` BIGINT AUTO_INCREMENT COMMENT 'Primary Key ID',\n" +
               "  `symbol` VARCHAR(255) COMMENT 'Trading Pair Symbol',\n" +
               "  `member_id` BIGINT COMMENT 'Member ID',\n" +
               "  `add_time` VARCHAR(30) COMMENT 'Add Time',\n" +
               "  PRIMARY KEY (`id`),\n" +
               "  UNIQUE KEY uk_member_symbol (`member_id`, `symbol`),\n" +
               "  INDEX idx_member_id (`member_id`)\n" +
               ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User Favorite Trading Pair Table';\n";
    }
    
    private static String generateExchangeOrderDetailSql() {
        return "-- ExchangeOrderDetail Collection Structure (MongoDB)\n" +
               "-- Note: This is MongoDB document, not relational database table\n" +
               "-- Collection Name: exchange_order_detail\n" +
               "-- Document Structure Example:\n" +
               "-- {\n" +
               "--   \"orderId\": \"Order ID\",\n" +
               "--   \"price\": \"Price\",\n" +
               "--   \"amount\": \"Amount\",\n" +
               "--   \"turnover\": \"Turnover\",\n" +
               "--   \"fee\": \"Fee\",\n" +
               "--   \"time\": \"Timestamp\"\n" +
               "-- }\n\n";
    }
    
    private static String generateExchangeTradeSql() {
        return "-- ExchangeTrade Collection Structure (MongoDB)\n" +
               "-- Note: This is MongoDB document, not relational database table\n" +
               "-- Collection Name: exchange_trade_{symbol} (One collection per trading pair)\n" +
               "-- Document Structure Example:\n" +
               "-- {\n" +
               "--   \"symbol\": \"Trading Pair\",\n" +
               "--   \"price\": \"Trade Price\",\n" +
               "--   \"amount\": \"Trade Amount\",\n" +
               "--   \"buyTurnover\": \"Buy Turnover\",\n" +
               "--   \"sellTurnover\": \"Sell Turnover\",\n" +
               "--   \"direction\": \"Trade Direction\",\n" +
               "--   \"buyOrderId\": \"Buy Order ID\",\n" +
               "--   \"sellOrderId\": \"Sell Order ID\",\n" +
               "--   \"time\": \"Timestamp\"\n" +
               "-- }\n\n";
    }
    
    private static String generateOrderDetailAggregationSql() {
        return "-- OrderDetailAggregation Collection Structure (MongoDB)\n" +
               "-- Note: This is MongoDB document, not relational database table\n" +
               "-- Collection Name: order_detail_aggregation\n" +
               "-- Document Structure Example:\n" +
               "-- {\n" +
               "--   \"orderId\": \"Order ID\",\n" +
               "--   \"type\": \"Order Type(EXCHANGE/OTC)\",\n" +
               "--   \"username\": \"Username\",\n" +
               "--   \"realName\": \"Real Name\",\n" +
               "--   \"memberId\": \"Member ID\",\n" +
               "--   \"time\": \"Timestamp\",\n" +
               "--   \"fee\": \"Fee\",\n" +
               "--   \"amount\": \"Amount\",\n" +
               "--   \"unit\": \"Currency Unit\",\n" +
               "--   \"direction\": \"Trade Direction\",\n" +
               "--   \"customerId\": \"Customer ID\",\n" +
               "--   \"customerName\": \"Customer Username\",\n" +
               "--   \"customerRealName\": \"Customer Real Name\"\n" +
               "-- }\n\n";
    }
}