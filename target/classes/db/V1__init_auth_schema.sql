-- =============================================================
--  LIS Inventory – Schema de autenticación y autorización
--  DB: PostgreSQL
--  Ejecutar como superuser o dueño del schema
-- =============================================================

-- ---------------------------------------------------------------
-- 1. Permisos atómicos (ej. "inventory:read", "users:manage")
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permissions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permission_name UNIQUE (name)
);

-- ---------------------------------------------------------------
-- 2. Roles (cada usuario tiene exactamente 1 rol)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_role_name UNIQUE (name)
);

-- ---------------------------------------------------------------
-- 3. Tabla pivot Rol ↔ Permisos  (N:M)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role
        FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------
-- 4. Usuarios  (1 usuario → 1 rol, autenticados vía Auth0/Google)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    full_name  VARCHAR(255),
    auth0_sub  VARCHAR(255),           -- subject del token Auth0 ("google-oauth2|xxxxx")
    role_id    BIGINT,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_email     UNIQUE (email),
    CONSTRAINT uq_user_auth0_sub UNIQUE (auth0_sub),
    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------
-- 5. Índices de rendimiento
-- ---------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_app_users_email     ON app_users(email);
CREATE INDEX IF NOT EXISTS idx_app_users_auth0_sub ON app_users(auth0_sub);
CREATE INDEX IF NOT EXISTS idx_app_users_role_id   ON app_users(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_rid ON role_permissions(role_id);

-- ---------------------------------------------------------------
-- 6. Datos semilla de ejemplo (ajusta según tu dominio)
-- ---------------------------------------------------------------

-- Permisos
INSERT INTO permissions (name, description) VALUES
    ('inventory:read',    'Ver registros de inventario'),
    ('inventory:write',   'Crear y editar registros de inventario'),
    ('inventory:delete',  'Eliminar registros de inventario'),
    ('users:read',        'Ver usuarios del sistema'),
    ('users:write',       'Crear y editar usuarios'),
    ('users:delete',      'Eliminar usuarios'),
    ('roles:manage',      'Gestionar roles y permisos'),
    ('reports:read',      'Ver reportes')
ON CONFLICT (name) DO NOTHING;

-- Roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN',    'Acceso total al sistema'),
    ('MANAGER',  'Gestión de inventario y reportes'),
    ('VIEWER',   'Solo lectura')
ON CONFLICT (name) DO NOTHING;

-- Asignación de permisos a roles
-- ADMIN → todos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- MANAGER → inventario + reportes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.name IN (
    'inventory:read','inventory:write','inventory:delete','reports:read'
)
WHERE r.name = 'MANAGER'
ON CONFLICT DO NOTHING;

-- VIEWER → solo lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.name IN ('inventory:read','reports:read')
WHERE r.name = 'VIEWER'
ON CONFLICT DO NOTHING;
