# TPC-H复杂查询接口使用说明

## 概述
实现了两个标准的TPC-H查询，支持执行时间记录和SQL执行计划分析功能，为数据库性能分析提供详细信息。

## 功能特性
- ✅ **精确执行时间记录**：毫秒级别的查询执行时间统计
- ✅ **执行计划分析**：通过EXPLAIN获取SQL执行计划
- ✅ **标准TPC-H查询**：完全符合TPC-H标准的查询实现
- ✅ **灵活参数配置**：支持自定义查询参数
- ✅ **完整结果返回**：包含原始数据、执行信息和计划分析
- ✅ **多种调用方式**：支持GET和POST两种请求方式

## 接口列表

### 1. 定价汇总报表查询 (TPC-H Q1)
查询已经开票、发货和退货三个类别订单的业务总量。

#### GET方式调用
**接口地址**: `GET /api/tpch/pricing-summary`

**参数说明**:
- `shipDate`: 截止发货日期 (默认: 2021-12-01)
- `intervalDays`: 间隔天数 (默认: 90)
- `includeExplain`: 是否包含执行计划 (默认: false)

**示例**:
```bash
GET /api/tpch/pricing-summary?shipDate=2021-12-01&intervalDays=90&includeExplain=true
```

#### POST方式调用
**接口地址**: `POST /api/tpch/pricing-summary`

**请求体**:
```json
{
  "shipDate": "2021-12-01",
  "intervalDays": 90,
  "includeExplain": true
}
```

#### 响应示例
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "results": [
      {
        "lReturnflag": "A",
        "lLinestatus": "F",
        "sumQty": 37734107.00,
        "sumBasePrice": 56586554400.73,
        "sumDiscPrice": 53758257134.87,
        "sumCharge": 55909065222.83,
        "avgQty": 25.52,
        "avgPrice": 38273.13,
        "avgDisc": 0.05,
        "countOrder": 1478493
      }
    ],
    "executionTime": 2156,
    "resultCount": 4,
    "sql": "SELECT l_returnflag, l_linestatus, SUM(l_quantity) AS sum_qty...",
    "parameters": {
      "shipDate": "2021-12-01",
      "intervalDays": 90
    },
    "executionPlan": [
      {
        "id": 1,
        "select_type": "SIMPLE",
        "table": "lineitem",
        "partitions": null,
        "type": "ALL",
        "possible_keys": "i_l_shipdate",
        "key": "i_l_shipdate",
        "key_len": "4",
        "ref": null,
        "rows": 6001215,
        "filtered": 33.33,
        "Extra": "Using where; Using temporary; Using filesort"
      }
    ]
  }
}
```

### 2. 最低成本供应商查询 (TPC-H Q2)
在给定区域中，针对给定类型和大小的零件，找到能够以最低价格供应的供应商。

#### GET方式调用
**接口地址**: `GET /api/tpch/min-cost-supplier`

**参数说明**:
- `partSize`: 零件尺寸 (默认: 15)
- `partType`: 零件类型 (默认: BRASS)
- `regionName`: 地区名称 (默认: EUROPE)
- `limit`: 限制返回数量 (默认: 100)
- `includeExplain`: 是否包含执行计划 (默认: false)

**示例**:
```bash
GET /api/tpch/min-cost-supplier?partSize=15&partType=BRASS&regionName=EUROPE&limit=100&includeExplain=true
```

#### POST方式调用
**接口地址**: `POST /api/tpch/min-cost-supplier`

**请求体**:
```json
{
  "partSize": 15,
  "partType": "BRASS",
  "regionName": "EUROPE",
  "limit": 100,
  "includeExplain": true
}
```

#### 响应示例
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "results": [
      {
        "sAcctbal": 9938.53,
        "sName": "Supplier#000005359",
        "nName": "UNITED KINGDOM",
        "pPartkey": 185358,
        "pMfgr": "Manufacturer#4",
        "sAddress": "QKuHYh,vZGiwu2FWEJoLDx04",
        "sPhone": "33-429-790-6131",
        "sComment": "uriously final requests are blithely. slyly express deposits sleep slyly"
      }
    ],
    "executionTime": 1856,
    "resultCount": 100,
    "sql": "SELECT s_acctbal, s_name, n_name, p_partkey, p_mfgr...",
    "parameters": {
      "partSize": 15,
      "partType": "BRASS",
      "regionName": "EUROPE",
      "limit": 100
    },
    "executionPlan": [
      {
        "id": 1,
        "select_type": "PRIMARY",
        "table": "region",
        "partitions": null,
        "type": "ALL",
        "possible_keys": "PRIMARY",
        "key": null,
        "key_len": null,
        "ref": null,
        "rows": 5,
        "filtered": 20.00,
        "Extra": "Using where"
      }
    ]
  }
}
```

