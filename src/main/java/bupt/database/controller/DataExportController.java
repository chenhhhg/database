package bupt.database.controller;

import bupt.database.dto.ExportRequest;
import bupt.database.dto.ExportResponse;
import bupt.database.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/export")
@Tag(name = "数据导出", description = "TPC-H数据导出相关接口（支持分批导出避免OOM）")
public class DataExportController {

    @Autowired
    private DataExportService dataExportService;

    @PostMapping("/data")
    @Operation(
        summary = "导出数据（分批导出）", 
        description = "将指定表的数据导出为txt文件，支持分批导出避免内存溢出。" +
                     "对于大型表（如LINEITEM、ORDERS等），默认启用内存安全模式。"
    )
    public ResponseEntity<ExportResponse> exportData(@RequestBody ExportRequest request) {
        log.info("开始导出数据 - 文件夹名: {}, 表名: {}, 分批导出: {}, 批次大小: {}, 内存安全模式: {}", 
                request.getFolderName(), request.getTableNames(), 
                request.getEnableBatchExport(), request.getBatchSize(), request.getMemorySafeMode());
        
        try {
            ExportResponse response = dataExportService.exportData(request);
            
            if (response.getSuccess()) {
                log.info("数据导出成功 - 导出路径: {}, 耗时: {}ms, 总行数: {}", 
                        response.getExportPath(), response.getDuration(),
                        response.getTableRowCounts().values().stream().mapToInt(Integer::intValue).sum());
                return ResponseEntity.ok(response);
            } else {
                log.error("数据导出失败: {}", response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("导出数据时发生异常", e);
            ExportResponse errorResponse = new ExportResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("导出失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/tables")
    @Operation(summary = "获取可导出的表列表", description = "返回所有可以导出的TPC-H表名")
    public ResponseEntity<List<String>> getAvailableTables() {
        log.info("获取可导出的表列表");
        
        try {
            List<String> tables = dataExportService.getAvailableTables();
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            log.error("获取表列表时发生异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/table/{tableName}")
    @Operation(
        summary = "导出单个表", 
        description = "导出指定的单个表，自动使用合适的分批导出策略"
    )
    public ResponseEntity<ExportResponse> exportSingleTable(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Parameter(description = "文件夹名") @RequestParam String folderName,
            @Parameter(description = "是否包含表头") @RequestParam(defaultValue = "true") Boolean includeHeader,
            @Parameter(description = "字段分隔符") @RequestParam(defaultValue = "\t") String delimiter,
            @Parameter(description = "是否启用分批导出") @RequestParam(defaultValue = "true") Boolean enableBatchExport,
            @Parameter(description = "批次大小") @RequestParam(defaultValue = "10000") Integer batchSize,
            @Parameter(description = "内存安全模式") @RequestParam(defaultValue = "true") Boolean memorySafeMode) {
        
        log.info("开始导出单个表: {}, 文件夹名: {}, 分批导出: {}, 批次大小: {}", 
                tableName, folderName, enableBatchExport, batchSize);
        
        try {
            ExportRequest request = new ExportRequest();
            request.setFolderName(folderName);
            request.setTableNames(List.of(tableName.toUpperCase()));
            request.setIncludeHeader(includeHeader);
            request.setDelimiter(delimiter);
            request.setEnableBatchExport(enableBatchExport);
            request.setBatchSize(batchSize);
            request.setMemorySafeMode(memorySafeMode);
            
            ExportResponse response = dataExportService.exportData(request);
            
            if (response.getSuccess()) {
                log.info("单表导出成功 - 表: {}, 导出路径: {}, 耗时: {}ms, 行数: {}", 
                        tableName, response.getExportPath(), response.getDuration(),
                        response.getTableRowCounts().get(tableName.toUpperCase()));
                return ResponseEntity.ok(response);
            } else {
                log.error("单表导出失败: {}", response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("导出单个表时发生异常", e);
            ExportResponse errorResponse = new ExportResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("导出失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/data/memory-safe")
    @Operation(
        summary = "内存安全导出", 
        description = "使用最保守的内存安全设置导出数据，适用于超大表或内存受限环境"
    )
    public ResponseEntity<ExportResponse> exportDataMemorySafe(@RequestBody ExportRequest request) {
        log.info("开始内存安全导出 - 文件夹名: {}, 表名: {}", request.getFolderName(), request.getTableNames());
        
        try {
            // 强制使用内存安全设置
            request.setEnableBatchExport(true);
            request.setMemorySafeMode(true);
            if (request.getBatchSize() == null || request.getBatchSize() > 5000) {
                request.setBatchSize(3000); // 更保守的批次大小
            }
            
            ExportResponse response = dataExportService.exportData(request);
            
            if (response.getSuccess()) {
                log.info("内存安全导出成功 - 导出路径: {}, 耗时: {}ms", 
                        response.getExportPath(), response.getDuration());
                return ResponseEntity.ok(response);
            } else {
                log.error("内存安全导出失败: {}", response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("内存安全导出时发生异常", e);
            ExportResponse errorResponse = new ExportResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("内存安全导出失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/performance/estimate/{tableName}")
    @Operation(
        summary = "估算导出性能", 
        description = "根据表大小估算导出时间和推荐的批次大小"
    )
    public ResponseEntity<ExportPerformanceEstimate> estimateExportPerformance(@PathVariable String tableName) {
        log.info("估算表 {} 的导出性能", tableName);
        
        try {
            // 这里可以实现性能估算逻辑
            ExportPerformanceEstimate estimate = new ExportPerformanceEstimate();
            estimate.setTableName(tableName.toUpperCase());
            
            // 根据表名设置推荐参数
            switch (tableName.toUpperCase()) {
                case "LINEITEM":
                    estimate.setRecommendedBatchSize(3000);
                    estimate.setEstimatedTime("5-15分钟");
                    estimate.setMemorySafeModeRecommended(true);
                    break;
                case "ORDERS":
                    estimate.setRecommendedBatchSize(5000);
                    estimate.setEstimatedTime("2-8分钟");
                    estimate.setMemorySafeModeRecommended(true);
                    break;
                case "CUSTOMER":
                    estimate.setRecommendedBatchSize(8000);
                    estimate.setEstimatedTime("30秒-2分钟");
                    estimate.setMemorySafeModeRecommended(false);
                    break;
                default:
                    estimate.setRecommendedBatchSize(10000);
                    estimate.setEstimatedTime("10-60秒");
                    estimate.setMemorySafeModeRecommended(false);
            }
            
            return ResponseEntity.ok(estimate);
        } catch (Exception e) {
            log.error("估算导出性能时发生异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出性能估算响应类
     */
    public static class ExportPerformanceEstimate {
        private String tableName;
        private Integer recommendedBatchSize;
        private String estimatedTime;
        private Boolean memorySafeModeRecommended;
        
        // getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public Integer getRecommendedBatchSize() { return recommendedBatchSize; }
        public void setRecommendedBatchSize(Integer recommendedBatchSize) { this.recommendedBatchSize = recommendedBatchSize; }
        
        public String getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(String estimatedTime) { this.estimatedTime = estimatedTime; }
        
        public Boolean getMemorySafeModeRecommended() { return memorySafeModeRecommended; }
        public void setMemorySafeModeRecommended(Boolean memorySafeModeRecommended) { this.memorySafeModeRecommended = memorySafeModeRecommended; }
    }
} 