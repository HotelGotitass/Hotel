package com.hotel.app;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad manual para generar el hash BCrypt de una contrasena (p. ej. para crear el primer
 * administrador directamente en la base). La contrasena se pasa como argumento; nunca se escribe en el codigo.
 * Uso: java -cp target/classes:... com.hotel.app.HashGen "MiContrasenaSegura"
 */
public class HashGen {
    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Uso: HashGen <contrasena>");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
