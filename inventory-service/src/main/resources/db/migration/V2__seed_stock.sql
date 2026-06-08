-- =====================================================================
-- Seed de estoque inicial para ambiente de DEV/PoC.
-- SKU-1 a SKU-5 com 1000 unidades cada.
-- O orders-service usa "SKU-1" nos exemplos da saga.
-- =====================================================================

INSERT INTO stock_item (sku, available_quantity, version) VALUES
    ('SKU-1', 1000, 0),
    ('SKU-2', 1000, 0),
    ('SKU-3', 1000, 0),
    ('SKU-4', 1000, 0),
    ('SKU-5', 1000, 0)
ON CONFLICT (sku) DO NOTHING;
