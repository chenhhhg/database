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
    private Integer pPartkey;

    private String pName;

    private String pMfgr;

    private String pBrand;

    private String pType;

    private Integer pSize;

    private String pContainer;

    private BigDecimal pRetailprice;

    private String pComment;

    private static final long serialVersionUID = 1L;
}