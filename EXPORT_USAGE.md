# TPC-H 数据导出模块使用说明

## 概述
数据导出模块提供了将TPC-H数据库中的表数据导出为txt文件的功能。支持自定义文件夹名、选择特定表、设置分隔符等功能。

## 基本信息
- **导出基础路径**: `/root/mysql/tpc/TPC-H V3.0.1/dbgen/export`
- **支持格式**: txt文件
- **默认分隔符**: 制表符(\t)
- **支持的表**: CUSTOMER, ORDERS, LINEITEM, NATION, PARTSUPP, PART, REGION, SUPPLIER

## API接口

### 1. 导出数据
**请求方式**: POST  
**接口地址**: `/api/export/data`  
**请求体**:
```json
{
  "folderName": "export_20241201",
  "tableNames": ["CUSTOMER", "ORDERS"],
  "includeHeader": true,
  "delimiter": "\t"
}
```

**参数说明**:
- `folderName`: 必填，导出文件夹名称
- `tableNames`: 可选，要导出的表名列表（不填则导出所有表）
- `includeHeader`: 可选，是否包含表头（默认true）
- `delimiter`: 可选，字段分隔符（默认制表符）

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
  "duration": 5000
}
```

### 2. 获取可导出表列表
**请求方式**: GET  
**接口地址**: `/api/export/tables`  

**响应示例**:
```json
["CUSTOMER", "ORDERS", "LINEITEM", "NATION", "PARTSUPP", "PART", "REGION", "SUPPLIER"]
```

### 3. 导出单个表
**请求方式**: POST  
**接口地址**: `/api/export/table/{tableName}`  
**请求参数**:
- `tableName`: 路径参数，表名
- `folderName`: 查询参数，文件夹名
- `includeHeader`: 查询参数，是否包含表头（可选，默认true）
- `delimiter`: 查询参数，字段分隔符（可选，默认制表符）

**示例**:
```
POST /api/export/table/CUSTOMER?folderName=customer_export&includeHeader=true&delimiter=%09
```

## 使用示例

### 1. 导出所有表
```bash
curl -X POST "http://localhost:8080/api/export/data" \
  -H "Content-Type: application/json" \
  -d '{
    "folderName": "full_export"
  }'
```

### 2. 导出指定表
```bash
curl -X POST "http://localhost:8080/api/export/data" \
  -H "Content-Type: application/json" \
  -d '{
    "folderName": "customer_orders_export",
    "tableNames": ["CUSTOMER", "ORDERS"],
    "includeHeader": true,
    "delimiter": ","
  }'
```

### 3. 导出单个表
```bash
curl -X POST "http://localhost:8080/api/export/table/CUSTOMER?folderName=customer_only&includeHeader=true"
```

### 4. 获取表列表
```bash
curl -X GET "http://localhost:8080/api/export/tables"
```

## 文件输出格式

导出的txt文件格式如下：
- 第一行为表头（如果includeHeader为true）
- 每行一条记录
- 字段之间用指定分隔符分隔
- 文件名为表名小写加.txt后缀

**示例**（customer.txt）:
```
C_CUSTKEY	C_NAME	C_ADDRESS	C_NATIONKEY	C_PHONE	C_ACCTBAL	C_MKTSEGMENT	C_COMMENT
1	Customer#000000001	IVhzIApeRb ot,c,E	15	25-989-741-2988	711.56	BUILDING	to the even, regular platelets...
2	Customer#000000002	XSTf4,NCwDVaWNe6tEgvwfmRchLXak	13	23-768-687-3665	121.65	AUTOMOBILE	l accounts. even requests...
```

## 注意事项

1. 导出文件夹会自动添加时间戳后缀，格式为：`{folderName}_{yyyyMMdd_HHmmss}`
2. 如果指定的导出目录不存在，系统会自动创建
3. 确保MySQL容器有足够的磁盘空间存储导出文件
4. 大表导出可能需要较长时间，请耐心等待
5. 系统会记录每个表的导出行数和总耗时

## 错误处理

如果导出失败，响应会包含错误信息：
```json
{
  "success": false,
  "errorMessage": "文件夹名不能为空",
  "duration": 100
}
```

常见错误原因：
- 文件夹名为空或null
- 数据库连接失败
- 磁盘空间不足
- 表名不存在
- 权限不足 