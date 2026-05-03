ALTER TABLE stock_balances
    ADD CONSTRAINT chk_stock_balances_quantity_non_negative CHECK (quantity >= 0);
