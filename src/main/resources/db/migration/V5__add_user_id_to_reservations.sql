ALTER TABLE reservations ADD COLUMN user_id BIGINT;
ALTER TABLE reservations ADD CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users(id);
