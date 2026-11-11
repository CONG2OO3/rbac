package com.company.rbac.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String operation;
    private String method;
    private String requestMethod;
    private String operatorName;
    private String requestUrl;
    private String ipAddress;
    private String requestParam;
    private String resultData;
    private String status;
    private String errorMessage;
    private Long costTime;
    private LocalDateTime operationTime;
}
