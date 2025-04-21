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
 * @TableName orders
 */
@TableName(value ="orders")
@Data
public class Orders implements Serializable {
    @TableId
    private Integer o_ORDERKEY;

    private Integer o_CUSTKEY;

    private String o_ORDERSTATUS;

    private BigDecimal o_TOTALPRICE;

    private Date o_ORDERDATE;

    private String o_ORDERPRIORITY;

    private String o_CLERK;

    private Integer o_SHIPPRIORITY;

    private String o_COMMENT;

    private static final long serialVersionUID = 1L;
}