package bupt.database.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExportRequest {
    
    /**
     * 导出文件夹名
     */
    private String folderName;
    
    /**
     * 要导出的表名列表，如果为空则导出所有表
     */
    private List<String> tableNames;
    
    /**
     * 是否包含表头
     */
    private Boolean includeHeader = true;
    
    /**
     * 字段分隔符，默认为制表符
     */
    private String delimiter = "\t";
} 