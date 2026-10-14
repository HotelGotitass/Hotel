CREATE TABLE complaints (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    phone VARCHAR(50),
    complaint_type VARCHAR(20) NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    detail TEXT NOT NULL,
    consumer_request TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL
);
