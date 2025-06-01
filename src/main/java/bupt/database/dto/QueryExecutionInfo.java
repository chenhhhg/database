package bupt.database.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class QueryExecutionInfo<T> {
    
    /**
     * 查询结果
     */
    private List<T> results;
    
    /**
     * 查询执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 结果数量
     */
    private Integer resultCount;
    
    /**
     * 执行计划
     */
    private List<Map<String, Object>> executionPlan;
    
    /**
     * 查询SQL语句
     */
    private String sql;
    
    /**
     * 查询参数
     */
    private Map<String, Object> parameters;
    
    public QueryExecutionInfo() {
    }
    
    public QueryExecutionInfo(List<T> results, Long executionTime, String sql) {
        this.results = results;
        this.executionTime = executionTime;
        this.resultCount = results != null ? results.size() : 0;
        this.sql = sql;
    }
} 