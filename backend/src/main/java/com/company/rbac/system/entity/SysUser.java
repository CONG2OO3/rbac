package com.company.rbac.system.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String userName;
    private String nickName;
    private String email;
    private String phone;
    private String sex;
    private String avatar;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String status;
    private Long deptId;
    private String loginIp;
    private java.time.LocalDateTime loginTime;
    private String remark;
}
