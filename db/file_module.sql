CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_suffix VARCHAR(32) NOT NULL DEFAULT '',
    object_key VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_type VARCHAR(128) NOT NULL DEFAULT 'application/octet-stream',
    file_type VARCHAR(32) NOT NULL,
    storage_type VARCHAR(32) NOT NULL,
    uploader VARCHAR(64) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_file_object_key (object_key),
    INDEX idx_file_type (file_type),
    INDEX idx_file_storage_type (storage_type),
    INDEX idx_file_create_time (create_time)
);
