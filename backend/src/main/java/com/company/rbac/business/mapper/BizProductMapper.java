package com.company.rbac.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rbac.business.entity.BizProduct;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BizProductMapper extends BaseMapper<BizProduct> {

    @Select("""
            SELECT * FROM biz_product
            WHERE id = #{id} AND deleted = 0
            FOR UPDATE
            """)
    BizProduct selectForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE biz_product
            SET stock = stock - #{quantity}, update_time = NOW()
            WHERE id = #{id} AND deleted = 0
              AND status = '0' AND stock >= #{quantity}
            """)
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("""
            UPDATE biz_product
            SET stock = stock + #{quantity}, update_time = NOW()
            WHERE id = #{id}
            """)
    int releaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Delete("DELETE FROM biz_product WHERE id = #{id}")
    int physicalDelete(@Param("id") Long id);
}
