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
