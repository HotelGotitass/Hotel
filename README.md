<div align="center">

<h1>Hotel Gotitas · API</h1>

<p><b>Backend del sistema de gestión hotelera</b><br>
reservas · pagos con Mercado Pago · caja de recepción · facturación simulada</p>

<a href="https://github.com/HotelGotitass/Hotel/actions/workflows/deploy-backend.yml"><img src="https://github.com/HotelGotitass/Hotel/actions/workflows/deploy-backend.yml/badge.svg?branch=main" alt="CI/CD"></a> <img src="https://img.shields.io/badge/Java-21-2E211B?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21"> <img src="https://img.shields.io/badge/Spring_Boot-3.2-2E211B?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 3.2"> <img src="https://img.shields.io/badge/PostgreSQL-Flyway-2E211B?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL Flyway"> <img src="https://img.shields.io/badge/Mercado_Pago-Checkout_Pro-ECA8D6?style=flat-square" alt="Mercado Pago Checkout Pro"> <img src="https://img.shields.io/badge/tests-Testcontainers-9C7A63?style=flat-square" alt="tests Testcontainers"> <img src="https://img.shields.io/badge/licencia-Apache_2.0-9C7A63?style=flat-square" alt="licencia Apache 2.0">

<p>
<a href="#arquitectura">Arquitectura</a> ·
<a href="#flujos">Flujos</a> ·
<a href="#inicio-rápido">Inicio rápido</a> ·
<a href="#configuración">Configuración</a> ·
<a href="#api">API</a> ·
<a href="#cicd">CI/CD</a> ·
<a href="https://github.com/HotelGotitass/Hotel-frontend">Frontend ↗</a>
</p>

</div>

---

## En un vistazo

<table>
<tr>
<td width="33%" valign="top"><b>Reservas</b><br><sub>Disponibilidad por fechas y huéspedes, reserva web con 15 min para pagar, recepción, check-in/out, consumos y cambio de fechas.</sub></td>
<td width="33%" valign="top"><b>Mercado Pago</b><br><sub>Checkout Pro con anticipo (50 %), total o saldo. Webhook firmado e idempotente.</sub></td>
<td width="33%" valign="top"><b>Caja</b><br><sub>Apertura con fondo, cobros por medio de pago, ingresos/egresos y cierre con arqueo. Una caja abierta a la vez.</sub></td>
</tr>
<tr>
<td width="33%" valign="top"><b>Facturación simulada</b><br><sub>Boleta B001 / factura F001, RUC/DNI validados, IGV, importe en letras, QR y nota de crédito.</sub></td>
<td width="33%" valign="top"><b>Cuentas</b><br><sub>Roles ADMIN, RECEPCIONISTA y CLIENT. Deshabilitar una cuenta cierra sus sesiones al instante.</sub></td>
<td width="33%" valign="top"><b>Catálogo</b><br><sub>Categorías con comodidades, habitaciones, servicios extra, huéspedes, reclamaciones y dashboard.</sub></td>
</tr>
</table>

> [!NOTE]
> La facturación electrónica es **simulada**: genera comprobantes con el formato peruano (series, IGV, QR) pero
> **no se conecta a SUNAT ni a ningún OSE/PSE**.

## Arquitectura

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
flowchart LR
    U["Navegador<br/>clientes y personal"]:::ext --> FE["Frontend React<br/>Vercel"]
    FE -->|"REST + JWT"| API["API Spring Boot<br/>Cloud Run"]:::dark
    API -->|"JPA · Flyway"| DB[("PostgreSQL")]:::accent
    API -->|"preferencia / consulta"| MP["Mercado Pago"]:::ext
    MP -->|"webhook firmado"| API
    U -.->|"Checkout Pro"| MP
    API -->|"SMTP"| MAIL["Gmail"]:::ext
    classDef dark fill:#2E211B,stroke:#2E211B,color:#F7F1E8
    classDef accent fill:#ECA8D6,stroke:#2E211B,color:#2E211B
    classDef ext fill:#FBF8F3,stroke:#9C7A63,color:#2E211B,stroke-dasharray:4 3
