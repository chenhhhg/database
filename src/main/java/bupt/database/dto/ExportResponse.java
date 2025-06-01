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
} 