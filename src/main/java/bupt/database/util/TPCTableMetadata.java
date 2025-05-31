package bupt.database.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * TPC-H表元数据工具类
 * 提供表字段配置、类型定义等辅助信息
 */
public class TPCTableMetadata {
    
    // 字段类型枚举
    public enum FieldType {
        INTEGER, DECIMAL, VARCHAR, DATE, CHAR
    }
    
    // 验证模式
    public static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    public static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    public static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    
    // 表字段配置
    private static final Map<String, List<String>> TABLE_FIELDS = new HashMap<>();
    
    // 字段类型配置
    private static final Map<String, Map<String, FieldType>> FIELD_TYPES = new HashMap<>();
    
    // 字段长度限制
    private static final Map<String, Map<String, Integer>> FIELD_LENGTHS = new HashMap<>();
    
    // 主键字段配置
    private static final Map<String, String> PRIMARY_KEYS = new HashMap<>();
    
    static {
        initTableMetadata();
    }
    
    private static void initTableMetadata() {
        // Customer表配置
        TABLE_FIELDS.put("customer", Arrays.asList(
            "c_custkey", "c_name", "c_address", "c_nationkey", "c_phone", "c_acctbal", "c_mktsegment", "c_comment"
        ));
        
        Map<String, FieldType> customerTypes = new HashMap<>();
        customerTypes.put("c_custkey", FieldType.INTEGER);
        customerTypes.put("c_name", FieldType.VARCHAR);
        customerTypes.put("c_address", FieldType.VARCHAR);
        customerTypes.put("c_nationkey", FieldType.INTEGER);
        customerTypes.put("c_phone", FieldType.CHAR);
        customerTypes.put("c_acctbal", FieldType.DECIMAL);
        customerTypes.put("c_mktsegment", FieldType.CHAR);
        customerTypes.put("c_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("customer", customerTypes);
        
        Map<String, Integer> customerLengths = new HashMap<>();
        customerLengths.put("c_name", 25);
        customerLengths.put("c_address", 40);
        customerLengths.put("c_phone", 15);
        customerLengths.put("c_mktsegment", 10);
        customerLengths.put("c_comment", 117);
        FIELD_LENGTHS.put("customer", customerLengths);
        
        PRIMARY_KEYS.put("customer", "c_custkey");
        
        // Orders表配置
        TABLE_FIELDS.put("orders", Arrays.asList(
            "o_orderkey", "o_custkey", "o_orderstatus", "o_totalprice", "o_orderdate", 
            "o_orderpriority", "o_clerk", "o_shippriority", "o_comment"
        ));
        
        Map<String, FieldType> ordersTypes = new HashMap<>();
        ordersTypes.put("o_orderkey", FieldType.INTEGER);
        ordersTypes.put("o_custkey", FieldType.INTEGER);
        ordersTypes.put("o_orderstatus", FieldType.CHAR);
        ordersTypes.put("o_totalprice", FieldType.DECIMAL);
        ordersTypes.put("o_orderdate", FieldType.DATE);
        ordersTypes.put("o_orderpriority", FieldType.CHAR);
        ordersTypes.put("o_clerk", FieldType.CHAR);
        ordersTypes.put("o_shippriority", FieldType.INTEGER);
        ordersTypes.put("o_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("orders", ordersTypes);
        
        Map<String, Integer> ordersLengths = new HashMap<>();
        ordersLengths.put("o_orderstatus", 1);
        ordersLengths.put("o_orderpriority", 15);
        ordersLengths.put("o_clerk", 15);
        ordersLengths.put("o_comment", 79);
        FIELD_LENGTHS.put("orders", ordersLengths);
        
        PRIMARY_KEYS.put("orders", "o_orderkey");
        
        // LineItem表配置
        TABLE_FIELDS.put("lineitem", Arrays.asList(
            "l_orderkey", "l_partkey", "l_suppkey", "l_linenumber", "l_quantity", "l_extendedprice", 
            "l_discount", "l_tax", "l_returnflag", "l_linestatus", "l_shipdate", "l_commitdate", 
            "l_receiptdate", "l_shipinstruct", "l_shipmode", "l_comment"
        ));
        
        Map<String, FieldType> lineitemTypes = new HashMap<>();
        lineitemTypes.put("l_orderkey", FieldType.INTEGER);
        lineitemTypes.put("l_partkey", FieldType.INTEGER);
        lineitemTypes.put("l_suppkey", FieldType.INTEGER);
        lineitemTypes.put("l_linenumber", FieldType.INTEGER);
        lineitemTypes.put("l_quantity", FieldType.DECIMAL);
        lineitemTypes.put("l_extendedprice", FieldType.DECIMAL);
        lineitemTypes.put("l_discount", FieldType.DECIMAL);
        lineitemTypes.put("l_tax", FieldType.DECIMAL);
        lineitemTypes.put("l_returnflag", FieldType.CHAR);
        lineitemTypes.put("l_linestatus", FieldType.CHAR);
        lineitemTypes.put("l_shipdate", FieldType.DATE);
        lineitemTypes.put("l_commitdate", FieldType.DATE);
        lineitemTypes.put("l_receiptdate", FieldType.DATE);
        lineitemTypes.put("l_shipinstruct", FieldType.CHAR);
        lineitemTypes.put("l_shipmode", FieldType.CHAR);
        lineitemTypes.put("l_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("lineitem", lineitemTypes);
        
        Map<String, Integer> lineitemLengths = new HashMap<>();
        lineitemLengths.put("l_returnflag", 1);
        lineitemLengths.put("l_linestatus", 1);
        lineitemLengths.put("l_shipinstruct", 25);
        lineitemLengths.put("l_shipmode", 10);
        lineitemLengths.put("l_comment", 44);
        FIELD_LENGTHS.put("lineitem", lineitemLengths);
        
        PRIMARY_KEYS.put("lineitem", "l_orderkey,l_linenumber");
        
        // Nation表配置
        TABLE_FIELDS.put("nation", Arrays.asList("n_nationkey", "n_name", "n_regionkey", "n_comment"));
        
        Map<String, FieldType> nationTypes = new HashMap<>();
        nationTypes.put("n_nationkey", FieldType.INTEGER);
        nationTypes.put("n_name", FieldType.CHAR);
        nationTypes.put("n_regionkey", FieldType.INTEGER);
        nationTypes.put("n_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("nation", nationTypes);
        
        Map<String, Integer> nationLengths = new HashMap<>();
        nationLengths.put("n_name", 25);
        nationLengths.put("n_comment", 152);
        FIELD_LENGTHS.put("nation", nationLengths);
        
        PRIMARY_KEYS.put("nation", "n_nationkey");
        
        // Part表配置
        TABLE_FIELDS.put("part", Arrays.asList(
            "p_partkey", "p_name", "p_mfgr", "p_brand", "p_type", "p_size", "p_container", "p_retailprice", "p_comment"
        ));
        
        Map<String, FieldType> partTypes = new HashMap<>();
        partTypes.put("p_partkey", FieldType.INTEGER);
        partTypes.put("p_name", FieldType.VARCHAR);
        partTypes.put("p_mfgr", FieldType.CHAR);
        partTypes.put("p_brand", FieldType.CHAR);
        partTypes.put("p_type", FieldType.VARCHAR);
        partTypes.put("p_size", FieldType.INTEGER);
        partTypes.put("p_container", FieldType.CHAR);
        partTypes.put("p_retailprice", FieldType.DECIMAL);
        partTypes.put("p_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("part", partTypes);
        
        Map<String, Integer> partLengths = new HashMap<>();
        partLengths.put("p_name", 55);
        partLengths.put("p_mfgr", 25);
        partLengths.put("p_brand", 10);
        partLengths.put("p_type", 25);
        partLengths.put("p_container", 10);
        partLengths.put("p_comment", 23);
        FIELD_LENGTHS.put("part", partLengths);
        
        PRIMARY_KEYS.put("part", "p_partkey");
        
        // PartSupp表配置
        TABLE_FIELDS.put("partsupp", Arrays.asList("ps_partkey", "ps_suppkey", "ps_availqty", "ps_supplycost", "ps_comment"));
        
        Map<String, FieldType> partsuppTypes = new HashMap<>();
        partsuppTypes.put("ps_partkey", FieldType.INTEGER);
        partsuppTypes.put("ps_suppkey", FieldType.INTEGER);
        partsuppTypes.put("ps_availqty", FieldType.INTEGER);
        partsuppTypes.put("ps_supplycost", FieldType.DECIMAL);
        partsuppTypes.put("ps_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("partsupp", partsuppTypes);
        
        Map<String, Integer> partsuppLengths = new HashMap<>();
        partsuppLengths.put("ps_comment", 199);
        FIELD_LENGTHS.put("partsupp", partsuppLengths);
        
        PRIMARY_KEYS.put("partsupp", "ps_partkey,ps_suppkey");
        
        // Region表配置
        TABLE_FIELDS.put("region", Arrays.asList("r_regionkey", "r_name", "r_comment"));
        
        Map<String, FieldType> regionTypes = new HashMap<>();
        regionTypes.put("r_regionkey", FieldType.INTEGER);
        regionTypes.put("r_name", FieldType.CHAR);
        regionTypes.put("r_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("region", regionTypes);
        
        Map<String, Integer> regionLengths = new HashMap<>();
        regionLengths.put("r_name", 25);
        regionLengths.put("r_comment", 152);
        FIELD_LENGTHS.put("region", regionLengths);
        
        PRIMARY_KEYS.put("region", "r_regionkey");
        
        // Supplier表配置
        TABLE_FIELDS.put("supplier", Arrays.asList(
            "s_suppkey", "s_name", "s_address", "s_nationkey", "s_phone", "s_acctbal", "s_comment"
        ));
        
        Map<String, FieldType> supplierTypes = new HashMap<>();
        supplierTypes.put("s_suppkey", FieldType.INTEGER);
        supplierTypes.put("s_name", FieldType.CHAR);
        supplierTypes.put("s_address", FieldType.VARCHAR);
        supplierTypes.put("s_nationkey", FieldType.INTEGER);
        supplierTypes.put("s_phone", FieldType.CHAR);
        supplierTypes.put("s_acctbal", FieldType.DECIMAL);
        supplierTypes.put("s_comment", FieldType.VARCHAR);
        FIELD_TYPES.put("supplier", supplierTypes);
        
        Map<String, Integer> supplierLengths = new HashMap<>();
        supplierLengths.put("s_name", 25);
        supplierLengths.put("s_address", 40);
        supplierLengths.put("s_phone", 15);
        supplierLengths.put("s_comment", 101);
        FIELD_LENGTHS.put("supplier", supplierLengths);
        
        PRIMARY_KEYS.put("supplier", "s_suppkey");
    }
    
    // 获取表字段列表
    public static List<String> getTableFields(String tableName) {
        return TABLE_FIELDS.get(tableName.toLowerCase());
    }
    
    // 获取字段类型配置
    public static Map<String, FieldType> getFieldTypes(String tableName) {
        return FIELD_TYPES.get(tableName.toLowerCase());
    }
    
    // 获取字段长度配置
    public static Map<String, Integer> getFieldLengths(String tableName) {
        return FIELD_LENGTHS.getOrDefault(tableName.toLowerCase(), new HashMap<>());
    }
    
    // 获取主键字段
    public static String getPrimaryKey(String tableName) {
        return PRIMARY_KEYS.get(tableName.toLowerCase());
    }
    
    // 获取字段类型
    public static FieldType getFieldType(String tableName, String fieldName) {
        Map<String, FieldType> types = getFieldTypes(tableName);
        return types != null ? types.get(fieldName.toLowerCase()) : null;
    }
    
    // 获取字段长度限制
    public static Integer getFieldLength(String tableName, String fieldName) {
        Map<String, Integer> lengths = getFieldLengths(tableName);
        return lengths.get(fieldName.toLowerCase());
    }
    
    // 验证字段值
    public static boolean isValidFieldValue(String tableName, String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            // 检查是否为主键字段
            String primaryKey = getPrimaryKey(tableName);
            if (primaryKey != null && primaryKey.contains(fieldName.toLowerCase())) {
                return false; // 主键不能为空
            }
            return true; // 非主键字段可以为空
        }
        
        FieldType fieldType = getFieldType(tableName, fieldName);
        if (fieldType == null) {
            return true;
        }
        
        switch (fieldType) {
            case INTEGER:
                return INTEGER_PATTERN.matcher(value.trim()).matches();
            case DECIMAL:
                return NUMBER_PATTERN.matcher(value.trim()).matches();
            case DATE:
                return DATE_PATTERN.matcher(value.trim()).matches();
            case VARCHAR:
            case CHAR:
                Integer maxLength = getFieldLength(tableName, fieldName);
                return maxLength == null || value.length() <= maxLength;
            default:
                return true;
        }
    }
    
    // 清洗字段值
    public static String cleanFieldValue(String tableName, String fieldName, String value) {
        if (value == null) {
            return getDefaultValue(tableName, fieldName);
        }
        
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return getDefaultValue(tableName, fieldName);
        }
        
        FieldType fieldType = getFieldType(tableName, fieldName);
        if (fieldType == null) {
            return cleaned;
        }
        
        try {
            switch (fieldType) {
                case INTEGER:
                    cleaned = cleaned.replaceAll("[^-\\d]", "");
                    if (cleaned.isEmpty() || cleaned.equals("-")) {
                        return "0";
                    }
                    Integer.parseInt(cleaned); // 验证
                    return cleaned;
                    
                case DECIMAL:
                    cleaned = cleaned.replaceAll("[^-\\d.]", "");
                    if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
                        return "0.00";
                    }
                    Double.parseDouble(cleaned); // 验证
                    return cleaned;
                    
                case DATE:
                    if (!DATE_PATTERN.matcher(cleaned).matches()) {
                        return "1970-01-01";
                    }
                    return cleaned;
                    
                case VARCHAR:
                case CHAR:
                    cleaned = cleaned.replaceAll("[^\\w\\s\\-.,()&]", "");
                    Integer maxLength = getFieldLength(tableName, fieldName);
                    if (maxLength != null && cleaned.length() > maxLength) {
                        cleaned = cleaned.substring(0, maxLength);
                    }
                    return cleaned;
                    
                default:
                    return cleaned;
            }
        } catch (Exception e) {
            return getDefaultValue(tableName, fieldName);
        }
    }
    
    // 获取字段默认值
    public static String getDefaultValue(String tableName, String fieldName) {
        FieldType fieldType = getFieldType(tableName, fieldName);
        if (fieldType == null) {
            return "";
        }
        
        switch (fieldType) {
            case INTEGER:
                return "0";
            case DECIMAL:
                return "0.00";
            case DATE:
                return "1970-01-01";
            case VARCHAR:
            case CHAR:
            default:
                return "";
        }
    }
} 