package bupt.database.service;

import bupt.database.dto.AuthDTO;
import bupt.database.dto.UserCreateDTO;
import bupt.database.entity.Customer;
import bupt.database.util.DatabaseConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 86157
* @description 针对表【customer】的数据库操作Service
* @createDate 2025-05-08 19:00:55
*/
public interface CustomerService extends IService<Customer> {

    boolean register(AuthDTO registerDTO);

    boolean login(AuthDTO loginDTO);

    List<Customer> getAllUsers(Integer page, Integer size);

    Object createUser(UserCreateDTO dto);

    void deleteUser(Integer id);

    void approveUser(Integer id);

    DatabaseConfig getDbConfig();

    void updateDbConfig(DatabaseConfig config);
}
