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
    private Integer l_ORDERKEY;

    private Integer l_LINENUMBER;

    private Integer l_PARTKEY;

    private Integer l_SUPPKEY;

    private BigDecimal l_QUANTITY;

    private BigDecimal l_EXTENDEDPRICE;

    private BigDecimal l_DISCOUNT;

    private BigDecimal l_TAX;

    private String l_RETURNFLAG;

    private String l_LINESTATUS;

    private Date l_SHIPDATE;

    private Date l_COMMITDATE;

    private Date l_RECEIPTDATE;

    private String l_SHIPINSTRUCT;

    private String l_SHIPMODE;

    private String l_COMMENT;

    private static final long serialVersionUID = 1L;
}