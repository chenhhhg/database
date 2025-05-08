package bupt.database.mapper;

import bupt.database.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 86157
* @description 针对表【customer】的数据库操作Mapper
* @createDate 2025-05-08 19:41:42
* @Entity bupt.database.entity.Customer
*/
public interface CustomerMapper extends BaseMapper<Customer> {

    List<Customer> selectListPage(@Param("page") Integer page, @Param("size") Integer size);
}




