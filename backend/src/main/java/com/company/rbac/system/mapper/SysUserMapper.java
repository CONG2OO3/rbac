package com.company.rbac.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rbac.system.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT *
            FROM sys_user
            WHERE user_name = #{userName}
            LIMIT 1
            """)
    SysUser selectByUserNameIncludingDeleted(@Param("userName") String userName);

    @Delete("""
            DELETE FROM sys_user
            WHERE id = #{id}
            """)
    int physicalDeleteById(@Param("id") Long id);
}
