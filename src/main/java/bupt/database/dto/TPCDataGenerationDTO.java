package bupt.database.dto;

import lombok.Data;

@Data
public class TPCDataGenerationDTO {
    /**
     * 生成数据的大小（GB）
     */
    private Double sizeInGB;
    
    /**
     * 目标路径名称（在/root/mysql/tpc/TPC-H V3.0.1/dbgen/tpc/下）
     */
    private String targetPath;
} 