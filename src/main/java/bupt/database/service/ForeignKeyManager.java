package bupt.database.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外键管理器
 * 负责在数据导入前删除外键约束，导入后恢复外键约束
 */
@Slf4j
@Service
public class ForeignKeyManager {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 存储原有的外键约束信息
     */
    private static class ForeignKeyInfo {
        String tableName;
        String constraintName;
        String columnName;
        String referencedTableName;
        String referencedColumnName;
        String constraintDefinition;
        
        public ForeignKeyInfo(String tableName, String constraintName, String columnName, 
                            String referencedTableName, String referencedColumnName, String constraintDefinition) {
            this.tableName = tableName;
            this.constraintName = constraintName;
            this.columnName = columnName;
            this.referencedTableName = referencedTableName;
            this.referencedColumnName = referencedColumnName;
            this.constraintDefinition = constraintDefinition;
        }
    }
    
    private List<ForeignKeyInfo> removedForeignKeys = new ArrayList<>();
    
    /**
     * 删除所有TPC-H表的外键约束
     */
    public void dropAllForeignKeys() {
        log.info("开始删除所有TPC-H表的外键约束");
        
        try {
            // 清空之前的记录
            removedForeignKeys.clear();
            
            // 获取当前数据库中的外键信息
            String getCurrentForeignKeysSQL = """
                SELECT DISTINCT
                    TABLE_NAME,
                    CONSTRAINT_NAME,
                    REFERENCED_TABLE_NAME
                FROM information_schema.KEY_COLUMN_USAGE 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND REFERENCED_TABLE_NAME IS NOT NULL 
                AND TABLE_NAME IN ('partsupp', 'orders', 'lineitem')
                ORDER BY TABLE_NAME, CONSTRAINT_NAME
                """;
            
            List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(getCurrentForeignKeysSQL);
            
            // 为每个外键约束创建详细信息
            Map<String, ForeignKeyInfo> processedConstraints = new HashMap<>();
            
            for (Map<String, Object> fk : foreignKeys) {
                String tableName = (String) fk.get("TABLE_NAME");
                String constraintName = (String) fk.get("CONSTRAINT_NAME");
                String referencedTableName = (String) fk.get("REFERENCED_TABLE_NAME");
                
                // 避免重复处理同一个约束
                String constraintKey = tableName + "." + constraintName;
                if (!processedConstraints.containsKey(constraintKey)) {
                    
                    // 构建外键定义用于后续恢复
                    String constraintDefinition = buildForeignKeyDefinition(tableName, constraintName, referencedTableName);
                    
                    // 保存外键信息
                    ForeignKeyInfo fkInfo = new ForeignKeyInfo(tableName, constraintName, "", 
                        referencedTableName, "", constraintDefinition);
                    removedForeignKeys.add(fkInfo);
                    processedConstraints.put(constraintKey, fkInfo);
                    
                    // 删除外键约束
                    String dropSQL = "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName;
                    try {
                        jdbcTemplate.execute(dropSQL);
                        log.info("成功删除外键约束: {}.{}", tableName, constraintName);
                    } catch (Exception e) {
                        log.warn("删除外键约束失败: {}.{}, 错误: {}", tableName, constraintName, e.getMessage());
                    }
                }
            }
            
            log.info("完成删除外键约束，共删除 {} 个外键", removedForeignKeys.size());
            
        } catch (Exception e) {
            log.error("删除外键约束过程中发生错误", e);
            throw new RuntimeException("删除外键约束失败", e);
        }
    }
    
