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
