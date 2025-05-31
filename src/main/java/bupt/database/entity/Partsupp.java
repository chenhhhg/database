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
    @TableId
    private Integer psPartkey;

    private Integer psSuppkey;

    private Integer psAvailqty;

    private BigDecimal psSupplycost;

    private String psComment;

    private static final long serialVersionUID = 1L;
}