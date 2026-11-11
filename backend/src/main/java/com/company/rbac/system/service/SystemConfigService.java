package com.company.rbac.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.entity.SysFile;
import com.company.rbac.file.service.FileService;
import com.company.rbac.file.storage.StorageStrategyFactory;
import com.company.rbac.system.entity.SysConfig;
import com.company.rbac.system.mapper.SysConfigMapper;
import com.company.rbac.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private static final String SYSTEM_NAME = "system.name";
    private static final String SYSTEM_DESCRIPTION = "system.description";
    private static final String SYSTEM_LOGO_FILE_ID = "system.logo_file_id";
    private static final String CAPTCHA_ENABLED = "login.captcha_enabled";
    private static final String REGISTRATION_ENABLED = "register.enabled";

    private final SysConfigMapper configMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final FileService fileService;
    private final StorageStrategyFactory storageStrategyFactory;

    public ConfigView publicConfig() {
        Map<String, String> values = new HashMap<>();
        configMapper.selectList(null).forEach(config ->
                values.put(config.getConfigKey(), config.getConfigValue()));
        return new ConfigView(
                values.getOrDefault(SYSTEM_NAME, "企业级管理系统"),
                values.getOrDefault(SYSTEM_DESCRIPTION, "高效 · 安全 · 可扩展"),
                logoUrl(values.get(SYSTEM_LOGO_FILE_ID)),
                Boolean.parseBoolean(values.getOrDefault(CAPTCHA_ENABLED, "false")),
                Boolean.parseBoolean(values.getOrDefault(REGISTRATION_ENABLED, "false")));
    }

    public boolean captchaEnabled() {
        return flag(CAPTCHA_ENABLED);
    }

    public boolean registrationEnabled() {
        return flag(REGISTRATION_ENABLED);
    }

    @Transactional
    public ConfigView updateSystem(SystemInput input) {
        checkAdmin();
        if (input == null || input.systemName() == null
                || input.systemName().isBlank()
                || input.systemName().trim().length() > 60) {
            throw new BusinessException("系统名称不能为空，且不能超过 60 字");
        }
        if (input.systemDescription() != null
                && input.systemDescription().trim().length() > 500) {
            throw new BusinessException("系统描述不能超过 500 字");
        }
        put(SYSTEM_NAME, input.systemName().trim());
        put(SYSTEM_DESCRIPTION, input.systemDescription() == null
                ? "" : input.systemDescription().trim());
        return publicConfig();
    }

    @Transactional
    public ConfigView updateLogin(BooleanInput input) {
        checkAdmin();
        if (input == null || input.enabled() == null) {
            throw new BusinessException("请选择是否开启验证码");
        }
        put(CAPTCHA_ENABLED, input.enabled().toString());
        return publicConfig();
    }

    @Transactional
    public ConfigView updateRegistration(BooleanInput input) {
        checkAdmin();
        if (input == null || input.enabled() == null) {
            throw new BusinessException("请选择是否允许注册");
        }
        put(REGISTRATION_ENABLED, input.enabled().toString());
        return publicConfig();
    }

    @Transactional
    public ConfigView uploadLogo(MultipartFile file) {
        checkAdmin();
        if (file == null || file.isEmpty() || file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("请选择不超过 2MB 的图片");
        }
        SysFile uploaded = fileService.upload(file, "image");
        put(SYSTEM_LOGO_FILE_ID, uploaded.getId().toString());
        return publicConfig();
    }

    @Transactional
    public ConfigView clearLogo() {
        checkAdmin();
        put(SYSTEM_LOGO_FILE_ID, "");
        return publicConfig();
    }

    private boolean flag(String key) {
        SysConfig config = configMapper.selectById(key);
        return config != null && Boolean.parseBoolean(config.getConfigValue());
    }

    private void put(String key, String value) {
        SysConfig config = configMapper.selectById(key);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        } else {
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        }
    }

    private String logoUrl(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        try {
            SysFile file = fileService.get(Long.valueOf(id));
            return storageStrategyFactory.get(file.getStorageType())
                    .temporaryUrl(file.getObjectKey(), Duration.ofHours(1));
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private void checkAdmin() {
        if (!StpUtil.isLogin()
                || !userRoleMapper.selectRoleKeys(StpUtil.getLoginIdAsLong())
                        .contains("admin")) {
            throw new BusinessException(403, "只有超级管理员可以修改系统配置");
        }
    }

    public record ConfigView(
            String systemName,
            String systemDescription,
            String logoUrl,
            boolean captchaEnabled,
            boolean registrationEnabled) {
    }

    public record SystemInput(String systemName, String systemDescription) {
    }

    public record BooleanInput(Boolean enabled) {
    }
}
