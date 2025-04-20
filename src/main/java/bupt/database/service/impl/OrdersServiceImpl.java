package bupt.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bupt.database.entity.Orders;
import bupt.database.service.OrdersService;
import bupt.database.mapper.OrdersMapper;
import org.springframework.stereotype.Service;

/**
* @author 86157
* @description 针对表【orders】的数据库操作Service实现
* @createDate 2025-04-10 10:38:05
*/
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders>
    implements OrdersService{

}




