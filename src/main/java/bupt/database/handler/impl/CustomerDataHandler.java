package bupt.database.handler.impl;

import bupt.database.entity.Customer;
import bupt.database.handler.AbstractTableDataHandler;
import bupt.database.mapper.CustomerMapper;
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
 * Customer表数据处理Handler
 */
@Slf4j
@Component
public class CustomerDataHandler extends AbstractTableDataHandler<Customer> {
    
    @Autowired
    private CustomerMapper customerMapper;
    
    @Override
    public String getTableName() {
        return "customer";
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
    public List<Customer> cleanData(List<List<String>> records) {
        log.info("开始清洗Customer表数据，原始记录数: {}", records.size());
        
        List<Customer> cleanedCustomers = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Customer customer = convertToEntity(record);
                    if (customer != null) {
                        cleanedCustomers.add(customer);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换Customer记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的Customer记录被过滤: {}", record);
            }
        }
        
        log.info("Customer表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedCustomers;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("Customer记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 验证主键字段（c_custkey）不能为空
        String custkey = record.get(0); // c_custkey是第一个字段
        if (custkey == null || custkey.trim().isEmpty()) {
            log.debug("Customer主键c_custkey为空");
            return false;
        }
        
        // 验证主键是否为有效整数
        if (!TPCTableMetadata.isValidFieldValue(getTableName(), "c_custkey", custkey)) {
            log.debug("Customer主键c_custkey格式无效: {}", custkey);
            return false;
        }
        
        return true;
    }
    
    @Override
    public Customer convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Customer customer = new Customer();
        List<String> fieldNames = getFieldNames();
        
        try {
            // c_custkey (INTEGER)
            String custkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "c_custkey", record.get(0));
            customer.setCCustkey(Integer.valueOf(custkeyStr));
            
            // c_name (VARCHAR)
            String name = TPCTableMetadata.cleanFieldValue(getTableName(), "c_name", record.get(1));
            customer.setCName(name);
            
            // c_address (VARCHAR)
            String address = TPCTableMetadata.cleanFieldValue(getTableName(), "c_address", record.get(2));
            customer.setCAddress(address);
            
            // c_nationkey (INTEGER)
            String nationkeyStr = TPCTableMetadata.cleanFieldValue(getTableName(), "c_nationkey", record.get(3));
            customer.setCNationkey(Integer.valueOf(nationkeyStr));
            
            // c_phone (CHAR)
            String phone = TPCTableMetadata.cleanFieldValue(getTableName(), "c_phone", record.get(4));
            customer.setCPhone(phone);
            
            // c_acctbal (DECIMAL)
            String acctbalStr = TPCTableMetadata.cleanFieldValue(getTableName(), "c_acctbal", record.get(5));
            customer.setCAcctbal(new BigDecimal(acctbalStr));
            
            // c_mktsegment (CHAR)
            String mktsegment = TPCTableMetadata.cleanFieldValue(getTableName(), "c_mktsegment", record.get(6));
            customer.setCMktsegment(mktsegment);
            
            // c_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "c_comment", record.get(7));
            customer.setCComment(comment);
            
            log.debug("成功转换Customer记录: custkey={}, name={}", customer.getCCustkey(), customer.getCName());
            return customer;
            
        } catch (Exception e) {
            log.error("转换Customer实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    protected Long getMaxPrimaryKey() {
        try {
            // 查询Customer表中最大的custkey值
            Customer maxCustomer = customerMapper.selectOne(
                new QueryWrapper<Customer>()
                    .select("MAX(c_custkey) as c_custkey")
                    .last("LIMIT 1")
            );
            
            if (maxCustomer != null && maxCustomer.getCCustkey() != null) {
                Long maxKey = maxCustomer.getCCustkey().longValue();
                log.debug("Customer表最大主键值: {}", maxKey);
                return maxKey;
            } else {
                log.debug("Customer表为空，返回主键值: 0");
                return 0L;
            }
        } catch (Exception e) {
            log.warn("获取Customer表最大主键失败，返回默认值0", e);
            return 0L;
        }
    }
    
    @Override
    protected void setEntityPrimaryKey(Customer entity, Long primaryKeyValue) {
        if (entity != null) {
            entity.setCCustkey(primaryKeyValue.intValue());
            log.debug("设置Customer主键: custkey={}", primaryKeyValue);
        }
    }
    
    @Override
    protected int doActualBatchInsert(List<Customer> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入Customer数据，记录数: {}", entities.size());
            
            List<BatchResult> list = customerMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("Customer批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("Customer批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    /**
     * 逐条插入（容错处理）
     */
    private int insertOneByOne(List<Customer> entities) {
        log.info("尝试逐条插入Customer数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Customer customer : entities) {
            try {
                int result = customerMapper.insert(customer);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条Customer记录失败: custkey={}, 错误: {}", 
                    customer.getCCustkey(), e.getMessage());
            }
        }
        
        log.info("Customer逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空Customer表数据");
            customerMapper.delete(new QueryWrapper<>());
            log.info("Customer表清空完成");
        } catch (Exception e) {
            log.error("清空Customer表失败", e);
            throw new RuntimeException("清空Customer表失败", e);
        }
    }
} 