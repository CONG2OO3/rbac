package com.company.rbac.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.config.StorageSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class AliyunOssStorageStrategy implements StorageStrategy {

    private final StorageSettingsService settings;

    @Override
    public String type() {
        return "aliyun";
    }

    @Override
    public String upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType) {
        OSS client = client();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            metadata.setContentType(contentType);
            client.putObject(config().bucket(), objectKey, inputStream, metadata);
            return objectKey;
        } catch (Exception exception) {
            throw new BusinessException("阿里云 OSS 上传失败：" + exception.getMessage());
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void download(String objectKey, OutputStream outputStream) {
        OSS client = client();
        try (InputStream inputStream = client.getObject(
                new GetObjectRequest(config().bucket(), objectKey))
                .getObjectContent()) {
            inputStream.transferTo(outputStream);
        } catch (Exception exception) {
            throw new BusinessException("阿里云 OSS 下载失败：" + exception.getMessage());
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void delete(String objectKey) {
        OSS client = client();
        try {
            client.deleteObject(config().bucket(), objectKey);
        } catch (Exception exception) {
            throw new BusinessException("阿里云 OSS 删除失败：" + exception.getMessage());
        } finally {
            client.shutdown();
        }
    }

    @Override
    public String temporaryUrl(String objectKey, Duration duration) {
        OSS client = client();
        try {
            Date expiration = new Date(System.currentTimeMillis() + duration.toMillis());
            return client.generatePresignedUrl(
                    config().bucket(),
                    objectKey,
                    expiration)
                    .toString();
        } catch (Exception exception) {
            throw new BusinessException("阿里云 OSS 地址生成失败：" + exception.getMessage());
        } finally {
            client.shutdown();
        }
    }

    private OSS client() {
        validate();
        return new OSSClientBuilder().build(
                endpoint(),
                config().accessKeyId(),
                config().accessKeySecret());
    }

    private StorageSettingsService.AliyunSettings config() {
        return settings.aliyun();
    }

    private String endpoint() {
        String endpoint = config().endpoint();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }

    private void validate() {
        if (isBlank(config().endpoint())
                || isBlank(config().bucket())
                || isBlank(config().accessKeyId())
                || isBlank(config().accessKeySecret())) {
            throw new BusinessException("阿里云 OSS 配置不完整，请到系统配置中设置");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
