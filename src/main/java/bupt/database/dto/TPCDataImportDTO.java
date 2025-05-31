package bupt.database.dto;

import lombok.Data;

@Data
public class TPCDataImportDTO {
    /**
     * 数据路径（相对于/root/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/）
     */
    private String dataPath;
    
    /**
     * 是否覆盖现有数据
     */
    private Boolean overwriteExisting = false;
    
    /**
     * 批处理大小（MB），默认400MB
     */
    private Integer batchSizeMB = 400;
    
    /**
     * 每批次最大记录数，默认10000条
     */
    private Integer batchRecordCount = 10000;
    
    /**
     * 是否启用数据清洗，默认true
     */
    private Boolean enableDataCleaning = true;
} 