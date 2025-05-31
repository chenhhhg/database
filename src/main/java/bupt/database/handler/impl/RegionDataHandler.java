package bupt.database.handler.impl;

import bupt.database.entity.Region;
import bupt.database.handler.AbstractTableDataHandler;
import bupt.database.mapper.RegionMapper;
import bupt.database.util.TPCTableMetadata;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Region表数据处理Handler
 * 重点清洗字段：r_regionkey（不为空，>=0）、r_name（不为空，长度检查）
 */
@Slf4j
@Component
public class RegionDataHandler extends AbstractTableDataHandler<Region> {
    
    @Autowired
    private RegionMapper regionMapper;
    
    @Override
    public String getTableName() {
        return "region";
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
    public List<Region> cleanData(List<List<String>> records) {
        log.info("开始清洗Region表数据，原始记录数: {}", records.size());
        
        List<Region> cleanedRegions = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        
        for (List<String> record : records) {
            if (validateRecord(record)) {
                try {
                    Region region = convertToEntity(record);
                    if (region != null) {
                        cleanedRegions.add(region);
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    log.warn("转换Region记录失败: {}", record, e);
                    invalidCount++;
                }
            } else {
                invalidCount++;
                log.debug("无效的Region记录被过滤: {}", record);
            }
        }
        
        log.info("Region表数据清洗完成，有效记录: {}, 无效记录: {}", validCount, invalidCount);
        return cleanedRegions;
    }
    
    @Override
    public boolean validateRecord(List<String> record) {
        List<String> fieldNames = getFieldNames();
        
        if (record == null || record.size() != fieldNames.size()) {
            log.debug("Region记录字段数量不匹配，期望: {}, 实际: {}", 
                fieldNames.size(), record != null ? record.size() : 0);
            return false;
        }
        
        // 特殊清洗：r_regionkey（主键不为空检查，>=0）
        String regionkey = record.get(0); // r_regionkey
        if (regionkey == null || regionkey.trim().isEmpty()) {
            log.debug("Region主键字段r_regionkey为空");
            return false;
        }
        
        try {
            int regionkeyValue = Integer.parseInt(regionkey.trim());
            if (regionkeyValue < 0) {
                log.debug("Region主键字段r_regionkey小于0: {}", regionkeyValue);
                return false;
            }
        } catch (NumberFormatException e) {
            log.debug("Region主键字段r_regionkey格式无效: {}", regionkey);
            return false;
        }
        
        // 特殊清洗：r_name（不为空检查，长度检查）
        String name = record.get(1); // r_name
        if (name == null || name.trim().isEmpty()) {
            log.debug("Region区域名称字段r_name为空");
            return false;
        }
        
        // 检查名称长度
        Integer maxLength = TPCTableMetadata.getFieldLength(getTableName(), "r_name");
        if (maxLength != null && name.trim().length() > maxLength) {
            log.debug("Region区域名称字段r_name长度超限: 当前={}, 最大={}", name.trim().length(), maxLength);
            return false;
        }
        
        return true;
    }
    
    @Override
    public Region convertToEntity(List<String> record) {
        if (!validateRecord(record)) {
            return null;
        }
        
        Region region = new Region();
        
        try {
            // r_regionkey (INTEGER) - 特殊清洗：确保>=0
            String regionkeyStr = record.get(0).trim();
            int regionkey = Integer.parseInt(regionkeyStr);
            if (regionkey < 0) {
                regionkey = 0;
                log.warn("Region主键字段修正为0: 原值={}", regionkeyStr);
            }
            region.setRRegionkey(regionkey);
            
            // r_name (CHAR) - 特殊清洗：不为空，长度检查
            String name = record.get(1).trim();
            if (name.isEmpty()) {
                name = "UNKNOWN";
                log.warn("Region区域名称字段修正为UNKNOWN: 原值为空");
            }
            
            // 长度截断处理
            Integer maxLength = TPCTableMetadata.getFieldLength(getTableName(), "r_name");
            if (maxLength != null && name.length() > maxLength) {
                name = name.substring(0, maxLength);
                log.warn("Region区域名称字段截断: 新长度={}", maxLength);
            }
            region.setRName(name);
            
            // r_comment (VARCHAR)
            String comment = TPCTableMetadata.cleanFieldValue(getTableName(), "r_comment", record.get(2));
            region.setRComment(comment);
            
            log.debug("成功转换Region记录: regionkey={}, name={}", region.getRRegionkey(), region.getRName());
            return region;
            
        } catch (Exception e) {
            log.error("转换Region实体失败，记录: {}", record, e);
            return null;
        }
    }
    
    @Override
    protected Long getMaxPrimaryKey() {
        try {
            // 查询Region表中最大的regionkey值
            Region maxRegion = regionMapper.selectOne(
                new QueryWrapper<Region>()
                    .select("MAX(r_regionkey) as r_regionkey")
                    .last("LIMIT 1")
            );
            
            if (maxRegion != null && maxRegion.getRRegionkey() != null) {
                Long maxKey = maxRegion.getRRegionkey().longValue();
                log.debug("Region表最大主键值: {}", maxKey);
                return maxKey;
            } else {
                log.debug("Region表为空，返回主键值: 0");
                return 0L;
            }
        } catch (Exception e) {
            log.warn("获取Region表最大主键失败，返回默认值0", e);
            return 0L;
        }
    }
    
    @Override
    protected void setEntityPrimaryKey(Region entity, Long primaryKeyValue) {
        if (entity != null) {
            entity.setRRegionkey(primaryKeyValue.intValue());
            log.debug("设置Region主键: regionkey={}", primaryKeyValue);
        }
    }
    
    @Override
    protected int doActualBatchInsert(List<Region> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        
        try {
            log.info("开始批量插入Region数据，记录数: {}", entities.size());
            
            List<BatchResult> list = regionMapper.insert(entities);
            int insertedCount = list.size();
            
            log.info("Region批量插入完成，成功插入: {} 条记录", insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("Region批量插入失败", e);
            return insertOneByOne(entities);
        }
    }
    
    private int insertOneByOne(List<Region> entities) {
        log.info("尝试逐条插入Region数据，记录数: {}", entities.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Region region : entities) {
            try {
                int result = regionMapper.insert(region);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.debug("插入单条Region记录失败: regionkey={}, 错误: {}", 
                    region.getRRegionkey(), e.getMessage());
            }
        }
        
        log.info("Region逐条插入完成，成功: {}, 失败: {}", successCount, failCount);
        return successCount;
    }
    
    @Override
    public void truncateTable() {
        try {
            log.info("清空Region表数据");
            regionMapper.delete(new QueryWrapper<>());
            log.info("Region表清空完成");
        } catch (Exception e) {
            log.error("清空Region表失败", e);
            throw new RuntimeException("清空Region表失败", e);
        }
    }
} 