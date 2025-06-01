# TPC-H数据导出分批配置指南

## 环境配置建议

### 1. 生产环境（推荐配置）
适用于生产环境，稳定性优先，避免对业务产生影响。

```json
{
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 5000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

**特点**:
- 保守的批次大小，确保内存安全
- 启用内存安全模式，大型表自动使用更小批次
- 适合服务器内存 2-8GB 的环境

### 2. 开发/测试环境（平衡配置）
适用于开发测试环境，在性能和稳定性之间平衡。

```json
{
  "enableBatchExport": true,
  "memorySafeMode": false,
  "batchSize": 15000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

**特点**:
- 较大的批次大小，提升导出速度
- 关闭内存安全模式，提高性能
- 适合服务器内存 8GB+ 的环境

### 3. 高性能环境（激进配置）
适用于高性能服务器，追求最大导出速度。

```json
{
  "enableBatchExport": true,
  "memorySafeMode": false,
  "batchSize": 50000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

**特点**:
- 大批次处理，最大化吞吐量
- 需要充足的内存（16GB+）
- 仅适合处理中小型表（< 500万行）

### 4. 内存受限环境（保守配置）
适用于内存严重受限的环境，如容器化部署。

```json
{
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 2000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

**特点**:
- 极小的批次大小，最小内存占用
- 强制启用内存安全模式
- 适合内存 < 2GB 的环境

## 表级别配置建议

### LINEITEM表（超大表）
通常是TPC-H中最大的表，需要特别处理。

```json
{
  "tableNames": ["LINEITEM"],
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 3000,
  "includeHeader": true,
  "delimiter": "|"
}
```

**说明**:
- 使用管道符分隔符，兼容TPC-H标准
- 超小批次，避免内存溢出
- 预计导出时间：5-30分钟（取决于数据量）

### ORDERS表（大表）
订单表，通常包含数百万到千万条记录。

```json
{
  "tableNames": ["ORDERS"],
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 5000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

### CUSTOMER表（中表）
客户表，通常包含数十万到数百万条记录。

```json
{
  "tableNames": ["CUSTOMER"],
  "enableBatchExport": true,
  "memorySafeMode": false,
  "batchSize": 8000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

### 小表组合（NATION, REGION, SUPPLIER等）
对于小表可以关闭分批导出，提高效率。

```json
{
  "tableNames": ["NATION", "REGION", "SUPPLIER"],
  "enableBatchExport": false,
  "includeHeader": true,
  "delimiter": "\t"
}
```

## 批次大小计算公式

### 基础计算
```
推荐批次大小 = min(
  基础批次大小,
  可用内存(MB) * 500,
  表预估行数 / 100
)
```

### 内存安全系数
```
if (内存安全模式 && 大型表) {
  批次大小 = 批次大小 * 0.5
}
```

### 示例计算
假设：
- 可用内存：4GB
- 表行数：1000万行
- 启用内存安全模式

```
基础批次大小 = 10000
内存限制 = 4000MB * 500 = 2,000,000（忽略，太大）
行数限制 = 10,000,000 / 100 = 100,000
最小值 = min(10000, 100000) = 10000

应用内存安全系数：
最终批次大小 = 10000 * 0.5 = 5000
```

## 性能监控阈值

### 正常性能指标
- **批次处理时间**: < 500ms
- **内存使用率**: < 70%
- **数据库CPU**: < 80%
- **导出速度**: > 1000行/秒

### 警告阈值
- **批次处理时间**: 500ms - 2s
- **内存使用率**: 70% - 85%
- **数据库CPU**: 80% - 90%
- **导出速度**: 500-1000行/秒

### 危险阈值
- **批次处理时间**: > 2s
- **内存使用率**: > 85%
- **数据库CPU**: > 90%
- **导出速度**: < 500行/秒

## 故障场景应对

### 场景1：内存溢出
**症状**: OutOfMemoryError异常
**解决方案**:
```json
{
  "memorySafeMode": true,
  "batchSize": 1000,
  "enableBatchExport": true
}
```

### 场景2：导出超时
**症状**: 数据库连接超时
**解决方案**:
```json
{
  "batchSize": 10000,
  "memorySafeMode": false,
  "enableBatchExport": true
}
```
同时调整数据库连接超时配置。

### 场景3：磁盘空间不足
**症状**: 磁盘写入失败
**解决方案**:
1. 清理磁盘空间
2. 分批导出不同表
3. 使用压缩格式（未来支持）

### 场景4：导出过慢
**症状**: 导出速度 < 100行/秒
**解决方案**:
```json
{
  "batchSize": 20000,
  "memorySafeMode": false,
  "enableBatchExport": true
}
```

## 配置模板

### 模板1：通用生产环境
```json
{
  "folderName": "production_export",
  "tableNames": ["CUSTOMER", "ORDERS", "LINEITEM"],
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 5000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

### 模板2：快速测试导出
```json
{
  "folderName": "quick_test",
  "tableNames": ["NATION", "REGION"],
  "enableBatchExport": false,
  "includeHeader": true,
  "delimiter": ","
}
```

### 模板3：完整数据备份
```json
{
  "folderName": "full_backup",
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 8000,
  "includeHeader": true,
  "delimiter": "\t"
}
```

### 模板4：超大表专用
```json
{
  "folderName": "lineitem_export",
  "tableNames": ["LINEITEM"],
  "enableBatchExport": true,
  "memorySafeMode": true,
  "batchSize": 2000,
  "includeHeader": true,
  "delimiter": "|"
}
```

## 最佳实践总结

1. **始终启用分批导出**，除非确认是小表（< 1万行）
2. **生产环境优先使用内存安全模式**
3. **根据服务器内存调整批次大小**
4. **监控导出过程，及时调整参数**
5. **大表导出建议在业务低峰期进行**
6. **保留导出日志，便于性能分析**

通过合理配置这些参数，可以在不同环境下安全、高效地完成TPC-H数据导出任务。 