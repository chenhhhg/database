package bupt.database.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExportResponse {
    
    /**
     * 导出是否成功
     */
    private Boolean success;
    
    /**
     * 导出的文件夹路径
     */
    private String exportPath;
    
    /**
     * 导出的表及对应的行数
     */
    private Map<String, Integer> tableRowCounts;
    
    /**
     * 导出的文件列表
     */
    private List<String> exportedFiles;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 导出耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 分批导出统计信息
     */
    private Map<String, BatchExportStats> batchStats;
    
    /**
     * 分批导出统计信息内部类
     */
    @Data
    public static class BatchExportStats {
        /**
         * 处理的批次数量
         */
        private Integer batchCount;
        
        /**
         * 平均每批处理时间（毫秒）
         */
        private Long avgBatchTime;
        
        /**
         * 最大批次处理时间（毫秒）
         */
        private Long maxBatchTime;
        
        /**
         * 是否使用了内存安全模式
         */
        private Boolean memorySafeModeUsed;
        
        /**
         * 实际使用的批次大小
         */
        private Integer actualBatchSize;
    }
} 