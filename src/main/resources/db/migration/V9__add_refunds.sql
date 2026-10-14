CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_by VARCHAR(100),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refund_res FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);
