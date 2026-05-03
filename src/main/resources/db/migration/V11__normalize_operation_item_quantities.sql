-- Ранее ISSUE/WRITE_OFF могли сохранять отрицательные quantity; семантика теперь всегда положительная величина.
UPDATE stock_operation_items
SET quantity = ABS(quantity)
WHERE quantity < 0;
