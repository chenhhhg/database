package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @TableName part
 */
@TableName(value ="part")
@Data
public class Part implements Serializable {
    @TableId
    private Integer p_PARTKEY;

    private String p_NAME;

    private String p_MFGR;

    private String p_BRAND;

    private String p_TYPE;

    private Integer p_SIZE;

    private String p_CONTAINER;

    private BigDecimal p_RETAILPRICE;

    private String p_COMMENT;

    private static final long serialVersionUID = 1L;
}