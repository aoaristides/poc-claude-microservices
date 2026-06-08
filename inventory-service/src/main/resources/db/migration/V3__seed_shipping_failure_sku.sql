-- =====================================================================
-- Seed do SKU sentinela usado no cenário E2E de "falha de entrega".
-- Precisa ter estoque para que a reserva (inventory) e o pagamento (payment)
-- tenham sucesso; a falha ocorre só no shipping, que recusa este SKU.
-- Mantenha em sincronia com shipping.undeliverable-sku do shipping-service.
-- =====================================================================

INSERT INTO stock_item (sku, available_quantity, version) VALUES
    ('SKU-FAIL-SHIP', 1000, 0)
ON CONFLICT (sku) DO NOTHING;
