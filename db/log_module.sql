CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(64) NOT NULL DEFAULT '',
    ip_address VARCHAR(128) NOT NULL DEFAULT '',
    browser VARCHAR(64) NOT NULL DEFAULT '',
    operating_system VARCHAR(64) NOT NULL DEFAULT '',
    status CHAR(1) NOT NULL DEFAULT '0',
    message VARCHAR(255) NOT NULL DEFAULT '',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_login_user_name (user_name),
    INDEX idx_login_status (status),
    INDEX idx_login_time (login_time)
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(64) NOT NULL DEFAULT '',
    operation VARCHAR(128) NOT NULL DEFAULT '',
    method VARCHAR(500) NOT NULL DEFAULT '',
    request_method VARCHAR(16) NOT NULL DEFAULT '',
    operator_name VARCHAR(64) NOT NULL DEFAULT '',
    request_url VARCHAR(500) NOT NULL DEFAULT '',
    ip_address VARCHAR(128) NOT NULL DEFAULT '',
    request_param TEXT,
    result_data TEXT,
    status CHAR(1) NOT NULL DEFAULT '0',
    error_message TEXT,
    cost_time BIGINT NOT NULL DEFAULT 0,
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_module (module),
    INDEX idx_operation_operator (operator_name),
    INDEX idx_operation_status (status),
    INDEX idx_operation_time (operation_time)
);

