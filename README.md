# BackEnd LIS-Inventory

API REST construida con Spring Boot 3.4.1 para el sistema de inventario del Laboratorio de Ingeniería de Sistemas (LIS). Implementa autenticación mediante Auth0 con Google Workspace (`@udea.edu.co`) y emite un JWT propio con roles y permisos.

---

## Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 3.4.1 | Framework principal |
| Spring Security + OAuth2 Resource Server | — | Seguridad y validación JWT |
| JJWT | 0.12.6 | Emisión del JWT propio |
| Spring Data JPA + Hibernate | — | Persistencia |
| PostgreSQL | — | Base de datos |
| SpringDoc OpenAPI | 2.7.0 | Swagger UI |
| Lombok | — | Reducción de boilerplate |
| H2 | — | Base de datos en memoria para tests |

---

## Arquitectura

Monolito modular con dos módulos:

```
src/main/java/com/lis/inventory/
├── LisInventoryApplication.java
├── iam/                         # Identity & Access Management
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
└── shared/                      # Configuración y utilidades transversales
    ├── config/
    └── exception/
```

---

## Módulo IAM

Gestiona autenticación, usuarios, roles y permisos.

### Entidades (DB-First)

| Tabla | Descripción |
|---|---|
| `permissions` | Permisos atómicos del sistema |
| `roles` | Roles con sus permisos asignados (N:M) |
| `app_users` | Usuarios con un único rol asignado |

El esquema se crea con `src/main/resources/db/V1__init_auth_schema.sql`.

### Permisos del sistema (seed)

| Permiso | Descripción |
|---|---|
| `users:read` | Ver usuarios |
| `users:write` | Modificar usuarios |
| `users:delete` | Eliminar usuarios |
| `roles:manage` | Gestionar roles y permisos |

### Roles del sistema (seed)

| Rol | Permisos |
|---|---|
| `ADMIN` | Todos |
| `MANAGER` | `users:read`, `users:write` |
| `VIEWER` | `users:read` |

---

## Flujo de autenticación

```
Frontend                Auth0                  Backend
   │                      │                       │
   │──── loginWithRedirect() ──────────────────>  │
   │                      │                       │
   │<── Google login ────>│                       │
   │                      │                       │
   │<── access_token ─────│                       │
   │                                              │
   │──── POST /api/auth/token { idToken } ──────> │
   │                                              │ valida firma JWKS
   │                                              │ valida issuer + audience
   │                                              │ valida @udea.edu.co
   │                                              │ crea usuario si es nuevo
   │                                              │ emite JWT propio (HMAC-SHA256)
   │<──── { token, role, permissions[] } ─────────│
   │                                              │
   │──── GET /api/iam/users (Bearer JWT) ────────>│
```

---

## Endpoints

Base URL: `http://localhost:8080/api`

### Auth (público)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/token` | Intercambia Access Token de Auth0 por JWT propio |

### IAM – Usuarios

| Método | Endpoint | Permiso requerido |
|---|---|---|
| GET | `/iam/users` | `users:read` |
| GET | `/iam/users/{id}` | `users:read` |
| PATCH | `/iam/users/{id}/role` | `users:write` |
| PATCH | `/iam/users/{id}/toggle-active` | `users:write` |
| DELETE | `/iam/users/{id}` | `users:delete` |

### IAM – Roles

| Método | Endpoint | Permiso requerido |
|---|---|---|
| GET | `/iam/roles` | `roles:manage` |
| GET | `/iam/roles/{id}` | `roles:manage` |
| POST | `/iam/roles` | `roles:manage` |
| PUT | `/iam/roles/{id}` | `roles:manage` |
| PUT | `/iam/roles/{id}/permissions` | `roles:manage` |
| DELETE | `/iam/roles/{id}` | `roles:manage` |

### IAM – Permisos

| Método | Endpoint | Permiso requerido |
|---|---|---|
| GET | `/iam/permissions` | `roles:manage` |
| GET | `/iam/permissions/{id}` | `roles:manage` |
| POST | `/iam/permissions` | `roles:manage` |
| PUT | `/iam/permissions/{id}` | `roles:manage` |
| DELETE | `/iam/permissions/{id}` | `roles:manage` |

---

## Swagger UI

```
http://localhost:8080/api/swagger-ui.html
```

---

## Configuración local

El proyecto usa perfiles de Spring. Los secretos **nunca se suben a Git**.

1. Crea el archivo `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    password: <tu_password_postgres>

auth0:
  client-secret: <auth0_client_secret>

app:
  jwt:
    secret: <base64_256bits>
```

Genera el JWT secret con:
```bash
openssl rand -base64 32
```

2. En VS Code la configuración de ejecución (`.vscode/launch.json`) ya activa el perfil `local` automáticamente.

---

## Requisitos previos

- Java 21
- PostgreSQL corriendo en `localhost:5432`
- Base de datos `lis_inventory` creada
- Schema inicializado con `V1__init_auth_schema.sql`
- Auth0 tenant configurado con Google OAuth2 (credenciales propias de Google Cloud Console)

---

## Ejecutar en VS Code

Usar **Run → Start Debugging** con la configuración `LisInventoryApplication (local)`.

## Ejecutar con Maven

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

