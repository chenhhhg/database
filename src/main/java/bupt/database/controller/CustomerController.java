package bupt.database.controller;

import bupt.database.dto.AuthDTO;
import bupt.database.dto.UserCreateDTO;
import bupt.database.entity.Customer;
import bupt.database.service.CustomerService;
import bupt.database.util.DatabaseConfig;
import bupt.database.util.R;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

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
    public R<?> login(@RequestBody AuthDTO loginDTO, HttpServletResponse response) {
        try{
            boolean result = customerService.login(loginDTO);
            if (result){
                response.addCookie(new Cookie("database",loginDTO.getUsername()));
                return R.success(null);
            }
            return R.fail("fail");
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
    public DatabaseConfig getDbConfig() {
        return customerService.getDbConfig();
    }

    @PutMapping("/db-config")
    public void updateDbConfig(@RequestBody DatabaseConfig config) {
        customerService.updateDbConfig(config);
    }
}

