package bupt.database.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PricingSummaryResult {
    
    /**
     * 退货标志
     */
    private String lReturnflag;
    
    /**
     * 订单状态
     */
    private String lLinestatus;
    
    /**
     * 总数量
     */
    private BigDecimal sumQty;
    
    /**
     * 总基础价格
     */
    private BigDecimal sumBasePrice;
    
    /**
     * 总折扣价格
     */
    private BigDecimal sumDiscPrice;
    
    /**
     * 总价格（含税）
     */
    private BigDecimal sumCharge;
    
    /**
     * 平均数量
     */
    private BigDecimal avgQty;
    
    /**
     * 平均价格
     */
    private BigDecimal avgPrice;
    
    /**
     * 平均折扣
     */
    private BigDecimal avgDisc;
    
    /**
     * 订单数量
     */
    private Long countOrder;
} 