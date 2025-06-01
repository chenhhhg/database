package bupt.database.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MinCostSupplierResult {
    
    /**
     * 供应商账户余额
     */
    private BigDecimal sAcctbal;
    
    /**
     * 供应商名称
     */
    private String sName;
    
    /**
     * 国家名称
     */
    private String nName;
    
    /**
     * 零件key
     */
    private Integer pPartkey;
    
    /**
     * 制造商
     */
    private String pMfgr;
    
    /**
     * 供应商地址
     */
    private String sAddress;
    
    /**
     * 供应商电话
     */
    private String sPhone;
    
    /**
     * 供应商备注
     */
    private String sComment;
} 