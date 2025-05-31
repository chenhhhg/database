package bupt.database.service.impl;

import bupt.database.service.DataCleaningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 默认数据清洗服务实现
 * 提供基础的数据清洗功能
 */
@Slf4j
@Service
public class DefaultDataCleaningServiceImpl implements DataCleaningService {
    
    // TPC-H表字段配置
    private static final Map<String, List<String>> TABLE_FIELDS = new HashMap<>();
    
    // 字段类型配置
    private static final Map<String, Map<String, String>> FIELD_TYPES = new HashMap<>();
    
    // 数值字段的正则表达式
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    
    // 日期字段的正则表达式
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    static {
        // 初始化表字段配置
        TABLE_FIELDS.put("customer", Arrays.asList(
            "c_custkey", "c_name", "c_address", "c_nationkey", "c_phone", "c_acctbal", "c_mktsegment", "c_comment"
        ));
        
        TABLE_FIELDS.put("orders", Arrays.asList(
            "o_orderkey", "o_custkey", "o_orderstatus", "o_totalprice", "o_orderdate", "o_orderpriority", "o_clerk", "o_shippriority", "o_comment"
        ));
        
        TABLE_FIELDS.put("lineitem", Arrays.asList(
            "l_orderkey", "l_partkey", "l_suppkey", "l_linenumber", "l_quantity", "l_extendedprice", "l_discount", "l_tax",
            "l_returnflag", "l_linestatus", "l_shipdate", "l_commitdate", "l_receiptdate", "l_shipinstruct", "l_shipmode", "l_comment"
        ));
        
        TABLE_FIELDS.put("nation", Arrays.asList(
            "n_nationkey", "n_name", "n_regionkey", "n_comment"
        ));
        
        TABLE_FIELDS.put("partsupp", Arrays.asList(
            "ps_partkey", "ps_suppkey", "ps_availqty", "ps_supplycost", "ps_comment"
        ));
        
        TABLE_FIELDS.put("part", Arrays.asList(
            "p_partkey", "p_name", "p_mfgr", "p_brand", "p_type", "p_size", "p_container", "p_retailprice", "p_comment"
        ));
        
        TABLE_FIELDS.put("region", Arrays.asList(
            "r_regionkey", "r_name", "r_comment"
        ));
        
        TABLE_FIELDS.put("supplier", Arrays.asList(
            "s_suppkey", "s_name", "s_address", "s_nationkey", "s_phone", "s_acctbal", "s_comment"
        ));
        
        // 初始化字段类型配置
        initFieldTypes();
    }
    
    private static void initFieldTypes() {
        // Customer表字段类型
        Map<String, String> customerTypes = new HashMap<>();
        customerTypes.put("c_custkey", "INTEGER");
        customerTypes.put("c_name", "VARCHAR");
        customerTypes.put("c_address", "VARCHAR");
        customerTypes.put("c_nationkey", "INTEGER");
        customerTypes.put("c_phone", "VARCHAR");
        customerTypes.put("c_acctbal", "DECIMAL");
        customerTypes.put("c_mktsegment", "VARCHAR");
        customerTypes.put("c_comment", "VARCHAR");
        FIELD_TYPES.put("customer", customerTypes);
        
        // Orders表字段类型
        Map<String, String> ordersTypes = new HashMap<>();
        ordersTypes.put("o_orderkey", "INTEGER");
        ordersTypes.put("o_custkey", "INTEGER");
        ordersTypes.put("o_orderstatus", "VARCHAR");
        ordersTypes.put("o_totalprice", "DECIMAL");
        ordersTypes.put("o_orderdate", "DATE");
        ordersTypes.put("o_orderpriority", "VARCHAR");
        ordersTypes.put("o_clerk", "VARCHAR");
        ordersTypes.put("o_shippriority", "INTEGER");
        ordersTypes.put("o_comment", "VARCHAR");
        FIELD_TYPES.put("orders", ordersTypes);
        
        // 为其他表添加类似的配置...
        // 这里简化处理，实际使用时可以扩展
    }
    
