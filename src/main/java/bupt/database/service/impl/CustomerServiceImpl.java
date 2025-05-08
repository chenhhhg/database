package bupt.database.service.impl;

import bupt.database.dto.AuthDTO;
import bupt.database.dto.UserCreateDTO;
import bupt.database.util.DatabaseConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bupt.database.entity.Customer;
import bupt.database.service.CustomerService;
import bupt.database.mapper.CustomerMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
* @author 86157
* @description 针对表【customer】的数据库操作Service实现
* @createDate 2025-05-08 19:00:55
*/
@Service
@Slf4j
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer>
    implements CustomerService{
    @Autowired
    CustomerMapper mapper;
    @Autowired
    DataSource dataSource;
    @Override
    public boolean register(AuthDTO registerDTO) {
        try {
            Customer customer = mapper.selectOne(new QueryWrapper<Customer>()
                    .eq("C_NAME", registerDTO.getUsername()));
            if (customer!=null){
                return false;
            }
            customer = new Customer();
            customer.setCName(registerDTO.getUsername());
            customer.setCPassword(registerDTO.getPassword());
            customer.setCRole(1);
            mapper.insert(customer);
            return true;
        }catch (Exception e){
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean login(AuthDTO loginDTO) {
        try {
            Customer customer = mapper.selectOne(new QueryWrapper<Customer>()
                    .eq("C_NAME", loginDTO.getUsername()));
            return customer != null;
        }catch (Exception e){
            log.error(e.getMessage());
            return false;
        }
    }

    public List<Customer> getAllUsers(Integer page, Integer size) {
        return mapper.selectListPage(page * size, size);
    }

    public Customer createUser(UserCreateDTO dto) {
        Customer user = new Customer();
        user.setCName(dto.getUsername());
        user.setCPassword(dto.getPassword());
        user.setCRole(0);
        mapper.insert(user);
        return user;
    }

    @Override
    public void deleteUser(Integer id) {
        mapper.deleteById(id);
    }

    public void approveUser(Integer id) {
        Customer user = mapper.selectById(id);
        user.setCRole(0);
        mapper.updateById(user);
    }

    @Override
    public DatabaseConfig getDbConfig() {
        if (dataSource instanceof HikariDataSource){
            HikariDataSource source = (HikariDataSource) dataSource;
            DatabaseConfig databaseConfig = new DatabaseConfig();
            databaseConfig.setConnectionTimeout(source.getConnectionTimeout());
            try {
                Connection connection = source.getConnection();
                databaseConfig.setConnectionClient(connection.getMetaData().getUserName());
                databaseConfig.setConnectionInfo(connection.getMetaData().getURL());
            }catch (Exception e){
                e.printStackTrace();
            }
            databaseConfig.setBufferPoolSize(source.getMaximumPoolSize());
            return databaseConfig;
        }
        return null;
    }

    @Override
    public void updateDbConfig(DatabaseConfig config) {

    }
}




