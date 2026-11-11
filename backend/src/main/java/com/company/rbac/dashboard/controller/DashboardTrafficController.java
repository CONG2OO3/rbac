package com.company.rbac.dashboard.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.company.rbac.common.api.Result;
import com.company.rbac.dashboard.service.VisitService;
import com.company.rbac.dashboard.service.VisitService.TrafficSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardTrafficController {

    private final VisitService visitService;

    @PostMapping("/visit")
    public Result<Void> recordVisit() {
        visitService.recordVisit(StpUtil.getLoginIdAsLong());
        return Result.success();
    }

    @GetMapping("/traffic")
    public Result<TrafficSummary> traffic(
            @RequestParam(defaultValue = "7d") String range) {
        return Result.success(visitService.traffic(range));
    }
}
