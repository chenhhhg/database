package bupt.database.dto;

import bupt.database.entity.Customer;
import lombok.Data;
import java.util.List;

@Data
public class CustomerQueryResponse {
    
    /**
     * 客户列表
     */
    private List<Customer> customers;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
    
    /**
     * 查询耗时（毫秒）
     */
    private Long duration;
    
    public CustomerQueryResponse() {
    }
    
    public CustomerQueryResponse(List<Customer> customers, Long total, Integer page, Integer size, Long duration) {
        this.customers = customers;
        this.total = total;
        this.page = page;
        this.size = size;
        this.duration = duration;
        
        // 计算分页信息
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }
} 