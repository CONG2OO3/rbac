package com.company.rbac.file.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

public interface StorageStrategy {

    String type();

    String upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType);

    void download(String objectKey, OutputStream outputStream);

    void delete(String objectKey);

    String temporaryUrl(String objectKey, Duration duration);
}
