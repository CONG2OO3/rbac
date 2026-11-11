package com.company.rbac.file.storage;

import com.company.rbac.file.config.StorageSettingsService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageStrategySelectionTest {

    @Test
    void switchingActiveStorageDoesNotRemoveHistoricalStrategies() {
        StorageSettingsService settings = mock(StorageSettingsService.class);
        StorageStrategy minio = strategy("minio");
        StorageStrategy aliyun = strategy("aliyun");
        StorageStrategy starimg = strategy("starimg");
        StorageStrategyFactory factory = new StorageStrategyFactory(
                settings,
                List.of(minio, aliyun, starimg));

        when(settings.activeType()).thenReturn("minio", "starimg");
        assertEquals(minio, factory.current());
        assertEquals(starimg, factory.current());
        assertEquals(aliyun, factory.get("aliyun"));
    }

    private StorageStrategy strategy(String type) {
        StorageStrategy strategy = mock(StorageStrategy.class);
        when(strategy.type()).thenReturn(type);
        return strategy;
    }
}
