package com.company.rbac.file.storage;

import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.config.StorageSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StarimgStorageStrategy implements StorageStrategy {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private final StorageSettingsService settings;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public String type() {
        return "starimg";
    }

    @Override
    public String upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType) {
        if (settings.starimg().apiKey().isBlank()) {
            throw new BusinessException("星云图 API Key 未配置，请在系统配置中设置");
        }
        if (!contentType.startsWith("image/")) {
            throw new BusinessException("星云图仅支持图片上传");
        }

        String boundary = "----rbac-" + UUID.randomUUID();
        String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        try {
            HttpRequest request = HttpRequest.newBuilder(uploadEndpoint())
                    .timeout(REQUEST_TIMEOUT)
                    .header("X-API-Key", settings.starimg().apiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.concat(
                            HttpRequest.BodyPublishers.ofByteArray(
                                    header.getBytes(StandardCharsets.UTF_8)),
                            HttpRequest.BodyPublishers.ofByteArray(inputStream.readAllBytes()),
                            HttpRequest.BodyPublishers.ofByteArray(
                                    footer.getBytes(StandardCharsets.UTF_8))))
                    .build();
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("星云图上传失败，HTTP " + response.statusCode()
                        + "：" + responseMessage(response.body()));
            }
            return parseImageUrl(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("星云图上传已中断");
        } catch (Exception exception) {
            throw new BusinessException("星云图上传失败：" + exception.getMessage());
        }
    }

    @Override
    public void download(String objectKey, OutputStream outputStream) {
        try {
            HttpRequest request = HttpRequest.newBuilder(imageUri(objectKey))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new BusinessException("星云图下载失败，HTTP " + response.statusCode());
                }
                body.transferTo(outputStream);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("星云图下载已中断");
        } catch (Exception exception) {
            throw new BusinessException("星云图下载失败：" + exception.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        // The supplied Starimg API does not include a remote delete operation.
        log.warn("星云图未提供删除接口，仅删除本地文件记录");
    }

    @Override
    public String temporaryUrl(String objectKey, Duration duration) {
        return imageUri(objectKey).toString();
    }

    String parseImageUrl(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.path("success").isBoolean() && !root.path("success").asBoolean()) {
                throw new BusinessException("星云图上传失败：" + responseMessage(responseBody));
            }
            JsonNode data = root.path("data");
            String url = firstUrl(data);
            if (url == null) {
                url = firstUrl(root);
            }
            if (url == null || url.length() > 500) {
                throw new BusinessException("星云图上传响应中没有可用的图片地址");
            }
            return imageUri(url).toString();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("星云图上传响应无法解析");
        }
    }

    private String firstUrl(JsonNode node) {
        if (node.isTextual() && node.asText().startsWith("https://")) {
            return node.asText();
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String url = firstUrl(item);
                if (url != null) {
                    return url;
                }
            }
        }
        if (node.isObject()) {
            for (String name : new String[]{"url", "imageUrl", "image_url", "link", "src"}) {
                JsonNode value = node.path(name);
                if (value.isTextual() && !value.asText().isBlank()) {
                    return value.asText();
                }
            }
            for (String name : new String[]{"image", "file", "result", "data"}) {
                String url = firstUrl(node.path(name));
                if (url != null) {
                    return url;
                }
            }
        }
        return null;
    }

    private String responseMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            for (String name : new String[]{"message", "msg", "detail", "error"}) {
                JsonNode value = root.path(name);
                if (value.isTextual()) {
                    return value.asText().substring(0, Math.min(200, value.asText().length()));
                }
            }
        } catch (Exception ignored) {
            // Preserve a short, non-sensitive diagnostic when the server returns plain text.
        }
        return responseBody.substring(0, Math.min(200, responseBody.length()));
    }

    private URI uploadEndpoint() {
        String endpoint = settings.starimg().endpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessException("星云图上传地址未配置");
        }
        URI uri = URI.create(endpoint);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null) {
            throw new BusinessException("星云图上传地址必须是 HTTPS 地址");
        }
        return uri;
    }

    private URI imageUri(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null) {
            throw new BusinessException("星云图返回了无效的 HTTPS 图片地址");
        }
        return uri;
    }
}
