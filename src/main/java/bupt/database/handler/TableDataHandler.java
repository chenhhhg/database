package bupt.database.handler;

import java.util.List;
import java.util.Map;

/**
 * 表数据处理Handler接口
 * 每个TPC-H表都需要实现此接口
 */
public interface TableDataHandler<T> {
    
    /**
     * 获取表名
     */
    String getTableName();
    
    /**
     * 获取字段配置
     */
    Map<String, String> getFieldTypes();
    
    /**
     * 获取字段名列表
     */
    List<String> getFieldNames();
    
    /**
     * 清洗数据记录
     * @param records 原始数据记录列表
     * @return 清洗后的实体对象列表
     */
    List<T> cleanData(List<List<String>> records);
    
    /**
     * 验证单条记录
     * @param record 原始数据记录
     * @return 是否有效
     */
    boolean validateRecord(List<String> record);
    
    /**
     * 将原始记录转换为实体对象
     * @param record 原始数据记录
     * @return 实体对象
     */
    T convertToEntity(List<String> record);
    
    /**
     * 批量插入数据
     * @param entities 实体对象列表
     * @return 插入成功的记录数
     */
    int batchInsert(List<T> entities);
    
    /**
     * 清空表数据
     */
    void truncateTable();
} 