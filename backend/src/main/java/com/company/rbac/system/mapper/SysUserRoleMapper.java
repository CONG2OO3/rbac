package com.company.rbac.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rbac.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("""
            SELECT role.role_key
            FROM sys_user_role relation
            JOIN sys_role role ON role.id = relation.role_id
            WHERE relation.user_id = #{userId}
              AND role.status = '0'
              AND role.deleted = 0
            """)
    List<String> selectRoleKeys(Long userId);
}
