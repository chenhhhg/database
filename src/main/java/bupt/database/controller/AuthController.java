package bupt.database.controller;

import bupt.database.dto.AuthDto;
import bupt.database.entity.Customer;
import bupt.database.service.CustomerService;
import bupt.database.util.R;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import bupt.database.util.JwtUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private CustomerService customerService;
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long expiration;
    @PostMapping("/login")
    public R<Object> login(@RequestBody AuthDto authDto) {
        Customer customer = customerService.login(authDto);
        if (customer == null) {
            return R.error("用户名或密码错误");
        }
        // 生成JWT
        String token = JwtUtils.generateToken(customer.getC_CUSTKEY(), secret, expiration);

        // 返回用户基本信息+token
        Map<String, Object> result = new HashMap<>();
        result.put("userId", customer.getC_CUSTKEY());
        result.put("username", customer.getC_NAME());
        result.put("token", token);

        return R.success(result);
    }

    @PostMapping("/register")
    public R<Object> register(@RequestBody AuthDto authDto) {
        Customer c = customerService.register(authDto);
        if (c == null) {
            return R.error("用户已存在");
        }
        return R.success(null);
    }

}
