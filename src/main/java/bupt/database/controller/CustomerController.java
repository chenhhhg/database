package bupt.database.controller;

import bupt.database.config.DynamicDataSourceConfig;
import bupt.database.dto.AuthDTO;
import bupt.database.dto.CustomerQueryRequest;
import bupt.database.dto.CustomerQueryResponse;
import bupt.database.dto.LoginResponseDTO;
import bupt.database.dto.UserCreateDTO;
import bupt.database.entity.Customer;
import bupt.database.service.CustomerService;
import bupt.database.util.DatabaseConfig;
import bupt.database.util.JwtUtil;
import bupt.database.util.R;
import com.zaxxer.hikari.HikariDataSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/customer")
@Tag(name = "客户管理", description = "客户信息管理相关接口")
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private DynamicDataSourceConfig dynamicDataSourceConfig;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @PostMapping("/register")
    public R<?> register(@RequestBody AuthDTO registerDTO) {
        try{
            boolean result = customerService.register(registerDTO);
            if (result){
                return R.success(null);
            }
            return R.fail("fail");
        }catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    public R<?> login(@RequestBody AuthDTO loginDTO) {
        try{
            Customer customer = customerService.loginAndGetUser(loginDTO);
            if (customer != null){
                // 生成JWT token
                String token = jwtUtil.generateToken(
                    customer.getCName(), 
                    customer.getCCustkey(), 
                    customer.getCRole()
                );
                
                // 创建登录响应DTO
                LoginResponseDTO loginResponse = new LoginResponseDTO(
                    token,
                    customer.getCName(),
                    customer.getCCustkey(),
                    customer.getCRole(),
                    jwtExpiration
                );
                
                return R.success(loginResponse);
            }
            return R.fail("用户名或密码错误");
        }catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    // 用户管理
    @GetMapping("/users")
    public List<Customer> getAllUsers(@RequestParam(defaultValue = "0") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        return customerService.getAllUsers(page, size);
    }

    @GetMapping("/user-cnt")
    public R<Long> getUserCount(){
        return R.success(customerService.getUserCnt());
    }

    @PostMapping("/users")
    public R<?> createUser(@RequestBody UserCreateDTO dto) {
        return R.success(customerService.createUser(dto));
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Integer id) {
        customerService.deleteUser(id);
    }

    @PatchMapping("/users/{id}/approve")
    public void approveUser(@PathVariable Integer id) {
        customerService.approveUser(id);
    }

    // 数据库配置管理
    @GetMapping("/db-config")
    public R<DatabaseConfig> getDbConfig() {
        try {
            DatabaseConfig config = customerService.getDbConfig();
            return R.success(config);
        } catch (Exception e) {
            return R.fail("获取数据库配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/db-config")
    public R<?> updateDbConfig(@RequestBody DatabaseConfig config) {
        try {
            // 验证配置参数
            String validationError = validateDatabaseConfig(config);
            if (validationError != null) {
                return R.fail(validationError);
            }
            
            customerService.updateDbConfig(config);
            return R.success("数据库配置更新成功");
        } catch (Exception e) {
            return R.fail("更新数据库配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/db-config/test")
    public R<?> testDbConnection(@RequestBody DatabaseConfig config) {
        try {
            // 验证配置参数
            String validationError = validateDatabaseConfig(config);
            if (validationError != null) {
                return R.fail(validationError);
            }
            
            // 创建测试数据源
            HikariDataSource testDataSource = dynamicDataSourceConfig.createDataSource(config);
            
            // 测试连接
            boolean isConnected = dynamicDataSourceConfig.testConnection(testDataSource);
            
            // 关闭测试数据源
            testDataSource.close();
            
            if (isConnected) {
                return R.success("数据库连接测试成功");
            } else {
                return R.fail("数据库连接测试失败，请检查配置参数");
            }
        } catch (Exception e) {
            return R.fail("数据库连接测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/db-config/status")
    public R<?> getDbStatus() {
        try {
            // 获取当前配置和连接池状态
            DatabaseConfig currentConfig = customerService.getDbConfig();
            java.util.Map<String, Object> poolStatus = dynamicDataSourceConfig.getPoolStatus();
            
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("currentConfig", currentConfig);
            status.put("poolStatus", poolStatus);
            
            return R.success(status);
        } catch (Exception e) {
            return R.fail("获取数据库状态失败: " + e.getMessage());
        }
    }

    /**
     * 验证数据库配置参数
     * @param config 数据库配置
     * @return 验证错误信息，如果验证通过返回null
     */
    private String validateDatabaseConfig(DatabaseConfig config) {
        if (config == null) {
            return "配置信息不能为空";
        }
        
        // 验证连接池大小
        if (config.getMaximumPoolSize() != null && config.getMaximumPoolSize() <= 0) {
            return "最大连接池大小必须大于0";
        }
        
        if (config.getMinimumIdle() != null && config.getMinimumIdle() < 0) {
            return "最小空闲连接数不能小于0";
        }
        
        if (config.getMaximumPoolSize() != null && config.getMinimumIdle() != null 
            && config.getMinimumIdle() > config.getMaximumPoolSize()) {
            return "最小空闲连接数不能大于最大连接池大小";
        }
        
        // 验证超时时间
        if (config.getConnectionTimeout() != null && config.getConnectionTimeout() <= 0) {
            return "连接超时时间必须大于0";
        }
        
        if (config.getMaxLifetime() != null && config.getMaxLifetime() <= 0) {
            return "连接最大生命周期必须大于0";
        }
        
        if (config.getValidationTimeout() != null && config.getValidationTimeout() <= 0) {
            return "验证超时时间必须大于0";
        }
        
        return null; // 验证通过
    }

    /**
     * 灵活查询客户信息
     */
    @PostMapping("/query")
    @Operation(summary = "灵活查询客户信息", description = "根据多个条件灵活查询客户信息，支持分页、排序、模糊查询等")
    public R<CustomerQueryResponse> queryCustomers(@RequestBody CustomerQueryRequest request) {
        try {
            log.info("收到客户查询请求: {}", request);
            CustomerQueryResponse response = customerService.queryCustomers(request);
            return R.success(response);
        } catch (Exception e) {
            log.error("查询客户信息失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 快速查询接口 - 根据ID查询
     */
    @GetMapping("/query/id/{id}")
    @Operation(summary = "根据ID查询客户", description = "根据客户ID精确查询客户信息")
    public R<CustomerQueryResponse> queryCustomerById(
            @Parameter(description = "客户ID") @PathVariable Integer id) {
        try {
            CustomerQueryRequest request = new CustomerQueryRequest();
            request.setCCustkey(id);
            request.setSize(1);
            
            CustomerQueryResponse response = customerService.queryCustomers(request);
            return R.success(response);
        } catch (Exception e) {
            log.error("根据ID查询客户失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 快速查询接口 - 根据名称模糊查询
     */
    @GetMapping("/query/name")
    @Operation(summary = "根据名称查询客户", description = "根据客户名称进行模糊查询")
    public R<CustomerQueryResponse> queryCustomerByName(
            @Parameter(description = "客户名称") @RequestParam String name,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            CustomerQueryRequest request = new CustomerQueryRequest();
            request.setCName(name);
            request.setPage(page);
            request.setSize(size);
            request.setFuzzySearch(true);
            
            CustomerQueryResponse response = customerService.queryCustomers(request);
            return R.success(response);
        } catch (Exception e) {
            log.error("根据名称查询客户失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 快速查询接口 - 根据国家查询
     */
    @GetMapping("/query/nation/{nationkey}")
    @Operation(summary = "根据国家查询客户", description = "根据国家ID查询该国的所有客户")
    public R<CustomerQueryResponse> queryCustomerByNation(
            @Parameter(description = "国家ID") @PathVariable Integer nationkey,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            CustomerQueryRequest request = new CustomerQueryRequest();
            request.setCNationkey(nationkey);
            request.setPage(page);
            request.setSize(size);
            
            CustomerQueryResponse response = customerService.queryCustomers(request);
            return R.success(response);
        } catch (Exception e) {
            log.error("根据国家查询客户失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 快速查询接口 - 根据市场细分查询
     */
    @GetMapping("/query/segment/{segment}")
    @Operation(summary = "根据市场细分查询客户", description = "根据市场细分类型查询客户")
    public R<CustomerQueryResponse> queryCustomerBySegment(
            @Parameter(description = "市场细分") @PathVariable String segment,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            CustomerQueryRequest request = new CustomerQueryRequest();
            request.setCMktsegment(segment);
            request.setPage(page);
            request.setSize(size);
            
            CustomerQueryResponse response = customerService.queryCustomers(request);
            return R.success(response);
        } catch (Exception e) {
            log.error("根据市场细分查询客户失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 快速查询接口 - 根据余额范围查询
     */
    @GetMapping("/query/balance")
    @Operation(summary = "根据余额范围查询客户", description = "根据账户余额范围查询客户")
    public R<CustomerQueryResponse> queryCustomerByBalance(
            @Parameter(description = "最小余额") @RequestParam(required = false) BigDecimal minBalance,
            @Parameter(description = "最大余额") @RequestParam(required = false) BigDecimal maxBalance,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            CustomerQueryRequest request = new CustomerQueryRequest();
            request.setCAcctbalMin(minBalance);
            request.setCAcctbalMax(maxBalance);
            request.setPage(page);
            request.setSize(size);
            
            CustomerQueryResponse response = customerService.queryCustomers(request);
            return R.success(response);
        } catch (Exception e) {
            log.error("根据余额范围查询客户失败", e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }
}

