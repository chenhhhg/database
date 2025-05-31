package bupt.database.handler.impl;

import bupt.database.entity.Partsupp;
import bupt.database.handler.AbstractTableDataHandler;
import bupt.database.mapper.PartsuppMapper;
import bupt.database.util.TPCTableMetadata;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PartSupp表数据处理Handler
 * 重点清洗字段：ps_availqty（零件供应数量>=0）、ps_supplycost（数值型，>=0）
 */
@Slf4j
@Component
public class PartSuppDataHandler extends AbstractTableDataHandler<Partsupp> {
    
    @Autowired
    private PartsuppMapper partsuppMapper;
    
    @Override
    public String getTableName() {
        return "partsupp";
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
    public List<Partsupp> cleanData(List<List<String>> records) {
        log.info("开始清洗PartSupp表数据，原始记录数: {}", records.size());
        
        List<Partsupp> cleanedPartsupps = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Partsupp partsupp = convertToEntity(record);
                    if (partsupp != null) {
                        cleanedPartsupps.add(partsupp);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换PartSupp记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的PartSupp记录被过滤: {}", record);
            }
        }
        
        log.info("PartSupp表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedPartsupps;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("PartSupp记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 验证主键字段不能为空
        String partkey = record.get(0); // ps_partkey
        String suppkey = record.get(1); // ps_suppkey
        if (partkey == null || partkey.trim().isEmpty() || 
            suppkey == null || suppkey.trim().isEmpty()) {
            log.debug("PartSupp主键字段为空: partkey={}, suppkey={}", partkey, suppkey);
            return false;
        }
        
        // 特殊清洗：ps_availqty（零件供应数量范围检查>=0）
        String availqty = record.get(2); // ps_availqty
        if (availqty != null && !availqty.trim().isEmpty()) {
            try {
                int availqtyValue = Integer.parseInt(availqty.trim());
                if (availqtyValue < 0) {
                    log.debug("PartSupp零件供应数量字段ps_availqty小于0: {}", availqtyValue);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.debug("PartSupp零件供应数量字段ps_availqty格式无效: {}", availqty);
                return false;
            }
        }
        
        // 特殊清洗：ps_supplycost（数值类型检查，>=0）
        String supplycost = record.get(3); // ps_supplycost
        if (supplycost != null && !supplycost.trim().isEmpty()) {
            try {
                BigDecimal costValue = new BigDecimal(supplycost.trim());
                if (costValue.compareTo(BigDecimal.ZERO) < 0) {
                    log.debug("PartSupp供应成本字段ps_supplycost小于0: {}", costValue);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.debug("PartSupp供应成本字段ps_supplycost格式无效: {}", supplycost);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Partsupp convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Partsupp partsupp = new Partsupp();
        
        try {
            // ps_partkey (INTEGER)
            String partkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "ps_partkey", record.get(0));
            partsupp.setPsPartkey(Integer.valueOf(partkeyStr));
            
            // ps_suppkey (INTEGER)
            String suppkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "ps_suppkey", record.get(1));
            partsupp.setPsSuppkey(Integer.valueOf(suppkeyStr));
            
            // ps_availqty (INTEGER) - 特殊清洗：确保>=0
            String availqtyStr = record.get(2).trim();
            int availqty = Integer.parseInt(availqtyStr);
            if (availqty < 0) {
                availqty = 0;
                log.warn("PartSupp零件供应数量字段修正为0: 原值={}", availqtyStr);
            }
            partsupp.setPsAvailqty(availqty);
            
            // ps_supplycost (DECIMAL) - 特殊清洗：确保>=0
            String supplycostStr = record.get(3).trim();
            BigDecimal supplycost = new BigDecimal(supplycostStr);
            if (supplycost.compareTo(BigDecimal.ZERO) < 0) {
                supplycost = BigDecimal.ZERO;
                log.warn("PartSupp供应成本字段修正为0: 原值={}", supplycostStr);
            }
            partsupp.setPsSupplycost(supplycost);
            
            // ps_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "ps_comment", record.get(4));
            partsupp.setPsComment(comment);
            
            log.debug("成功转换PartSupp记录: partkey={}, suppkey={}, availqty={}", 
                partsupp.getPsPartkey(), partsupp.getPsSuppkey(), partsupp.getPsAvailqty());
            return partsupp;
            
        } catch (Exception e) {
            log.error("转换PartSupp实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    protected void setEntityPrimaryKeyNull(Partsupp entity) {
        if (entity != null) {
            entity.setPsPartkey(null);
            entity.setPsSuppkey(null);
            log.debug("设置PartSupp主键为null: partkey=null, suppkey=null");
        }
    }
    
    @Override
    protected int doActualBatchInsert(List<Partsupp> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入PartSupp数据，记录数: {}", entities.size());
            
            List<BatchResult> list = partsuppMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("PartSupp批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("PartSupp批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    private int insertOneByOne(List<Partsupp> entities) {
        log.info("尝试逐条插入PartSupp数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Partsupp partsupp : entities) {
            try {
                int result = partsuppMapper.insert(partsupp);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条PartSupp记录失败: partkey={}, suppkey={}, 错误: {}", 
                    partsupp.getPsPartkey(), partsupp.getPsSuppkey(), e.getMessage());
            }
        }
        
        log.info("PartSupp逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空PartSupp表数据");
            partsuppMapper.delete(new QueryWrapper<>());
            log.info("PartSupp表清空完成");
        } catch (Exception e) {
            log.error("清空PartSupp表失败", e);
            throw new RuntimeException("清空PartSupp表失败", e);
        }
    }
} 