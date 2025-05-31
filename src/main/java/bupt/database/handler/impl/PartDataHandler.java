package bupt.database.handler.impl;

import bupt.database.entity.Part;
import bupt.database.handler.AbstractTableDataHandler;
import bupt.database.mapper.PartMapper;
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
 * Part表数据处理Handler
 * 重点清洗字段：p_size（整数型，>=0）、p_retailprice（数值型，>=0）
 */
@Slf4j
@Component
public class PartDataHandler extends AbstractTableDataHandler<Part> {
    
    @Autowired
    private PartMapper partMapper;
    
    @Override
    public String getTableName() {
        return "part";
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
    public List<Part> cleanData(List<List<String>> records) {
        log.info("开始清洗Part表数据，原始记录数: {}", records.size());
        
        List<Part> cleanedParts = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Part part = convertToEntity(record);
                    if (part != null) {
                        cleanedParts.add(part);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换Part记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的Part记录被过滤: {}", record);
            }
        }
        
        log.info("Part表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedParts;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("Part记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 验证主键字段不能为空
        String partkey = record.get(0); // p_partkey
        if (partkey == null || partkey.trim().isEmpty()) {
            log.debug("Part主键字段p_partkey为空");
            return false;
        }
        
        // 特殊清洗：p_size（整数类型检查，>=0）
        String size = record.get(5); // p_size
        if (size != null && !size.trim().isEmpty()) {
            try {
                int sizeValue = Integer.parseInt(size.trim());
                if (sizeValue < 0) {
                    log.debug("Part尺寸字段p_size小于0: {}", sizeValue);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.debug("Part尺寸字段p_size格式无效: {}", size);
                return false;
            }
        }
        
        // 特殊清洗：p_retailprice（数值类型检查，>=0）
        String retailprice = record.get(7); // p_retailprice
        if (retailprice != null && !retailprice.trim().isEmpty()) {
            try {
                BigDecimal priceValue = new BigDecimal(retailprice.trim());
                if (priceValue.compareTo(BigDecimal.ZERO) < 0) {
                    log.debug("Part零售价格字段p_retailprice小于0: {}", priceValue);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.debug("Part零售价格字段p_retailprice格式无效: {}", retailprice);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Part convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Part part = new Part();
        
        try {
            // p_partkey (INTEGER)
            String partkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "p_partkey", record.get(0));
            part.setPPartkey(Integer.valueOf(partkeyStr));
            
            // p_name (VARCHAR)
            String name = TPCTableMetadata.cleanFieldValue(getTableName(), "p_name", record.get(1));
            part.setPName(name);
            
            // p_mfgr (CHAR)
            String mfgr = TPCTableMetadata.cleanFieldValue(getTableName(), "p_mfgr", record.get(2));
            part.setPMfgr(mfgr);
            
            // p_brand (CHAR)
            String brand = TPCTableMetadata.cleanFieldValue(getTableName(), "p_brand", record.get(3));
            part.setPBrand(brand);
            
            // p_type (VARCHAR)
            String type = TPCTableMetadata.cleanFieldValue(getTableName(), "p_type", record.get(4));
            part.setPType(type);
            
            // p_size (INTEGER) - 特殊清洗：确保>=0
            String sizeStr = record.get(5).trim();
            int size = Integer.parseInt(sizeStr);
            if (size < 0) {
                size = 0;
                log.warn("Part尺寸字段修正为0: 原值={}", sizeStr);
            }
            part.setPSize(size);
            
            // p_container (CHAR)
            String container = TPCTableMetadata.cleanFieldValue(getTableName(), "p_container", record.get(6));
            part.setPContainer(container);
            
            // p_retailprice (DECIMAL) - 特殊清洗：确保>=0
            String retailpriceStr = record.get(7).trim();
            BigDecimal retailprice = new BigDecimal(retailpriceStr);
            if (retailprice.compareTo(BigDecimal.ZERO) < 0) {
                retailprice = BigDecimal.ZERO;
                log.warn("Part零售价格字段修正为0: 原值={}", retailpriceStr);
            }
            part.setPRetailprice(retailprice);
            
            // p_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "p_comment", record.get(8));
            part.setPComment(comment);
            
            log.debug("成功转换Part记录: partkey={}, name={}, size={}", 
                part.getPPartkey(), part.getPName(), part.getPSize());
            return part;
            
        } catch (Exception e) {
            log.error("转换Part实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    protected void setEntityPrimaryKeyNull(Part entity) {
        if (entity != null) {
            entity.setPPartkey(null);
            log.debug("设置Part主键为null: partkey=null");
        }
    }
    
    @Override
    protected int doActualBatchInsert(List<Part> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入Part数据，记录数: {}", entities.size());
            
            List<BatchResult> list = partMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("Part批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("Part批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    private int insertOneByOne(List<Part> entities) {
        log.info("尝试逐条插入Part数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Part part : entities) {
            try {
                int result = partMapper.insert(part);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条Part记录失败: partkey={}, 错误: {}", 
                    part.getPPartkey(), e.getMessage());
            }
        }
        
        log.info("Part逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空Part表数据");
            partMapper.delete(new QueryWrapper<>());
            log.info("Part表清空完成");
        } catch (Exception e) {
            log.error("清空Part表失败", e);
            throw new RuntimeException("清空Part表失败", e);
        }
    }
} 