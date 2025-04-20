package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @TableName customer
 */
@TableName(value ="customer")
@Data
public class Customer implements Serializable {
    private Integer c_CUSTKEY;

    private String c_NAME;

    private String c_ADDRESS;

    private Integer c_NATIONKEY;

    private String c_PHONE;

    private BigDecimal c_ACCTBAL;

    private String c_MKTSEGMENT;

    private String c_COMMENT;

    private static final long serialVersionUID = 1L;
}