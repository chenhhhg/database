package bupt.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bupt.database.entity.Part;
import bupt.database.service.PartService;
import bupt.database.mapper.PartMapper;
import org.springframework.stereotype.Service;

/**
* @author 86157
* @description 针对表【part】的数据库操作Service实现
* @createDate 2025-04-10 10:38:05
*/
@Service
public class PartServiceImpl extends ServiceImpl<PartMapper, Part>
    implements PartService{

}




