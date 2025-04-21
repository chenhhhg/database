package bupt.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * @TableName region
 */
@TableName(value ="region")
@Data
public class Region implements Serializable {
    @TableId
    private Integer r_REGIONKEY;

    private String r_NAME;

    private String r_COMMENT;

    private static final long serialVersionUID = 1L;
}