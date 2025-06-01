# TPC-H 数据导出模块使用说明（分批导出版）

## 概述
数据导出模块提供了将TPC-H数据库中的表数据导出为txt文件的功能。**全面支持分批导出和内存安全模式**，有效避免大表导出时的内存溢出问题。

## 基本信息
- **导出基础路径**: `/root/mysql/tpc/TPC-H V3.0.1/dbgen/export`
- **支持格式**: txt文件
- **默认分隔符**: 制表符(\t)
- **支持的表**: CUSTOMER, ORDERS, LINEITEM, NATION, PARTSUPP, PART, REGION, SUPPLIER

## ⚠️ 内存优化特性

### 分批导出机制
- **默认启用**: 所有导出操作默认使用分批导出
- **批次大小**: 默认10000条记录/批次
- **内存安全模式**: 大型表自动使用更小的批次（5000条）
- **进度监控**: 实时显示导出进度和性能统计

### 大型表识别
以下表被识别为大型表，自动启用内存安全模式：
- **LINEITEM** - 通常是最大的表
- **ORDERS** - 订单表
- **CUSTOMER** - 客户表  
- **PARTSUPP** - 零件供应商表

## API接口

### 1. 分批导出数据（推荐）
**请求方式**: POST  
**接口地址**: `/api/export/data`  
**请求体**:
```json
{
  "folderName": "export_20241201",
  "tableNames": ["CUSTOMER", "ORDERS"],
  "includeHeader": true,
  "delimiter": "\t",
  "enableBatchExport": true,
  "batchSize": 10000,
  "memorySafeMode": true
}
```

**新增参数说明**:
- `enableBatchExport`: 是否启用分批导出（默认true，强烈推荐）
- `batchSize`: 批次大小，每批处理的记录数（默认10000，范围100-100000）
- `memorySafeMode`: 内存安全模式（默认true，大型表自动使用更小批次）

**响应示例**:
```json
{
  "success": true,
  "exportPath": "/root/mysql/tpc/TPC-H V3.0.1/dbgen/export/export_20241201_20241201_143022",
  "tableRowCounts": {
    "CUSTOMER": 150000,
    "ORDERS": 1500000
  },
  "exportedFiles": ["customer.txt", "orders.txt"],
  "duration": 5000,
  "batchStats": {
    "CUSTOMER": {
      "batchCount": 19,
      "avgBatchTime": 245,
      "maxBatchTime": 380,
      "memorySafeModeUsed": false,
      "actualBatchSize": 8000
    },
    "ORDERS": {
      "batchCount": 300,
      "avgBatchTime": 180,
      "maxBatchTime": 420,
      "memorySafeModeUsed": true,
      "actualBatchSize": 5000
    }
  }
}
```

### 2. 内存安全导出（超大表专用）
**请求方式**: POST  
**接口地址**: `/api/export/data/memory-safe`  
**说明**: 使用最保守的内存设置，适用于超大表或内存受限环境

**请求体**:
```json
{
  "folderName": "lineitem_safe_export",
  "tableNames": ["LINEITEM"],
  "includeHeader": true,
  "delimiter": "|"
}
```

### 3. 导出性能估算
**请求方式**: GET  
**接口地址**: `/api/export/performance/estimate/{tableName}`  

**响应示例**:
```json
{
  "tableName": "LINEITEM",
  "recommendedBatchSize": 3000,
  "estimatedTime": "5-15分钟",
  "memorySafeModeRecommended": true
}
```

### 4. 获取可导出表列表
**请求方式**: GET  
**接口地址**: `/api/export/tables`  

### 5. 导出单个表（增强版）
**请求方式**: POST  
**接口地址**: `/api/export/table/{tableName}`  
**请求参数**:
- `tableName`: 路径参数，表名
- `folderName`: 查询参数，文件夹名
- `includeHeader`: 查询参数，是否包含表头（默认true）
- `delimiter`: 查询参数，字段分隔符（默认制表符）
- `enableBatchExport`: 查询参数，是否启用分批导出（默认true）
- `batchSize`: 查询参数，批次大小（默认10000）
- `memorySafeMode`: 查询参数，内存安全模式（默认true）

## 使用示例

### 1. 标准分批导出
```bash
curl -X POST "http://localhost:8001/api/export/data" \
  -H "Content-Type: application/json" \
  -d '{
    "folderName": "standard_export",
    "tableNames": ["CUSTOMER", "ORDERS"],
    "enableBatchExport": true,
    "batchSize": 10000,
    "memorySafeMode": true
  }'
```

### 2. 超大表安全导出
```bash
curl -X POST "http://localhost:8001/api/export/data/memory-safe" \
  -H "Content-Type: application/json" \
  -d '{
    "folderName": "lineitem_export",
    "tableNames": ["LINEITEM"],
    "includeHeader": true,
    "delimiter": "|"
  }'
```

