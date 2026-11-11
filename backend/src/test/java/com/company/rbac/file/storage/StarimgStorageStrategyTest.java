package com.company.rbac.file.storage;

import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.config.StorageSettingsService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarimgStorageStrategyTest {

    private final StorageSettingsService settings = mock(StorageSettingsService.class);
    private final StarimgStorageStrategy strategy = new StarimgStorageStrategy(settings);

    @Test
    void extractsNestedImageUrl() {
        assertEquals(
                "https://img.starimg.vip/example.jpg",
                strategy.parseImageUrl("""
                        {"success":true,"data":{"url":"https://img.starimg.vip/example.jpg"}}
                        """));
    }

    @Test
    void rejectsResponseWithoutImageUrl() {
        assertThrows(BusinessException.class,
                () -> strategy.parseImageUrl("{\"success\":true,\"data\":{}}"));
    }

    @Test
    void requiresApiKeyBeforeSendingRequest() {
        when(settings.starimg()).thenReturn(new StorageSettingsService.StarimgSettings(
                "https://starimg.vip/api/upload", ""));
        assertThrows(BusinessException.class,
                () -> strategy.upload(
                        "images/test.jpg",
                        new ByteArrayInputStream(new byte[]{1}),
                        1,
                        "image/jpeg"));
    }
}
