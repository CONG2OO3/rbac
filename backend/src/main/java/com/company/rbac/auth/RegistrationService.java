package com.company.rbac.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.system.entity.SysRole;
import com.company.rbac.system.entity.SysUser;
import com.company.rbac.system.mapper.SysRoleMapper;
import com.company.rbac.system.service.SystemConfigService;
import com.company.rbac.system.service.SystemCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final SystemConfigService configService;
    private final CaptchaService captchaService;
    private final SystemCrudService userService;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegistrationInput input) {
        if (!configService.registrationEnabled()) {
            throw new BusinessException(403, "当前系统未开放用户注册");
        }
        if (input == null || input.username() == null
                || !input.username().matches("[A-Za-z][A-Za-z0-9_]{3,29}")) {
            throw new BusinessException("账号需为 4-30 位字母、数字或下划线，且以字母开头");
        }
        if (input.nickName() == null || input.nickName().isBlank()
                || input.nickName().trim().length() > 30) {
            throw new BusinessException("用户昵称不能为空且不能超过 30 字");
        }
        if (input.password() == null || input.password().length() < 6
                || input.password().length() > 72
                || !input.password().equals(input.confirmPassword())) {
            throw new BusinessException("密码需为 6-72 位，且两次输入一致");
        }
        if (configService.captchaEnabled()) {
            captchaService.verify(input.captchaId(), input.captchaCode());
        }
        SysRole ordinaryRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, "user")
                .eq(SysRole::getStatus, "0"));
        if (ordinaryRole == null) {
            throw new BusinessException("普通用户角色尚未配置，请联系管理员");
        }
        SysUser user = new SysUser();
        user.setUserName(input.username());
        user.setNickName(input.nickName().trim());
        user.setPassword(passwordEncoder.encode(input.password()));
        user.setStatus("0");
        user.setSex("0");
        user.setCreateBy("self-registration");
        userService.save(user);
        userService.setUserRoles(user.getId(), List.of(ordinaryRole.getId()));
    }

    public record RegistrationInput(
            String username,
            String nickName,
            String password,
            String confirmPassword,
            String captchaId,
            String captchaCode) {
    }
}
