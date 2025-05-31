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
    @TableId
    private Integer nNationkey;

    private String nName;

    private Integer nRegionkey;

    private String nComment;

    private static final long serialVersionUID = 1L;
}