## 返回字段说明

### QueryExecutionInfo 通用响应结构

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `results` | List | 查询结果列表 |
| `executionTime` | Long | 查询执行时间（毫秒） |
| `resultCount` | Integer | 结果数量 |
| `sql` | String | 执行的SQL语句 |
| `parameters` | Map | 查询参数 |
| `executionPlan` | List<Map> | 执行计划（可选） |

### PricingSummaryResult 定价汇总结果字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `lReturnflag` | String | 退货标志 |
| `lLinestatus` | String | 订单状态 |
| `sumQty` | BigDecimal | 总数量 |
| `sumBasePrice` | BigDecimal | 总基础价格 |
| `sumDiscPrice` | BigDecimal | 总折扣价格 |
| `sumCharge` | BigDecimal | 总价格（含税） |
| `avgQty` | BigDecimal | 平均数量 |
| `avgPrice` | BigDecimal | 平均价格 |
| `avgDisc` | BigDecimal | 平均折扣 |
| `countOrder` | Long | 订单数量 |

### MinCostSupplierResult 最低成本供应商结果字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `sAcctbal` | BigDecimal | 供应商账户余额 |
| `sName` | String | 供应商名称 |
| `nName` | String | 国家名称 |
| `pPartkey` | Integer | 零件key |
| `pMfgr` | String | 制造商 |
| `sAddress` | String | 供应商地址 |
| `sPhone` | String | 供应商电话 |
| `sComment` | String | 供应商备注 |

## 使用示例

### 1. 查询定价汇总报表（包含执行计划）
```bash
curl -X GET "http://localhost:8080/api/tpch/pricing-summary?shipDate=2021-12-01&intervalDays=90&includeExplain=true"
```

### 2. 查询最低成本供应商（指定零件尺寸）
```bash
curl -X GET "http://localhost:8080/api/tpch/min-cost-supplier?partSize=20&partType=STEEL&regionName=ASIA&limit=50&includeExplain=true"
```

### 3. POST方式查询定价汇总
```bash
curl -X POST "http://localhost:8080/api/tpch/pricing-summary" \
  -H "Content-Type: application/json" \
  -d '{
    "shipDate": "2020-12-01",
    "intervalDays": 120,
    "includeExplain": true
  }'
```

### 4. POST方式查询最低成本供应商
```bash
curl -X POST "http://localhost:8080/api/tpch/min-cost-supplier" \
  -H "Content-Type: application/json" \
  -d '{
    "partSize": 25,
    "partType": "COPPER",
    "regionName": "AMERICA",
    "limit": 200,
    "includeExplain": true
  }'
```

## 执行计划分析

当设置 `includeExplain=true` 时，响应会包含详细的SQL执行计划，包含以下信息：

| 字段名 | 说明 |
|--------|------|
| `id` | 执行步骤ID |
| `select_type` | 查询类型 |
| `table` | 表名 |
| `type` | 访问类型 |
| `possible_keys` | 可能使用的索引 |
| `key` | 实际使用的索引 |
| `rows` | 预计扫描行数 |
| `filtered` | 过滤百分比 |
| `Extra` | 额外信息 |

## 性能优化建议

1. **索引优化**：确保相关字段有适当的索引
   - lineitem表：l_shipdate, l_returnflag, l_linestatus
   - part表：p_size, p_type
   - supplier表：s_nationkey
   - 等等

2. **查询优化**：
   - 适当调整查询参数以减少数据量
   - 在性能测试时不包含执行计划（includeExplain=false）
   - 使用合适的limit限制结果集大小

3. **监控建议**：
   - 关注executionTime字段来监控查询性能
   - 分析executionPlan来优化慢查询
   - 定期检查数据库统计信息

## 注意事项

1. **数据格式**：shipDate参数使用 YYYY-MM-DD 格式
2. **性能影响**：包含执行计划会额外消耗时间，生产环境谨慎使用
3. **结果限制**：最低成本供应商查询建议设置合理的limit值
4. **日期计算**：定价汇总查询使用 shipDate - intervalDays 作为截止条件
5. **模糊查询**：partType参数使用LIKE匹配，会在参数后添加%通配符

这些接口为TPC-H标准查询提供了完整的实现，可以用于数据库性能测试和分析！ 