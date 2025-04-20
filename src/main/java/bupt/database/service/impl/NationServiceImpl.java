package bupt.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bupt.database.entity.Nation;
import bupt.database.service.NationService;
import bupt.database.mapper.NationMapper;
import org.springframework.stereotype.Service;

/**
* @author 86157
* @description 针对表【nation】的数据库操作Service实现
* @createDate 2025-04-10 10:38:05
*/
@Service
public class NationServiceImpl extends ServiceImpl<NationMapper, Nation>
    implements NationService{

}




