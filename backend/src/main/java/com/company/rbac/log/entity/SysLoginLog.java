package com.company.rbac.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userName;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String status;
    private String message;
    private LocalDateTime loginTime;
}
