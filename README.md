# Hotel Gotitas · Backend

API REST del sistema de gestión de **Hotel Gotitas** (una sola sede): reservas web y en recepción, pagos con
Mercado Pago, caja de recepción, facturación electrónica simulada y panel administrativo.

> Frontend: [HotelGotitass/Hotel-frontend](https://github.com/HotelGotitass/Hotel-frontend)

## Contenido

- [Stack](#stack) · [Arquitectura](#arquitectura) · [Funcionalidades](#funcionalidades) · [Flujos](#flujos)
- [Modelo de datos](#modelo-de-datos) · [Variables de entorno](#variables-de-entorno) · [Ejecutar en local](#ejecutar-en-local)
- [Tests](#tests) · [API](#api) · [Migraciones](#migraciones) · [Despliegue](#despliegue)

## Stack

- Java 21 · Spring Boot 3.2 (Web, Security, Data JPA, Validation, Mail)
- PostgreSQL · Flyway · JWT (jjwt) · BCrypt
- OpenPDF + ZXing (comprobantes PDF con QR) · Testcontainers (tests)

## Arquitectura

```mermaid
flowchart LR
    U["Navegador<br/>(clientes y personal)"] --> FE["Frontend React<br/>Vercel"]
    FE -->|"REST + JWT"| API["API Spring Boot<br/>Google Cloud Run"]
    API -->|"JPA / Flyway"| DB[("PostgreSQL")]
    API -->|"Crear preferencia<br/>Consultar pago"| MP["Mercado Pago API"]
    MP -->|"Webhook firmado<br/>POST /payments/webhook"| API
    U -.->|"Checkout Pro"| MP
    API -->|"SMTP"| MAIL["Gmail"]
    GH["GitHub Actions"] -->|"tests + imagen Docker"| API
```

Organización interna por capas:

```mermaid
flowchart TB
    REQ["Petición HTTP"] --> SEC["JwtAuthFilter<br/>valida token, cuenta activa y versión de sesión"]
    SEC --> CTRL["Controllers<br/>@PreAuthorize por rol"]
    CTRL --> SRV["Services<br/>reglas de negocio + @Transactional"]
    SRV --> REPO["Repositories<br/>Spring Data JPA"]
    SRV --> INT["Integraciones<br/>MercadoPagoClient · SimulatedSunatService · EmailService"]
    REPO --> DB[("PostgreSQL")]
    CTRL -. "errores" .-> EX["GlobalExceptionHandler<br/>400 / 401 / 403 / 404 / 409"]
```

## Funcionalidades

| Módulo | Qué hace |
|---|---|
| **Reservas** | Disponibilidad por fechas y huéspedes, reserva web (15 min para pagar) y en recepción, check-in/out, consumos, cambio de fechas y cancelación con solicitud de reembolso. |
| **Mercado Pago** | Checkout Pro: anticipo (50 %), total o saldo. Webhook con firma `x-signature`; estado y monto se confirman en la API de Mercado Pago; pagos duplicados se registran una vez. |
| **Caja** | Apertura con fondo (una caja abierta a la vez), cobros por medio de pago, ingresos/egresos, reembolsos en efectivo como egreso y cierre con arqueo. |
| **Facturación simulada** | Boleta `B001` / factura `F001`, validación de RUC/DNI, IGV 18 %, importe en letras, QR formato SUNAT y nota de crédito. **Sin conexión a SUNAT ni OSE/PSE.** |
| **Cuentas** | Roles `ADMIN`, `RECEPCIONISTA`, `CLIENT`; cambio y restablecimiento de contraseña; deshabilitar cuentas invalida sus sesiones al instante. |
| **Catálogo** | Categorías con comodidades, habitaciones, servicios extra, huéspedes, reclamaciones y dashboard. |

## Flujos

### Pago con Mercado Pago

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant FE as Frontend
    participant API as API
    participant MP as Mercado Pago
    C->>FE: Confirma reserva y elige anticipo o total
    FE->>API: POST /client/reservations
    API-->>FE: Reserva PENDING (expira en 15 min)
    FE->>API: POST /client/reservations/{id}/payment-preference
    API->>MP: Crear preferencia (external_reference = código de reserva)
    MP-->>API: init_point
    FE->>MP: Redirige al Checkout Pro
    C->>MP: Paga
    MP->>API: Webhook (x-signature)
    API->>MP: GET /v1/payments/{id} (fuente de verdad)
    API->>API: Registra pago y, si cubre el anticipo, CONFIRMED
    MP-->>FE: Vuelve a /payment/result
    FE->>API: Consulta la reserva hasta verla confirmada
```

### Estados de una reserva

```mermaid
stateDiagram-v2
    [*] --> PENDING: Reserva web
    [*] --> CONFIRMED: Reserva en recepción
    PENDING --> CONFIRMED: Pago que cubre el anticipo
    PENDING --> CANCELLED: Expira (15 min) o el cliente cancela
    CONFIRMED --> CHECKED_IN: Check-in con habitación
    CONFIRMED --> CANCELLED: Cancelación (genera reembolso si hubo pago)
    CHECKED_IN --> CHECKED_OUT: Check-out
    CHECKED_OUT --> [*]
    CANCELLED --> [*]
```

### Caja de recepción

```mermaid
flowchart LR
    A(["Recepcionista inicia sesión"]) --> B{"¿Tiene caja abierta?"}
    B -->|"No"| C["Abrir caja<br/>con fondo inicial"]
    B -->|"Sí, propia"| D["Panel de trabajo"]
    B -->|"Abierta por otra persona"| X["Bloqueado hasta que la cierre<br/>quien la abrió o un admin"]
    C --> D
    D --> E["Cobros: efectivo, tarjeta,<br/>transferencia, Yape/Plin"]
    D --> F["Ingresos / egresos manuales<br/>reembolsos en efectivo"]
    E --> D
    F --> D
    D --> G["Cerrar caja: arqueo<br/>esperado = fondo + ingresos − egresos (efectivo)"]
    G --> H(["Puede cerrar sesión"])
```

### Deshabilitar una cuenta

```mermaid
sequenceDiagram
    actor A as Admin
    participant API as API
    actor U as Usuario con sesión abierta
    A->>API: PATCH /users/{id}/status (active=false)
    API->>API: active=false y token_version + 1
    U->>API: Cualquier petición o latido /auth/session
    API-->>U: 401 ACCOUNT_DISABLED
    Note over U: El frontend cierra la sesión y muestra el motivo
```

## Modelo de datos

```mermaid
erDiagram
    USERS ||--o{ RESERVATIONS : "crea (web)"
    GUESTS ||--o{ RESERVATIONS : "hospeda"
    ROOM_CATEGORIES ||--o{ ROOMS : "agrupa"
    ROOM_CATEGORIES ||--o{ RESERVATIONS : "reservada"
    ROOM_CATEGORIES }o--o{ AMENITIES : "category_amenities"
    ROOMS |o--o{ RESERVATIONS : "asignada"
    RESERVATIONS ||--o{ PAYMENTS : "pagos"
    RESERVATIONS ||--o{ PAYMENT_ATTEMPTS : "intentos MP"
    RESERVATIONS ||--o{ REFUNDS : "reembolsos"
    RESERVATIONS ||--o{ CONSUMPTIONS : "consumos"
    EXTRA_SERVICES ||--o{ CONSUMPTIONS : "servicio"
    RESERVATIONS ||--o{ INVOICES : "comprobantes (uno vigente)"
    CASH_SESSIONS ||--o{ CASH_MOVEMENTS : "movimientos"
    PAYMENTS |o--o| CASH_MOVEMENTS : "cobro en caja"
    USERS ||--o| VERIFICATION_TOKENS : "OTP"

    USERS {
        bigint id PK
        string email
        string role
        boolean active
        int token_version
    }
    RESERVATIONS {
        bigint id PK
        string booking_code
        string status
        date check_in_date
        date check_out_date
        int guests
        numeric total_price
    }
    PAYMENTS {
        bigint id PK
        numeric amount
        string payment_method
        string reference
    }
    INVOICES {
        bigint id PK
        string document_type
        string series
        int correlative
        string status
        string sunat_status
    }
    CASH_SESSIONS {
        bigint id PK
        string status
        numeric opening_amount
        numeric expected_cash
        numeric counted_amount
    }
    CASH_MOVEMENTS {
        bigint id PK
        string type
        string method
        numeric amount
        string concept
    }
```

## Variables de entorno

Toda la configuración sensible se lee de variables de entorno; **no hay credenciales en el repositorio**.
Plantilla: [`.env.example`](.env.example) (los `.env` reales están en `.gitignore`).

<!-- AUTO-GENERATED: variables de entorno (fuente: .env.example) - no editar a mano; regenerar desde el codigo -->
| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | Sí | JDBC de PostgreSQL |
| `DB_USERNAME` | Sí | Usuario de la base |
| `DB_PASSWORD` | Sí | Contraseña de la base |
| `JWT_SECRET` | Sí | Clave HMAC en Base64 (mín. 32 bytes): `openssl rand -base64 48` |
| `JWT_EXPIRATION` | Sí | Vigencia del token en ms |
| `CORS_ALLOWED_ORIGINS` | Sí | Orígenes del frontend, separados por coma |
| `MAIL_USERNAME` | Sí | Cuenta SMTP (Gmail) |
| `MAIL_PASSWORD` | Sí | Contraseña de aplicación de Gmail |
| `MP_ACCESS_TOKEN` | No | Access token de Mercado Pago (`TEST-...` en sandbox); sin él no se crean pagos |
| `MP_WEBHOOK_SECRET` | No | Clave de webhooks de Mercado Pago (valida `x-signature`) |
| `FRONTEND_URL` | Sí | URL pública del frontend (retorno de Mercado Pago) |
| `BACKEND_URL` | Sí | URL pública **https** del backend (Mercado Pago solo notifica a https) |
| `BILLING_RUC` | No | RUC del emisor en comprobantes simulados |
| `BILLING_NAME` | No | Razón social del emisor |
| `BILLING_ADDRESS` | No | Dirección fiscal del emisor |
| `PORT` | No | Puerto HTTP (por defecto `8080`) |
<!-- /AUTO-GENERATED -->

## Ejecutar en local

Requisitos: **Java 21** y **Docker**.

```bash
# 1. Base de datos local
docker run -d --name hotel-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=hotel -p 5432:5432 postgres:16-alpine

# 2. Variables (ver .env.example) y arranque; Flyway crea el esquema
export DB_URL=jdbc:postgresql://localhost:5432/hotel DB_USERNAME=postgres DB_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 48) JWT_EXPIRATION=86400000
export CORS_ALLOWED_ORIGINS=http://localhost:5173 MAIL_USERNAME=... MAIL_PASSWORD=...
./mvnw spring-boot:run
```

**Primer administrador:** no se crea automáticamente. Genera el hash y, con la base ya migrada, insértalo:

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

Los tests de integración levantan su **propia PostgreSQL en Docker** (Testcontainers); nunca usan una base real
ni envían correos. Requiere Docker en ejecución.

## API

Base `/api/v1`. Endpoints sin rol explícito requieren sesión iniciada (cualquier rol).

<!-- AUTO-GENERATED: referencia de endpoints (fuente: controllers @RequestMapping/@PreAuthorize + SecurityConfig) - no editar a mano; regenerar desde el codigo -->
| Método | Ruta | Acceso | Módulo |
|---|---|---|---|
| `GET` | `/api/v1/account/me` | ADMIN, RECEPCIONISTA | Account |
| `PATCH` | `/api/v1/account/password` | ADMIN, RECEPCIONISTA | Account |
| `GET` | `/api/v1/admin/refunds` | ADMIN, RECEPCIONISTA | AdminRefund |
| `POST` | `/api/v1/admin/refunds/{id}/process` | ADMIN, RECEPCIONISTA | AdminRefund |
| `GET` | `/api/v1/amenities` | Autenticado | Amenity |
| `POST` | `/api/v1/amenities` | ADMIN | Amenity |
| `POST` | `/api/v1/auth/login` | Público | Auth |
| `GET` | `/api/v1/auth/session` | Autenticado | Auth |
| `POST` | `/api/v1/auth/register` | ADMIN | Auth |
| `GET` | `/api/v1/cash/current` | ADMIN, RECEPCIONISTA | Cash |
| `POST` | `/api/v1/cash/open` | ADMIN, RECEPCIONISTA | Cash |
| `POST` | `/api/v1/cash/movements` | ADMIN, RECEPCIONISTA | Cash |
| `POST` | `/api/v1/cash/close` | ADMIN, RECEPCIONISTA | Cash |
| `GET` | `/api/v1/cash/sessions` | ADMIN, RECEPCIONISTA | Cash |
| `GET` | `/api/v1/cash/sessions/{id}` | ADMIN, RECEPCIONISTA | Cash |
| `POST` | `/api/v1/client/auth/register` | Público | ClientAuth |
| `POST` | `/api/v1/client/auth/verify` | Público | ClientAuth |
| `POST` | `/api/v1/client/auth/forgot-password` | Público | ClientPasswordReset |
| `POST` | `/api/v1/client/auth/reset-password` | Público | ClientPasswordReset |
| `PUT` | `/api/v1/client/profile` | Autenticado | ClientProfile |
| `POST` | `/api/v1/client/profile/password-request` | Autenticado | ClientProfile |
| `POST` | `/api/v1/client/profile/password-change` | Autenticado | ClientProfile |
| `POST` | `/api/v1/client/reservations` | CLIENT | ClientReservation |
| `GET` | `/api/v1/client/reservations` | CLIENT | ClientReservation |
| `POST` | `/api/v1/client/reservations/{id}/cancel` | CLIENT | ClientReservation |
| `PATCH` | `/api/v1/client/reservations/{id}/dates` | CLIENT | ClientReservation |
| `POST` | `/api/v1/client/reservations/{id}/payment-preference` | CLIENT | ClientReservation |
| `GET` | `/api/v1/client/reservations/{id}/invoice/pdf` | CLIENT | ClientReservation |
| `POST` | `/api/v1/complaints` | Público | Complaint |
| `GET` | `/api/v1/complaints` | ADMIN, RECEPCIONISTA | Complaint |
| `PATCH` | `/api/v1/complaints/{id}/status` | ADMIN, RECEPCIONISTA | Complaint |
| `POST` | `/api/v1/consumptions` | ADMIN, RECEPCIONISTA | Consumption |
| `GET` | `/api/v1/consumptions/reservation/{reservationId}` | ADMIN, RECEPCIONISTA | Consumption |
| `DELETE` | `/api/v1/consumptions/{id}` | ADMIN | Consumption |
| `GET` | `/api/v1/dashboard` | ADMIN, RECEPCIONISTA | Dashboard |
| `POST` | `/api/v1/services` | ADMIN | ExtraService |
| `PUT` | `/api/v1/services/{id}` | ADMIN | ExtraService |
| `GET` | `/api/v1/services` | ADMIN, RECEPCIONISTA | ExtraService |
| `POST` | `/api/v1/guests` | ADMIN, RECEPCIONISTA | Guest |
| `GET` | `/api/v1/guests/{id}` | ADMIN, RECEPCIONISTA | Guest |
| `GET` | `/api/v1/guests` | ADMIN, RECEPCIONISTA | Guest |
| `PUT` | `/api/v1/guests/{id}` | ADMIN, RECEPCIONISTA | Guest |
| `POST` | `/api/v1/invoices/reservation/{reservationId}` | ADMIN, RECEPCIONISTA | Invoice |
| `GET` | `/api/v1/invoices/reservation/{reservationId}` | ADMIN, RECEPCIONISTA | Invoice |
| `GET` | `/api/v1/invoices` | ADMIN, RECEPCIONISTA | Invoice |
| `PATCH` | `/api/v1/invoices/{id}/void` | ADMIN, RECEPCIONISTA | Invoice |
| `PATCH` | `/api/v1/invoices/{id}/pay` | ADMIN, RECEPCIONISTA | Invoice |
| `GET` | `/api/v1/invoices/{id}/pdf` | ADMIN, RECEPCIONISTA | Invoice |
| `GET` | `/api/v1/public/search` | Público | PublicSearch |
| `GET` | `/api/v1/rooms/search` | Público | Reservation |
| `POST` | `/api/v1/reservations` | ADMIN, RECEPCIONISTA | Reservation |
| `GET` | `/api/v1/reservations` | ADMIN, RECEPCIONISTA | Reservation |
| `PATCH` | `/api/v1/reservations/{id}/checkin` | ADMIN, RECEPCIONISTA | Reservation |
| `PATCH` | `/api/v1/reservations/{id}/checkout` | ADMIN, RECEPCIONISTA | Reservation |
| `PATCH` | `/api/v1/reservations/{id}/cancel` | ADMIN, RECEPCIONISTA | Reservation |
| `POST` | `/api/v1/reservations/{id}/payments` | ADMIN, RECEPCIONISTA | Reservation |
| `GET` | `/api/v1/reservations/{id}/payments` | ADMIN, RECEPCIONISTA | Reservation |
| `GET` | `/api/v1/reservations/code/{bookingCode}` | ADMIN, RECEPCIONISTA | Reservation |
| `POST` | `/api/v1/categories` | ADMIN | RoomCategory |
| `POST` | `/api/v1/categories/{id}/image` | ADMIN | RoomCategory |
| `PUT` | `/api/v1/categories/{id}` | ADMIN | RoomCategory |
| `DELETE` | `/api/v1/categories/{id}` | ADMIN | RoomCategory |
| `GET` | `/api/v1/categories/{id}` | Público | RoomCategory |
| `GET` | `/api/v1/categories` | Público | RoomCategory |
| `POST` | `/api/v1/rooms` | ADMIN | Room |
| `PUT` | `/api/v1/rooms/{id}` | ADMIN | Room |
| `DELETE` | `/api/v1/rooms/{id}` | ADMIN | Room |
| `GET` | `/api/v1/rooms/{id}` | Autenticado | Room |
| `GET` | `/api/v1/rooms` | Autenticado | Room |
| `PATCH` | `/api/v1/rooms/{id}/status` | ADMIN | Room |
| `GET` | `/api/v1/users` | ADMIN | User |
| `GET` | `/api/v1/users/clients` | ADMIN | User |
| `PATCH` | `/api/v1/users/{id}/status` | ADMIN | User |
| `POST` | `/api/v1/users` | ADMIN | User |
| `PUT` | `/api/v1/users/{id}` | ADMIN | User |
| `PATCH` | `/api/v1/users/{id}/password` | ADMIN | User |
| `DELETE` | `/api/v1/users/{id}` | ADMIN | User |
| `POST` | `/api/v1/payments/webhook` | Público (firma Mercado Pago) | WebhookPayment |
<!-- /AUTO-GENERATED -->

## Migraciones

Flyway las aplica automáticamente al arrancar.

<!-- AUTO-GENERATED: migraciones (fuente: src/main/resources/db/migration) - no editar a mano; regenerar desde el codigo -->
| Versión | Migración |
|---|---|
| V1 | init |
| V2 | add reservation fields |
| V3 | verification tokens |
| V4 | add expires at to reservations |
| V5 | add user id to reservations |
| V6 | add payment attempts |
| V7 | add payments |
| V8 | add cancellation fields |
| V9 | add refunds |
| V10 | add translation fields |
| V11 | remove translation fields |
| V12 | update users role check |
| V13 | add purpose to verification tokens |
| V14 | add complaints |
| V15 | mercadopago payments |
| V16 | simulated electronic invoicing |
| V17 | cash register and guests |
| V18 | account status and sessions |
| V19 | refund method |
<!-- /AUTO-GENERATED -->

## Despliegue

```mermaid
flowchart LR
    P["git push a main"] --> CI["GitHub Actions: mvn test<br/>(Testcontainers)"]
    CI -->|"OK"| IMG["Imagen Docker<br/>gcr.io"]
    IMG --> CR["Cloud Run<br/>variables desde GitHub Secrets"]
    CR --> FW["Flyway migra la base al arrancar"]
    CI -->|"Falla"| STOP["No se despliega"]
```

## Licencia

Apache 2.0 — ver [LICENSE](LICENSE).
