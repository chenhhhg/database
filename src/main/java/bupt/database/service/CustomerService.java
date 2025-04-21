package bupt.database.service;

import bupt.database.dto.AuthDto;
import bupt.database.entity.Customer;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.ResultMap;
import org.springframework.web.bind.annotation.RequestMapping;

/**
* @author 86157
* @description 针对表【customer】的数据库操作Service
* @createDate 2025-04-10 10:38:05
*/
public interface CustomerService extends IService<Customer> {

    Customer login(AuthDto authDto);

    Customer register(AuthDto authDto);
}
