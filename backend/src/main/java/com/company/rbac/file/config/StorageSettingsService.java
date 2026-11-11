package com.company.rbac.file.config;

import cn.dev33.satoken.stp.StpUtil;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.system.entity.SysConfig;
import com.company.rbac.system.mapper.SysConfigMapper;
import com.company.rbac.system.mapper.SysUserRoleMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StorageSettingsService {

    private static final String PREFIX = "storage.";
    private static final Set<String> TYPES = Set.of("aliyun", "minio", "starimg");
    private static final String DEFAULT_STARIMG_ENDPOINT = "https://starimg.vip/api/upload";

    private final SysConfigMapper configMapper;
    private final SysUserRoleMapper userRoleMapper;

    public String activeType() {
        String type = value("active_type");
        return type.isBlank() ? "minio" : type;
    }

    public AliyunSettings aliyun() {
        return new AliyunSettings(
                value("aliyun.endpoint"),
                value("aliyun.bucket"),
                value("aliyun.access_key_id"),
                value("aliyun.access_key_secret"));
    }

    public MinioSettings minio() {
        return new MinioSettings(
                value("minio.endpoint"),
                value("minio.bucket"),
                value("minio.access_key"),
                value("minio.secret_key"));
    }

    public StarimgSettings starimg() {
        String endpoint = value("starimg.endpoint");
        return new StarimgSettings(
                endpoint.isBlank() ? DEFAULT_STARIMG_ENDPOINT : endpoint,
                value("starimg.api_key"));
    }

    public StorageView view() {
        checkAdmin();
        AliyunSettings aliyun = aliyun();
        MinioSettings minio = minio();
        StarimgSettings starimg = starimg();
        return new StorageView(
                activeType(),
                new AliyunView(
                        aliyun.endpoint(),
                        aliyun.bucket(),
                        aliyun.accessKeyId(),
                        !aliyun.accessKeySecret().isBlank()),
                new MinioView(
                        minio.endpoint(),
                        minio.bucket(),
                        minio.accessKey(),
                        !minio.secretKey().isBlank()),
                new StarimgView(
                        starimg.endpoint(),
                        !starimg.apiKey().isBlank()));
    }

    @Transactional
    public StorageView update(StorageInput input) {
        checkAdmin();
        if (input == null || !TYPES.contains(input.type())) {
            throw new BusinessException("请选择有效的存储类型");
        }
        switch (input.type()) {
            case "aliyun" -> updateAliyun(input);
            case "minio" -> updateMinio(input);
            case "starimg" -> updateStarimg(input);
            default -> throw new BusinessException("不支持的存储类型");
        }
        put("active_type", input.type());
        return view();
    }

    private void updateAliyun(StorageInput input) {
        AliyunSettings old = aliyun();
        String endpoint = required(input.endpoint(), "OSS Endpoint", 255);
        String bucket = required(input.bucket(), "OSS Bucket", 128);
        String accessKeyId = required(input.accessKeyId(), "AccessKey ID", 128);
        String secret = retainedSecret(input.accessKeySecret(), old.accessKeySecret(), "AccessKey Secret");
        validateEndpoint(endpoint, false);
        put("aliyun.endpoint", endpoint);
        put("aliyun.bucket", bucket);
        put("aliyun.access_key_id", accessKeyId);
        if (!secret.equals(old.accessKeySecret())) {
            put("aliyun.access_key_secret", secret);
        }
    }

    private void updateMinio(StorageInput input) {
        MinioSettings old = minio();
        String endpoint = required(input.endpoint(), "MinIO Endpoint", 255);
        String bucket = required(input.bucket(), "MinIO Bucket", 128);
        String accessKey = required(input.accessKey(), "Access Key", 128);
        String secret = retainedSecret(input.secretKey(), old.secretKey(), "Secret Key");
        validateEndpoint(endpoint, false);
        put("minio.endpoint", endpoint);
        put("minio.bucket", bucket);
        put("minio.access_key", accessKey);
        if (!secret.equals(old.secretKey())) {
            put("minio.secret_key", secret);
        }
    }

    private void updateStarimg(StorageInput input) {
        StarimgSettings old = starimg();
        String endpoint = required(input.endpoint(), "星云图上传地址", 255);
        String apiKey = retainedSecret(input.apiKey(), old.apiKey(), "星云图 API Key");
        validateEndpoint(endpoint, true);
        put("starimg.endpoint", endpoint);
        if (!apiKey.equals(old.apiKey())) {
            put("starimg.api_key", apiKey);
        }
    }

    private String value(String key) {
        SysConfig config = configMapper.selectById(PREFIX + key);
        return config == null || config.getConfigValue() == null
                ? ""
                : config.getConfigValue();
    }

    private void put(String key, String value) {
        String fullKey = PREFIX + key;
        SysConfig config = configMapper.selectById(fullKey);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(fullKey);
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        } else {
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        }
    }

    private String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new BusinessException(label + "不能为空，且不能超过 " + maxLength + " 字");
        }
        return value.trim();
    }

    private String retainedSecret(String candidate, String current, String label) {
        String secret = candidate == null || candidate.isBlank() ? current : candidate.trim();
        if (secret.isBlank() || secret.length() > 500) {
            throw new BusinessException(label + "不能为空，且不能超过 500 字");
        }
        return secret;
    }

    private void validateEndpoint(String endpoint, boolean httpsOnly) {
        try {
            URI uri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
            if (uri.getHost() == null
                    || (httpsOnly && !"https".equalsIgnoreCase(uri.getScheme()))
                    || (!httpsOnly && !"https".equalsIgnoreCase(uri.getScheme())
                            && !"http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(httpsOnly
                    ? "上传地址必须是有效的 HTTPS 地址"
                    : "Endpoint 必须是有效的 HTTP/HTTPS 地址");
        }
    }

    private void checkAdmin() {
        if (!StpUtil.isLogin()
                || !userRoleMapper.selectRoleKeys(StpUtil.getLoginIdAsLong())
                        .contains("admin")) {
            throw new BusinessException(403, "只有超级管理员可以修改存储配置");
        }
    }

    public record AliyunSettings(String endpoint, String bucket, String accessKeyId, String accessKeySecret) {
    }

    public record MinioSettings(String endpoint, String bucket, String accessKey, String secretKey) {
    }

    public record StarimgSettings(String endpoint, String apiKey) {
    }

    public record AliyunView(String endpoint, String bucket, String accessKeyId, boolean secretConfigured) {
    }

    public record MinioView(String endpoint, String bucket, String accessKey, boolean secretConfigured) {
    }

    public record StarimgView(String endpoint, boolean apiKeyConfigured) {
    }

    public record StorageView(String activeType, AliyunView aliyun, MinioView minio, StarimgView starimg) {
    }

    public record StorageInput(
            String type,
            String endpoint,
            String bucket,
            String accessKeyId,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String accessKeySecret,
            String accessKey,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String secretKey,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String apiKey) {
    }
}
