package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @TableName customer
 */
//todo 除了service之外的所有表重新映射一下
@TableName(value ="customer")
@Data
public class Customer implements Serializable {
    @TableId
    private Integer c_CUSTKEY;

    private String c_NAME;

    private String C_PASSWORD;

    private String c_ADDRESS;

    private Integer c_NATIONKEY;

    private String c_PHONE;

    private BigDecimal c_ACCTBAL;

    private String c_MKTSEGMENT;

    private String c_COMMENT;

    private static final long serialVersionUID = 1L;
}