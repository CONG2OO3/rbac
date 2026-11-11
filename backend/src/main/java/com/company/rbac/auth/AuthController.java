package com.company.rbac.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.rbac.common.api.Result;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.log.service.LogService;
import com.company.rbac.system.service.SystemConfigService;
import com.company.rbac.system.entity.SysUser;
import com.company.rbac.system.mapper.SysUserMapper;
import com.company.rbac.system.mapper.SysUserRoleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final SystemConfigService configService;
    private final CaptchaService captchaService;
    private final RegistrationService registrationService;

    @GetMapping("/captcha")
    public Result<CaptchaService.Captcha> captcha() {
        if (!configService.captchaEnabled()) {
            throw new BusinessException("验证码未开启");
        }
        return Result.success(captchaService.create());
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegistrationService.RegistrationInput input) {
        registrationService.register(input);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        if (request == null || request.getUsername() == null
                || request.getPassword() == null) {
            throw new BusinessException("请输入账号和密码");
        }
        if (configService.captchaEnabled()) {
            try {
                captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
            } catch (BusinessException exception) {
                logService.recordLogin(
                        request.getUsername(),
                        "1",
                        exception.getMessage(),
                        servletRequest);
                throw exception;
            }
        }
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("user_name", request.getUsername()));
        boolean valid = user != null
                && "0".equals(user.getStatus())
                && (request.getPassword().equals(user.getPassword())
                || passwordEncoder.matches(request.getPassword(), user.getPassword()));
        if (!valid) {
            logService.recordLogin(
                    request.getUsername(),
                    "1",
                    "账号或密码错误",
                    servletRequest);
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userMapper.updateById(user);
        }
        user.setLoginIp(LogService.clientIp(servletRequest));
        user.setLoginTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
        StpUtil.login(user.getId());
        StpUtil.getTokenSession()
                .set("loginIp", LogService.clientIp(servletRequest))
                .set("browser", LogService.browser(servletRequest.getHeader("User-Agent")))
                .set("operatingSystem", LogService.operatingSystem(servletRequest.getHeader("User-Agent")));
        logService.recordLogin(
                user.getUserName(),
                "0",
                "登录成功",
                servletRequest);
        return Result.success(Map.of(
                "token", StpUtil.getTokenValue(),
                "userId", user.getId(),
                "userName", user.getUserName(),
                "nickName", user.getNickName()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    @GetMapping("/info")
    public Result<?> info() {
        SysUser user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        List<String> roles = userRoleMapper.selectRoleKeys(user.getId());
        return Result.success(Map.of(
                "user", user,
                "roles", roles,
                "permissions", roles.contains("admin")
                        ? new String[]{"*:*:*"} : new String[]{}));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String captchaId;
        private String captchaCode;
    }
}
