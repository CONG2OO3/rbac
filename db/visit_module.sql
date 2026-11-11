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
