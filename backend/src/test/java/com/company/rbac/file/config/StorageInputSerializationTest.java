package com.company.rbac.file.config;

import com.company.rbac.file.config.StorageSettingsService.StorageInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StorageInputSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsSecretsButNeverSerializesThem() throws Exception {
        StorageInput input = objectMapper.readValue("""
                {"type":"starimg","endpoint":"https://starimg.vip/api/upload",
                 "apiKey":"private-api-key","secretKey":"private-secret",
                 "accessKeySecret":"private-oss-secret"}
                """, StorageInput.class);

        assertEquals("private-api-key", input.apiKey());
        String serialized = objectMapper.writeValueAsString(input);
        assertFalse(serialized.contains("private-api-key"));
        assertFalse(serialized.contains("private-secret"));
        assertFalse(serialized.contains("private-oss-secret"));
    }
}
