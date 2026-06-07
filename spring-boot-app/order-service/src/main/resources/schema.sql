CREATE TABLE IF NOT EXISTS t_orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(255),
    status VARCHAR(50),
    user_id VARCHAR(128),
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    created_at TIMESTAMP
);

ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS user_id VARCHAR(128);
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);
ALTER TABLE t_orders ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS t_orders_order_line_items_list (
    order_id BIGINT NOT NULL,
    order_line_items_list_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS t_order_line_items (
    id SERIAL PRIMARY KEY,
    sku_code VARCHAR(255),
    price DECIMAL,
    quantity INTEGER
);
