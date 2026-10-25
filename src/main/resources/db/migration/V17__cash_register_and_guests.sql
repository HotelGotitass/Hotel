-- Numero de huespedes de la reserva (las existentes quedan con 1)
ALTER TABLE reservations ADD COLUMN guests INT NOT NULL DEFAULT 1;

-- Caja de recepcion: una sesion (turno) abierta a la vez
CREATE TABLE cash_sessions (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(10) NOT NULL,
    opened_by VARCHAR(255) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    opening_amount NUMERIC(12,2) NOT NULL,
    closed_by VARCHAR(255),
    closed_at TIMESTAMP,
    expected_cash NUMERIC(12,2),
    counted_amount NUMERIC(12,2),
    difference NUMERIC(12,2),
    notes VARCHAR(500)
);
-- Garantiza a nivel de BD que nunca haya dos cajas abiertas
CREATE UNIQUE INDEX uq_cash_sessions_one_open ON cash_sessions (status) WHERE status = 'OPEN';

CREATE TABLE cash_movements (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES cash_sessions(id),
    type VARCHAR(10) NOT NULL,
    method VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    concept VARCHAR(255) NOT NULL,
    payment_id BIGINT REFERENCES payments(id),
    reservation_id BIGINT REFERENCES reservations(id),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cash_movements_session ON cash_movements (session_id);
