package com.company.rbac.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rbac.business.entity.BizOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BizOrderMapper extends BaseMapper<BizOrder> {

    @Select("""
            SELECT * FROM biz_order
            WHERE id = #{id} AND deleted = 0
            FOR UPDATE
            """)
    BizOrder selectForUpdate(@Param("id") Long id);
}
