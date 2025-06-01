package bupt.database.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerQueryRequest {
    
    /**
     * 客户ID
     */
    private Integer cCustkey;
    
    /**
     * 客户ID列表（支持批量查询）
     */
    private List<Integer> cCustkeyList;
    
    /**
     * 客户名称（支持模糊查询）
     */
    private String cName;
    
    /**
     * 客户地址（支持模糊查询）
     */
    private String cAddress;
    
    /**
     * 国家ID
     */
    private Integer cNationkey;
    
    /**
     * 国家ID列表
     */
    private List<Integer> cNationkeyList;
    
    /**
     * 电话号码（支持模糊查询）
     */
    private String cPhone;
    
    /**
     * 账户余额最小值
     */
    private BigDecimal cAcctbalMin;
    
    /**
     * 账户余额最大值
     */
    private BigDecimal cAcctbalMax;
    
    /**
     * 市场细分
     */
    private String cMktsegment;
    
    /**
     * 市场细分列表
     */
    private List<String> cMktsegmentList;
    
    /**
     * 备注信息（支持模糊查询）
     */
    private String cComment;
    
    /**
     * 用户角色
     */
    private Integer cRole;
    
    /**
     * 角色列表
     */
    private List<Integer> cRoleList;
    
    // 查询配置参数
    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 排序字段
     */
    private String sortField = "cCustkey";
    
    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDirection = "ASC";
    
    /**
     * 是否启用模糊查询（默认true）
     */
    private Boolean fuzzySearch = true;
} 