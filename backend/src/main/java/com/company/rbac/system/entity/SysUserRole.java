package com.company.rbac.system.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRole {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Long roleId;
}
