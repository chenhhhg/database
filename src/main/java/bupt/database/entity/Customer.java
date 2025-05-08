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
    @TableId("C_CUSTKEY")
    private Integer cCustkey;

    private String cName;

    private String cAddress;

    private Integer cNationkey;

    private String cPhone;

    private BigDecimal cAcctbal;

    private String cMktsegment;

    private String cComment;

    private String cPassword;

    private Integer cRole;
}