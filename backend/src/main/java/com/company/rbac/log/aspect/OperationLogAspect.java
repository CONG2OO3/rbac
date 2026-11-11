package com.company.rbac.log.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.log.entity.SysOperationLog;
import com.company.rbac.log.service.LogService;
import com.company.rbac.file.config.StorageSettingsService.StorageInput;
import com.company.rbac.system.entity.SysUser;
import com.company.rbac.system.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final LogService logService;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(
            ProceedingJoinPoint joinPoint,
            OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            saveLog(joinPoint, operationLog, result, failure, startTime);
        }
    }

    private void saveLog(
            ProceedingJoinPoint joinPoint,
            OperationLog annotation,
            Object result,
            Throwable failure,
            long startTime) {
        try {
            HttpServletRequest request = currentRequest();
            SysOperationLog log = new SysOperationLog();
            log.setModule(annotation.module());
            log.setOperation(annotation.operation());
            log.setMethod(joinPoint.getSignature().toLongString());
            log.setRequestMethod(request == null ? "" : request.getMethod());
            log.setOperatorName(operatorName());
            log.setRequestUrl(request == null ? "" : request.getRequestURI());
            log.setIpAddress(request == null ? "" : LogService.clientIp(request));
            log.setRequestParam(limit(toJson(safeArguments(joinPoint.getArgs())), 2000));
            log.setResultData(failure == null ? limit(toJson(result), 2000) : "");
            log.setStatus(failure == null ? "0" : "1");
            log.setErrorMessage(failure == null ? "" : limit(failure.getMessage(), 1000));
            log.setCostTime(System.currentTimeMillis() - startTime);
            log.setOperationTime(LocalDateTime.now());
            logService.saveOperation(log);
        } catch (Exception ignored) {
            // 日志记录失败不能影响正常业务请求。
        }
    }

    private Object[] safeArguments(Object[] arguments) {
        return Arrays.stream(arguments)
                .filter(argument -> !(argument instanceof ServletRequest))
                .filter(argument -> !(argument instanceof ServletResponse))
                .filter(argument -> !(argument instanceof MultipartFile))
                .filter(argument -> !(argument instanceof StorageInput))
                .toArray();
    }

    private String operatorName() {
        if (!StpUtil.isLogin()) {
            return "匿名用户";
        }
        SysUser user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        return user == null ? String.valueOf(StpUtil.getLoginId()) : user.getNickName();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value)
                    .replaceAll("(?i)\\\"password\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\"password\":\"******\"");
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
