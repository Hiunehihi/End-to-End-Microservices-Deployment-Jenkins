CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    sku_code VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_sku_code
    ON inventory (sku_code);

CREATE TABLE IF NOT EXISTS processed_inventory_events (
    order_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
