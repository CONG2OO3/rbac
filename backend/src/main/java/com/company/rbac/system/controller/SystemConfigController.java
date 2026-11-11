package com.company.rbac.system.controller;

import com.company.rbac.common.api.Result;
import com.company.rbac.file.config.StorageSettingsService;
import com.company.rbac.file.config.StorageSettingsService.StorageInput;
import com.company.rbac.file.config.StorageSettingsService.StorageView;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.service.SystemConfigService;
import com.company.rbac.system.service.SystemConfigService.BooleanInput;
import com.company.rbac.system.service.SystemConfigService.ConfigView;
import com.company.rbac.system.service.SystemConfigService.SystemInput;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService service;
    private final StorageSettingsService storageSettingsService;

    @GetMapping("/public/config")
    public Result<ConfigView> publicConfig() {
        return Result.success(service.publicConfig());
    }

    @GetMapping("/system/config")
    public Result<ConfigView> config() {
        return Result.success(service.publicConfig());
    }

    @PutMapping("/system/config/system")
    @OperationLog(module = "系统配置", operation = "修改系统信息")
    public Result<ConfigView> updateSystem(@RequestBody SystemInput input) {
        return Result.success(service.updateSystem(input));
    }

    @PutMapping("/system/config/login")
    @OperationLog(module = "系统配置", operation = "修改登录配置")
    public Result<ConfigView> updateLogin(@RequestBody BooleanInput input) {
        return Result.success(service.updateLogin(input));
    }

    @PutMapping("/system/config/register")
    @OperationLog(module = "系统配置", operation = "修改注册配置")
    public Result<ConfigView> updateRegistration(@RequestBody BooleanInput input) {
        return Result.success(service.updateRegistration(input));
    }

    @PostMapping("/system/config/logo")
    @OperationLog(module = "系统配置", operation = "上传系统 Logo")
    public Result<ConfigView> uploadLogo(@RequestParam("file") MultipartFile file) {
        return Result.success(service.uploadLogo(file));
    }

    @DeleteMapping("/system/config/logo")
    @OperationLog(module = "系统配置", operation = "移除系统 Logo")
    public Result<ConfigView> clearLogo() {
        return Result.success(service.clearLogo());
    }

    @GetMapping("/system/config/storage")
    public Result<StorageView> storageConfig() {
        return Result.success(storageSettingsService.view());
    }

    @PutMapping("/system/config/storage")
    @OperationLog(module = "系统配置", operation = "修改存储配置")
    public Result<StorageView> updateStorage(@RequestBody StorageInput input) {
        return Result.success(storageSettingsService.update(input));
    }
}
