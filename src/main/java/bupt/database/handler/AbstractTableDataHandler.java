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
     * 为实体对象设置主键为null（用于插入时让数据库自动生成主键）
     * @param entity 实体对象
     */
    protected abstract void setEntityPrimaryKeyNull(T entity);
    
    /**
     * 批量设置实体对象主键为null
     * @param entities 实体对象列表
     */
    protected void setEntitiesPrimaryKeyNull(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        
        log.debug("开始设置{}个{}实体的主键为null", entities.size(), getTableName());
        
        for (T entity : entities) {
            if (entity != null) {
                setEntityPrimaryKeyNull(entity);
            }
        }
        
        log.debug("完成设置{}实体主键为null", getTableName());
    }
    
    /**
     * 重写batchInsert方法，在插入前自动设置主键为null
     */
    @Override
    public int batchInsert(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        // 在插入前设置主键为null
        setEntitiesPrimaryKeyNull(entities);
        
        // 调用具体实现的插入逻辑
        return doActualBatchInsert(entities);
    }
    
    /**
     * 子类需要实现的实际批量插入逻辑
     * @param entities 实体对象列表（主键已设置为null）
     * @return 插入成功的记录数
     */
    protected abstract int doActualBatchInsert(List<T> entities);
} 