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
@Tag(name = "数据导出", description = "TPC-H数据导出相关接口")
public class DataExportController {

    @Autowired
    private DataExportService dataExportService;

    @PostMapping("/data")
    @Operation(summary = "导出数据", description = "将指定表的数据导出为txt文件")
    public ResponseEntity<ExportResponse> exportData(@RequestBody ExportRequest request) {
        log.info("开始导出数据，文件夹名: {}, 表名: {}", request.getFolderName(), request.getTableNames());
        
        try {
            ExportResponse response = dataExportService.exportData(request);
            
            if (response.getSuccess()) {
                log.info("数据导出成功，导出路径: {}, 耗时: {}ms", 
                        response.getExportPath(), response.getDuration());
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
    @Operation(summary = "导出单个表", description = "导出指定的单个表")
    public ResponseEntity<ExportResponse> exportSingleTable(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Parameter(description = "文件夹名") @RequestParam String folderName,
            @Parameter(description = "是否包含表头") @RequestParam(defaultValue = "true") Boolean includeHeader,
            @Parameter(description = "字段分隔符") @RequestParam(defaultValue = "\t") String delimiter) {
        
        log.info("开始导出单个表: {}, 文件夹名: {}", tableName, folderName);
        
        try {
            ExportRequest request = new ExportRequest();
            request.setFolderName(folderName);
            request.setTableNames(List.of(tableName.toUpperCase()));
            request.setIncludeHeader(includeHeader);
            request.setDelimiter(delimiter);
            
            ExportResponse response = dataExportService.exportData(request);
            
            if (response.getSuccess()) {
                log.info("单表导出成功，表: {}, 导出路径: {}, 耗时: {}ms", 
                        tableName, response.getExportPath(), response.getDuration());
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
} 