ALTER TABLE reservations 
ADD COLUMN booking_code VARCHAR(50) UNIQUE,
ADD COLUMN origin_channel VARCHAR(20) DEFAULT 'RECEPTION',
ADD COLUMN modification_count INT DEFAULT 0;

-- Set default values for existing reservations
UPDATE reservations SET booking_code = 'HTL-' || id || '-' || substring(md5(random()::text) from 1 for 4) WHERE booking_code IS NULL;

-- Make booking_code NOT NULL after populating
ALTER TABLE reservations ALTER COLUMN booking_code SET NOT NULL;
