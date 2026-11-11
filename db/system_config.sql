CREATE TABLE IF NOT EXISTS sys_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value TEXT NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO sys_config (config_key, config_value)
VALUES
    ('system.name', '企业级管理系统'),
    ('system.description', '高效 · 安全 · 可扩展'),
    ('system.logo_file_id', ''),
    ('login.captcha_enabled', 'false'),
    ('register.enabled', 'false');

INSERT IGNORE INTO sys_menu
    (id, menu_name, parent_id, order_num, path, menu_type, visible,
     status, perms, icon, create_by)
VALUES
    (105, '系统配置', 1, 6, 'settings', 'C', '0', '0',
     'system:config:view', 'setting', 'system');
