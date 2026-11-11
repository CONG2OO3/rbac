CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(30) NOT NULL UNIQUE,
    nick_name VARCHAR(30) NOT NULL,
    email VARCHAR(80) DEFAULT '',
    phone VARCHAR(20) DEFAULT '',
    sex CHAR(1) DEFAULT '0',
    avatar VARCHAR(255) DEFAULT '',
    password VARCHAR(100) NOT NULL,
    status CHAR(1) DEFAULT '0',
    dept_id BIGINT NULL,
    login_ip VARCHAR(128) DEFAULT '',
    login_time DATETIME NULL,
    remark VARCHAR(500) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(30) NOT NULL,
    role_key VARCHAR(100) NOT NULL UNIQUE,
    role_sort INT DEFAULT 0,
    data_scope CHAR(1) DEFAULT '1',
    menu_check_strictly TINYINT DEFAULT 1,
    dept_check_strictly TINYINT DEFAULT 1,
    status CHAR(1) DEFAULT '0',
    remark VARCHAR(500) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    menu_name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    path VARCHAR(200) DEFAULT '',
    component VARCHAR(255) DEFAULT '',
    query VARCHAR(255) DEFAULT '',
    route_name VARCHAR(50) DEFAULT '',
    is_frame TINYINT DEFAULT 0,
    is_cache TINYINT DEFAULT 1,
    menu_type CHAR(1) DEFAULT 'C',
    visible CHAR(1) DEFAULT '0',
    status CHAR(1) DEFAULT '0',
    perms VARCHAR(100) DEFAULT '',
    icon VARCHAR(100) DEFAULT '#',
    remark VARCHAR(500) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0,
    ancestors VARCHAR(500) DEFAULT '',
    dept_name VARCHAR(30) NOT NULL,
    order_num INT DEFAULT 0,
    leader VARCHAR(20) DEFAULT '',
    phone VARCHAR(20) DEFAULT '',
    email VARCHAR(80) DEFAULT '',
    status CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_code VARCHAR(64) NOT NULL UNIQUE,
    post_name VARCHAR(50) NOT NULL,
    post_sort INT DEFAULT 0,
    status CHAR(1) DEFAULT '0',
    remark VARCHAR(500) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value TEXT NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

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

CREATE TABLE IF NOT EXISTS sys_visit_daily (
    visit_date DATE PRIMARY KEY,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_visit_visitor (
    visit_date DATE NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (visit_date, user_id),
    INDEX idx_visit_visitor_user (user_id)
);

CREATE TABLE IF NOT EXISTS biz_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(64) NOT NULL UNIQUE,
    product_name VARCHAR(120) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT '',
    unit VARCHAR(20) NOT NULL DEFAULT '件',
    price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    status CHAR(1) NOT NULL DEFAULT '0',
    description VARCHAR(500) NOT NULL DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_product_name (product_name),
    INDEX idx_product_status (status)
);

CREATE TABLE IF NOT EXISTS biz_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL UNIQUE,
    customer_name VARCHAR(80) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    total_quantity INT NOT NULL DEFAULT 0,
    total_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    remark VARCHAR(500) NOT NULL DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT '',
    update_by VARCHAR(64) DEFAULT '',
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_order_customer (customer_name),
    INDEX idx_order_status (status),
    INDEX idx_order_create_time (create_time)
);

CREATE TABLE IF NOT EXISTS biz_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(14, 2) NOT NULL,
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_product (product_id)
);

