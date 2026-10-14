# Sistema de Gestión Hotelera - Sprint 1

Este es el backend del sistema de gestión hotelera (Sprint 1), construido con Java 21, Spring Boot 3.3.x, PostgreSQL y Spring Security con JWT.

## Requisitos Previos

- **Java 21** instalado en tu sistema.
- **Maven** (o usar el wrapper de maven incluido).
- **PostgreSQL** corriendo en `localhost:5432`.
- Una base de datos llamada `hoteldb` creada en PostgreSQL.

## Configuración de la Base de Datos

El sistema está configurado para conectarse a PostgreSQL con las siguientes credenciales por defecto (ver `src/main/resources/application.properties`):
- **URL**: `jdbc:postgresql://localhost:5432/hoteldb`
- **Usuario**: `postgres`
- **Contraseña**: `blae`

Si tus credenciales son distintas, modifica el archivo `application.properties`.

*Nota: La propiedad `spring.jpa.hibernate.ddl-auto=update` está habilitada, por lo que las tablas se crearán automáticamente al arrancar la aplicación.*

## Cómo levantar el proyecto localmente

1. Abre una terminal en la raíz del proyecto.
2. Compila el proyecto usando Maven:
   ```bash
   ./mvnw clean install
   ```
   *(Si usas Windows sin bash, ejecuta `mvnw.cmd clean install`)*

3. Ejecuta la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(O `mvnw.cmd spring-boot:run` en Windows)*

4. El servidor estará escuchando en el puerto `8080`.

## Endpoints Principales

- **POST** `/api/v1/auth/login`: Iniciar sesión y obtener JWT.
- **POST** `/api/v1/auth/register`: Registrar un nuevo usuario (solo rol ADMIN).
- **CRUD** `/api/v1/categories`: Gestionar las categorías de habitación.
- **CRUD** `/api/v1/rooms`: Gestionar las habitaciones.
- **PATCH** `/api/v1/rooms/{id}/status`: Actualizar el estado de una habitación (ADMIN o RECEPCIONISTA).
