-- Como se devolvio el dinero (efectivo de caja, transferencia, Yape/Plin, extorno, Mercado Pago)
ALTER TABLE refunds ADD COLUMN refund_method VARCHAR(20);
ALTER TABLE refunds ADD COLUMN refund_reference VARCHAR(100);
