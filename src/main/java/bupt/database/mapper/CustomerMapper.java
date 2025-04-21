package bupt.database.mapper;

import bupt.database.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

/**
* @author 86157
* @description 针对表【customer】的数据库操作Mapper
* @createDate 2025-04-10 10:38:05
* @Entity bupt.database.entity.Customer
*/
public interface CustomerMapper extends BaseMapper<Customer> {
    @ResultMap("BaseResultMap")
    @Select("select * from customer where c_name = #{name} and c_password = #{password}")
    Customer selectByNameAndPassword(@Param("name")String name, @Param("password")String password);

}




