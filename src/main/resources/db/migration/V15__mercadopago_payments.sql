-- Mercado Pago: guardar la preferencia creada y permitir estados mas largos
ALTER TABLE payment_attempts ADD COLUMN preference_id VARCHAR(100);
ALTER TABLE payment_attempts ALTER COLUMN status TYPE VARCHAR(30);

-- Idempotencia del webhook: un mismo pago externo solo se registra una vez
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_reference ON payments (reference) WHERE reference IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_reservation ON payments (reservation_id);
