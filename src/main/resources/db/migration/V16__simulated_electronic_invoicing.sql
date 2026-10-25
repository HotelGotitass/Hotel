-- Facturacion electronica SIMULADA (formato peruano: boleta/factura, serie-correlativo, nota de credito)

ALTER TABLE invoices ADD COLUMN document_type VARCHAR(10) NOT NULL DEFAULT 'BOLETA';
ALTER TABLE invoices ADD COLUMN series VARCHAR(4);
ALTER TABLE invoices ADD COLUMN correlative INT;
ALTER TABLE invoices ADD COLUMN customer_doc_type VARCHAR(20);
ALTER TABLE invoices ADD COLUMN customer_doc_number VARCHAR(20);
ALTER TABLE invoices ADD COLUMN customer_name VARCHAR(255);
ALTER TABLE invoices ADD COLUMN customer_address VARCHAR(255);
ALTER TABLE invoices ADD COLUMN sunat_status VARCHAR(20);
ALTER TABLE invoices ADD COLUMN sunat_message VARCHAR(255);
ALTER TABLE invoices ADD COLUMN sunat_hash VARCHAR(64);
ALTER TABLE invoices ADD COLUMN void_reason VARCHAR(255);
ALTER TABLE invoices ADD COLUMN voided_at TIMESTAMP;
ALTER TABLE invoices ADD COLUMN credit_note_series VARCHAR(4);
ALTER TABLE invoices ADD COLUMN credit_note_correlative INT;

-- Correlativos por serie (se bloquea la fila al emitir para no repetir numeros)
CREATE TABLE invoice_series (
    series VARCHAR(4) PRIMARY KEY,
    last_number INT NOT NULL
);
INSERT INTO invoice_series (series, last_number) VALUES ('B001', 0), ('F001', 0), ('BC01', 0), ('FC01', 0);

-- Comprobantes existentes: se numeran como boletas B001 en orden de emision
WITH numbered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM invoices)
UPDATE invoices i SET series = 'B001', correlative = n.rn FROM numbered n WHERE i.id = n.id;

UPDATE invoices i
SET customer_doc_type = g.document_type,
    customer_doc_number = g.document_number,
    customer_name = g.first_name || ' ' || g.last_name,
    customer_address = g.address,
    sunat_status = CASE WHEN i.status = 'VOID' THEN 'ANULADO' ELSE 'ACEPTADO' END,
    sunat_message = 'Comprobante migrado (simulado)',
    sunat_hash = encode(sha256(convert_to(i.series || '-' || i.correlative || '|' || i.total_amount, 'UTF8')), 'base64')
FROM reservations r JOIN guests g ON g.id = r.guest_id
WHERE r.id = i.reservation_id;

UPDATE invoice_series SET last_number = (SELECT COALESCE(MAX(correlative), 0) FROM invoices WHERE series = 'B001')
WHERE series = 'B001';

CREATE UNIQUE INDEX uq_invoices_number ON invoices (series, correlative) WHERE series IS NOT NULL;

-- Una reserva puede tener varios comprobantes en el tiempo (anular boleta y emitir factura),
-- pero solo uno activo. Se elimina el UNIQUE original sobre reservation_id, sea cual sea su nombre.
DO $$
DECLARE c text;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'invoices'::regclass AND con.contype = 'u' AND a.attname = 'reservation_id'
    LOOP
        EXECUTE format('ALTER TABLE invoices DROP CONSTRAINT %I', c);
    END LOOP;
END $$;

CREATE UNIQUE INDEX uq_invoices_active_reservation ON invoices (reservation_id) WHERE status <> 'VOID';
