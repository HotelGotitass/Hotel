CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_res FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);
