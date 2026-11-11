package com.company.rbac.monitor.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.monitor.service.MonitorService;
import com.company.rbac.monitor.service.MonitorService.OnlineUser;
import com.company.rbac.monitor.service.MonitorService.ServerSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/server")
    public Result<ServerSnapshot> server() {
        return Result.success(monitorService.serverSnapshot());
    }

    @GetMapping("/online")
    public Result<PageResult<OnlineUser>> online(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(monitorService.onlineUsers(pageNum, pageSize, keyword));
    }

    @DeleteMapping("/online/{sessionId}")
    @OperationLog(module = "在线用户", operation = "强制下线")
    public Result<Void> kickout(@PathVariable String sessionId) {
        monitorService.kickout(sessionId);
        return Result.success();
    }
}
