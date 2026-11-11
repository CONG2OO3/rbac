package com.company.rbac.file.storage;

import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.config.StorageSettingsService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StorageStrategyFactory {

    private final StorageSettingsService settings;
    private final Map<String, StorageStrategy> strategies;

    public StorageStrategyFactory(
            StorageSettingsService settings,
            List<StorageStrategy> strategies) {
        this.settings = settings;
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.type().toLowerCase(Locale.ROOT),
                        Function.identity()));
    }

    public StorageStrategy current() {
        return get(settings.activeType());
    }

    public StorageStrategy get(String type) {
        String key = type == null
                ? settings.activeType()
                : type.toLowerCase(Locale.ROOT);
        StorageStrategy strategy = strategies.get(key);
        if (strategy == null) {
            throw new BusinessException(
                    "文件使用 " + key + " 存储，当前未启用该存储类型");
        }
        return strategy;
    }
}