    @Override
    public List<List<String>> cleanData(String tableName, List<List<String>> records) {
        log.info("开始清洗表 {} 的数据，记录数: {}", tableName, records.size());
        
        List<List<String>> cleanedRecords = new ArrayList<>();
        List<String> fieldNames = TABLE_FIELDS.get(tableName.toLowerCase());
        
        if (fieldNames == null) {
            log.warn("未知的表名: {}, 跳过数据清洗", tableName);
            return records;
        }
        
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(tableName, record)) {
                List<String> cleanedRecord = new ArrayList<>();
                
                for (int i = 0; i < record.size() && i < fieldNames.size(); i++) {
                    String fieldName = fieldNames.get(i);
                    String originalValue = record.get(i);
                    String cleanedValue = cleanFieldValue(tableName, fieldName, originalValue);
                    cleanedRecord.add(cleanedValue);
                }
                
                cleanedRecords.add(cleanedRecord);
                validCount++;
            } else {
                invalidCount++;
                log.debug("无效记录被过滤: {}", record);
            }
        }
        
        log.info("表 {} 数据清洗完成，有效记录: {}, 无效记录: {}", tableName, validCount, invalidCount);
        return cleanedRecords;
    }
    
    @Override
    public boolean validateRecord(String tableName, List<String> record) {
        List<String> fieldNames = TABLE_FIELDS.get(tableName.toLowerCase());
        
        if (fieldNames == null || record == null) {
            return false;
        }
        
        // 检查字段数量
        if (record.size() != fieldNames.size()) {
            log.debug("记录字段数量不匹配，期望: {}, 实际: {}", fieldNames.size(), record.size());
            return false;
        }
        
        // 检查关键字段不能为空
        for (int i = 0; i < record.size(); i++) {
            String value = record.get(i);
            String fieldName = fieldNames.get(i);
            
            // 主键字段不能为空
            if (fieldName.endsWith("key") && (value == null || value.trim().isEmpty())) {
                log.debug("主键字段 {} 为空", fieldName);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Map<String, Object> getTableFieldConfig(String tableName) {
        Map<String, Object> config = new HashMap<>();
        config.put("fields", TABLE_FIELDS.get(tableName.toLowerCase()));
        config.put("fieldTypes", FIELD_TYPES.get(tableName.toLowerCase()));
        return config;
    }
    
    @Override
    public String cleanFieldValue(String tableName, String fieldName, String value) {
        if (value == null) {
            return "";
        }
        
        // 移除首尾空格
        String cleaned = value.trim();
        
        // 获取字段类型
        Map<String, String> fieldTypes = FIELD_TYPES.get(tableName.toLowerCase());
        if (fieldTypes == null) {
            return cleaned;
        }
        
        String fieldType = fieldTypes.get(fieldName.toLowerCase());
        if (fieldType == null) {
            return cleaned;
        }
        
        try {
            switch (fieldType.toUpperCase()) {
                case "INTEGER":
                    // 清洗整数字段
                    if (cleaned.isEmpty()) {
                        return "0";
                    }
                    // 移除非数字字符（除了负号）
                    cleaned = cleaned.replaceAll("[^-\\d]", "");
                    if (cleaned.isEmpty() || cleaned.equals("-")) {
                        return "0";
                    }
                    // 验证是否为有效整数
                    Integer.parseInt(cleaned);
                    return cleaned;
                    
                case "DECIMAL":
                    // 清洗小数字段
                    if (cleaned.isEmpty()) {
                        return "0.00";
                    }
                    // 移除非数字字符（除了负号和小数点）
                    cleaned = cleaned.replaceAll("[^-\\d.]", "");
                    if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
                        return "0.00";
                    }
                    // 验证是否为有效数字
                    Double.parseDouble(cleaned);
                    return cleaned;
                    
                case "DATE":
                    // 清洗日期字段
                    if (cleaned.isEmpty()) {
                        return "1970-01-01";
                    }
                    // 简单的日期格式验证
                    if (DATE_PATTERN.matcher(cleaned).matches()) {
                        return cleaned;
                    }
                    return "1970-01-01";
                    
                case "VARCHAR":
                default:
                    // 清洗字符串字段
                    // 移除特殊字符，保留字母、数字、空格和常见标点
                    cleaned = cleaned.replaceAll("[^\\w\\s\\-.,()&]", "");
                    // 限制长度（根据具体需求调整）
                    if (cleaned.length() > 255) {
                        cleaned = cleaned.substring(0, 255);
                    }
                    return cleaned;
            }
        } catch (Exception e) {
            log.warn("清洗字段值失败: 表={}, 字段={}, 值={}, 错误={}", tableName, fieldName, value, e.getMessage());
            // 返回默认值
            switch (fieldType.toUpperCase()) {
                case "INTEGER":
                    return "0";
                case "DECIMAL":
                    return "0.00";
                case "DATE":
                    return "1970-01-01";
                default:
                    return "";
            }
        }
    }
} 