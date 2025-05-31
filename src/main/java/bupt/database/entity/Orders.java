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
    private Integer oOrderkey;

    private Integer oCustkey;

    private String oOrderstatus;

    private BigDecimal oTotalprice;

    private Date oOrderdate;

    private String oOrderpriority;

    private String oClerk;

    private Integer oShippriority;

    private String oComment;

    private static final long serialVersionUID = 1L;
}