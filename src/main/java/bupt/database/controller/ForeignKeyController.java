package bupt.database.controller;

import bupt.database.service.ForeignKeyManager;
import bupt.database.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 外键管理Controller
 * 提供外键约束管理的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/foreign-key")
public class ForeignKeyController {
    
    @Autowired
    private ForeignKeyManager foreignKeyManager;
    
    /**
     * 删除所有外键约束
     */
    @PostMapping("/drop-all")
    public R<String> dropAllForeignKeys() {
        try {
            log.info("收到删除所有外键约束的请求");
            foreignKeyManager.dropAllForeignKeys();
            return R.success("成功删除所有外键约束");
        } catch (Exception e) {
            log.error("删除外键约束失败", e);
            return R.fail("删除外键约束失败: " + e.getMessage());
        }
    }
    
    /**
     * 恢复所有外键约束
     */
    @PostMapping("/restore-all")
    public R<String> restoreAllForeignKeys() {
        try {
            log.info("收到恢复所有外键约束的请求");
            foreignKeyManager.restoreAllForeignKeys();
            return R.success("成功恢复所有外键约束");
        } catch (Exception e) {
            log.error("恢复外键约束失败", e);
            return R.fail("恢复外键约束失败: " + e.getMessage());
        }
    }
    
    /**
     * 强制删除已知的外键约束
     */
    @PostMapping("/force-drop")
    public R<String> forceDropKnownForeignKeys() {
        try {
            log.info("收到强制删除已知外键约束的请求");
            foreignKeyManager.forceDropKnownForeignKeys();
            return R.success("成功强制删除已知外键约束");
        } catch (Exception e) {
            log.error("强制删除外键约束失败", e);
            return R.fail("强制删除外键约束失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建标准的TPC-H外键约束
     */
    @PostMapping("/create-standard")
    public R<String> createStandardForeignKeys() {
        try {
            log.info("收到创建标准TPC-H外键约束的请求");
            foreignKeyManager.createStandardForeignKeys();
            return R.success("成功创建标准TPC-H外键约束");
        } catch (Exception e) {
            log.error("创建标准外键约束失败", e);
            return R.fail("创建标准外键约束失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询当前数据库中的外键状态
     */
    @GetMapping("/status")
    public R<Object> getForeignKeyStatus() {
        try {
            log.info("收到查询外键状态的请求");
            var currentForeignKeys = foreignKeyManager.getCurrentForeignKeys();
            int savedCount = foreignKeyManager.getSavedForeignKeysCount();
            
            Map<String, Object> status = new HashMap<>();
            status.put("currentForeignKeysCount", currentForeignKeys.size());
            status.put("currentForeignKeys", currentForeignKeys);
            status.put("savedForeignKeysCount", savedCount);
            status.put("canRestore", savedCount > 0);
            
            return R.success(status);
        } catch (Exception e) {
            log.error("查询外键状态失败", e);
            return R.fail("查询外键状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空已保存的外键信息
     */
    @PostMapping("/clear-saved")
    public R<String> clearSavedForeignKeys() {
        try {
            log.info("收到清空已保存外键信息的请求");
            int count = foreignKeyManager.getSavedForeignKeysCount();
            foreignKeyManager.clearSavedForeignKeys();
            return R.success("成功清空 " + count + " 个已保存的外键信息");
        } catch (Exception e) {
            log.error("清空已保存外键信息失败", e);
            return R.fail("清空已保存外键信息失败: " + e.getMessage());
        }
    }
} 