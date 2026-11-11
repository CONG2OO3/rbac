INSERT IGNORE INTO sys_user
    (id, user_name, nick_name, password, status, dept_id, create_by)
VALUES
    (1, 'admin', '张三', '123456', '0', 1, 'system');

INSERT IGNORE INTO sys_role
    (id, role_name, role_key, role_sort, status, create_by)
VALUES
    (1, '超级管理员', 'admin', 1, '0', 'system'),
    (2, '普通用户', 'user', 2, '0', 'system');

INSERT IGNORE INTO sys_dept
    (id, parent_id, ancestors, dept_name, order_num, leader, status, create_by)
VALUES
    (1, 0, '0', '总公司', 1, '张三', '0', 'system'),
    (2, 1, '0,1', '技术研发部', 2, '李四', '0', 'system'),
    (3, 1, '0,1', '产品设计部', 3, '王五', '0', 'system'),
    (4, 1, '0,1', '市场运营部', 4, '赵六', '0', 'system');

INSERT IGNORE INTO sys_post
    (id, post_code, post_name, post_sort, status, create_by)
VALUES
    (1, 'RD-001', '高级研发工程师', 1, '0', 'system'),
    (2, 'PM-001', '产品经理', 2, '0', 'system'),
    (3, 'OP-001', '运营专员', 3, '0', 'system');

INSERT IGNORE INTO sys_menu
    (id, menu_name, parent_id, order_num, path, menu_type, visible, status, perms, icon, create_by)
VALUES
    (1, '系统管理', 0, 1, 'system', 'M', '0', '0', '', 'setting', 'system'),
    (100, '用户管理', 1, 1, 'users', 'C', '0', '0', 'system:user:list', 'user', 'system'),
    (101, '角色管理', 1, 2, 'roles', 'C', '0', '0', 'system:role:list', 'peoples', 'system'),
    (102, '菜单管理', 1, 3, 'menus', 'C', '0', '0', 'system:menu:list', 'tree-table', 'system'),
    (103, '部门管理', 1, 4, 'departments', 'C', '0', '0', 'system:dept:list', 'tree', 'system'),
    (104, '岗位管理', 1, 5, 'positions', 'C', '0', '0', 'system:post:list', 'post', 'system'),
    (105, '系统配置', 1, 6, 'settings', 'C', '0', '0', 'system:config:view', 'setting', 'system'),
    (2, '业务管理', 0, 2, 'business', 'M', '0', '0', '', 'briefcase', 'system'),
    (201, '产品管理', 2, 1, 'products', 'C', '0', '0', 'business:product:list', 'product', 'system'),
    (202, '订单管理', 2, 2, 'orders', 'C', '0', '0', 'business:order:list', 'order', 'system'),
    (3, '数据管理', 0, 3, 'data', 'C', '0', '0', 'system:data:list', 'database', 'system'),
    (4, '日志管理', 0, 4, 'logs', 'C', '0', '0', 'system:log:list', 'log', 'system'),
    (5, '文件管理', 0, 5, 'files', 'M', '0', '0', '', 'folder', 'system'),
    (501, '图片管理', 5, 1, 'images', 'C', '0', '0', 'system:image:list', 'image', 'system'),
    (502, '附件管理', 5, 2, 'attachments', 'C', '0', '0', 'system:attachment:list', 'paperclip', 'system'),
    (503, '视频管理', 5, 3, 'videos', 'C', '0', '0', 'system:video:list', 'video', 'system'),
    (6, '系统监控', 0, 6, 'monitor', 'M', '0', '0', '', 'monitor', 'system'),
    (601, '服务监控', 6, 1, 'server', 'C', '0', '0', 'monitor:server:list', 'server', 'system'),
    (602, '在线用户', 6, 2, 'online', 'C', '0', '0', 'monitor:online:list', 'online', 'system');

UPDATE sys_menu
SET menu_type = 'M', perms = ''
WHERE id = 6 AND path = 'monitor';

UPDATE sys_menu
SET menu_type = 'M', perms = ''
WHERE id = 2 AND path = 'business';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

INSERT IGNORE INTO sys_config (config_key, config_value)
VALUES
    ('system.name', '企业级管理系统'),
    ('system.description', '高效 · 安全 · 可扩展'),
    ('system.logo_file_id', ''),
    ('login.captcha_enabled', 'false'),
    ('register.enabled', 'false');
