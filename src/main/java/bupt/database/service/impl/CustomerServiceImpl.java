package bupt.database.service.impl;

import bupt.database.dto.AuthDto;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bupt.database.entity.Customer;
import bupt.database.service.CustomerService;
import bupt.database.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* @author 86157
* @description 针对表【customer】的数据库操作Service实现
* @createDate 2025-04-10 10:38:05
*/
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer>
    implements CustomerService{
    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public Customer login(AuthDto authDto) {
        QueryWrapper<Customer> eq = new QueryWrapper<Customer>()
                .eq("c_name", authDto.getUsername())
                .eq("c_password", authDto.getPassword());
        return customerMapper.selectByNameAndPassword(authDto.getUsername(), authDto.getPassword());
    }

    @Override
    public Customer register(AuthDto authDto) {
        QueryWrapper<Customer> eq = new QueryWrapper<Customer>()
                .eq("c_name", authDto.getUsername());
        Customer customer = customerMapper.selectOne(eq);
        if(customer != null){
            return null;
        }
        customer = new Customer();
        customer.setC_NAME(authDto.getUsername());
        customer.setC_PASSWORD(authDto.getPassword());
        customer.setC_ACCTBAL(BigDecimal.ZERO);
        customer.setC_PHONE("12345678901");
        customer.setC_COMMENT(LocalDateTime.now().toString());
        customer.setC_MKTSEGMENT("中国");
        customer.setC_NATIONKEY(1);
        customer.setC_ADDRESS("中国");
        customerMapper.insert(customer);
        return customer;
    }
}




