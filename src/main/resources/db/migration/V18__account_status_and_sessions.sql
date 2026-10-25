-- Cuentas deshabilitables por el administrador
ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
-- Version de sesion: viaja en el JWT (claim "ver"); al incrementarla todos los tokens anteriores dejan de valer
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
