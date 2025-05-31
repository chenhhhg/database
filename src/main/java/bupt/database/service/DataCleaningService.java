package bupt.database.service;

import java.util.List;
import java.util.Map;

/**
 * 数据清洗服务接口
 * 提供可扩展的数据清洗功能
 */
public interface DataCleaningService {
    
    /**
     * 清洗数据记录
     * 
     * @param tableName 表名
     * @param records 原始数据记录列表，每条记录是字段值的列表
     * @return 清洗后的数据记录列表
     */
    List<List<String>> cleanData(String tableName, List<List<String>> records);
    
    /**
     * 验证数据记录
     * 
     * @param tableName 表名
     * @param record 数据记录
     * @return 验证结果，true表示数据有效
     */
    boolean validateRecord(String tableName, List<String> record);
    
    /**
     * 获取表的字段配置
     * 
     * @param tableName 表名
     * @return 字段配置信息，包括字段名、类型、长度限制等
     */
    Map<String, Object> getTableFieldConfig(String tableName);
    
    /**
     * 清洗单个字段值
     * 
     * @param tableName 表名
     * @param fieldName 字段名
     * @param value 原始值
     * @return 清洗后的值
     */
    String cleanFieldValue(String tableName, String fieldName, String value);
} 