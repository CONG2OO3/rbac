package com.company.rbac.log.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.log.entity.SysLoginLog;
import com.company.rbac.log.entity.SysOperationLog;
import com.company.rbac.log.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SystemLogController {

    private final LogService logService;

    @GetMapping("/login/list")
    public Result<PageResult<SysLoginLog>> loginList(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(
                logService.loginLogs(pageNum, pageSize, keyword, status)));
    }

    @GetMapping("/operation/list")
    public Result<PageResult<SysOperationLog>> operationList(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(
                logService.operationLogs(pageNum, pageSize, keyword, status)));
    }

    @DeleteMapping("/login/{ids}")
    @OperationLog(module = "登录日志", operation = "删除登录日志")
    public Result<Void> removeLogin(@PathVariable String ids) {
        logService.removeLoginLogs(ids);
        return Result.success();
    }

    @DeleteMapping("/operation/{ids}")
    @OperationLog(module = "操作日志", operation = "删除操作日志")
    public Result<Void> removeOperation(@PathVariable String ids) {
        logService.removeOperationLogs(ids);
        return Result.success();
    }

    @DeleteMapping("/login/clear")
    @OperationLog(module = "登录日志", operation = "清空登录日志")
    public Result<Void> clearLogin() {
        logService.clearLoginLogs();
        return Result.success();
    }

    @DeleteMapping("/operation/clear")
    @OperationLog(module = "操作日志", operation = "清空操作日志")
    public Result<Void> clearOperation() {
        logService.clearOperationLogs();
        return Result.success();
    }
}
