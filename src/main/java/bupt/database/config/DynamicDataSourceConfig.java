package bupt.database.config;

import bupt.database.util.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Slf4j
public class DynamicDataSourceConfig {

    @Autowired
    @Getter
    public DataSource currentDataSource;

    @Value("${spring.datasource.url:}")
    private String defaultUrl;

    @Value("${spring.datasource.username:}")
    private String defaultUsername;

    @Value("${spring.datasource.password:}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name:}")
    private String defaultDriverClassName;

    /**
     * 创建新的数据源
     * @param config 数据库配置
     * @return 新的数据源
     */
    public HikariDataSource createDataSource(DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        // 设置数据库连接信息
        hikariConfig.setJdbcUrl(config.getUrl() != null ? config.getUrl() : defaultUrl);
        hikariConfig.setUsername(config.getUsername() != null ? config.getUsername() : defaultUsername);
        hikariConfig.setPassword(config.getPassword() != null ? config.getPassword() : defaultPassword);
        hikariConfig.setDriverClassName(config.getDriverClassName() != null ? config.getDriverClassName() : defaultDriverClassName);

        // 设置连接池配置
        if (config.getConnectionTimeout() != null) {
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        }
        if (config.getMaximumPoolSize() != null) {
            hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        }
        if (config.getMinimumIdle() != null) {
            hikariConfig.setMinimumIdle(config.getMinimumIdle());
        }
        if (config.getMaxLifetime() != null) {
            hikariConfig.setMaxLifetime(config.getMaxLifetime());
        }
        if (config.getAutoCommit() != null) {
            hikariConfig.setAutoCommit(config.getAutoCommit());
        }
        if (config.getValidationTimeout() != null) {
            hikariConfig.setValidationTimeout(config.getValidationTimeout() * 1000L); // 转换为毫秒
        }

        // 设置默认的连接池配置
        hikariConfig.setConnectionTimeout(hikariConfig.getConnectionTimeout() > 0 ? hikariConfig.getConnectionTimeout() : 30000);
        hikariConfig.setMaximumPoolSize(hikariConfig.getMaximumPoolSize() > 0 ? hikariConfig.getMaximumPoolSize() : 10);
        hikariConfig.setMinimumIdle(hikariConfig.getMinimumIdle() >= 0 ? hikariConfig.getMinimumIdle() : 5);
        hikariConfig.setMaxLifetime(hikariConfig.getMaxLifetime() > 0 ? hikariConfig.getMaxLifetime() : 1800000); // 30分钟

        return new HikariDataSource(hikariConfig);
    }

    /**
     * 测试数据源连接
     * @param dataSource 数据源
     * @return 是否连接成功
     */
    public boolean testConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5秒超时
        } catch (SQLException e) {
            log.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前数据源的配置信息
     * @return 数据库配置
     */
    public DatabaseConfig getCurrentConfig() {
        DatabaseConfig config = new DatabaseConfig();
        
        if (currentDataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) currentDataSource;
            
            config.setConnectionTimeout(hikariDataSource.getConnectionTimeout());
            config.setMaximumPoolSize(hikariDataSource.getMaximumPoolSize());
            config.setMinimumIdle(hikariDataSource.getMinimumIdle());
            config.setMaxLifetime((int) hikariDataSource.getMaxLifetime());
            config.setAutoCommit(hikariDataSource.isAutoCommit());
            config.setValidationTimeout((int) (hikariDataSource.getValidationTimeout() / 1000));

            try (Connection connection = hikariDataSource.getConnection()) {
                config.setConnectionClient(connection.getMetaData().getUserName());
                config.setConnectionInfo(connection.getMetaData().getURL());
            } catch (SQLException e) {
                log.error("获取数据库连接信息失败: {}", e.getMessage());
            }
        }

        return config;
    }

    /**
     * 获取HikariCP连接池的详细状态信息
     * @return 连接池状态信息
     */
    public java.util.Map<String, Object> getPoolStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        if (currentDataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) currentDataSource;
            
            try {
                status.put("poolName", hikari.getPoolName());
                status.put("isRunning", !hikari.isClosed());

                // 获取MXBean状态
                try {
                    com.zaxxer.hikari.HikariPoolMXBean poolMXBean = hikari.getHikariPoolMXBean();
                    if (poolMXBean != null) {
                        status.put("activeConnections", poolMXBean.getActiveConnections());
                        status.put("idleConnections", poolMXBean.getIdleConnections());
                        status.put("totalConnections", poolMXBean.getTotalConnections());
                        status.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                    }
                } catch (Exception e) {
                    log.warn("无法获取HikariCP MXBean信息: {}", e.getMessage());
                    status.put("mxBeanError", e.getMessage());
                }
                
            } catch (Exception e) {
                log.error("获取连接池状态失败: {}", e.getMessage());
                status.put("error", e.getMessage());
            }
        } else {
            status.put("error", "当前数据源不是HikariDataSource");
        }
        
        return status;
    }
} 