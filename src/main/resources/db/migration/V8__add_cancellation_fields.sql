ALTER TABLE reservations ADD COLUMN cancelled_by VARCHAR(20);
ALTER TABLE reservations ADD COLUMN cancellation_reason VARCHAR(255);
