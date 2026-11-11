package com.company.rbac.file.storage;

import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.config.StorageSettingsService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MinioStorageStrategy implements StorageStrategy {

    private final StorageSettingsService settings;

    @Override
    public String type() {
        return "minio";
    }

    @Override
    public String upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType) {
        try {
            MinioClient client = client();
            ensureBucket(client);
            client.putObject(PutObjectArgs.builder()
                    .bucket(config().bucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception exception) {
            throw new BusinessException("MinIO 上传失败：" + exception.getMessage());
        }
    }

    @Override
    public void download(String objectKey, OutputStream outputStream) {
        try (InputStream inputStream = client().getObject(GetObjectArgs.builder()
                .bucket(config().bucket())
                .object(objectKey)
                .build())) {
            inputStream.transferTo(outputStream);
        } catch (Exception exception) {
            throw new BusinessException("MinIO 下载失败：" + exception.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client().removeObject(RemoveObjectArgs.builder()
                    .bucket(config().bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException("MinIO 删除失败：" + exception.getMessage());
        }
    }

    @Override
    public String temporaryUrl(String objectKey, Duration duration) {
        try {
            return client().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(config().bucket())
                    .object(objectKey)
                    .expiry((int) duration.toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException("MinIO 地址生成失败：" + exception.getMessage());
        }
    }

    private MinioClient client() {
        validate();
        return MinioClient.builder()
                .endpoint(config().endpoint())
                .credentials(config().accessKey(), config().secretKey())
                .build();
    }

    private void ensureBucket(MinioClient client) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(config().bucket())
                .build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(config().bucket())
                    .build());
        }
    }

    private StorageSettingsService.MinioSettings config() {
        return settings.minio();
    }

    private void validate() {
        if (isBlank(config().endpoint())
                || isBlank(config().bucket())
                || isBlank(config().accessKey())
                || isBlank(config().secretKey())) {
            throw new BusinessException("MinIO 配置不完整，请到系统配置中设置");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
