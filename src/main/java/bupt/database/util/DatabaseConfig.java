package bupt.database.util;

import lombok.Data;

@Data
public class DatabaseConfig {
    // 连接池配置
    private Long connectionTimeout;        // 连接超时时间（毫秒）
    private Integer bufferPoolSize;        // 连接池大小
    private Integer maxLifetime;           // 连接最大生命周期（毫秒）
    private Integer minimumIdle;           // 最小空闲连接数
    private Integer maximumPoolSize;       // 最大连接池大小
    
    // 连接信息（只读，用于显示当前配置）
    private String connectionClient;       // 连接客户端信息
    private String connectionInfo;         // 连接URL信息
    
    // 数据库连接配置
    private String url;                    // 数据库URL
    private String username;               // 数据库用户名
    private String password;               // 数据库密码
    private String driverClassName;        // 驱动类名
    
    // 其他配置
    private Boolean autoCommit;            // 是否自动提交
    private String validationQuery;        // 验证查询语句
    private Integer validationTimeout;     // 验证超时时间（秒）
}
