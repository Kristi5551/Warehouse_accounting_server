CREATE TABLE stock_operation_items (
    id BIGSERIAL PRIMARY KEY,
    operation_id BIGINT NOT NULL REFERENCES stock_operations(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity NUMERIC(12, 3) NOT NULL,
    price NUMERIC(12, 2),
    reason VARCHAR(255)
);
