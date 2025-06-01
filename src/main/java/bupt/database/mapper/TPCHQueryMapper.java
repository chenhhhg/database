package bupt.database.mapper;

import bupt.database.dto.MinCostSupplierResult;
import bupt.database.dto.PricingSummaryResult;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TPCHQueryMapper {
    
    /**
     * 定价汇总报表查询
     * @param shipDate 截止发货日期
     * @param intervalDays 间隔天数
     * @return 定价汇总结果列表
     */
    List<PricingSummaryResult> pricingSummaryReport(@Param("shipDate") String shipDate, 
                                                   @Param("intervalDays") Integer intervalDays);
    
    /**
     * 最低成本供应商查询
     * @param partSize 零件尺寸
     * @param partType 零件类型（like查询）
     * @param regionName 地区名称
     * @param limit 限制返回数量
     * @return 最低成本供应商结果列表
     */
    List<MinCostSupplierResult> minCostSupplierQuery(@Param("partSize") Integer partSize,
                                                    @Param("partType") String partType,
                                                    @Param("regionName") String regionName,
                                                    @Param("limit") Integer limit);
    
    /**
     * 获取定价汇总报表查询的执行计划
     */
    @MapKey("id")
    List<Map<String, Object>> explainPricingSummaryReport(@Param("shipDate") String shipDate, 
                                                          @Param("intervalDays") Integer intervalDays);
    
    /**
     * 获取最低成本供应商查询的执行计划
     */
    @MapKey("id")
    List<Map<String, Object>> explainMinCostSupplierQuery(@Param("partSize") Integer partSize,
                                                          @Param("partType") String partType,
                                                          @Param("regionName") String regionName,
                                                          @Param("limit") Integer limit);
} 