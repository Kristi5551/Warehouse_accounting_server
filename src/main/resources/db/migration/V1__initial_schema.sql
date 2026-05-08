CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    article VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    unit VARCHAR(50) NOT NULL,
    purchase_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    sale_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    min_stock NUMERIC(12, 3) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_balances (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    quantity NUMERIC(12, 3) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, warehouse_id)
);

CREATE TABLE stock_operations (
    id BIGSERIAL PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comment TEXT
);

CREATE TABLE stock_operation_items (
    id BIGSERIAL PRIMARY KEY,
    operation_id BIGINT NOT NULL REFERENCES stock_operations(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity NUMERIC(12, 3) NOT NULL,
    price NUMERIC(12, 2),
    reason VARCHAR(255)
);

INSERT INTO warehouses (name, address, is_active)
VALUES ('Главный склад', 'г. Москва, ул. Складская, 1', TRUE);

INSERT INTO categories (name, description, is_active)
VALUES
    ('Напитки', 'Безалкогольные и соки', TRUE),
    ('Бакалея', 'Крупы, макароны, консервы', TRUE),
    ('Хозтовары', 'Бытовая химия и расходники', TRUE);

INSERT INTO products (article, name, category_id, unit, purchase_price, sale_price, min_stock, is_active)
VALUES
    ('ART-1001', 'Вода минеральная 0.5л', 1, 'шт', 15.50, 29.90, 50.000, TRUE),
    ('ART-1002', 'Сок апельсиновый 1л', 1, 'шт', 45.00, 79.00, 30.000, TRUE),
    ('ART-2001', 'Гречка 900г', 2, 'шт', 85.00, 119.00, 20.000, TRUE),
    ('ART-3001', 'Перчатки латекс L', 3, 'уп', 120.00, 199.00, 15.000, TRUE);

INSERT INTO stock_balances (product_id, warehouse_id, quantity)
VALUES
    (1, 1, 100.000),
    (2, 1, 40.000),
    (3, 1, 25.000),
    (4, 1, 18.000);

UPDATE warehouses w
SET
    name = 'Основной склад',
    updated_at = CURRENT_TIMESTAMP
WHERE w.name = 'Главный склад'
  AND NOT EXISTS (
      SELECT 1
      FROM warehouses o
      WHERE o.name = 'Основной склад'
  );

ALTER TABLE stock_balances
    ADD CONSTRAINT chk_stock_balances_quantity_non_negative CHECK (quantity >= 0);

UPDATE stock_operation_items
SET quantity = ABS(quantity)
WHERE quantity < 0;
