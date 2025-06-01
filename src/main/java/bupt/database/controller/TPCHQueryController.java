package bupt.database.controller;

import bupt.database.dto.MinCostSupplierResult;
import bupt.database.dto.PricingSummaryResult;
import bupt.database.dto.QueryExecutionInfo;
import bupt.database.service.TPCHQueryService;
import bupt.database.util.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tpch")
@Tag(name = "TPC-H复杂查询", description = "TPC-H标准查询接口，包含执行时间统计和执行计划分析")
public class TPCHQueryController {

    @Autowired
    private TPCHQueryService tpchQueryService;

    @GetMapping("/pricing-summary")
    @Operation(summary = "定价汇总报表查询", 
               description = "查询已经开票、发货和退货三个类别订单的业务总量，支持执行时间记录和执行计划分析")
    public ResponseEntity<R<QueryExecutionInfo<PricingSummaryResult>>> pricingSummaryReport(
            @Parameter(description = "截止发货日期", example = "2021-12-01") 
            @RequestParam(defaultValue = "2021-12-01") String shipDate,
            
            @Parameter(description = "间隔天数", example = "90") 
            @RequestParam(defaultValue = "90") Integer intervalDays,
            
            @Parameter(description = "是否包含执行计划") 
            @RequestParam(defaultValue = "false") Boolean includeExplain) {
        
        try {
            log.info("收到定价汇总报表查询请求，截止日期: {}, 间隔天数: {}, 包含执行计划: {}", 
                    shipDate, intervalDays, includeExplain);
            
            QueryExecutionInfo<PricingSummaryResult> result = tpchQueryService.executePricingSummaryReport(
                    shipDate, intervalDays, includeExplain);
            
            return ResponseEntity.ok(R.success(result));
            
        } catch (Exception e) {
            log.error("定价汇总报表查询失败", e);
            return ResponseEntity.ok(R.fail("查询失败: " + e.getMessage()));
        }
    }

    @GetMapping("/min-cost-supplier")
    @Operation(summary = "最低成本供应商查询", 
               description = "在给定区域中，针对给定类型和大小的零件，找到能够以最低价格供应的供应商")
    public ResponseEntity<R<QueryExecutionInfo<MinCostSupplierResult>>> minCostSupplierQuery(
            @Parameter(description = "零件尺寸", example = "15") 
            @RequestParam(defaultValue = "15") Integer partSize,
            
            @Parameter(description = "零件类型（支持like查询）", example = "BRASS") 
            @RequestParam(defaultValue = "BRASS") String partType,
            
            @Parameter(description = "地区名称", example = "EUROPE") 
            @RequestParam(defaultValue = "EUROPE") String regionName,
            
            @Parameter(description = "限制返回数量", example = "100") 
            @RequestParam(defaultValue = "100") Integer limit,
            
            @Parameter(description = "是否包含执行计划") 
            @RequestParam(defaultValue = "false") Boolean includeExplain) {
        
        try {
            log.info("收到最低成本供应商查询请求，零件尺寸: {}, 零件类型: {}, 地区: {}, 限制数量: {}, 包含执行计划: {}", 
                    partSize, partType, regionName, limit, includeExplain);
            
            QueryExecutionInfo<MinCostSupplierResult> result = tpchQueryService.executeMinCostSupplierQuery(
                    partSize, partType, regionName, limit, includeExplain);
            
            return ResponseEntity.ok(R.success(result));
            
        } catch (Exception e) {
            log.error("最低成本供应商查询失败", e);
            return ResponseEntity.ok(R.fail("查询失败: " + e.getMessage()));
        }
    }

    @PostMapping("/pricing-summary")
    @Operation(summary = "定价汇总报表查询（POST方式）", 
               description = "通过POST方式执行定价汇总报表查询，支持更复杂的参数配置")
    public ResponseEntity<R<QueryExecutionInfo<PricingSummaryResult>>> pricingSummaryReportPost(
            @RequestBody PricingSummaryRequest request) {
        
        try {
            log.info("收到定价汇总报表查询POST请求: {}", request);
            
            QueryExecutionInfo<PricingSummaryResult> result = tpchQueryService.executePricingSummaryReport(
                    request.getShipDate(), request.getIntervalDays(), request.getIncludeExplain());
            
            return ResponseEntity.ok(R.success(result));
            
        } catch (Exception e) {
            log.error("定价汇总报表查询失败", e);
            return ResponseEntity.ok(R.fail("查询失败: " + e.getMessage()));
        }
    }

    @PostMapping("/min-cost-supplier")
    @Operation(summary = "最低成本供应商查询（POST方式）", 
               description = "通过POST方式执行最低成本供应商查询，支持更复杂的参数配置")
    public ResponseEntity<R<QueryExecutionInfo<MinCostSupplierResult>>> minCostSupplierQueryPost(
            @RequestBody MinCostSupplierRequest request) {
        
        try {
            log.info("收到最低成本供应商查询POST请求: {}", request);
            
            QueryExecutionInfo<MinCostSupplierResult> result = tpchQueryService.executeMinCostSupplierQuery(
                    request.getPartSize(), request.getPartType(), request.getRegionName(), 
                    request.getLimit(), request.getIncludeExplain());
            
            return ResponseEntity.ok(R.success(result));
            
        } catch (Exception e) {
            log.error("最低成本供应商查询失败", e);
            return ResponseEntity.ok(R.fail("查询失败: " + e.getMessage()));
        }
    }

    // 内部类：定价汇总报表查询请求
    public static class PricingSummaryRequest {
        private String shipDate = "2021-12-01";
        private Integer intervalDays = 90;
        private Boolean includeExplain = false;

        // getters and setters
        public String getShipDate() { return shipDate; }
        public void setShipDate(String shipDate) { this.shipDate = shipDate; }
        public Integer getIntervalDays() { return intervalDays; }
        public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
        public Boolean getIncludeExplain() { return includeExplain; }
        public void setIncludeExplain(Boolean includeExplain) { this.includeExplain = includeExplain; }
        
        @Override
        public String toString() {
            return String.format("PricingSummaryRequest{shipDate='%s', intervalDays=%d, includeExplain=%s}", 
                    shipDate, intervalDays, includeExplain);
        }
    }

    // 内部类：最低成本供应商查询请求
    public static class MinCostSupplierRequest {
        private Integer partSize = 15;
        private String partType = "BRASS";
        private String regionName = "EUROPE";
        private Integer limit = 100;
        private Boolean includeExplain = false;

        // getters and setters
        public Integer getPartSize() { return partSize; }
        public void setPartSize(Integer partSize) { this.partSize = partSize; }
        public String getPartType() { return partType; }
        public void setPartType(String partType) { this.partType = partType; }
        public String getRegionName() { return regionName; }
        public void setRegionName(String regionName) { this.regionName = regionName; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        public Boolean getIncludeExplain() { return includeExplain; }
        public void setIncludeExplain(Boolean includeExplain) { this.includeExplain = includeExplain; }
        
        @Override
        public String toString() {
            return String.format("MinCostSupplierRequest{partSize=%d, partType='%s', regionName='%s', limit=%d, includeExplain=%s}", 
                    partSize, partType, regionName, limit, includeExplain);
        }
    }
} 