### 3. 自定义批次大小导出
```bash
curl -X POST "http://localhost:8001/api/export/data" \
  -H "Content-Type: application/json" \
  -d '{
    "folderName": "custom_batch_export",
    "tableNames": ["PARTSUPP"],
    "batchSize": 5000,
    "memorySafeMode": true
  }'
```

### 4. 小表快速导出（一次性）
```bash
curl -X POST "http://localhost:8001/api/export/data" \
  -H "Content-Type: application/json" \
  -d '{
    "folderName": "small_table_export",
    "tableNames": ["NATION", "REGION"],
    "enableBatchExport": false
  }'
```

### 5. 单表导出带性能估算
```bash
# 先估算性能
curl -X GET "http://localhost:8001/api/export/performance/estimate/LINEITEM"

# 根据推荐参数导出
curl -X POST "http://localhost:8001/api/export/table/LINEITEM?folderName=lineitem_export&batchSize=3000&memorySafeMode=true"
```

## 性能调优建议

### 1. 批次大小选择
| 表大小 | 推荐批次大小 | 说明 |
|--------|-------------|------|
| < 10万行 | 10000 | 标准批次 |
| 10万-100万行 | 8000 | 中等批次 |
| 100万-1000万行 | 5000 | 小批次 |
| > 1000万行 | 3000 | 超小批次 |

### 2. 内存安全模式使用场景
- ✅ **推荐使用**: LINEITEM、ORDERS、大型CUSTOMER表
- ✅ **内存受限环境**: 服务器内存 < 4GB
- ❌ **可选关闭**: NATION、REGION等小表

### 3. 性能监控指标
- **平均批次时间**: 理想情况下 < 500ms
- **最大批次时间**: 应当 < 2秒
- **批次数量**: 根据表大小自动计算
- **总体速度**: 目标 > 1000行/秒

## 日志监控

导出过程中会输出详细的日志信息：

```
2024-12-01 14:30:22 INFO  开始数据导出
2024-12-01 14:30:22 INFO  导出模式: 分批导出
2024-12-01 14:30:22 INFO  批次大小: 10000
2024-12-01 14:30:22 INFO  内存安全模式: true
2024-12-01 14:30:22 INFO  要导出的表: [CUSTOMER, ORDERS]

2024-12-01 14:30:23 INFO  开始导出表: CUSTOMER
2024-12-01 14:30:23 INFO  开始分批导出表 CUSTOMER，批次大小: 8000, 主键列: C_CUSTKEY
2024-12-01 14:30:23 INFO  批次 1 完成: 8000 行, 耗时 245ms, 累计 8000 行
2024-12-01 14:30:24 INFO  批次 2 完成: 8000 行, 耗时 230ms, 累计 16000 行
...
2024-12-01 14:30:27 INFO  表 CUSTOMER 分批导出完成，总共 19 个批次，150000 行记录
2024-12-01 14:30:27 INFO  表 CUSTOMER 导出完成: 150000 行, 耗时 4200ms, 19 个批次

2024-12-01 14:30:27 INFO  数据导出完成
2024-12-01 14:30:27 INFO  总耗时: 25000ms, 总行数: 1650000, 平均速度: 66000 行/秒
```

## 错误处理和故障排除

### 常见问题及解决方案

1. **内存溢出 (OutOfMemoryError)**
   - 解决方案：启用内存安全模式，减小批次大小
   ```json
   {
     "memorySafeMode": true,
     "batchSize": 3000
   }
   ```

2. **批次大小超出范围**
   - 错误信息：批次大小不能小于100 / 不能超过100000
   - 解决方案：设置合理的批次大小

3. **磁盘空间不足**
   - 监控磁盘使用率，确保导出路径有足够空间
   - 可以分批导出不同的表

4. **数据库连接超时**
   - 对于超大表，导出时间可能很长
   - 确保数据库连接池配置合理

### 最佳实践

1. **生产环境导出**:
   ```json
   {
     "enableBatchExport": true,
     "memorySafeMode": true,
     "batchSize": 5000
   }
   ```

2. **测试环境快速导出**:
   ```json
   {
     "enableBatchExport": true,
     "memorySafeMode": false,  
     "batchSize": 15000
   }
   ```

3. **内存受限环境**:
   ```json
   {
     "enableBatchExport": true,
     "memorySafeMode": true,
     "batchSize": 2000
   }
   ```

## 注意事项

1. **分批导出默认启用**，如需关闭请明确设置 `enableBatchExport: false`
2. **内存安全模式**对大型表自动生效，无需手动配置
3. **导出文件夹自动添加时间戳**，避免文件覆盖
4. **支持断点续传**（未来版本支持）
5. **建议在低峰期进行大表导出**，减少对生产环境的影响

通过以上优化，数据导出模块现在可以安全地处理任意大小的表，同时提供详细的性能监控和错误处理机制。 