package bupt.database.util;

import lombok.Data;

@Data
public class DatabaseConfig {
    private Long connectionTimeout;
    private Integer bufferPoolSize;
    private String connectionClient;
    private String connectionInfo;
}
