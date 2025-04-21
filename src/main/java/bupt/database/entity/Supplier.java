package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @TableName supplier
 */
@TableName(value ="supplier")
@Data
public class Supplier implements Serializable {
    @TableId
    private Integer s_SUPPKEY;

    private String s_NAME;

    private String s_ADDRESS;

    private Integer s_NATIONKEY;

    private String s_PHONE;

    private BigDecimal s_ACCTBAL;

    private String s_COMMENT;

    private static final long serialVersionUID = 1L;
}