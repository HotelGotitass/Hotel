# Hotel Gotitas · Backend

API REST del sistema de gestión de **Hotel Gotitas** (una sola sede): reservas web y en recepción, pagos con
Mercado Pago, caja de recepción, facturación electrónica simulada y panel administrativo.

> Frontend: [HotelGotitass/Hotel-frontend](https://github.com/HotelGotitass/Hotel-frontend)

## Stack

- Java 21 · Spring Boot 3.2 (Web, Security, Data JPA, Validation, Mail)
- PostgreSQL · Flyway (migraciones versionadas en `src/main/resources/db/migration`)
- JWT (jjwt) · BCrypt
- OpenPDF + ZXing (comprobantes PDF con QR)
- Testcontainers (tests contra PostgreSQL desechable en Docker)

## Funcionalidades

| Módulo | Qué hace |
|---|---|
| **Reservas** | Búsqueda de disponibilidad por fechas y huéspedes, reserva web (bloqueo de 15 min para pagar) y reserva en recepción, check-in / check-out, consumos, modificación de fechas y cancelación con solicitud de reembolso. |
| **Pagos – Mercado Pago** | Checkout Pro: anticipo (50 %), pago total o saldo. Webhook con validación de firma `x-signature`; el estado y el monto se confirman consultando la API de Mercado Pago. Pagos duplicados se registran una sola vez. |
| **Caja de recepción** | Apertura con fondo inicial (una caja abierta a la vez), cobros en efectivo / tarjeta / transferencia / Yape-Plin, ingresos y egresos manuales, reembolsos en efectivo como egreso, cierre con arqueo (esperado vs contado) e historial de turnos. |
| **Facturación (simulada)** | Boleta `B001` y factura `F001` con correlativo, validación de RUC/DNI, IGV 18 %, importe en letras, QR en formato SUNAT y anulación con nota de crédito `BC01`/`FC01`. **No se conecta a SUNAT ni a ningún OSE/PSE.** |
| **Cuentas y seguridad** | Roles `ADMIN`, `RECEPCIONISTA`, `CLIENT`. Cambio de contraseña propio, restablecimiento por el admin y cuentas deshabilitables: deshabilitar o restablecer invalida al instante todas las sesiones (versión de sesión en el JWT). |
| **Catálogo** | Categorías de habitación con comodidades (íconos), habitaciones, servicios extra, huéspedes, libro de reclamaciones y dashboard. |

## Variables de entorno

Toda la configuración sensible se lee de variables de entorno; **no hay credenciales en el repositorio**.
Copia `.env.example` como referencia (los archivos `.env` están en `.gitignore`).

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | Sí | JDBC de PostgreSQL, p. ej. `jdbc:postgresql://host:5432/hotel` |
| `DB_USERNAME` / `DB_PASSWORD` | Sí | Credenciales de la base |
| `JWT_SECRET` | Sí | Clave HMAC en **Base64** (mínimo 32 bytes). Generar: `openssl rand -base64 48` |
| `JWT_EXPIRATION` | Sí | Vigencia del token en ms (p. ej. `86400000` = 24 h) |
| `CORS_ALLOWED_ORIGINS` | Sí | Orígenes del frontend separados por coma |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Sí | Cuenta SMTP (Gmail con contraseña de aplicación) |
| `MP_ACCESS_TOKEN` | Para pagos | Access token de Mercado Pago (`TEST-...` en sandbox) |
| `MP_WEBHOOK_SECRET` | Recomendada | Clave secreta de webhooks de Mercado Pago (valida `x-signature`) |
| `FRONTEND_URL` | Sí | URL pública del frontend (retorno de Mercado Pago) |
| `BACKEND_URL` | Sí | URL pública **https** del backend (Mercado Pago solo envía webhooks a https) |
| `BILLING_RUC` / `BILLING_NAME` / `BILLING_ADDRESS` | No | Datos del emisor en los comprobantes simulados |
| `PORT` | No | Puerto HTTP (por defecto `8080`) |

## Ejecutar en local

Requisitos: **Java 21** y **Docker**.

```bash
# 1. Base de datos local
docker run -d --name hotel-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=hotel -p 5432:5432 postgres:16-alpine

# 2. Variables de entorno (ver .env.example) y arranque; Flyway crea el esquema
export DB_URL=jdbc:postgresql://localhost:5432/hotel DB_USERNAME=postgres DB_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 48) JWT_EXPIRATION=86400000
export CORS_ALLOWED_ORIGINS=http://localhost:5173 MAIL_USERNAME=... MAIL_PASSWORD=...
./mvnw spring-boot:run
```

**Primer administrador:** no se crea automáticamente. Genera el hash de una contraseña y, ya migrada la base, insértalo:

```bash
./mvnw -q compile
java -cp "target/classes:$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" com.hotel.app.HashGen "TuContraseñaSegura"
# INSERT INTO users (email, password, role, is_verified, active, token_version, created_at)
# VALUES ('admin@tuhotel.com', '<hash>', 'ADMIN', true, true, 0, now());
```

Para recibir webhooks de Mercado Pago en local, expón el backend por https (p. ej. `ngrok http 8080`) y usa esa URL en `BACKEND_URL`.

## Tests

```bash
./mvnw test
```

Los tests de integración levantan su **propia PostgreSQL en Docker** (Testcontainers) y nunca usan una base real
ni envían correos (SMTP local inexistente / `@MockBean JavaMailSender`). Requiere Docker en ejecución.

## API (resumen)

Base: `/api/v1`

| Ruta | Acceso | Descripción |
|---|---|---|
| `POST /auth/login` · `GET /auth/session` | Público / autenticado | Login y latido de sesión |
| `/client/auth/**` | Público | Registro y verificación OTP de clientes |
| `GET /rooms/search` · `GET /categories` · `/public/**` | Público | Disponibilidad y catálogo |
| `/client/reservations/**` | CLIENT | Reservas propias, pago con Mercado Pago, comprobante |
| `/reservations/**` | ADMIN, RECEPCIONISTA | Reservas, check-in/out, cobros en recepción |
| `/cash/**` | ADMIN, RECEPCIONISTA | Caja: apertura, movimientos, cierre, historial |
| `/invoices/**` | ADMIN, RECEPCIONISTA | Emisión, cobro, anulación y PDF de comprobantes |
| `/admin/refunds/**` | ADMIN, RECEPCIONISTA | Reembolsos |
| `/users/**` | ADMIN | Personal, cuentas de clientes, contraseñas y estado |
| `/account/**` | ADMIN, RECEPCIONISTA | Mi cuenta (cambio de contraseña) |
| `POST /payments/webhook` | Mercado Pago | Notificaciones de pago |

## Despliegue

`.github/workflows/deploy-backend.yml`: en cada push a `main` ejecuta los tests y, si pasan, construye la imagen
Docker y la despliega en **Google Cloud Run**. Las variables se toman de los *secrets* del repositorio de GitHub.
Las migraciones Flyway se aplican automáticamente al arrancar.

## Licencia

Apache 2.0 — ver [LICENSE](LICENSE).
