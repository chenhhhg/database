package bupt.database.service;

import bupt.database.dto.ExportRequest;
import bupt.database.dto.ExportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class DataExportService {

    @Autowired
    private DataSource dataSource;

    private static final String BASE_PATH = "/root/mysql/tpc/TPC-H V3.0.1/dbgen/export";
    
    // TPC-H 标准表名
    private static final List<String> DEFAULT_TABLES = Arrays.asList(
        "CUSTOMER", "ORDERS", "LINEITEM", "NATION", 
        "PARTSUPP", "PART", "REGION", "SUPPLIER"
    );
    
    // 大型表列表（需要特殊处理）
    private static final List<String> LARGE_TABLES = Arrays.asList(
        "LINEITEM", "ORDERS", "CUSTOMER", "PARTSUPP"
    );
    
    // 内存安全模式下的批次大小
    private static final int MEMORY_SAFE_BATCH_SIZE = 5000;
    private static final int DEFAULT_BATCH_SIZE = 10000;

    /**
     * 导出数据到指定文件夹
     */
    public ExportResponse exportData(ExportRequest request) {
        long startTime = System.currentTimeMillis();
        ExportResponse response = new ExportResponse();
        
        try {
            // 验证请求参数
            validateRequest(request);

            // 创建导出目录
            String exportPath = createExportDirectory(request.getFolderName());
            response.setExportPath(exportPath);

            // 确定要导出的表
            List<String> tablesToExport = getTableToExport(request.getTableNames());
            
            // 导出数据
            Map<String, Integer> tableRowCounts = new HashMap<>();
            Map<String, ExportResponse.BatchExportStats> batchStats = new HashMap<>();
            List<String> exportedFiles = new ArrayList<>();
            
            log.info("=== 开始数据导出 ===");
            log.info("导出模式: {}", request.getEnableBatchExport() ? "分批导出" : "一次性导出");
            log.info("批次大小: {}", getBatchSize(request));
            log.info("内存安全模式: {}", request.getMemorySafeMode());
            log.info("要导出的表: {}", tablesToExport);
            
            for (String tableName : tablesToExport) {
                try {
                    log.info("开始导出表: {}", tableName);
                    long tableStartTime = System.currentTimeMillis();
                    
                    ExportTableResult result = exportTableWithBatching(tableName, exportPath, request);
                    
                    tableRowCounts.put(tableName, result.getRowCount());
                    batchStats.put(tableName, result.getBatchStats());
                    exportedFiles.add(result.getFileName());
                    
                    long tableTime = System.currentTimeMillis() - tableStartTime;
                    log.info("表 {} 导出完成: {} 行, 耗时 {}ms, {} 个批次", 
                            tableName, result.getRowCount(), tableTime, result.getBatchStats().getBatchCount());
                            
                } catch (Exception e) {
                    log.error("导出表 {} 失败: {}", tableName, e.getMessage(), e);
                    throw e;
                }
            }

            response.setSuccess(true);
            response.setTableRowCounts(tableRowCounts);
            response.setExportedFiles(exportedFiles);
            response.setBatchStats(batchStats);
            
            long totalTime = System.currentTimeMillis() - startTime;
            int totalRows = tableRowCounts.values().stream().mapToInt(Integer::intValue).sum();
            log.info("=== 数据导出完成 ===");
            log.info("总耗时: {}ms, 总行数: {}, 平均速度: {} 行/秒", 
                    totalTime, totalRows, totalTime > 0 ? (totalRows * 1000L / totalTime) : 0);
            
        } catch (Exception e) {
            log.error("数据导出失败: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
        } finally {
            response.setDuration(System.currentTimeMillis() - startTime);
        }

        return response;
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(ExportRequest request) {
        if (request.getFolderName() == null || request.getFolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("文件夹名不能为空");
        }
        if (request.getBatchSize() != null && request.getBatchSize() < 100) {
            throw new IllegalArgumentException("批次大小不能小于100");
        }
        if (request.getBatchSize() != null && request.getBatchSize() > 100000) {
            throw new IllegalArgumentException("批次大小不能超过100000，避免内存溢出");
        }
    }

    /**
     * 分批导出单个表
     */
    private ExportTableResult exportTableWithBatching(String tableName, String exportPath, ExportRequest request) 
            throws SQLException, IOException {
        
        String fileName = tableName.toLowerCase() + ".txt";
        Path filePath = Paths.get(exportPath, fileName);
        
        ExportTableResult result = new ExportTableResult();
        result.setFileName(fileName);
        
        ExportResponse.BatchExportStats stats = new ExportResponse.BatchExportStats();
        stats.setMemorySafeModeUsed(shouldUseMemorySafeMode(tableName, request));
        stats.setActualBatchSize(getBatchSize(request, tableName));
        
        List<Long> batchTimes = new ArrayList<>();
        int totalRowCount = 0;
        int batchCount = 0;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            
            // 获取表的主键列名用于分页
            String primaryKeyColumn = getPrimaryKeyColumn(tableName);
            
            // 写入表头
            if (request.getIncludeHeader()) {
                writeTableHeader(tableName, writer, request.getDelimiter());
            }
            
            if (request.getEnableBatchExport()) {
                // 分批导出
                totalRowCount = exportTableInBatches(tableName, writer, request, primaryKeyColumn, batchTimes);
                batchCount = batchTimes.size();
            } else {
                // 一次性导出（仅用于小表）
                long batchStartTime = System.currentTimeMillis();
                totalRowCount = exportTableAtOnce(tableName, writer, request);
                batchTimes.add(System.currentTimeMillis() - batchStartTime);
                batchCount = 1;
            }
            
            writer.flush();
        }
        
        // 设置批次统计信息
        stats.setBatchCount(batchCount);
        if (!batchTimes.isEmpty()) {
            stats.setAvgBatchTime(batchTimes.stream().mapToLong(Long::longValue).sum() / batchTimes.size());
            stats.setMaxBatchTime(batchTimes.stream().mapToLong(Long::longValue).max().orElse(0L));
        }
        
        result.setRowCount(totalRowCount);
        result.setBatchStats(stats);
        
        return result;
    }

    /**
     * 分批导出表数据
     */
    private int exportTableInBatches(String tableName, BufferedWriter writer, ExportRequest request, 
                                   String primaryKeyColumn, List<Long> batchTimes) throws SQLException, IOException {
        
        int batchSize = getBatchSize(request, tableName);
        int totalRows = 0;
        int batchNumber = 1;
        long lastMaxId = 0;
        
        log.info("开始分批导出表 {}，批次大小: {}, 主键列: {}", tableName, batchSize, primaryKeyColumn);
        
        while (true) {
            long batchStartTime = System.currentTimeMillis();
            
            String sql = buildBatchSelectSql(tableName, primaryKeyColumn, lastMaxId, batchSize);
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = createOptimizedStatement(conn, sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                int batchRowCount = 0;
                long currentMaxId = lastMaxId;
                
                // 处理当前批次的数据
                while (rs.next()) {
                    List<String> values = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        values.add(value != null ? value.toString() : "");
                    }
                    writer.write(String.join(request.getDelimiter(), values));
                    writer.newLine();
                    
                    batchRowCount++;
                    
                    // 更新最大ID
                    Object idValue = rs.getObject(primaryKeyColumn);
                    if (idValue instanceof Number) {
                        currentMaxId = ((Number) idValue).longValue();
                    }
                }
                
                long batchTime = System.currentTimeMillis() - batchStartTime;
                batchTimes.add(batchTime);
                totalRows += batchRowCount;
                
                log.info("批次 {} 完成: {} 行, 耗时 {}ms, 累计 {} 行", 
                        batchNumber, batchRowCount, batchTime, totalRows);
                
                // 如果当前批次没有数据或数据量少于批次大小，说明已经到达末尾
                if (batchRowCount == 0 || batchRowCount < batchSize) {
                    break;
                }
                
                lastMaxId = currentMaxId;
                batchNumber++;
                
                // 内存清理提示
                if (batchNumber % 10 == 0) {
                    System.gc(); // 建议垃圾回收
                    log.info("已处理 {} 个批次，建议进行垃圾回收", batchNumber);
                }
            }
        }
        
        log.info("表 {} 分批导出完成，总共 {} 个批次，{} 行记录", tableName, batchNumber, totalRows);
        return totalRows;
    }

    /**
     * 一次性导出表数据（用于小表）
     */
    private int exportTableAtOnce(String tableName, BufferedWriter writer, ExportRequest request) 
            throws SQLException, IOException {
        
        String sql = "SELECT * FROM " + tableName.toLowerCase();
        int totalRows = 0;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = createOptimizedStatement(conn, sql);
             ResultSet rs = stmt.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 写入数据行
            while (rs.next()) {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    values.add(value != null ? value.toString() : "");
                }
                writer.write(String.join(request.getDelimiter(), values));
                writer.newLine();
                totalRows++;
            }
        }
        
        return totalRows;
    }

    /**
     * 构建分批查询SQL
     */
    private String buildBatchSelectSql(String tableName, String primaryKeyColumn, long lastMaxId, int batchSize) {
        return String.format(
            "SELECT * FROM %s WHERE %s > %d ORDER BY %s LIMIT %d",
            tableName.toLowerCase(), primaryKeyColumn, lastMaxId, primaryKeyColumn, batchSize
        );
    }

    /**
     * 创建优化的Statement
     */
    private PreparedStatement createOptimizedStatement(Connection conn, String sql) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql, 
                ResultSet.TYPE_FORWARD_ONLY, 
                ResultSet.CONCUR_READ_ONLY);
        
        // 设置fetch size以优化内存使用
        stmt.setFetchSize(1000);
        
        return stmt;
    }

    /**
     * 获取表的主键列名
     */
    private String getPrimaryKeyColumn(String tableName) {
        // TPC-H表的主键映射
        Map<String, String> primaryKeys = Map.of(
            "CUSTOMER", "C_CUSTKEY",
            "ORDERS", "O_ORDERKEY", 
            "LINEITEM", "L_ORDERKEY", // LINEITEM使用复合主键，这里使用主要的排序列
            "NATION", "N_NATIONKEY",
            "PARTSUPP", "PS_PARTKEY", // PARTSUPP使用复合主键，这里使用主要的排序列
            "PART", "P_PARTKEY",
            "REGION", "R_REGIONKEY",
            "SUPPLIER", "S_SUPPKEY"
        );
        
        return primaryKeys.getOrDefault(tableName.toUpperCase(), "id");
    }

    /**
     * 写入表头
     */
    private void writeTableHeader(String tableName, BufferedWriter writer, String delimiter) 
            throws SQLException, IOException {
        
        String sql = "SELECT * FROM " + tableName.toLowerCase() + " LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                headers.add(metaData.getColumnName(i));
            }
            writer.write(String.join(delimiter, headers));
            writer.newLine();
        }
    }

    /**
     * 判断是否应该使用内存安全模式
     */
    private boolean shouldUseMemorySafeMode(String tableName, ExportRequest request) {
        return request.getMemorySafeMode() && LARGE_TABLES.contains(tableName.toUpperCase());
    }

    /**
     * 获取批次大小
     */
    private int getBatchSize(ExportRequest request) {
        if (request.getBatchSize() != null) {
            return request.getBatchSize();
        }
        return DEFAULT_BATCH_SIZE;
    }

    /**
     * 获取特定表的批次大小
     */
    private int getBatchSize(ExportRequest request, String tableName) {
        int baseBatchSize = getBatchSize(request);
        
        // 如果启用内存安全模式且是大型表，使用更小的批次
        if (shouldUseMemorySafeMode(tableName, request)) {
            return Math.min(baseBatchSize, MEMORY_SAFE_BATCH_SIZE);
        }
        
        return baseBatchSize;
    }

    /**
     * 创建导出目录
     */
    private String createExportDirectory(String folderName) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dirName = folderName + "_" + timestamp;
        Path exportDir = Paths.get(BASE_PATH, dirName);
        
        Files.createDirectories(exportDir);
        log.info("创建导出目录: {}", exportDir.toAbsolutePath());
        
        return exportDir.toAbsolutePath().toString();
    }

    /**
     * 确定要导出的表
     */
    private List<String> getTableToExport(List<String> requestedTables) {
        if (requestedTables != null && !requestedTables.isEmpty()) {
            return requestedTables.stream()
                    .map(String::toUpperCase)
                    .toList();
        }
        return new ArrayList<>(DEFAULT_TABLES);
    }

    /**
     * 获取表的行数
     */
    private int getTableRowCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * 获取可导出的表列表
     */
    public List<String> getAvailableTables() {
        return new ArrayList<>(DEFAULT_TABLES);
    }

    /**
     * 导出表结果内部类
     */
    private static class ExportTableResult {
        private String fileName;
        private int rowCount;
        private ExportResponse.BatchExportStats batchStats;
        
        // getters and setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        
        public ExportResponse.BatchExportStats getBatchStats() { return batchStats; }
        public void setBatchStats(ExportResponse.BatchExportStats batchStats) { this.batchStats = batchStats; }
    }
} 