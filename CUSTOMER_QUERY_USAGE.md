# 客户信息查询接口使用说明

## 概述
新增的客户查询功能提供了灵活的多条件查询能力，支持根据客户的任意字段进行查询，包括精确查询、模糊查询、范围查询、批量查询等多种查询方式。

## 功能特性
- ✅ **多字段查询**：支持根据客户ID、姓名、地址、国家、电话、余额、市场细分、备注、角色等任意字段查询
- ✅ **灵活查询模式**：支持精确匹配和模糊查询
- ✅ **范围查询**：支持余额范围查询
- ✅ **批量查询**：支持多个ID或国家的批量查询
- ✅ **分页支持**：完整的分页功能，包含总数统计
- ✅ **排序功能**：支持按任意字段排序（升序/降序）
- ✅ **性能统计**：返回查询耗时信息

## API接口列表

### 1. 灵活查询接口
**请求方式**: POST  
**接口地址**: `/customer/query`  
**描述**: 支持多条件组合查询的万能接口

#### 请求体示例
```json
{
  "cName": "Customer",
  "cNationkey": 15,
  "cAcctbalMin": 1000.00,
  "cAcctbalMax": 5000.00,
  "cMktsegment": "BUILDING",
  "page": 1,
  "size": 20,
  "sortField": "cAcctbal",
  "sortDirection": "DESC",
  "fuzzySearch": true
}
```

#### 响应示例
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "customers": [
      {
        "cCustkey": 1,
        "cName": "Customer#000000001",
        "cAddress": "IVhzIApeRb ot,c,E",
        "cNationkey": 15,
        "cPhone": "25-989-741-2988",
        "cAcctbal": 711.56,
        "cMktsegment": "BUILDING",
        "cComment": "to the even, regular platelets...",
        "cRole": 0
      }
    ],
    "total": 150000,
    "page": 1,
    "size": 20,
    "totalPages": 7500,
    "hasNext": true,
    "hasPrevious": false,
    "duration": 45
  }
}
```

### 2. 根据ID查询
**请求方式**: GET  
**接口地址**: `/customer/query/id/{id}`  
**描述**: 根据客户ID精确查询

#### 示例
```bash
GET /customer/query/id/1
```

### 3. 根据名称查询
**请求方式**: GET  
**接口地址**: `/customer/query/name?name={name}&page={page}&size={size}`  
**描述**: 根据客户名称模糊查询

#### 示例
```bash
GET /customer/query/name?name=Customer&page=1&size=10
```

### 4. 根据国家查询
**请求方式**: GET  
**接口地址**: `/customer/query/nation/{nationkey}?page={page}&size={size}`  
**描述**: 查询指定国家的所有客户

#### 示例
```bash
GET /customer/query/nation/15?page=1&size=20
```

### 5. 根据市场细分查询
**请求方式**: GET  
**接口地址**: `/customer/query/segment/{segment}?page={page}&size={size}`  
**描述**: 查询指定市场细分的客户

#### 示例
```bash
GET /customer/query/segment/BUILDING?page=1&size=15
```

### 6. 根据余额范围查询
**请求方式**: GET  
**接口地址**: `/customer/query/balance?minBalance={min}&maxBalance={max}&page={page}&size={size}`  
**描述**: 查询余额在指定范围内的客户

#### 示例
```bash
GET /customer/query/balance?minBalance=1000&maxBalance=5000&page=1&size=10
```

## 查询参数详解

### CustomerQueryRequest 参数说明

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| `cCustkey` | Integer | 客户ID（精确匹配） | 1 |
| `cCustkeyList` | List<Integer> | 客户ID列表（批量查询） | [1, 2, 3] |
| `cName` | String | 客户名称（支持模糊查询） | "Customer" |
| `cAddress` | String | 客户地址（支持模糊查询） | "New York" |
| `cNationkey` | Integer | 国家ID | 15 |
| `cNationkeyList` | List<Integer> | 国家ID列表 | [15, 16, 17] |
| `cPhone` | String | 电话号码（支持模糊查询） | "989-741" |
| `cAcctbalMin` | BigDecimal | 最小余额 | 1000.00 |
| `cAcctbalMax` | BigDecimal | 最大余额 | 5000.00 |
| `cMktsegment` | String | 市场细分 | "BUILDING" |
| `cMktsegmentList` | List<String> | 市场细分列表 | ["BUILDING", "AUTOMOBILE"] |
| `cComment` | String | 备注（支持模糊查询） | "regular" |
| `cRole` | Integer | 用户角色 | 0 |
| `cRoleList` | List<Integer> | 角色列表 | [0, 1] |
| `page` | Integer | 页码（从1开始） | 1 |
| `size` | Integer | 每页大小 | 10 |
| `sortField` | String | 排序字段 | "cAcctbal" |
| `sortDirection` | String | 排序方向（ASC/DESC） | "DESC" |
| `fuzzySearch` | Boolean | 是否启用模糊查询 | true |

### 支持的排序字段
- `cCustkey` - 客户ID
- `cName` - 客户名称
- `cAddress` - 客户地址
- `cNationkey` - 国家ID
- `cPhone` - 电话号码
- `cAcctbal` - 账户余额
- `cMktsegment` - 市场细分
- `cComment` - 备注
- `cRole` - 用户角色

## 使用示例

### 1. 查询指定国家余额最高的客户
```bash
curl -X POST "http://localhost:8080/customer/query" \
  -H "Content-Type: application/json" \
  -d '{
    "cNationkey": 15,
    "page": 1,
    "size": 10,
    "sortField": "cAcctbal",
    "sortDirection": "DESC"
  }'
