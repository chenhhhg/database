package bupt.database.handler.impl;

import bupt.database.entity.Orders;
import bupt.database.handler.AbstractTableDataHandler;
import bupt.database.mapper.OrdersMapper;
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
 * Orders表数据处理Handler
 * 重点清洗字段：o_totalprice（数值型，>=0）、o_orderdate（不为空，日期检查）
 */
@Slf4j
@Component
public class OrdersDataHandler extends AbstractTableDataHandler<Orders> {
    
    @Autowired
    private OrdersMapper ordersMapper;
    
    @Override
    public String getTableName() {
        return "orders";
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
    public List<Orders> cleanData(List<List<String>> records) {
        log.info("开始清洗Orders表数据，原始记录数: {}", records.size());
        
        List<Orders> cleanedOrders = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Orders order = convertToEntity(record);
                    if (order != null) {
                        cleanedOrders.add(order);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换Orders记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.info("无效的Orders记录被过滤: {}", record);
            }
        }
        
        log.info("Orders表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedOrders;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.info("Orders记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 验证主键字段（o_orderkey）不能为空
        String orderkey = record.get(0); // o_orderkey是第一个字段
        if (orderkey == null || orderkey.trim().isEmpty()) {
            log.info("Orders主键o_orderkey为空");
            return false;
        }
        
        // 验证主键是否为有效整数
        if (!TPCTableMetadata.isValidFieldValue(getTableName(), "o_orderkey", orderkey)) {
            log.info("Orders主键o_orderkey格式无效: {}", orderkey);
            return false;
        }
        
        // 特殊清洗：o_totalprice（数值类型检查，>=0）
        String totalprice = record.get(3); // o_totalprice
        if (totalprice != null && !totalprice.trim().isEmpty()) {
            try {
                BigDecimal priceValue = new BigDecimal(totalprice.trim());
                if (priceValue.compareTo(BigDecimal.ZERO) < 0) {
                    log.info("Orders总价字段o_totalprice小于0: {}", priceValue);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.info("Orders总价字段o_totalprice格式无效: {}", totalprice);
                return false;
            }
        }
        
        // 特殊清洗：o_orderdate（不为空检查）
        String orderdate = record.get(4); // o_orderdate
        if (orderdate == null || orderdate.trim().isEmpty()) {
            log.info("Orders订单日期字段o_orderdate为空，记录无效");
            return false;
        }
        
        return true;
    }
    
    @Override
    public Orders convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Orders order = new Orders();
        
        try {
            // o_orderkey (INTEGER)
            String orderkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "o_orderkey", record.get(0));
            order.setOOrderkey(Integer.valueOf(orderkeyStr));
            
            // o_custkey (INTEGER)
            String custkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "o_custkey", record.get(1));
            order.setOCustkey(Integer.valueOf(custkeyStr));
            
            // o_orderstatus (CHAR)
            String orderstatus = TPCTableMetadata.cleanFieldValue(getTableName(), "o_orderstatus", record.get(2));
            order.setOOrderstatus(orderstatus);
            
            // o_totalprice (DECIMAL) - 特殊清洗：确保>=0
            String totalpriceStr = record.get(3).trim();
            BigDecimal totalprice = new BigDecimal(totalpriceStr);
            if (totalprice.compareTo(BigDecimal.ZERO) < 0) {
                totalprice = BigDecimal.ZERO;
                log.warn("Orders总价字段修正为0: 原值={}", totalpriceStr);
            }
            order.setOTotalprice(totalprice);
            
            // o_orderdate (DATE) - 特殊清洗：不为空检查
            String orderdateStr = record.get(4).trim();
            if (orderdateStr.isEmpty()) {
                orderdateStr = "1970-01-01";
                log.warn("Orders订单日期字段为空，修正为默认日期: 1970-01-01");
            }
            order.setOOrderdate(Date.valueOf(orderdateStr));
            
            // o_orderpriority (CHAR)
            String orderpriority = TPCTableMetadata.cleanFieldValue(getTableName(), "o_orderpriority", record.get(5));
            order.setOOrderpriority(orderpriority);
            
            // o_clerk (CHAR)
            String clerk = TPCTableMetadata.cleanFieldValue(getTableName(), "o_clerk", record.get(6));
            order.setOClerk(clerk);
            
            // o_shippriority (INTEGER)
            String shippriorityStr = TPCTableMetadata.cleanFieldValue(getTableName(), "o_shippriority", record.get(7));
            order.setOShippriority(Integer.valueOf(shippriorityStr));
            
            // o_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "o_comment", record.get(8));
            order.setOComment(comment);
            
            log.info("成功转换Orders记录: orderkey={}, custkey={}", order.getOOrderkey(), order.getOCustkey());
            return order;
            
        } catch (Exception e) {
            log.error("转换Orders实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    protected Long getMaxPrimaryKey() {
        try {
            // 查询Orders表中最大的orderkey值
            Orders maxOrder = ordersMapper.selectOne(
                new QueryWrapper<Orders>()
                    .select("MAX(o_orderkey) as o_orderkey")
                    .last("LIMIT 1")
            );
            
            if (maxOrder != null && maxOrder.getOOrderkey() != null) {
                Long maxKey = maxOrder.getOOrderkey().longValue();
                log.info("Orders表最大主键值: {}", maxKey);
                return maxKey;
            } else {
                log.info("Orders表为空，返回主键值: 0");
                return 0L;
            }
        } catch (Exception e) {
            log.warn("获取Orders表最大主键失败，返回默认值0", e);
            return 0L;
        }
    }
    
    @Override
    protected void setEntityPrimaryKey(Orders entity, Long primaryKeyValue) {
        if (entity != null) {
            entity.setOOrderkey(primaryKeyValue.intValue());
            log.info("设置Orders主键: orderkey={}", primaryKeyValue);
        }
    }
    
    @Override
    protected int doActualBatchInsert(List<Orders> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入Orders数据，记录数: {}", entities.size());
            
            List<BatchResult> list = ordersMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("Orders批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("Orders批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    /**
     * 逐条插入（容错处理）
     */
    private int insertOneByOne(List<Orders> entities) {
        log.info("尝试逐条插入Orders数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Orders order : entities) {
            try {
                int result = ordersMapper.insert(order);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.info("插入单条Orders记录失败: orderkey={}, 错误: {}", 
                    order.getOOrderkey(), e.getMessage());
            }
        }
        
        log.info("Orders逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空Orders表数据");
            ordersMapper.delete(new QueryWrapper<>());
            log.info("Orders表清空完成");
        } catch (Exception e) {
            log.error("清空Orders表失败", e);
            throw new RuntimeException("清空Orders表失败", e);
        }
    }
} 