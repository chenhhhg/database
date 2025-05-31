package bupt.database.handler.impl;

import bupt.database.entity.Lineitem;
import bupt.database.handler.TableDataHandler;
import bupt.database.mapper.LineitemMapper;
import bupt.database.util.TPCTableMetadata;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LineItem表数据处理Handler
 * 重点清洗字段：l_quantity（数量不为空，>=0）、l_extendedprice（数值型，>=0）
 */
@Slf4j
@Component
public class LineItemDataHandler implements TableDataHandler<Lineitem> {
    
    @Autowired
    private LineitemMapper lineitemMapper;
    
    @Override
    public String getTableName() {
        return "lineitem";
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
    public List<Lineitem> cleanData(List<List<String>> records) {
        log.info("开始清洗LineItem表数据，原始记录数: {}", records.size());
        
        List<Lineitem> cleanedLineitems = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Lineitem lineitem = convertToEntity(record);
                    if (lineitem != null) {
                        cleanedLineitems.add(lineitem);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换LineItem记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的LineItem记录被过滤: {}", record);
            }
        }
        
        log.info("LineItem表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedLineitems;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("LineItem记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 验证主键字段不能为空
        String orderkey = record.get(0); // l_orderkey
        String linenumber = record.get(3); // l_linenumber
        if (orderkey == null || orderkey.trim().isEmpty() || 
            linenumber == null || linenumber.trim().isEmpty()) {
            log.debug("LineItem主键字段为空: orderkey={}, linenumber={}", orderkey, linenumber);
            return false;
        }
        
        // 特殊清洗：l_quantity（数量不为空检查）
        String quantity = record.get(4); // l_quantity
        if (quantity == null || quantity.trim().isEmpty()) {
            log.debug("LineItem数量字段l_quantity为空，记录无效");
            return false;
        }
        
        // 特殊清洗：l_quantity数值范围检查（>=0）
        try {
            BigDecimal quantityValue = new BigDecimal(quantity.trim());
            if (quantityValue.compareTo(BigDecimal.ZERO) < 0) {
                log.debug("LineItem数量字段l_quantity小于0: {}", quantityValue);
                return false;
            }
        } catch (NumberFormatException e) {
            log.debug("LineItem数量字段l_quantity格式无效: {}", quantity);
            return false;
        }
        
        // 特殊清洗：l_extendedprice数值类型检查
        String extendedprice = record.get(5); // l_extendedprice
        if (extendedprice != null && !extendedprice.trim().isEmpty()) {
            try {
                BigDecimal priceValue = new BigDecimal(extendedprice.trim());
                if (priceValue.compareTo(BigDecimal.ZERO) < 0) {
                    log.debug("LineItem扩展价格字段l_extendedprice小于0: {}", priceValue);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.debug("LineItem扩展价格字段l_extendedprice格式无效: {}", extendedprice);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Lineitem convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Lineitem lineitem = new Lineitem();
        
        try {
            // l_orderkey (INTEGER)
            String orderkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_orderkey", record.get(0));
            lineitem.setL_ORDERKEY(Integer.valueOf(orderkeyStr));
            
            // l_partkey (INTEGER)
            String partkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_partkey", record.get(1));
            lineitem.setL_PARTKEY(Integer.valueOf(partkeyStr));
            
            // l_suppkey (INTEGER)
            String suppkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_suppkey", record.get(2));
            lineitem.setL_SUPPKEY(Integer.valueOf(suppkeyStr));
            
            // l_linenumber (INTEGER)
            String linenumberStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_linenumber", record.get(3));
            lineitem.setL_LINENUMBER(Integer.valueOf(linenumberStr));
            
            // l_quantity (DECIMAL) - 特殊清洗：确保>=0
            String quantityStr = record.get(4).trim();
            BigDecimal quantity = new BigDecimal(quantityStr);
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                quantity = BigDecimal.ZERO;
                log.warn("LineItem数量字段修正为0: 原值={}", quantityStr);
            }
            lineitem.setL_QUANTITY(quantity);
            
            // l_extendedprice (DECIMAL) - 特殊清洗：确保>=0
            String extendedpriceStr = record.get(5).trim();
            BigDecimal extendedprice = new BigDecimal(extendedpriceStr);
            if (extendedprice.compareTo(BigDecimal.ZERO) < 0) {
                extendedprice = BigDecimal.ZERO;
                log.warn("LineItem扩展价格字段修正为0: 原值={}", extendedpriceStr);
            }
            lineitem.setL_EXTENDEDPRICE(extendedprice);
            
            // l_discount (DECIMAL)
            String discountStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_discount", record.get(6));
            lineitem.setL_DISCOUNT(new BigDecimal(discountStr));
            
            // l_tax (DECIMAL)
            String taxStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_tax", record.get(7));
            lineitem.setL_TAX(new BigDecimal(taxStr));
            
            // l_returnflag (CHAR)
            String returnflag = TPCTableMetadata.cleanFieldValue(getTableName(), "l_returnflag", record.get(8));
            lineitem.setL_RETURNFLAG(returnflag);
            
            // l_linestatus (CHAR)
            String linestatus = TPCTableMetadata.cleanFieldValue(getTableName(), "l_linestatus", record.get(9));
            lineitem.setL_LINESTATUS(linestatus);
            
            // l_shipdate (DATE)
            String shipdateStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_shipdate", record.get(10));
            lineitem.setL_SHIPDATE(Date.valueOf(shipdateStr));
            
            // l_commitdate (DATE)
            String commitdateStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_commitdate", record.get(11));
            lineitem.setL_COMMITDATE(Date.valueOf(commitdateStr));
            
            // l_receiptdate (DATE)
            String receiptdateStr = TPCTableMetadata.cleanFieldValue(getTableName(), "l_receiptdate", record.get(12));
            lineitem.setL_RECEIPTDATE(Date.valueOf(receiptdateStr));
            
            // l_shipinstruct (CHAR)
            String shipinstruct = TPCTableMetadata.cleanFieldValue(getTableName(), "l_shipinstruct", record.get(13));
            lineitem.setL_SHIPINSTRUCT(shipinstruct);
            
            // l_shipmode (CHAR)
            String shipmode = TPCTableMetadata.cleanFieldValue(getTableName(), "l_shipmode", record.get(14));
            lineitem.setL_SHIPMODE(shipmode);
            
            // l_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "l_comment", record.get(15));
            lineitem.setL_COMMENT(comment);
            
            log.debug("成功转换LineItem记录: orderkey={}, linenumber={}, quantity={}", 
                lineitem.getL_ORDERKEY(), lineitem.getL_LINENUMBER(), lineitem.getL_QUANTITY());
            return lineitem;
            
        } catch (Exception e) {
            log.error("转换LineItem实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    public int batchInsert(List<Lineitem> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入LineItem数据，记录数: {}", entities.size());

            List<BatchResult> list = lineitemMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("LineItem批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("LineItem批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    private int insertOneByOne(List<Lineitem> entities) {
        log.info("尝试逐条插入LineItem数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Lineitem lineitem : entities) {
            try {
                int result = lineitemMapper.insert(lineitem);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条LineItem记录失败: orderkey={}, linenumber={}, 错误: {}", 
                    lineitem.getL_ORDERKEY(), lineitem.getL_LINENUMBER(), e.getMessage());
            }
        }
        
        log.info("LineItem逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空LineItem表数据");
            lineitemMapper.delete(new QueryWrapper<>());
            log.info("LineItem表清空完成");
        } catch (Exception e) {
            log.error("清空LineItem表失败", e);
            throw new RuntimeException("清空LineItem表失败", e);
        }
    }
} 