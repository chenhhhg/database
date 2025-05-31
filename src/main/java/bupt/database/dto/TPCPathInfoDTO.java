package bupt.database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TPCPathInfoDTO {
    /**
     * 路径名称
     */
    private String pathName;
    
    /**
     * 完整路径
     */
    private String fullPath;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 数据大小（字节）
     */
    private Long dataSize;
    
    /**
     * 包含的表文件列表
     */
    private List<String> tableFiles;
    
    /**
     * 是否可用于导入
     */
    private Boolean importable;
} 