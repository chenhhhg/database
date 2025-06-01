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

    /**
     * 导出数据到指定文件夹
     */
    public ExportResponse exportData(ExportRequest request) {
        long startTime = System.currentTimeMillis();
        ExportResponse response = new ExportResponse();
        
        try {
            // 验证请求参数
            if (request.getFolderName() == null || request.getFolderName().trim().isEmpty()) {
                throw new IllegalArgumentException("文件夹名不能为空");
            }

            // 创建导出目录
            String exportPath = createExportDirectory(request.getFolderName());
            response.setExportPath(exportPath);

            // 确定要导出的表
            List<String> tablesToExport = getTableToExport(request.getTableNames());
            
            // 导出数据
            Map<String, Integer> tableRowCounts = new HashMap<>();
            List<String> exportedFiles = new ArrayList<>();
            
            for (String tableName : tablesToExport) {
                try {
                    String fileName = exportTable(tableName, exportPath, request);
                    int rowCount = getTableRowCount(tableName);
                    
                    tableRowCounts.put(tableName, rowCount);
                    exportedFiles.add(fileName);
                    
                    log.info("成功导出表 {} 到文件 {}, 共 {} 行", tableName, fileName, rowCount);
                } catch (Exception e) {
                    log.error("导出表 {} 失败: {}", tableName, e.getMessage(), e);
                    throw e;
                }
            }

            response.setSuccess(true);
            response.setTableRowCounts(tableRowCounts);
            response.setExportedFiles(exportedFiles);
            
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
     * 导出单个表
     */
    private String exportTable(String tableName, String exportPath, ExportRequest request) 
            throws SQLException, IOException {
        
        String fileName = tableName.toLowerCase() + ".txt";
        Path filePath = Paths.get(exportPath, fileName);
        
        String sql = "SELECT * FROM " + tableName;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery();
             BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 写入表头
            if (request.getIncludeHeader()) {
                List<String> headers = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    headers.add(metaData.getColumnName(i));
                }
                writer.write(String.join(request.getDelimiter(), headers));
                writer.newLine();
            }
            
            // 写入数据行
            while (rs.next()) {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    values.add(value != null ? value.toString() : "");
                }
                writer.write(String.join(request.getDelimiter(), values));
                writer.newLine();
            }
            
            writer.flush();
        }
        
        return fileName;
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
} 