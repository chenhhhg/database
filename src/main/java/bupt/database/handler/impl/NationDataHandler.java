package bupt.database.handler.impl;

import bupt.database.entity.Nation;
import bupt.database.handler.TableDataHandler;
import bupt.database.mapper.NationMapper;
import bupt.database.util.TPCTableMetadata;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Nation表数据处理Handler
 * 重点清洗字段：n_nationkey（不为空，>=0）、n_name（不为空，长度检查）
 */
@Slf4j
@Component
public class NationDataHandler implements TableDataHandler<Nation> {
    
    @Autowired
    private NationMapper nationMapper;
    
    @Override
    public String getTableName() {
        return "nation";
    }
    
    @Override
    public Map<String, String> getFieldTypes() {
        return TPCTableMetadata.getFieldTypes(getTableName())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().name()
                ));
    }
    
    @Override
    public List<String> getFieldNames() {
        return TPCTableMetadata.getTableFields(getTableName());
    }
    
    @Override
    public List<Nation> cleanData(List<List<String>> records) {
        log.info("开始清洗Nation表数据，原始记录数: {}", records.size());
        
        List<Nation> cleanedNations = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Nation nation = convertToEntity(record);
                    if (nation != null) {
                        cleanedNations.add(nation);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换Nation记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的Nation记录被过滤: {}", record);
            }
        }
        
        log.info("Nation表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedNations;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("Nation记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 特殊清洗：n_nationkey（主键不为空检查，>=0）
        String nationkey = record.get(0); // n_nationkey
        if (nationkey == null || nationkey.trim().isEmpty()) {
            log.debug("Nation主键字段n_nationkey为空");
            return false;
        }
        
        try {
            int nationkeyValue = Integer.parseInt(nationkey.trim());
            if (nationkeyValue < 0) {
                log.debug("Nation主键字段n_nationkey小于0: {}", nationkeyValue);
                return false;
            }
        } catch (NumberFormatException e) {
            log.debug("Nation主键字段n_nationkey格式无效: {}", nationkey);
            return false;
        }
        
        // 特殊清洗：n_name（不为空检查，长度检查）
        String name = record.get(1); // n_name
        if (name == null || name.trim().isEmpty()) {
            log.debug("Nation国家名称字段n_name为空");
            return false;
        }
        
        // 检查名称长度
        Integer maxLength = TPCTableMetadata.getFieldLength(getTableName(), "n_name");
        if (maxLength != null && name.trim().length() > maxLength) {
            log.debug("Nation国家名称字段n_name长度超限: 当前={}, 最大={}", name.trim().length(), maxLength);
            return false;
        }
        
        return true;
    }
    
    @Override
    public Nation convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Nation nation = new Nation();
        
        try {
            // n_nationkey (INTEGER) - 特殊清洗：确保>=0
            String nationkeyStr = record.get(0).trim();
            int nationkey = Integer.parseInt(nationkeyStr);
            if (nationkey < 0) {
                nationkey = 0;
                log.warn("Nation主键字段修正为0: 原值={}", nationkeyStr);
            }
            nation.setN_NATIONKEY(nationkey);
            
            // n_name (CHAR) - 特殊清洗：不为空，长度检查
            String name = record.get(1).trim();
            if (name.isEmpty()) {
                name = "UNKNOWN";
                log.warn("Nation国家名称字段修正为UNKNOWN: 原值为空");
            }
            
            // 长度截断处理
            Integer maxLength = TPCTableMetadata.getFieldLength(getTableName(), "n_name");
            if (maxLength != null && name.length() > maxLength) {
                name = name.substring(0, maxLength);
                log.warn("Nation国家名称字段截断: 新长度={}", maxLength);
            }
            nation.setN_NAME(name);
            
            // n_regionkey (INTEGER)
            String regionkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "n_regionkey", record.get(2));
            nation.setN_REGIONKEY(Integer.valueOf(regionkeyStr));
            
            // n_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "n_comment", record.get(3));
            nation.setN_COMMENT(comment);
            
            log.debug("成功转换Nation记录: nationkey={}, name={}", nation.getN_NATIONKEY(), nation.getN_NAME());
            return nation;
            
        } catch (Exception e) {
            log.error("转换Nation实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    public int batchInsert(List<Nation> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入Nation数据，记录数: {}", entities.size());
            
            List<BatchResult> list = nationMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("Nation批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("Nation批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    private int insertOneByOne(List<Nation> entities) {
        log.info("尝试逐条插入Nation数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Nation nation : entities) {
            try {
                int result = nationMapper.insert(nation);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条Nation记录失败: nationkey={}, 错误: {}", 
                    nation.getN_NATIONKEY(), e.getMessage());
            }
        }
        
        log.info("Nation逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空Nation表数据");
            nationMapper.delete(new QueryWrapper<>());
            log.info("Nation表清空完成");
        } catch (Exception e) {
            log.error("清空Nation表失败", e);
            throw new RuntimeException("清空Nation表失败", e);
        }
    }
} 