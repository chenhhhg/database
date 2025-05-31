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
    private static final String TPC_DATA_BASE_PATH = "/root/mysql/tpc/TPC-H V3.0.1/dbgen/tbl";
    private static final String MYSQL_TBL_PATH = "/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl";
    
    // TPC-H表名列表
    private static final List<String> TPC_TABLES = Arrays.asList(
        "customer", "orders", "lineitem", "nation", "partsupp", "part", "region", "supplier"
    );

    /**
     * 生成TPC数据
     */
    public R<String> generateTPCData(TPCDataGenerationDTO dto) {
        log.info("=== 开始TPC数据生成流程 ===");
        log.info("接收到的请求参数: {}", dto);
        
        try {
            // 验证参数
            log.info("步骤1: 开始验证输入参数");
            if (dto.getSizeInGB() == null || dto.getSizeInGB() <= 0) {
                log.error("参数验证失败: 数据大小无效 - {}", dto.getSizeInGB());
                return R.fail("数据大小必须大于0");
            }
            log.info("数据大小验证通过: {}GB", dto.getSizeInGB());
            
            if (dto.getTargetPath() == null || dto.getTargetPath().trim().isEmpty()) {
                log.error("参数验证失败: 目标路径为空");
                return R.fail("目标路径不能为空");
            }
            log.info("目标路径原始值: '{}'", dto.getTargetPath());
            
            // 清理路径名称，确保安全
            log.info("步骤2: 开始路径名称清理和验证");
            String sanitizedPath = dto.getTargetPath().replaceAll("[^a-zA-Z0-9_-]", "");
            log.info("路径清理后: '{}' -> '{}'", dto.getTargetPath(), sanitizedPath);
            if (sanitizedPath.isEmpty()) {
                log.error("路径清理失败: 清理后路径为空");
                return R.fail("目标路径名称无效");
            }
            
            String targetFullPath = TPC_DATA_BASE_PATH + "/" + sanitizedPath;
            log.info("计算出的完整目标路径: {}", targetFullPath);
            
            // 检查目标路径是否已存在
            log.info("步骤3: 检查目标路径是否存在");
            Path targetPath = Paths.get(targetFullPath);
            log.info("检查路径: {}", targetPath.toAbsolutePath());
            if (Files.exists(targetPath)) {
                log.error("目标路径已存在: {}", targetPath.toAbsolutePath());
                return R.fail("目标路径已存在: " + sanitizedPath);
            }
            log.info("目标路径不存在，可以继续");
            
            // 创建目标目录
            log.info("步骤4: 创建目标目录");
            try {
                Files.createDirectories(targetPath);
                log.info("目标目录创建成功: {}", targetPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("创建目标目录失败: {}", e.getMessage(), e);
                return R.fail("创建目标目录失败: " + e.getMessage());
            }
            
            log.info("开始生成TPC数据，大小: {}GB，目标路径: {}", dto.getSizeInGB(), targetFullPath);
            
            // 构建命令
            log.info("步骤5: 构建dbgen执行命令");
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add("-c");
            String dbgenCommand = String.format("cd %s && ./dbgen -s %f", DBGEN_PATH, dto.getSizeInGB());
            command.add(dbgenCommand);
            
            log.info("构建的完整命令: {}", command);
            log.info("dbgen工作目录: {}", DBGEN_PATH);
            log.info("执行的dbgen命令: {}", dbgenCommand);
            
            // 检查dbgen可执行文件是否存在
            log.info("步骤6: 检查dbgen可执行文件");
            Path dbgenExecutable = Paths.get(DBGEN_PATH, "dbgen");
            log.info("检查dbgen可执行文件路径: {}", dbgenExecutable.toAbsolutePath());
            if (!Files.exists(dbgenExecutable)) {
                log.error("dbgen可执行文件不存在: {}", dbgenExecutable.toAbsolutePath());
                return R.fail("dbgen可执行文件不存在，请检查路径: " + dbgenExecutable.toAbsolutePath());
            }
            if (!Files.isExecutable(dbgenExecutable)) {
                log.error("dbgen文件存在但不可执行: {}", dbgenExecutable.toAbsolutePath());
                return R.fail("dbgen文件不可执行，请检查权限: " + dbgenExecutable.toAbsolutePath());
            }
            log.info("dbgen可执行文件检查通过");
            
            // 执行命令
            log.info("步骤7: 开始执行dbgen命令");
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(DBGEN_PATH));
            processBuilder.redirectErrorStream(true);
            log.info("ProcessBuilder配置完成，工作目录: {}", processBuilder.directory().getAbsolutePath());
            
            long startTime = System.currentTimeMillis();
            log.info("开始启动进程，时间戳: {}", startTime);
            
            Process process;
            try {
                process = processBuilder.start();
                log.info("进程启动成功，PID可能为: {}", process.pid());
            } catch (IOException e) {
                log.error("启动进程失败: {}", e.getMessage(), e);
                return R.fail("启动dbgen进程失败: " + e.getMessage());
            }
            
            // 读取输出
            log.info("步骤8: 开始读取进程输出");
            StringBuilder output = new StringBuilder();
            int lineCount = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    output.append(line).append("\n");
                    log.info("dbgen输出[第{}行]: {}", lineCount, line);
                    
                    // 每10行输出一次进度
                    if (lineCount % 10 == 0) {
                        long currentTime = System.currentTimeMillis();
                        long elapsed = currentTime - startTime;
                        log.info("已读取{}行输出，已耗时: {}秒", lineCount, elapsed / 1000);
                    }
                }
            } catch (IOException e) {
                log.error("读取进程输出失败: {}", e.getMessage(), e);
            }
            
            log.info("进程输出读取完成，共{}行", lineCount);
            
            // 等待进程完成
            log.info("步骤9: 等待进程完成（最大30分钟）");
            boolean finished;
            try {
                finished = process.waitFor(30, TimeUnit.MINUTES);
                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                log.info("进程等待结束，是否在时限内完成: {}, 总耗时: {}秒", finished, totalTime / 1000);
            } catch (InterruptedException e) {
                log.error("等待进程时被中断: {}", e.getMessage(), e);
                process.destroyForcibly();
                return R.fail("等待进程时被中断: " + e.getMessage());
            }
            
            if (!finished) {
                log.error("进程执行超时（30分钟），强制终止进程");
                process.destroyForcibly();
                return R.fail("数据生成超时（30分钟）");
            }
            
            int exitCode = process.exitValue();
            log.info("进程退出码: {}", exitCode);
            if (exitCode != 0) {
                log.error("dbgen执行失败，退出码: {}", exitCode);
                log.error("完整输出内容:\n{}", output.toString());
                return R.fail("数据生成失败，退出码: " + exitCode + "\n输出: " + output.toString());
            }
            
            log.info("dbgen执行成功完成");
            
            // 检查生成的文件
            log.info("步骤10: 检查dbgen工作目录中生成的文件");
            Path dbgenWorkDir = Paths.get(DBGEN_PATH);
            List<String> generatedFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dbgenWorkDir, "*.tbl")) {
                for (Path file : stream) {
                    generatedFiles.add(file.getFileName().toString());
                    long fileSize = Files.size(file);
                    log.info("发现生成的文件: {}, 大小: {} bytes", file.getFileName(), fileSize);
                }
            } catch (IOException e) {
                log.error("检查生成文件失败: {}", e.getMessage(), e);
            }
            
            if (generatedFiles.isEmpty()) {
                log.error("未发现任何生成的.tbl文件");
                return R.fail("数据生成完成但未发现.tbl文件");
            }
            
            log.info("发现{}个.tbl文件: {}", generatedFiles.size(), generatedFiles);
            
            // 移动生成的.tbl文件到目标路径
            log.info("步骤11: 移动生成的文件到目标路径");
            try {
                moveGeneratedFiles(targetFullPath);
                log.info("文件移动完成");
            } catch (IOException e) {
                log.error("移动文件失败: {}", e.getMessage(), e);
                return R.fail("移动生成文件失败: " + e.getMessage());
            }
            
            // 验证移动后的文件
            log.info("步骤12: 验证移动后的文件");
            Path finalTargetPath = Paths.get(targetFullPath);
            List<String> movedFiles = new ArrayList<>();
            long totalSize = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(finalTargetPath, "*.tbl")) {
                for (Path file : stream) {
                    movedFiles.add(file.getFileName().toString());
                    long fileSize = Files.size(file);
                    totalSize += fileSize;
                    log.info("目标目录中的文件: {}, 大小: {} bytes", file.getFileName(), fileSize);
                }
            } catch (IOException e) {
                log.error("验证移动文件失败: {}", e.getMessage(), e);
            }
            
            log.info("最终在目标目录中发现{}个文件，总大小: {} bytes", movedFiles.size(), totalSize);
            
            long finalTime = System.currentTimeMillis();
            long totalElapsed = finalTime - startTime;
            log.info("=== TPC数据生成流程完成 ===");
            log.info("总耗时: {}秒, 生成路径: {}", totalElapsed / 1000, sanitizedPath);
            
            return R.success("数据生成成功，路径: " + sanitizedPath);
            
        } catch (Exception e) {
            log.error("=== TPC数据生成流程异常终止 ===", e);
            log.error("异常类型: {}", e.getClass().getSimpleName());
            log.error("异常消息: {}", e.getMessage());
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

            log.info("即将开始检查路径下导入数据");
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
        log.info("=== 开始移动生成的文件 ===");
        Path source = Paths.get(DBGEN_PATH);
        Path target = Paths.get(targetPath);
        
        log.info("源目录: {}", source.toAbsolutePath());
        log.info("目标目录: {}", target.toAbsolutePath());
        
        int movedCount = 0;
        int skippedCount = 0;
        
        for (String table : TPC_TABLES) {
            String fileName = table + ".tbl";
            Path sourceFile = source.resolve(fileName);
            Path targetFile = target.resolve(fileName);
            
            log.info("处理表文件: {}", fileName);
            log.info("  源文件路径: {}", sourceFile.toAbsolutePath());
            log.info("  目标文件路径: {}", targetFile.toAbsolutePath());
            
            if (Files.exists(sourceFile)) {
                try {
                    long fileSize = Files.size(sourceFile);
                    log.info("  源文件存在，大小: {} bytes", fileSize);
                    
                    Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    movedCount++;
                    
                    log.info("  文件移动成功: {} -> {}", sourceFile.getFileName(), targetFile.getFileName());
                    
                    // 验证移动后的文件
                    if (Files.exists(targetFile)) {
                        long newFileSize = Files.size(targetFile);
                        log.info("  移动后文件验证成功，大小: {} bytes", newFileSize);
                        if (newFileSize != fileSize) {
                            log.warn("  警告：移动后文件大小不匹配！原始: {}, 现在: {}", fileSize, newFileSize);
                        }
                    } else {
                        log.error("  错误：移动后目标文件不存在！");
                        throw new IOException("移动文件失败：目标文件不存在 - " + targetFile);
                    }
                } catch (IOException e) {
                    log.error("  移动文件失败: {}", e.getMessage(), e);
                    throw e;
                }
            } else {
                skippedCount++;
                log.warn("  源文件不存在，跳过: {}", sourceFile.toAbsolutePath());
            }
        }
        
        log.info("=== 文件移动完成 ===");
        log.info("成功移动: {} 个文件", movedCount);
        log.info("跳过文件: {} 个文件", skippedCount);
        log.info("预期文件数: {} 个文件", TPC_TABLES.size());
        
        if (movedCount == 0) {
            throw new IOException("没有任何文件被移动，可能dbgen没有生成预期的文件");
        }
        
        if (skippedCount > 0) {
            log.warn("有{}个文件未找到，可能dbgen未完全成功", skippedCount);
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