```

<details>
<summary><b>Capas internas de la API</b></summary>

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
flowchart TB
    REQ["Petición HTTP"]:::ext --> SEC["JwtAuthFilter<br/>token · cuenta activa · versión de sesión"]:::dark
    SEC --> CTRL["Controllers<br/>@PreAuthorize por rol"]
    CTRL --> SRV["Services<br/>reglas de negocio · @Transactional"]
    SRV --> REPO["Repositories<br/>Spring Data JPA"]
    SRV --> INT["Integraciones<br/>MercadoPagoClient · SimulatedSunatService · EmailService"]:::accent
    REPO --> DB[("PostgreSQL")]
    CTRL -.->|"excepciones"| EX["GlobalExceptionHandler<br/>400 · 401 · 403 · 404 · 409"]:::ext
    classDef dark fill:#2E211B,stroke:#2E211B,color:#F7F1E8
    classDef accent fill:#ECA8D6,stroke:#2E211B,color:#2E211B
    classDef ext fill:#FBF8F3,stroke:#9C7A63,color:#2E211B,stroke-dasharray:4 3
```

</details>

## Flujos

### Pago con Mercado Pago

El webhook nunca se cree a ciegas: la API consulta el pago en Mercado Pago antes de registrarlo.

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
sequenceDiagram
    autonumber
    participant C as Cliente
    participant FE as Frontend
    participant API as API
    participant MP as Mercado Pago
    C->>FE: Elige anticipo o pago total
    FE->>API: POST /client/reservations
    API-->>FE: Reserva PENDING · expira en 15 min
    C->>FE: Ingresa la tarjeta en el Payment Brick
    FE->>MP: Tokeniza la tarjeta (Public Key)
    FE->>API: POST /client/reservations/{id}/payments (token)
    API->>MP: POST /v1/payments (Access Token, referencia = código de reserva)
    Note over API: Registra el pago una sola vez<br/>y confirma si cubre el anticipo
    MP-->>API: Webhook (cambios posteriores del pago)
    FE->>API: /payment/result consulta hasta ver CONFIRMED
```

### Ciclo de vida de una reserva

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
stateDiagram-v2
    direction LR
    [*] --> PENDING: reserva web
    [*] --> CONFIRMED: reserva en recepción
    PENDING --> CONFIRMED: pago cubre el anticipo
    PENDING --> CANCELLED: expira o se cancela
    CONFIRMED --> CHECKED_IN: check-in
    CONFIRMED --> CANCELLED: cancelación + reembolso
    CHECKED_IN --> CHECKED_OUT: check-out
    CHECKED_OUT --> [*]
    CANCELLED --> [*]
```

### Caja de recepción

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
flowchart LR
    A(["Inicia sesión"]):::ext --> B{"¿Caja abierta?"}
    B -->|"no"| C["Abrir caja<br/>fondo inicial"]:::accent
    B -->|"sí, propia"| D["Turno de trabajo"]:::dark
    B -->|"de otra persona"| X["Bloqueado hasta<br/>que la cierren"]:::ext
    C --> D
    D --> E["Cobros<br/>efectivo · tarjeta · transferencia · Yape/Plin"]
    D --> F["Ingresos y egresos<br/>reembolsos en efectivo"]
    E --> D
    F --> D
    D --> G["Cerrar caja · arqueo<br/>fondo + ingresos − egresos"]:::accent
    G --> H(["Puede cerrar sesión"]):::ext
    classDef dark fill:#2E211B,stroke:#2E211B,color:#F7F1E8
    classDef accent fill:#ECA8D6,stroke:#2E211B,color:#2E211B
    classDef ext fill:#FBF8F3,stroke:#9C7A63,color:#2E211B,stroke-dasharray:4 3
```

<details>
<summary><b>Deshabilitar una cuenta (expulsión inmediata)</b></summary>

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
sequenceDiagram
    participant A as Admin
    participant API as API
    participant U as Usuario conectado
    A->>API: PATCH /users/{id}/status · active=false
    API->>API: active=false · token_version + 1
    U->>API: Siguiente petición o latido /auth/session
    API-->>U: 401 ACCOUNT_DISABLED
    Note over U: El frontend cierra la sesión<br/>y muestra el motivo
```

</details>