```

### 2. 模糊查询客户名称包含"Smith"的客户
```bash
curl -X POST "http://localhost:8080/customer/query" \
  -H "Content-Type: application/json" \
  -d '{
    "cName": "Smith",
    "fuzzySearch": true,
    "page": 1,
    "size": 20
  }'
```

### 3. 查询多个国家的建筑行业客户
```bash
curl -X POST "http://localhost:8080/customer/query" \
  -H "Content-Type: application/json" \
  -d '{
    "cNationkeyList": [15, 16, 17],
    "cMktsegment": "BUILDING",
    "page": 1,
    "size": 50
  }'
```

### 4. 查询余额在1000-5000之间的客户
```bash
curl -X POST "http://localhost:8080/customer/query" \
  -H "Content-Type: application/json" \
  -d '{
    "cAcctbalMin": 1000.00,
    "cAcctbalMax": 5000.00,
    "sortField": "cAcctbal",
    "sortDirection": "ASC",
    "page": 1,
    "size": 25
  }'
```

### 5. 批量查询指定客户ID
```bash
curl -X POST "http://localhost:8080/customer/query" \
  -H "Content-Type: application/json" \
  -d '{
    "cCustkeyList": [1, 2, 3, 4, 5],
    "sortField": "cCustkey",
    "size": 10
  }'
```

## 响应字段说明

### CustomerQueryResponse 响应字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `customers` | List<Customer> | 客户列表 |
| `total` | Long | 总记录数 |
| `page` | Integer | 当前页码 |
| `size` | Integer | 每页大小 |
| `totalPages` | Integer | 总页数 |
| `hasNext` | Boolean | 是否有下一页 |
| `hasPrevious` | Boolean | 是否有上一页 |
| `duration` | Long | 查询耗时（毫秒） |

## 性能优化建议

1. **使用索引字段查询**：优先使用 `cCustkey`、`cNationkey` 等有索引的字段
2. **合理设置分页大小**：建议每页大小不超过100条记录
3. **避免过于宽泛的模糊查询**：模糊查询时尽量提供更具体的关键词
4. **使用精确查询**：当知道确切值时，将 `fuzzySearch` 设为 `false`
5. **合理使用排序**：只在必要时使用排序功能

## 注意事项

1. 分页从第1页开始计数
2. 模糊查询对性能有一定影响，建议结合其他精确条件使用
3. 批量查询时建议限制ID列表大小（不超过1000个）
4. 排序字段必须是有效的数据库列名
5. 余额范围查询支持只设置最小值或最大值的单边界查询

这个查询接口为您提供了强大而灵活的客户信息查询能力，可以满足各种复杂的查询需求！ 