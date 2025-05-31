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
    private Integer sSuppkey;

    private String sName;

    private String sAddress;

    private Integer sNationkey;

    private String sPhone;

    private BigDecimal sAcctbal;

    private String sComment;

    private static final long serialVersionUID = 1L;
}