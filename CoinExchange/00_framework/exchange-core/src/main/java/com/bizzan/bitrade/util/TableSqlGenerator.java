package com.bizzan.bitrade.util;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

/**
 * JPA实体表结构SQL生成器
 * 根据实体类自动生成对应的建表SQL语句
 */
public class TableSqlGenerator {
    
    /**
     * 生成指定包下所有JPA实体的建表SQL
     * @param packageName 实体类包名
     * @return SQL语句列表
     */
    public static List<String> generateCreateTableSql(String packageName) {
        List<String> sqlList = new ArrayList<>();
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                
                if (directory.exists()) {
                    processDirectory(directory, packageName, sqlList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return sqlList;
    }
    
    private static void processDirectory(File directory, String packageName, List<String> sqlList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归处理子目录
                    processDirectory(file, packageName + "." + file.getName(), sqlList);
                } else if (file.getName().endsWith(".class")) {
                    processClassFile(file, packageName, sqlList);
                }
            }
        }
    }
    
    private static void processClassFile(File file, String packageName, List<String> sqlList) {
        try {
            String className = packageName + "." + 
                file.getName().substring(0, file.getName().length() - 6);
            
            Class<?> clazz = Class.forName(className);
            
            // 检查是否有@Entity注解
            if (clazz.isAnnotationPresent(Entity.class)) {
                String sql = generateTableSqlForEntity(clazz);
                if (sql != null && !sql.isEmpty()) {
                    sqlList.add(sql);
                }
            }
        } catch (Exception e) {
            System.err.println("处理类文件失败: " + file.getName() + ", 错误: " + e.getMessage());
        }
    }
    
    private static String generateTableSqlForEntity(Class<?> entityClass) {
        try {
            String tableName = getTableName(entityClass);
            if (tableName == null || tableName.isEmpty()) {
                return null;
            }
            
            StringBuilder sql = new StringBuilder();
            sql.append("-- ").append(entityClass.getSimpleName()).append(" 表结构\n");
            sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
            
            // 获取所有字段
            java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
            List<String> columnDefinitions = new ArrayList<>();
            String primaryKey = null;
            
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String columnDef = generateColumnDefinition(field);
                if (columnDef != null) {
                    columnDefinitions.add(columnDef);
                    
                    // 检查是否为主键
                    if (field.isAnnotationPresent(javax.persistence.Id.class)) {
                        primaryKey = field.getName();
                    }
                }
            }
            
            // 添加字段定义
            for (int i = 0; i < columnDefinitions.size(); i++) {
                sql.append("  ").append(columnDefinitions.get(i));
                if (i < columnDefinitions.size() - 1) {
                    sql.append(",");
                }
                sql.append("\n");
            }
            
            // 添加主键约束
            if (primaryKey != null) {
                sql.append(",  PRIMARY KEY (`").append(primaryKey).append("`)\n");
            }
            
            sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n\n");
            
            return sql.toString();
        } catch (Exception e) {
            System.err.println("生成实体SQL失败: " + entityClass.getName() + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    private static String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            String name = tableAnnotation.name();
            if (!name.isEmpty()) {
                return name;
            }
        }
        // 使用默认命名规则：驼峰转下划线
        return convertToSnakeCase(entityClass.getSimpleName());
    }
    
    private static String generateColumnDefinition(java.lang.reflect.Field field) {
        try {
            String columnName = field.getName();
            Class<?> fieldType = field.getType();
            
            // 处理@Column注解
            if (field.isAnnotationPresent(javax.persistence.Column.class)) {
                javax.persistence.Column columnAnnotation = field.getAnnotation(javax.persistence.Column.class);
                if (!columnAnnotation.name().isEmpty()) {
                    columnName = columnAnnotation.name();
                }
            }
            
            // 转换字段名为下划线格式
            columnName = convertToSnakeCase(columnName);
            
            StringBuilder columnDef = new StringBuilder();
            columnDef.append("`").append(columnName).append("` ");
            
            // 根据Java类型确定MySQL类型
            String mysqlType = getMysqlType(fieldType, field);
            if (mysqlType == null) {
                return null; // 不支持的类型
            }
            
            columnDef.append(mysqlType);
            
            // 处理@Column的其他属性
            if (field.isAnnotationPresent(javax.persistence.Column.class)) {
                javax.persistence.Column columnAnnotation = field.getAnnotation(javax.persistence.Column.class);
                
                // NOT NULL约束
                if (!columnAnnotation.nullable()) {
                    columnDef.append(" NOT NULL");
                }
                
                // 默认值
                if (!columnAnnotation.columnDefinition().isEmpty()) {
                    String columnDefStr = columnAnnotation.columnDefinition();
                    // 提取DEFAULT部分
                    if (columnDefStr.contains("default")) {
                        int defaultIndex = columnDefStr.indexOf("default");
                        String defaultValue = columnDefStr.substring(defaultIndex);
                        if (defaultValue.contains(" ")) {
                            defaultValue = defaultValue.substring(0, defaultValue.indexOf(" "));
                        }
                        columnDef.append(" ").append(defaultValue.toUpperCase());
                    }
                }
            }
            
            return columnDef.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String getMysqlType(Class<?> javaType, java.lang.reflect.Field field) {
        if (javaType == String.class) {
            // 检查是否有长度限制
            if (field.isAnnotationPresent(javax.persistence.Column.class)) {
                javax.persistence.Column column = field.getAnnotation(javax.persistence.Column.class);
                if (column.length() > 0) {
                    return "VARCHAR(" + column.length() + ")";
                }
            }
            return "VARCHAR(255)";
        } else if (javaType == Integer.class || javaType == int.class) {
            return "INT";
        } else if (javaType == Long.class || javaType == long.class) {
            return "BIGINT";
        } else if (javaType == Double.class || javaType == double.class) {
            return "DOUBLE";
        } else if (javaType == Float.class || javaType == float.class) {
            return "FLOAT";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "TINYINT(1)";
        } else if (javaType == java.math.BigDecimal.class) {
            return "DECIMAL(18,8)";
        } else if (javaType == java.util.Date.class || javaType == java.sql.Timestamp.class) {
            return "DATETIME";
        } else if (javaType.isEnum()) {
            return "VARCHAR(50)";
        }
        return null; // 不支持的类型
    }
    
    private static String convertToSnakeCase(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * 主方法 - 生成并打印SQL
     */
    public static void main(String[] args) {
        System.out.println("开始生成JPA实体对应的建表SQL...\n");
        
        List<String> sqlStatements = generateCreateTableSql("com.bizzan.bitrade.entity");
        
        System.out.println("=== 生成的建表SQL语句 ===\n");
        for (String sql : sqlStatements) {
            System.out.println(sql);
        }
        
        System.out.println("总共生成了 " + sqlStatements.size() + " 个表的SQL语句");
    }
}