## Modelo de datos

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
erDiagram
    USERS ||--o{ RESERVATIONS : "reserva en web"
    GUESTS ||--o{ RESERVATIONS : "se hospeda"
    ROOM_CATEGORIES ||--o{ ROOMS : "agrupa"
    ROOM_CATEGORIES ||--o{ RESERVATIONS : "categoría"
    ROOM_CATEGORIES }o--o{ AMENITIES : "comodidades"
    ROOMS |o--o{ RESERVATIONS : "asignada"
    RESERVATIONS ||--o{ PAYMENTS : "pagos"
    RESERVATIONS ||--o{ PAYMENT_ATTEMPTS : "intentos MP"
    RESERVATIONS ||--o{ REFUNDS : "reembolsos"
    RESERVATIONS ||--o{ CONSUMPTIONS : "consumos"
    EXTRA_SERVICES ||--o{ CONSUMPTIONS : "servicio"
    RESERVATIONS ||--o{ INVOICES : "comprobantes"
    CASH_SESSIONS ||--o{ CASH_MOVEMENTS : "movimientos"
    PAYMENTS |o--o| CASH_MOVEMENTS : "cobro en caja"
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
    INVOICES {
        bigint id PK
        string document_type
        string series
        int correlative
        string sunat_status
    }
    CASH_SESSIONS {
        bigint id PK
        string status
        numeric opening_amount
        numeric expected_cash
        numeric counted_amount
    }
```

## Inicio rápido

| Requisito | Versión |
|:--|:--|
| Java | 21 |
| Docker | cualquiera reciente (base local y tests) |

**1 · Levanta PostgreSQL**

```bash
docker run -d --name hotel-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=hotel -p 5432:5432 postgres:16-alpine
```

**2 · Configura y arranca** (Flyway crea el esquema al iniciar)

```bash
cp .env.example .env          # completa los valores; ver Configuración
set -a; source .env; set +a
./mvnw spring-boot:run        # http://localhost:8080/api/v1
```

**3 · Crea el primer administrador**

```bash
./mvnw -q compile
java -cp "target/classes:$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  com.hotel.app.HashGen "TuContraseñaSegura"
```

```sql
INSERT INTO users (email, password, role, is_verified, active, token_version, created_at)
VALUES ('admin@tuhotel.com', '<hash generado>', 'ADMIN', true, true, 0, now());
```

> [!TIP]
> Para probar webhooks en local expón el puerto por https (`ngrok http 8080`) y usa esa URL en `BACKEND_URL`.

## Configuración

Toda la configuración sensible llega por variables de entorno; **el repositorio no contiene credenciales**.
Plantilla: [`.env.example`](.env.example). Los `.env` reales están en `.gitignore`.

<sub>● obligatoria · ○ opcional</sub>

<!-- AUTO-GENERATED: variables de entorno (fuente: .env.example) - no editar a mano -->
| | Variable | Descripción |
|:-:|:--|:--|
| ● | `DB_URL` | JDBC de PostgreSQL <sub>· Base de datos</sub> |
| ● | `DB_USERNAME` | Usuario de la base <sub>· Base de datos</sub> |
| ● | `DB_PASSWORD` | Contraseña de la base <sub>· Base de datos</sub> |
| ● | `JWT_SECRET` | Clave HMAC en Base64, mín. 32 bytes · `openssl rand -base64 48` <sub>· Seguridad</sub> |
| ● | `JWT_EXPIRATION` | Vigencia del token (ms) <sub>· Seguridad</sub> |
| ● | `CORS_ALLOWED_ORIGINS` | Orígenes del frontend, separados por coma <sub>· Seguridad</sub> |
| ● | `MAIL_USERNAME` | Cuenta SMTP (Gmail) <sub>· Correo</sub> |
| ● | `MAIL_PASSWORD` | Contraseña de aplicación de Gmail <sub>· Correo</sub> |
| ○ | `MP_ACCESS_TOKEN` | Access token (`TEST-…` en sandbox); sin él no se crean pagos <sub>· Mercado Pago</sub> |
| ○ | `MP_PUBLIC_KEY` | Public key del Payment Brick; se expone en `/api/v1/public/payments/config` <sub>· Mercado Pago</sub> |
| ● | `FRONTEND_URL` | URL pública del frontend (retorno del checkout) <sub>· Mercado Pago</sub> |
| ● | `BACKEND_URL` | URL pública **https** del backend (destino del webhook) <sub>· Mercado Pago</sub> |
| ○ | `BILLING_RUC` | RUC del emisor <sub>· Comprobantes</sub> |
| ○ | `BILLING_NAME` | Razón social del emisor <sub>· Comprobantes</sub> |
| ○ | `BILLING_ADDRESS` | Dirección fiscal del emisor <sub>· Comprobantes</sub> |
| ○ | `PORT` | Puerto HTTP (por defecto `8080`) <sub>· Servidor</sub> |
<!-- /AUTO-GENERATED -->

## Tests

```bash
./mvnw test
```

Cada ejecución levanta una **PostgreSQL desechable en Docker** (Testcontainers): nunca toca una base real ni envía correos.

## API

Base `/api/v1` · 78 endpoints · autenticación `Authorization: Bearer <jwt>`.

<!-- AUTO-GENERATED: endpoints por módulo (fuente: controllers: @RequestMapping + @PreAuthorize) - no editar a mano -->
<details>
<summary><b>Autenticación <sub>· 7 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/auth/login` | Público |
| `GET` | `/auth/session` | Sesión |
| `POST` | `/auth/register` | ADMIN |
| `POST` | `/client/auth/register` | Público |
| `POST` | `/client/auth/verify` | Público |
| `POST` | `/client/auth/forgot-password` | Público |
| `POST` | `/client/auth/reset-password` | Público |

</details>

<details>
<summary><b>Mi cuenta <sub>· 2 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/account/me` | ADMIN, RECEP. |
| `PATCH` | `/account/password` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Perfil del cliente <sub>· 3 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `PUT` | `/client/profile` | Sesión |
| `POST` | `/client/profile/password-request` | Sesión |
| `POST` | `/client/profile/password-change` | Sesión |

</details>

<details>
<summary><b>Reservas del cliente <sub>· 6 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/client/reservations` | CLIENT |
| `GET` | `/client/reservations` | CLIENT |
| `POST` | `/client/reservations/{id}/cancel` | CLIENT |
| `PATCH` | `/client/reservations/{id}/dates` | CLIENT |
| `POST` | `/client/reservations/{id}/payment-preference` | CLIENT |
| `GET` | `/client/reservations/{id}/invoice/pdf` | CLIENT |

</details>

<details>
<summary><b>Reservas (recepción) <sub>· 9 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/rooms/search` | Público |
| `POST` | `/reservations` | ADMIN, RECEP. |
| `GET` | `/reservations` | ADMIN, RECEP. |
| `PATCH` | `/reservations/{id}/checkin` | ADMIN, RECEP. |
| `PATCH` | `/reservations/{id}/checkout` | ADMIN, RECEP. |
| `PATCH` | `/reservations/{id}/cancel` | ADMIN, RECEP. |
| `POST` | `/reservations/{id}/payments` | ADMIN, RECEP. |
| `GET` | `/reservations/{id}/payments` | ADMIN, RECEP. |
| `GET` | `/reservations/code/{bookingCode}` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Pagos – Mercado Pago <sub>· 1 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/payments/webhook` | Público · firma MP |

</details>

<details>
<summary><b>Caja <sub>· 6 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/cash/current` | ADMIN, RECEP. |
| `POST` | `/cash/open` | ADMIN, RECEP. |
| `POST` | `/cash/movements` | ADMIN, RECEP. |
| `POST` | `/cash/close` | ADMIN, RECEP. |
| `GET` | `/cash/sessions` | ADMIN, RECEP. |
| `GET` | `/cash/sessions/{id}` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Facturación simulada <sub>· 6 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/invoices/reservation/{reservationId}` | ADMIN, RECEP. |
| `GET` | `/invoices/reservation/{reservationId}` | ADMIN, RECEP. |
| `GET` | `/invoices` | ADMIN, RECEP. |
| `PATCH` | `/invoices/{id}/void` | ADMIN, RECEP. |
| `PATCH` | `/invoices/{id}/pay` | ADMIN, RECEP. |
| `GET` | `/invoices/{id}/pdf` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Reembolsos <sub>· 2 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/admin/refunds` | ADMIN, RECEP. |
| `POST` | `/admin/refunds/{id}/process` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Consumos <sub>· 3 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/consumptions` | ADMIN, RECEP. |
| `GET` | `/consumptions/reservation/{reservationId}` | ADMIN, RECEP. |
| `DELETE` | `/consumptions/{id}` | ADMIN |

</details>

<details>
<summary><b>Huéspedes <sub>· 4 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/guests` | ADMIN, RECEP. |
| `GET` | `/guests/{id}` | ADMIN, RECEP. |
| `GET` | `/guests` | ADMIN, RECEP. |
| `PUT` | `/guests/{id}` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Catálogo <sub>· 18 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/amenities` | Público |
| `POST` | `/amenities` | ADMIN |
| `POST` | `/services` | ADMIN |
| `PUT` | `/services/{id}` | ADMIN |
| `GET` | `/services` | ADMIN, RECEP. |
| `GET` | `/public/search` | Público |
| `POST` | `/categories` | ADMIN |
| `POST` | `/categories/{id}/image` | ADMIN |
| `PUT` | `/categories/{id}` | ADMIN |
| `DELETE` | `/categories/{id}` | ADMIN |
| `GET` | `/categories/{id}` | Público |
| `GET` | `/categories` | Público |
| `POST` | `/rooms` | ADMIN |
| `PUT` | `/rooms/{id}` | ADMIN |
| `DELETE` | `/rooms/{id}` | ADMIN |
| `GET` | `/rooms/{id}` | Público |
| `GET` | `/rooms` | Público |
| `PATCH` | `/rooms/{id}/status` | ADMIN |

</details>

<details>
<summary><b>Personal y cuentas <sub>· 7 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/users` | ADMIN |
| `GET` | `/users/clients` | ADMIN |
| `PATCH` | `/users/{id}/status` | ADMIN |
| `POST` | `/users` | ADMIN |
| `PUT` | `/users/{id}` | ADMIN |
| `PATCH` | `/users/{id}/password` | ADMIN |
| `DELETE` | `/users/{id}` | ADMIN |

</details>

<details>
<summary><b>Reclamaciones <sub>· 3 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `POST` | `/complaints` | Público |
| `GET` | `/complaints` | ADMIN, RECEP. |
| `PATCH` | `/complaints/{id}/status` | ADMIN, RECEP. |

</details>

<details>
<summary><b>Dashboard <sub>· 1 endpoints</sub></b></summary>

| Método | Ruta | Acceso |
|:--|:--|:--|
| `GET` | `/dashboard` | ADMIN, RECEP. |

</details>
<!-- /AUTO-GENERATED -->

## Migraciones

<details>
<summary><b>19 migraciones Flyway <sub>· se aplican solas al arrancar</sub></b></summary>

<!-- AUTO-GENERATED: migraciones (fuente: src/main/resources/db/migration) - no editar a mano -->
| Versión | Cambio |
|:--|:--|
| `V1` | init |
| `V2` | add reservation fields |
| `V3` | verification tokens |
| `V4` | add expires at to reservations |
| `V5` | add user id to reservations |
| `V6` | add payment attempts |
| `V7` | add payments |
| `V8` | add cancellation fields |
| `V9` | add refunds |
| `V10` | add translation fields |
| `V11` | remove translation fields |
| `V12` | update users role check |
| `V13` | add purpose to verification tokens |
| `V14` | add complaints |
| `V15` | mercadopago payments |
| `V16` | simulated electronic invoicing |
| `V17` | cash register and guests |
| `V18` | account status and sessions |
| `V19` | refund method |
<!-- /AUTO-GENERATED -->

</details>

<a id="cicd"></a>
## CI/CD

El repositorio incluye un pipeline de **GitHub Actions** ([`deploy-backend.yml`](.github/workflows/deploy-backend.yml))
que corre en cada push a `main` (o a mano con *Run workflow*). Nada llega a producción sin pasar los tests.

```mermaid
%%{init: {"theme":"base","themeVariables":{"primaryColor":"#F7F1E8","primaryTextColor":"#2E211B","primaryBorderColor":"#2E211B","lineColor":"#9C7A63","secondaryColor":"#ECA8D6","tertiaryColor":"#FBF8F3","edgeLabelBackground":"#F7F1E8","noteBkgColor":"#ECA8D6","noteTextColor":"#2E211B","noteBorderColor":"#2E211B","actorBkg":"#2E211B","actorTextColor":"#F7F1E8","actorBorder":"#2E211B","actorLineColor":"#9C7A63","signalColor":"#9C7A63","signalTextColor":"#9C7A63","labelBoxBkgColor":"#ECA8D6","labelTextColor":"#2E211B","loopTextColor":"#9C7A63","attributeBackgroundColorOdd":"#FBF8F3","attributeBackgroundColorEven":"#F7F1E8"}}}%%
flowchart LR
    P(["push a main"]):::ext --> CI
    subgraph GA["GitHub Actions"]
        CI["CI<br/>mvnw clean test<br/>Testcontainers"]:::accent
        CI -->|"tests en verde"| B["CD<br/>docker build"]
        B --> R["Container<br/>Registry"]
    end
    R --> CR["Cloud Run"]:::dark
    S[("GitHub Secrets")]:::ext -.->|"variables de entorno"| CR
    CR --> FW["Flyway migra<br/>y arranca"]:::accent
    CI -->|"falla"| STOP["No se despliega"]:::ext
    classDef dark fill:#2E211B,stroke:#2E211B,color:#F7F1E8
    classDef accent fill:#ECA8D6,stroke:#2E211B,color:#2E211B
    classDef ext fill:#FBF8F3,stroke:#9C7A63,color:#2E211B,stroke-dasharray:4 3
```

| Etapa | Job | Qué hace |
|:--|:--|:--|
| **CI** | `ci` | Java 21 (Temurin) con caché de Maven · `./mvnw clean test` con PostgreSQL efímera (Testcontainers) |
| **CD** | `cd` · `needs: ci` | Autentica en Google Cloud · construye la imagen con el [`Dockerfile`](Dockerfile) multi-etapa · la publica en Container Registry · despliega en **Cloud Run** |
| **Arranque** | — | Flyway aplica las migraciones pendientes antes de recibir tráfico |

<details>
<summary><b>Secrets requeridos</b> <sub>· Settings → Secrets and variables → Actions</sub></summary>

Los valores viven **solo** en GitHub Secrets y se inyectan en Cloud Run como variables de entorno;
nunca se escriben en el código ni en los logs.

<!-- AUTO-GENERATED: secrets (solo nombres) (fuente: .github/workflows/deploy-backend.yml) - no editar a mano -->
| Secret | Uso |
|:--|:--|
| `GCP_PROJECT_ID` | ID del proyecto de Google Cloud |
| `GCP_CREDENTIALS` | JSON de la cuenta de servicio (Cloud Run + Container Registry) |
| `DB_URL` | Variable del contenedor · JDBC de PostgreSQL |
| `DB_USERNAME` | Variable del contenedor · Usuario de la base |
| `DB_PASSWORD` | Variable del contenedor · Contraseña de la base |
| `JWT_SECRET` | Variable del contenedor · Clave HMAC en Base64, mín. 32 bytes · `openssl rand -base64 48` |
| `JWT_EXPIRATION` | Variable del contenedor · Vigencia del token (ms) |
| `CORS_ALLOWED_ORIGINS` | Variable del contenedor · Orígenes del frontend, separados por coma |
| `MP_ACCESS_TOKEN` | Variable del contenedor · Access token (`TEST-…` en sandbox); sin él no se crean pagos |
| `MP_PUBLIC_KEY` | Variable del contenedor · Public key del Payment Brick (la lee el frontend) |
| `FRONTEND_URL` | Variable del contenedor · URL pública del frontend (retorno del checkout) |
| `BACKEND_URL` | Variable del contenedor · URL pública **https** del backend (destino del webhook) |
| `MAIL_USERNAME` | Variable del contenedor · Cuenta SMTP (Gmail) |
| `MAIL_PASSWORD` | Variable del contenedor · Contraseña de aplicación de Gmail |
<!-- /AUTO-GENERATED -->

</details>

> [!IMPORTANT]
> Si rotas una credencial (base de datos, correo, JWT, Mercado Pago), actualiza el secret: el siguiente
> despliegue la aplica. Un secret vacío deja la variable vacía en el contenedor.
>
> `BILLING_RUC`, `BILLING_NAME`, `BILLING_ADDRESS` no se pasan en el workflow; el contenedor usa sus valores por defecto.

---

<div align="center">
<sub>Apache 2.0 · <a href="LICENSE">LICENSE</a></sub>
</div>
