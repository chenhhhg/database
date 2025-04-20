package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * @TableName nation
 */
@TableName(value ="nation")
@Data
public class Nation implements Serializable {
    private Integer n_NATIONKEY;

    private String n_NAME;

    private Integer n_REGIONKEY;

    private String n_COMMENT;

    private static final long serialVersionUID = 1L;
}