package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * @TableName lineitem
 */
@TableName(value ="lineitem")
@Data
public class Lineitem implements Serializable {
    @TableId
    private Integer lOrderkey;

    private Integer lLinenumber;

    private Integer lPartkey;

    private Integer lSuppkey;

    private BigDecimal lQuantity;

    private BigDecimal lExtendedprice;

    private BigDecimal lDiscount;

    private BigDecimal lTax;

    private String lReturnflag;

    private String lLinestatus;

    private Date lShipdate;

    private Date lCommitdate;

    private Date lReceiptdate;

    private String lShipinstruct;

    private String lShipmode;

    private String lComment;

    private static final long serialVersionUID = 1L;
}