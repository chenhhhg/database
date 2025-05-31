package bupt.database.handler.impl;

import bupt.database.entity.Supplier;
import bupt.database.handler.AbstractTableDataHandler;
import bupt.database.mapper.SupplierMapper;
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
 * Supplier表数据处理Handler
 * 重点清洗字段：s_suppkey（不为空，>=0）、s_acctbal（数值型）
 */
@Slf4j
@Component
public class SupplierDataHandler extends AbstractTableDataHandler<Supplier> {
    
    @Autowired
    private SupplierMapper supplierMapper;
    
    @Override
    public String getTableName() {
        return "supplier";
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
    public List<Supplier> cleanData(List<List<String>> records) {
        log.info("开始清洗Supplier表数据，原始记录数: {}", records.size());
        
        List<Supplier> cleanedSuppliers = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Supplier supplier = convertToEntity(record);
                    if (supplier != null) {
                        cleanedSuppliers.add(supplier);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换Supplier记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的Supplier记录被过滤: {}", record);
            }
        }
        
        log.info("Supplier表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedSuppliers;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("Supplier记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 特殊清洗：s_suppkey（主键不为空检查，>=0）
        String suppkey = record.get(0); // s_suppkey
        if (suppkey == null || suppkey.trim().isEmpty()) {
            log.debug("Supplier主键字段s_suppkey为空");
            return false;
        }
        
        try {
            int suppkeyValue = Integer.parseInt(suppkey.trim());
            if (suppkeyValue < 0) {
                log.debug("Supplier主键字段s_suppkey小于0: {}", suppkeyValue);
                return false;
            }
        } catch (NumberFormatException e) {
            log.debug("Supplier主键字段s_suppkey格式无效: {}", suppkey);
            return false;
        }
        
        // 特殊清洗：s_acctbal（数值类型检查）
        String acctbal = record.get(5); // s_acctbal
        if (acctbal != null && !acctbal.trim().isEmpty()) {
            try {
                new BigDecimal(acctbal.trim()); // 验证是否为有效数值
            } catch (NumberFormatException e) {
                log.debug("Supplier账户余额字段s_acctbal格式无效: {}", acctbal);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Supplier convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Supplier supplier = new Supplier();
        
        try {
            // s_suppkey (INTEGER) - 特殊清洗：确保>=0
            String suppkeyStr = record.get(0).trim();
            int suppkey = Integer.parseInt(suppkeyStr);
            if (suppkey < 0) {
                suppkey = 0;
                log.warn("Supplier主键字段修正为0: 原值={}", suppkeyStr);
            }
            supplier.setSSuppkey(suppkey);
            
            // s_name (CHAR)
            String name = TPCTableMetadata.cleanFieldValue(getTableName(), "s_name", record.get(1));
            supplier.setSName(name);
            
            // s_address (VARCHAR)
            String address = TPCTableMetadata.cleanFieldValue(getTableName(), "s_address", record.get(2));
            supplier.setSAddress(address);
            
            // s_nationkey (INTEGER)
            String nationkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "s_nationkey", record.get(3));
            supplier.setSNationkey(Integer.valueOf(nationkeyStr));
            
            // s_phone (CHAR)
            String phone = TPCTableMetadata.cleanFieldValue(getTableName(), "s_phone", record.get(4));
            supplier.setSPhone(phone);
            
            // s_acctbal (DECIMAL) - 特殊清洗：数值类型检查
            String acctbalStr = record.get(5).trim();
            BigDecimal acctbal;
            if (acctbalStr.isEmpty()) {
                acctbal = BigDecimal.ZERO;
                log.warn("Supplier账户余额字段为空，修正为0");
            } else {
                try {
                    acctbal = new BigDecimal(acctbalStr);
                } catch (NumberFormatException e) {
                    acctbal = BigDecimal.ZERO;
                    log.warn("Supplier账户余额字段格式无效，修正为0: 原值={}", acctbalStr);
                }
            }
            supplier.setSAcctbal(acctbal);
            
            // s_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "s_comment", record.get(6));
            supplier.setSComment(comment);
            
            log.debug("成功转换Supplier记录: suppkey={}, name={}, acctbal={}", 
                supplier.getSSuppkey(), supplier.getSName(), supplier.getSAcctbal());
            return supplier;
            
        } catch (Exception e) {
            log.error("转换Supplier实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    protected Long getMaxPrimaryKey() {
        try {
            // 查询Supplier表中最大的suppkey值
            Supplier maxSupplier = supplierMapper.selectOne(
                new QueryWrapper<Supplier>()
                    .select("MAX(s_suppkey) as s_suppkey")
                    .last("LIMIT 1")
            );
            
            if (maxSupplier != null && maxSupplier.getSSuppkey() != null) {
                Long maxKey = maxSupplier.getSSuppkey().longValue();
                log.debug("Supplier表最大主键值: {}", maxKey);
                return maxKey;
            } else {
                log.debug("Supplier表为空，返回主键值: 0");
                return 0L;
            }
        } catch (Exception e) {
            log.warn("获取Supplier表最大主键失败，返回默认值0", e);
            return 0L;
        }
    }
    
    @Override
    protected void setEntityPrimaryKey(Supplier entity, Long primaryKeyValue) {
        if (entity != null) {
            entity.setSSuppkey(primaryKeyValue.intValue());
            log.debug("设置Supplier主键: suppkey={}", primaryKeyValue);
        }
    }
    
    @Override
    protected int doActualBatchInsert(List<Supplier> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入Supplier数据，记录数: {}", entities.size());
            
            List<BatchResult> list = supplierMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("Supplier批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("Supplier批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    private int insertOneByOne(List<Supplier> entities) {
        log.info("尝试逐条插入Supplier数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Supplier supplier : entities) {
            try {
                int result = supplierMapper.insert(supplier);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条Supplier记录失败: suppkey={}, 错误: {}", 
                    supplier.getSSuppkey(), e.getMessage());
            }
        }
        
        log.info("Supplier逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空Supplier表数据");
            supplierMapper.delete(new QueryWrapper<>());
            log.info("Supplier表清空完成");
        } catch (Exception e) {
            log.error("清空Supplier表失败", e);
            throw new RuntimeException("清空Supplier表失败", e);
        }
    }
} 