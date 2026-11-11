package com.company.rbac.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rbac.log.entity.SysLoginLog;
import com.company.rbac.log.entity.SysOperationLog;
import com.company.rbac.log.mapper.SysLoginLogMapper;
import com.company.rbac.log.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperationLogMapper operationLogMapper;

    public IPage<SysLoginLog> loginLogs(
            long pageNum,
            long pageSize,
            String keyword,
            String status) {
        LambdaQueryWrapper<SysLoginLog> query = new LambdaQueryWrapper<>();
        query.like(keyword != null && !keyword.isBlank(), SysLoginLog::getUserName, keyword)
                .eq(status != null && !status.isBlank(), SysLoginLog::getStatus, status)
                .orderByDesc(SysLoginLog::getId);
        return loginLogMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    public IPage<SysOperationLog> operationLogs(
            long pageNum,
            long pageSize,
            String keyword,
            String status) {
        LambdaQueryWrapper<SysOperationLog> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(item -> item
                    .like(SysOperationLog::getOperatorName, keyword)
                    .or()
                    .like(SysOperationLog::getModule, keyword)
                    .or()
                    .like(SysOperationLog::getOperation, keyword));
        }
        query.eq(status != null && !status.isBlank(), SysOperationLog::getStatus, status)
                .orderByDesc(SysOperationLog::getId);
        return operationLogMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    public void recordLogin(
            String userName,
            String status,
            String message,
            HttpServletRequest request) {
        SysLoginLog log = new SysLoginLog();
        log.setUserName(userName == null ? "" : userName);
        log.setIpAddress(clientIp(request));
        log.setBrowser(browser(request.getHeader("User-Agent")));
        log.setOperatingSystem(operatingSystem(request.getHeader("User-Agent")));
        log.setStatus(status);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    public void saveOperation(SysOperationLog log) {
        operationLogMapper.insert(log);
    }

    public void removeLoginLogs(String ids) {
        loginLogMapper.deleteBatchIds(parseIds(ids));
    }

    public void removeOperationLogs(String ids) {
        operationLogMapper.deleteBatchIds(parseIds(ids));
    }

    public void clearLoginLogs() {
        loginLogMapper.delete(null);
    }

    public void clearOperationLogs() {
        operationLogMapper.delete(null);
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .toList();
    }

    public static String browser(String userAgent) {
        if (userAgent == null) {
            return "未知";
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("Chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/")) {
            return "Safari";
        }
        return "其他";
    }

    public static String operatingSystem(String userAgent) {
        if (userAgent == null) {
            return "未知";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Mac OS")) {
            return "macOS";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "其他";
    }
}
