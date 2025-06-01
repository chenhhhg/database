package bupt.database.service.impl;

import bupt.database.config.DynamicDataSourceConfig;
import bupt.database.dto.AuthDTO;
import bupt.database.dto.CustomerQueryRequest;
import bupt.database.dto.CustomerQueryResponse;
import bupt.database.dto.UserCreateDTO;
import bupt.database.util.DatabaseConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bupt.database.entity.Customer;
import bupt.database.service.CustomerService;
import bupt.database.mapper.CustomerMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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
    @Autowired
    DynamicDataSourceConfig dynamicDataSourceConfig;
    
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
                    .eq("C_NAME", loginDTO.getUsername())
                    .eq("C_PASSWORD", loginDTO.getPassword()));
            return customer != null;
        }catch (Exception e){
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Customer loginAndGetUser(AuthDTO loginDTO) {
        try {
            Customer customer = mapper.selectOne(new QueryWrapper<Customer>()
                    .eq("C_NAME", loginDTO.getUsername())
                    .eq("C_PASSWORD", loginDTO.getPassword()));
            return customer;
        }catch (Exception e){
            log.error(e.getMessage());
            return null;
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
        return dynamicDataSourceConfig.getCurrentConfig();
    }

    @Override
    public void updateDbConfig(DatabaseConfig config) {
        try {
            log.info("开始更新数据库配置...");
            
            if (!(dataSource instanceof HikariDataSource)) {
                throw new RuntimeException("当前数据源不是HikariDataSource，无法动态更新配置");
            }
            
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            // 记录更新前的配置
            log.info("更新前配置: ");
            log.info("- 连接超时: {}ms", hikariDataSource.getConnectionTimeout());
            log.info("- 最大连接池大小: {}", hikariDataSource.getMaximumPoolSize());
            log.info("- 最小空闲连接: {}", hikariDataSource.getMinimumIdle());
            log.info("- 连接最大生命周期: {}ms", hikariDataSource.getMaxLifetime());
            log.info("- 验证超时: {}ms", hikariDataSource.getValidationTimeout());
            
            // 动态更新HikariCP配置
            if (config.getConnectionTimeout() != null) {
                hikariDataSource.setConnectionTimeout(config.getConnectionTimeout());
                log.info("更新连接超时时间: {} -> {}ms", 
                    hikariDataSource.getConnectionTimeout(), config.getConnectionTimeout());
            }
            
            if (config.getMaximumPoolSize() != null) {
                hikariDataSource.setMaximumPoolSize(config.getMaximumPoolSize());
                log.info("更新最大连接池大小: {} -> {}", 
                    hikariDataSource.getMaximumPoolSize(), config.getMaximumPoolSize());
            }
            
            if (config.getMinimumIdle() != null) {
                hikariDataSource.setMinimumIdle(config.getMinimumIdle());
                log.info("更新最小空闲连接数: {} -> {}", 
                    hikariDataSource.getMinimumIdle(), config.getMinimumIdle());
            }
            
            if (config.getMaxLifetime() != null) {
                hikariDataSource.setMaxLifetime(config.getMaxLifetime().longValue());
                log.info("更新连接最大生命周期: {} -> {}ms", 
                    hikariDataSource.getMaxLifetime(), config.getMaxLifetime());
            }
            
            if (config.getValidationTimeout() != null) {
                hikariDataSource.setValidationTimeout(config.getValidationTimeout() * 1000L);
                log.info("更新验证超时时间: {} -> {}s", 
                    hikariDataSource.getValidationTimeout() / 1000, config.getValidationTimeout());
            }
            
            // 测试更新后的连接
            boolean connectionTest = dynamicDataSourceConfig.testConnection(hikariDataSource);
            if (!connectionTest) {
                log.warn("配置更新后连接测试失败，但配置已生效");
            }
            
            // 记录更新后的配置
            log.info("配置更新完成，当前配置: ");
            log.info("- 连接超时: {}ms", hikariDataSource.getConnectionTimeout());
            log.info("- 最大连接池大小: {}", hikariDataSource.getMaximumPoolSize());
            log.info("- 最小空闲连接: {}", hikariDataSource.getMinimumIdle());
            log.info("- 连接最大生命周期: {}ms", hikariDataSource.getMaxLifetime());
            log.info("- 验证超时: {}ms", hikariDataSource.getValidationTimeout());
            
            log.info("数据库配置动态更新成功！");
            
        } catch (Exception e) {
            log.error("动态更新数据库配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("动态更新数据库配置失败: " + e.getMessage());
        }
    }

    @Override
    public Long getUserCnt() {
        return baseMapper.selectCount(new QueryWrapper<>());
    }

    @Override
    public CustomerQueryResponse queryCustomers(CustomerQueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数验证和默认值设置
            if (request.getPage() == null || request.getPage() < 1) {
                request.setPage(1);
            }
            if (request.getSize() == null || request.getSize() < 1) {
                request.setSize(10);
            }
            if (request.getFuzzySearch() == null) {
                request.setFuzzySearch(true);
            }
            
            // 计算分页偏移量
            int offset = (request.getPage() - 1) * request.getSize();
            request.setPage(offset);
            
            log.info("开始查询客户信息，条件: {}", request);
            
            // 查询数据和总数
            List<Customer> customers = mapper.selectByConditions(request);
            Long total = mapper.countByConditions(request);
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("客户查询完成，查询到 {} 条记录，总计 {} 条，耗时 {}ms", 
                    customers.size(), total, duration);
            
            // 恢复原始页码
            request.setPage((request.getPage() / request.getSize()) + 1);
            
            return new CustomerQueryResponse(customers, total, request.getPage(), request.getSize(), duration);
            
        } catch (Exception e) {
            log.error("查询客户信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询客户信息失败: " + e.getMessage());
        }
    }
}




