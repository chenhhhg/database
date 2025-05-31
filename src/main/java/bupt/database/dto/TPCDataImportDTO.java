package bupt.database.dto;

import lombok.Data;

@Data
public class TPCDataImportDTO {
    /**
     * 数据路径（相对于/root/mysql/tpc/TPC-H V3.0.1/dbgen/tpc/）
     */
    private String dataPath;
    
    /**
     * 是否覆盖现有数据
     */
    private Boolean overwriteExisting = false;
} 