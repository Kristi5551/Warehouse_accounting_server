-- Единое имя основного склада по спецификации приложения.
-- Идемпотентно: если «Основной склад» уже есть (ручное переименование / дубликат),
-- не трогаем «Главный склад», чтобы не нарушить warehouses_name_key.
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
