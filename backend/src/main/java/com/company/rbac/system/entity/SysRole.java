package com.company.rbac.system.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private Boolean menuCheckStrictly;
    private Boolean deptCheckStrictly;
    private String status;
    private String remark;
}
