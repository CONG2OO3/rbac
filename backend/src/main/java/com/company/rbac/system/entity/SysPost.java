package com.company.rbac.system.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_post")
public class SysPost extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String status;
    private String remark;
}
