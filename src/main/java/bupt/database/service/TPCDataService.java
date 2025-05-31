package bupt.database.service;

import bupt.database.dto.TPCDataGenerationDTO;
import bupt.database.dto.TPCDataImportDTO;
import bupt.database.dto.TPCPathInfoDTO;
import bupt.database.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TPCDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // TPC相关路径配置
    private static final String DBGEN_PATH = "/root/mysql/tpc/TPC-H V3.0.1/dbgen";
    private static final String TPC_DATA_BASE_PATH = "/root/mysql/tpc/TPC-H V3.0.1/dbgen/tpc";
    private static final String MYSQL_TBL_PATH = "/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl";
    
    // TPC-H表名列表
    private static final List<String> TPC_TABLES = Arrays.asList(
        "customer", "orders", "lineitem", "nation", "partsupp", "part", "region", "supplier"
    );

    /**
     * 生成TPC数据
     */
    public R<String> generateTPCData(TPCDataGenerationDTO dto) {
        try {
            // 验证参数
            if (dto.getSizeInGB() == null || dto.getSizeInGB() <= 0) {
                return R.fail("数据大小必须大于0");
            }
            
            if (dto.getTargetPath() == null || dto.getTargetPath().trim().isEmpty()) {
                return R.fail("目标路径不能为空");
            }
            
            // 清理路径名称，确保安全
            String sanitizedPath = dto.getTargetPath().replaceAll("[^a-zA-Z0-9_-]", "");
            if (sanitizedPath.isEmpty()) {
                return R.fail("目标路径名称无效");
            }
            
            String targetFullPath = TPC_DATA_BASE_PATH + "/" + sanitizedPath;
            
            // 检查目标路径是否已存在
            Path targetPath = Paths.get(targetFullPath);
            if (Files.exists(targetPath)) {
                return R.fail("目标路径已存在: " + sanitizedPath);
            }
            
            // 创建目标目录
            Files.createDirectories(targetPath);
            
            log.info("开始生成TPC数据，大小: {}GB，目标路径: {}", dto.getSizeInGB(), targetFullPath);
            
            // 构建命令
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add("-c");
            command.add(String.format("cd %s && ./dbgen -s %f", DBGEN_PATH, dto.getSizeInGB()));
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(DBGEN_PATH));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.info("dbgen输出: {}", line);
                }
            }
            
            // 等待进程完成
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return R.fail("数据生成超时（30分钟）");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return R.fail("数据生成失败，退出码: " + exitCode + "\n输出: " + output.toString());
            }
            
            // 移动生成的.tbl文件到目标路径
            moveGeneratedFiles(targetFullPath);
            
            log.info("TPC数据生成完成，路径: {}", targetFullPath);
            return R.success("数据生成成功，路径: " + sanitizedPath);
            
        } catch (Exception e) {
            log.error("生成TPC数据失败", e);
            return R.fail("生成数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的TPC数据路径列表
     */
    public R<List<TPCPathInfoDTO>> getAvailableTPCPaths() {
        try {
            List<TPCPathInfoDTO> paths = new ArrayList<>();
            Path baseDir = Paths.get(TPC_DATA_BASE_PATH);
            
            if (!Files.exists(baseDir)) {
                log.info("正在创建存放TBL数据的目录");
                Files.createDirectories(baseDir);
                return R.success(paths);
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, Files::isDirectory)) {
                for (Path path : stream) {
                    log.info("正在检查文件：{}",path.getFileName());
                    TPCPathInfoDTO pathInfo = createPathInfo(path);
                    if (pathInfo != null) {
                        paths.add(pathInfo);
                    }
                }
            }
            
            // 按创建时间排序（最新的在前）
            paths.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
            
            return R.success(paths);
            
        } catch (Exception e) {
            log.error("获取TPC路径列表失败", e);
            return R.fail("获取路径列表失败: " + e.getMessage());
        }
    }

    /**
     * 导入TPC数据到MySQL
     */
    public R<String> importTPCData(TPCDataImportDTO dto) {
        try {
            if (dto.getDataPath() == null || dto.getDataPath().trim().isEmpty()) {
                return R.fail("数据路径不能为空");
            }
            
            String dataPath = dto.getDataPath().trim();
            String sourceFullPath = TPC_DATA_BASE_PATH + "/" + dataPath;
            
            // 验证源路径存在
            Path sourcePath = Paths.get(sourceFullPath);
            if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
                return R.fail("数据路径不存在: " + dataPath);
            }
            
            // 验证必要的表文件存在
            List<String> missingFiles = new ArrayList<>();
            for (String table : TPC_TABLES) {
                Path tblFile = sourcePath.resolve(table + ".tbl");
                if (!Files.exists(tblFile)) {
                    missingFiles.add(table + ".tbl");
                }
            }
            
            if (!missingFiles.isEmpty()) {
                return R.fail("缺少必要的表文件: " + String.join(", ", missingFiles));
            }
            
            log.info("开始导入TPC数据，源路径: {}", sourceFullPath);
            
            // 创建目标路径（MySQL可访问的路径）
            String mysqlTargetPath = MYSQL_TBL_PATH + "/" + dataPath;
            createSymbolicLinkIfNeeded(sourceFullPath, mysqlTargetPath);
            
            // 执行数据导入
            int importedTables = 0;
            List<String> failedTables = new ArrayList<>();
            
            for (String table : TPC_TABLES) {
                try {
                    String sql = String.format(
                        "LOAD DATA INFILE '%s/%s.tbl' INTO TABLE %s FIELDS TERMINATED BY '|'",
                        mysqlTargetPath, table, table.toUpperCase()
                    );
                    
                    log.info("执行导入SQL: {}", sql);
                    jdbcTemplate.execute(sql);
                    importedTables++;
                    
                } catch (Exception e) {
                    log.error("导入表{}失败", table, e);
                    failedTables.add(table + ": " + e.getMessage());
                }
            }
            
            if (failedTables.isEmpty()) {
                log.info("TPC数据导入完成，成功导入{}个表", importedTables);
                return R.success(String.format("数据导入成功，共导入%d个表", importedTables));
            } else {
                String message = String.format("部分数据导入失败，成功导入%d个表，失败%d个表: %s", 
                    importedTables, failedTables.size(), String.join("; ", failedTables));
                return R.fail(message);
            }
            
        } catch (Exception e) {
            log.error("导入TPC数据失败", e);
            return R.fail("导入数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除TPC数据路径
     */
    public R<String> deleteTPCDataPath(String pathName) {
        try {
            if (pathName == null || pathName.trim().isEmpty()) {
                return R.fail("路径名称不能为空");
            }
            
            String fullPath = TPC_DATA_BASE_PATH + "/" + pathName.trim();
            Path targetPath = Paths.get(fullPath);
            
            if (!Files.exists(targetPath)) {
                return R.fail("路径不存在: " + pathName);
            }
            
            // 递归删除目录
            deleteDirectoryRecursively(targetPath);
            
            log.info("删除TPC数据路径: {}", fullPath);
            return R.success("路径删除成功");
            
        } catch (Exception e) {
            log.error("删除TPC数据路径失败", e);
            return R.fail("删除路径失败: " + e.getMessage());
        }
    }

    // 私有辅助方法

    private void moveGeneratedFiles(String targetPath) throws IOException {
        Path source = Paths.get(DBGEN_PATH);
        Path target = Paths.get(targetPath);
        
        for (String table : TPC_TABLES) {
            String fileName = table + ".tbl";
            Path sourceFile = source.resolve(fileName);
            Path targetFile = target.resolve(fileName);
            
            if (Files.exists(sourceFile)) {
                Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("移动文件: {} -> {}", sourceFile, targetFile);
            }
        }
    }

    private TPCPathInfoDTO createPathInfo(Path path) {
        try {
            TPCPathInfoDTO info = new TPCPathInfoDTO();
            info.setPathName(path.getFileName().toString());
            info.setFullPath(path.toString());
            
            // 获取创建时间
            LocalDateTime createTime = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(path).toInstant(),
                ZoneId.systemDefault()
            );
            info.setCreateTime(createTime);
            
            // 计算目录大小和文件列表
            long totalSize = 0;
            List<String> tableFiles = new ArrayList<>();
            boolean importable = true;
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.tbl")) {
                for (Path file : stream) {
                    totalSize += Files.size(file);
                    tableFiles.add(file.getFileName().toString());
                }
            }
            
            // 检查是否包含所有必要的表文件
            for (String table : TPC_TABLES) {
                if (!tableFiles.contains(table + ".tbl")) {
                    log.info("路径 {} 不包含表 {}, 不可导入！",path.getFileName(), table);
                    importable = false;
                    break;
                }
            }
            
            info.setDataSize(totalSize);
            info.setTableFiles(tableFiles);
            info.setImportable(importable);
            log.info("路径 {} 的导入配置为 {}",path.getFileName(), info);
            return info;
            
        } catch (Exception e) {
            log.error("创建路径信息失败: {}", path, e);
            return null;
        }
    }

    private void createSymbolicLinkIfNeeded(String sourcePath, String targetPath) throws IOException {
        Path target = Paths.get(targetPath);
        
        // 确保目标路径的父目录存在
        Files.createDirectories(target.getParent());
        
        // 如果目标路径已存在，先删除
        if (Files.exists(target)) {
            if (Files.isSymbolicLink(target)) {
                Files.delete(target);
            } else {
                deleteDirectoryRecursively(target);
            }
        }
        
        // 创建符号链接
        Files.createSymbolicLink(target, Paths.get(sourcePath));
        log.info("创建符号链接: {} -> {}", targetPath, sourcePath);
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    deleteDirectoryRecursively(child);
                }
            }
        }
        Files.delete(path);
    }
} 