    /**
     * 恢复所有外键约束
     */
    public void restoreAllForeignKeys() {
        log.info("开始恢复外键约束，共需恢复 {} 个外键", removedForeignKeys.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (ForeignKeyInfo fkInfo : removedForeignKeys) {
            try {
                String restoreSQL = "ALTER TABLE " + fkInfo.tableName + " ADD " + fkInfo.constraintDefinition;
                jdbcTemplate.execute(restoreSQL);
                log.info("成功恢复外键约束: {}.{}", fkInfo.tableName, fkInfo.constraintName);
                successCount++;
            } catch (Exception e) {
                log.error("恢复外键约束失败: {}.{}, 错误: {}", fkInfo.tableName, fkInfo.constraintName, e.getMessage());
                failCount++;
            }
        }
        
        log.info("外键约束恢复完成，成功: {}, 失败: {}", successCount, failCount);
        
        // 清空记录
        removedForeignKeys.clear();
    }
    
    /**
     * 构建外键定义SQL（根据表名和约束名推断结构）
     */
    private String buildForeignKeyDefinition(String tableName, String constraintName, String referencedTableName) {
        
        // 根据TPC-H标准外键结构构建定义
        switch (tableName.toLowerCase()) {
            case "partsupp":
                if (constraintName.contains("FK1") || constraintName.toLowerCase().contains("supp") || 
                    constraintName.toLowerCase().contains("ibfk_1")) {
                    return "CONSTRAINT " + constraintName + " FOREIGN KEY (PS_SUPPKEY) REFERENCES supplier(S_SUPPKEY)";
                } else if (constraintName.contains("FK2") || constraintName.toLowerCase().contains("part") || 
                           constraintName.toLowerCase().contains("ibfk_2")) {
                    return "CONSTRAINT " + constraintName + " FOREIGN KEY (PS_PARTKEY) REFERENCES part(P_PARTKEY)";
                }
                break;
                
            case "orders":
                if (constraintName.contains("FK1") || constraintName.toLowerCase().contains("cust") || 
                    constraintName.toLowerCase().contains("ibfk_1")) {
                    return "CONSTRAINT " + constraintName + " FOREIGN KEY (O_CUSTKEY) REFERENCES customer(C_CUSTKEY)";
                }
                break;
                
            case "lineitem":
                if (constraintName.contains("FK1") || constraintName.toLowerCase().contains("order") || 
                    constraintName.toLowerCase().contains("ibfk_1")) {
                    return "CONSTRAINT " + constraintName + " FOREIGN KEY (L_ORDERKEY) REFERENCES orders(O_ORDERKEY)";
                } else if (constraintName.contains("FK2") || constraintName.toLowerCase().contains("ibfk_2") ||
                           (constraintName.toLowerCase().contains("part") && constraintName.toLowerCase().contains("supp"))) {
                    return "CONSTRAINT " + constraintName + " FOREIGN KEY (L_PARTKEY, L_SUPPKEY) REFERENCES partsupp(PS_PARTKEY, PS_SUPPKEY)";
                }
                break;
        }
        
        // 尝试根据表名和引用表名自动推断
        log.info("尝试根据表名和引用表自动推断外键定义: {}.{} -> {}", tableName, constraintName, referencedTableName);
        
        if ("lineitem".equals(tableName)) {
            if ("orders".equals(referencedTableName)) {
                return "CONSTRAINT " + constraintName + " FOREIGN KEY (L_ORDERKEY) REFERENCES orders(O_ORDERKEY)";
            } else if ("partsupp".equals(referencedTableName)) {
                return "CONSTRAINT " + constraintName + " FOREIGN KEY (L_PARTKEY, L_SUPPKEY) REFERENCES partsupp(PS_PARTKEY, PS_SUPPKEY)";
            }
        } else if ("orders".equals(tableName)) {
            if ("customer".equals(referencedTableName)) {
                return "CONSTRAINT " + constraintName + " FOREIGN KEY (O_CUSTKEY) REFERENCES customer(C_CUSTKEY)";
            }
        } else if ("partsupp".equals(tableName)) {
            if ("supplier".equals(referencedTableName)) {
                return "CONSTRAINT " + constraintName + " FOREIGN KEY (PS_SUPPKEY) REFERENCES supplier(S_SUPPKEY)";
            } else if ("part".equals(referencedTableName)) {
                return "CONSTRAINT " + constraintName + " FOREIGN KEY (PS_PARTKEY) REFERENCES part(P_PARTKEY)";
            }
        }
        
        // 默认情况下，记录警告但仍尝试构建
        log.warn("无法确定外键定义结构，使用通用格式: {}.{} -> {}", tableName, constraintName, referencedTableName);
        return "CONSTRAINT " + constraintName + " FOREIGN KEY (unknown_column) REFERENCES " + referencedTableName + "(unknown_column)";
    }
    
    /**
     * 强制删除所有已知的TPC-H外键约束（备用方案）
     * 当查询information_schema失败时使用
     */
    public void forceDropKnownForeignKeys() {
        log.info("开始强制删除已知的TPC-H外键约束");
        
        // 已知的外键约束名称（包括标准命名和MySQL自动生成命名）
        String[][] knownForeignKeys = {
            // 标准TPC-H命名
            {"partsupp", "PARTSUPP_FK1"},
            {"partsupp", "PARTSUPP_FK2"},
            {"orders", "ORDERS_FK1"},
            {"lineitem", "LINEITEM_FK1"},
            {"lineitem", "LINEITEM_FK2"},
            // MySQL自动生成命名
            {"partsupp", "partsupp_ibfk_1"},
            {"partsupp", "partsupp_ibfk_2"},
            {"orders", "orders_ibfk_1"},
            {"lineitem", "lineitem_ibfk_1"},
            {"lineitem", "lineitem_ibfk_2"}
        };
        
        int successCount = 0;
        int skipCount = 0;
        
        for (String[] fk : knownForeignKeys) {
            String tableName = fk[0];
            String constraintName = fk[1];
            
            try {
                String dropSQL = "ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName;
                jdbcTemplate.execute(dropSQL);
                log.info("成功删除已知外键约束: {}.{}", tableName, constraintName);
                successCount++;
            } catch (Exception e) {
                log.debug("删除已知外键约束失败（可能不存在）: {}.{}", tableName, constraintName);
                skipCount++;
            }
        }
        
        log.info("完成强制删除已知外键约束，成功: {}, 跳过: {}", successCount, skipCount);
    }
    
    /**
     * 创建标准的TPC-H外键约束
     */
    public void createStandardForeignKeys() {
        log.info("开始创建标准的TPC-H外键约束");
        
        String[] foreignKeyStatements = {
            // PARTSUPP表外键
            "ALTER TABLE partsupp ADD CONSTRAINT PARTSUPP_FK1 FOREIGN KEY (PS_SUPPKEY) REFERENCES supplier(S_SUPPKEY)",
            "ALTER TABLE partsupp ADD CONSTRAINT PARTSUPP_FK2 FOREIGN KEY (PS_PARTKEY) REFERENCES part(P_PARTKEY)",
            
            // ORDERS表外键
            "ALTER TABLE orders ADD CONSTRAINT ORDERS_FK1 FOREIGN KEY (O_CUSTKEY) REFERENCES customer(C_CUSTKEY)",
            
            // LINEITEM表外键
            "ALTER TABLE lineitem ADD CONSTRAINT LINEITEM_FK1 FOREIGN KEY (L_ORDERKEY) REFERENCES orders(O_ORDERKEY)",
            "ALTER TABLE lineitem ADD CONSTRAINT LINEITEM_FK2 FOREIGN KEY (L_PARTKEY, L_SUPPKEY) REFERENCES partsupp(PS_PARTKEY, PS_SUPPKEY)"
        };
        
        int successCount = 0;
        int failCount = 0;
        
        for (String sql : foreignKeyStatements) {
            try {
                jdbcTemplate.execute(sql);
                log.info("成功创建外键约束: {}", sql.substring(0, Math.min(50, sql.length())) + "...");
                successCount++;
            } catch (Exception e) {
                log.error("创建外键约束失败: {}, 错误: {}", sql.substring(0, Math.min(50, sql.length())) + "...", e.getMessage());
                failCount++;
            }
        }
        
        log.info("标准外键约束创建完成，成功: {}, 失败: {}", successCount, failCount);
    }
    
    /**
     * 查询当前数据库中的外键状态
     * @return 外键信息列表
     */
    public List<Map<String, Object>> getCurrentForeignKeys() {
        try {
            String getCurrentForeignKeysSQL = """
                SELECT 
                    TABLE_NAME,
                    CONSTRAINT_NAME,
                    COLUMN_NAME,
                    REFERENCED_TABLE_NAME,
                    REFERENCED_COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND REFERENCED_TABLE_NAME IS NOT NULL 
                AND TABLE_NAME IN ('partsupp', 'orders', 'lineitem')
                ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION
                """;
            
            List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(getCurrentForeignKeysSQL);
            log.info("当前数据库中共有 {} 个外键约束", foreignKeys.size());
            
            for (Map<String, Object> fk : foreignKeys) {
                log.info("外键: {}.{} ({}) -> {}.{}", 
                    fk.get("TABLE_NAME"), 
                    fk.get("CONSTRAINT_NAME"),
                    fk.get("COLUMN_NAME"),
                    fk.get("REFERENCED_TABLE_NAME"),
                    fk.get("REFERENCED_COLUMN_NAME"));
            }
            
            return foreignKeys;
            
        } catch (Exception e) {
            log.error("查询外键状态失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取已保存的待恢复外键信息
     * @return 待恢复外键数量
     */
    public int getSavedForeignKeysCount() {
        return removedForeignKeys.size();
    }
    
    /**
     * 清空已保存的外键信息（慎用）
     */
    public void clearSavedForeignKeys() {
        log.warn("清空已保存的外键信息，共清空 {} 个", removedForeignKeys.size());
        removedForeignKeys.clear();
    }
} 