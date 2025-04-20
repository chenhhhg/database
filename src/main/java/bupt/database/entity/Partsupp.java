package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @TableName partsupp
 */
@TableName(value ="partsupp")
@Data
public class Partsupp implements Serializable {
    private Integer PS_PARTKEY;

    private Integer PS_SUPPKEY;

    private Integer PS_AVAILQTY;

    private BigDecimal PS_SUPPLYCOST;

    private String PS_COMMENT;

    private static final long serialVersionUID = 1L;
}