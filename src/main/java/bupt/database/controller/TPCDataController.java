package bupt.database.controller;

import bupt.database.dto.TPCDataGenerationDTO;
import bupt.database.dto.TPCDataImportDTO;
import bupt.database.dto.TPCPathInfoDTO;
import bupt.database.service.TPCDataService;
import bupt.database.util.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tpc")
@Tag(name = "TPC数据管理", description = "TPC-H数据生成、导入和管理接口")
public class TPCDataController {

    @Autowired
    private TPCDataService tpcDataService;

    @Operation(summary = "生成TPC数据", description = "使用dbgen工具生成指定大小的TPC-H数据")
    @PostMapping("/generate")
    public R<String> generateData(@RequestBody TPCDataGenerationDTO dto) {
        log.info("接收到TPC数据生成请求: {}", dto);
        
        try {
            return tpcDataService.generateTPCData(dto);
        } catch (Exception e) {
            log.error("TPC数据生成接口异常", e);
            return R.fail("服务异常: " + e.getMessage());
        }
    }

    @Operation(summary = "获取可用数据路径", description = "获取所有可用的TPC数据路径列表")
    @GetMapping("/paths")
    public R<List<TPCPathInfoDTO>> getAvailablePaths() {
        log.info("获取TPC数据路径列表");
        
        try {
            return tpcDataService.getAvailableTPCPaths();
        } catch (Exception e) {
            log.error("获取TPC数据路径列表异常", e);
            return R.fail("服务异常: " + e.getMessage());
        }
    }

    @Operation(summary = "导入TPC数据", description = "将指定路径的TPC数据导入到MySQL数据库")
    @PostMapping("/import")
    public R<String> importData(@RequestBody TPCDataImportDTO dto) {
        log.info("接收到TPC数据导入请求: {}", dto);
        
        try {
            return tpcDataService.importTPCData(dto);
        } catch (Exception e) {
            log.error("TPC数据导入接口异常", e);
            return R.fail("服务异常: " + e.getMessage());
        }
    }

    @Operation(summary = "删除数据路径", description = "删除指定的TPC数据路径及其所有文件")
    @DeleteMapping("/paths/{pathName}")
    public R<String> deletePath(@PathVariable String pathName) {
        log.info("删除TPC数据路径: {}", pathName);
        
        try {
            return tpcDataService.deleteTPCDataPath(pathName);
        } catch (Exception e) {
            log.error("删除TPC数据路径异常", e);
            return R.fail("服务异常: " + e.getMessage());
        }
    }

    @Operation(summary = "获取TPC数据生成状态", description = "检查是否有正在进行的数据生成任务")
    @GetMapping("/generation-status")
    public R<Object> getGenerationStatus() {
        // 这里可以实现任务状态查询逻辑
        // 由于数据生成可能需要较长时间，可以考虑使用异步处理
        
        return R.success("当前没有正在进行的生成任务");
    }
} 