package bupt.database.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 表数据处理Handler抽象类
 * 提供通用的主键处理功能
 */
@Slf4j
public abstract class AbstractTableDataHandler<T> implements TableDataHandler<T> {
    
    /**
     * 获取表中当前最大主键值
     * @return 最大主键值，如果表为空返回0
     */
    protected abstract Long getMaxPrimaryKey();
    
    /**
     * 为实体对象设置递增主键值
     * @param entity 实体对象
     * @param primaryKeyValue 主键值
     */
    protected abstract void setEntityPrimaryKey(T entity, Long primaryKeyValue);
    
    /**
     * 批量设置实体对象递增主键
     * @param entities 实体对象列表
     */
    protected void setEntitiesIncrementalPrimaryKey(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        
        log.debug("开始为{}个{}实体设置递增主键", entities.size(), getTableName());
        
        // 获取当前最大主键值
        Long maxPrimaryKey = getMaxPrimaryKey();
        log.debug("{}表当前最大主键值: {}", getTableName(), maxPrimaryKey);
        
        // 为每个实体设置递增主键
        Long currentPrimaryKey = maxPrimaryKey;
        for (T entity : entities) {
            if (entity != null) {
                currentPrimaryKey++;
                setEntityPrimaryKey(entity, currentPrimaryKey);
            }
        }
        
        log.debug("完成设置{}实体递增主键，起始值: {}, 结束值: {}", 
            getTableName(), maxPrimaryKey + 1, currentPrimaryKey);
    }
    
    /**
     * 重写batchInsert方法，在插入前自动设置递增主键
     */
    @Override
    public int batchInsert(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        // 在插入前设置递增主键
        setEntitiesIncrementalPrimaryKey(entities);
        
        // 调用具体实现的插入逻辑
        return doActualBatchInsert(entities);
    }
    
    /**
     * 子类需要实现的实际批量插入逻辑
     * @param entities 实体对象列表（主键已设置为递增值）
     * @return 插入成功的记录数
     */
    protected abstract int doActualBatchInsert(List<T> entities);
} 