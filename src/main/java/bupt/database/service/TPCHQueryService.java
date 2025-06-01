package bupt.database.service;

import bupt.database.dto.MinCostSupplierResult;
import bupt.database.dto.PricingSummaryResult;
import bupt.database.dto.QueryExecutionInfo;
import bupt.database.mapper.TPCHQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TPCHQueryService {

    @Autowired
    private TPCHQueryMapper tpchQueryMapper;

    /**
     * 执行定价汇总报表查询
     * @param shipDate 截止发货日期 (格式: YYYY-MM-DD)
     * @param intervalDays 间隔天数
     * @param includeExplain 是否包含执行计划
     * @return 查询执行信息
     */
    public QueryExecutionInfo<PricingSummaryResult> executePricingSummaryReport(
            String shipDate, Integer intervalDays, Boolean includeExplain) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始执行定价汇总报表查询，截止日期: {}, 间隔天数: {}", shipDate, intervalDays);
            
            // 执行查询
            List<PricingSummaryResult> results = tpchQueryMapper.pricingSummaryReport(shipDate, intervalDays);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 构建SQL语句用于记录
            String sql = String.format(
                "SELECT l_returnflag, l_linestatus, SUM(l_quantity) AS sum_qty, " +
                "SUM(l_extendedprice) AS sum_base_price, " +
                "SUM(l_extendedprice * (1 - l_discount)) AS sum_disc_price, " +
                "SUM(l_extendedprice * (1 - l_discount) * (1 + l_tax)) AS sum_charge, " +
                "AVG(l_quantity) AS avg_qty, AVG(l_extendedprice) AS avg_price, " +
                "AVG(l_discount) AS avg_disc, COUNT(*) AS count_order " +
                "FROM lineitem WHERE l_shipdate <= DATE_SUB('%s', INTERVAL %d DAY) " +
                "GROUP BY l_returnflag, l_linestatus ORDER BY l_returnflag, l_linestatus",
                shipDate, intervalDays
            );
            
            QueryExecutionInfo<PricingSummaryResult> info = new QueryExecutionInfo<>(results, executionTime, sql);
            
            // 设置查询参数
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("shipDate", shipDate);
            parameters.put("intervalDays", intervalDays);
            info.setParameters(parameters);
            
            // 如果需要执行计划
            if (includeExplain != null && includeExplain) {
                try {
                    List<Map<String, Object>> executionPlan = tpchQueryMapper.explainPricingSummaryReport(shipDate, intervalDays);
                    info.setExecutionPlan(executionPlan);
                    log.info("获取执行计划成功，计划步骤数: {}", executionPlan.size());
                } catch (Exception e) {
                    log.warn("获取执行计划失败: {}", e.getMessage());
                }
            }
            
            log.info("定价汇总报表查询完成，结果数量: {}, 执行时间: {}ms", results.size(), executionTime);
            return info;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("定价汇总报表查询失败，执行时间: {}ms", executionTime, e);
            throw new RuntimeException("定价汇总报表查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行最低成本供应商查询
     * @param partSize 零件尺寸
     * @param partType 零件类型
     * @param regionName 地区名称
     * @param limit 限制返回数量
     * @param includeExplain 是否包含执行计划
     * @return 查询执行信息
     */
    public QueryExecutionInfo<MinCostSupplierResult> executeMinCostSupplierQuery(
            Integer partSize, String partType, String regionName, Integer limit, Boolean includeExplain) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始执行最低成本供应商查询，零件尺寸: {}, 零件类型: {}, 地区: {}, 限制数量: {}", 
                    partSize, partType, regionName, limit);
            
            // 执行查询
            List<MinCostSupplierResult> results = tpchQueryMapper.minCostSupplierQuery(
                    partSize, partType, regionName, limit);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 构建SQL语句用于记录
            String sql = String.format(
                "SELECT s_acctbal, s_name, n_name, p_partkey, p_mfgr, s_address, s_phone, s_comment " +
                "FROM part, supplier, partsupp, nation, region " +
                "WHERE p_partkey = ps_partkey AND s_suppkey = ps_suppkey " +
                "AND p_size = %d AND p_type LIKE '%%%s' " +
                "AND s_nationkey = n_nationkey AND n_regionkey = r_regionkey " +
                "AND r_name = '%s' AND ps_supplycost = (subquery) " +
                "ORDER BY s_acctbal DESC, n_name, s_name, p_partkey" +
                (limit != null && limit > 0 ? " LIMIT " + limit : ""),
                partSize, partType, regionName
            );
            
            QueryExecutionInfo<MinCostSupplierResult> info = new QueryExecutionInfo<>(results, executionTime, sql);
            
            // 设置查询参数
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("partSize", partSize);
            parameters.put("partType", partType);
            parameters.put("regionName", regionName);
            parameters.put("limit", limit);
            info.setParameters(parameters);
            
            // 如果需要执行计划
            if (includeExplain != null && includeExplain) {
                try {
                    List<Map<String, Object>> executionPlan = tpchQueryMapper.explainMinCostSupplierQuery(
                            partSize, partType, regionName, limit);
                    info.setExecutionPlan(executionPlan);
                    log.info("获取执行计划成功，计划步骤数: {}", executionPlan.size());
                } catch (Exception e) {
                    log.warn("获取执行计划失败: {}", e.getMessage());
                }
            }
            
            log.info("最低成本供应商查询完成，结果数量: {}, 执行时间: {}ms", results.size(), executionTime);
            return info;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("最低成本供应商查询失败，执行时间: {}ms", executionTime, e);
            throw new RuntimeException("最低成本供应商查询失败: " + e.getMessage(), e);
        }
    }
} 