```sql
-- ============================================================
-- GESTOR DE CONTACTOS
-- Esquema inicial de base de datos
-- MySQL 8.x+
-- ============================================================

CREATE DATABASE IF NOT EXISTS gestor_contactos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE gestor_contactos;

-- ============================================================
-- TABLA: users
-- Usuarios del sistema y sus roles.
-- Los usuarios no se eliminan físicamente.
-- ============================================================

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_at DATETIME NULL,
    updated_by BIGINT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),

    CONSTRAINT fk_users_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_users_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX idx_users_role
    ON users(role);

CREATE INDEX idx_users_enabled
    ON users(enabled);


-- ============================================================
-- TABLA: contacts
-- Información principal de personas naturales y jurídicas.
--
-- person_type:
--   NATURAL
--   JURIDICA
--
-- document_type:
--   DNI
--   CARNET_EXTRANJERIA
--   RUC
--
-- status:
--   ACTIVO
--   INACTIVO
--
-- La eliminación funcional se maneja mediante:
--   deleted_at
--   deleted_by
--
-- La identidad no puede duplicarse, incluso si el contacto
-- fue eliminado lógicamente.
-- ============================================================

CREATE TABLE contacts (
    id BIGINT NOT NULL AUTO_INCREMENT,

    person_type VARCHAR(15) NOT NULL,

    first_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NULL,

    document_type VARCHAR(25) NOT NULL,
    document_number VARCHAR(11) NOT NULL,

    legal_name VARCHAR(200) NULL,

    address VARCHAR(200) NOT NULL,
    photo_path VARCHAR(500) NULL,

    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',

    created_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL,

    updated_at DATETIME NULL,
    updated_by BIGINT NULL,

    deleted_at DATETIME NULL,
    deleted_by BIGINT NULL,

    CONSTRAINT pk_contacts PRIMARY KEY (id),

    CONSTRAINT uq_contacts_document
        UNIQUE (document_type, document_number),

    CONSTRAINT fk_contacts_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_contacts_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_contacts_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX idx_contacts_status
    ON contacts(status);

CREATE INDEX idx_contacts_person_type
    ON contacts(person_type);

CREATE INDEX idx_contacts_deleted_at
    ON contacts(deleted_at);

CREATE INDEX idx_contacts_first_name
    ON contacts(first_name);

CREATE INDEX idx_contacts_last_name
    ON contacts(last_name);

CREATE INDEX idx_contacts_legal_name
    ON contacts(legal_name);


-- ============================================================
-- TABLA: contact_phones
-- Un contacto puede tener uno o más teléfonos.
--
-- phone_type:
--   CELULAR
--   FIJO
--
-- Para CELULAR:
--   area_code = ''
--   phone_number = 9 dígitos
--
-- Para FIJO:
--   area_code = código de área
--   phone_number = número local
--
-- El +51 NO se almacena como parte del número nacional.
-- ============================================================

CREATE TABLE contact_phones (
    id BIGINT NOT NULL AUTO_INCREMENT,

    contact_id BIGINT NOT NULL,

    phone_type VARCHAR(10) NOT NULL,
    area_code VARCHAR(5) NOT NULL DEFAULT '',
    phone_number VARCHAR(9) NOT NULL,

    CONSTRAINT pk_contact_phones PRIMARY KEY (id),

    CONSTRAINT uq_contact_phone
        UNIQUE (
            contact_id,
            phone_type,
            area_code,
            phone_number
        ),

    CONSTRAINT fk_contact_phones_contact
        FOREIGN KEY (contact_id)
        REFERENCES contacts(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX idx_contact_phones_contact_id
    ON contact_phones(contact_id);


-- ============================================================
-- TABLA: contact_emails
-- Un contacto puede tener uno o más correos.
--
-- Los correos se almacenan normalizados en minúsculas.
-- ============================================================

CREATE TABLE contact_emails (
    id BIGINT NOT NULL AUTO_INCREMENT,

    contact_id BIGINT NOT NULL,
    email VARCHAR(254) NOT NULL,

    CONSTRAINT pk_contact_emails PRIMARY KEY (id),

    CONSTRAINT uq_contact_email
        UNIQUE (contact_id, email),

    CONSTRAINT fk_contact_emails_contact
        FOREIGN KEY (contact_id)
        REFERENCES contacts(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX idx_contact_emails_contact_id
    ON contact_emails(contact_id);


-- ============================================================
-- TABLA: contact_commercial_types
--
-- Un mismo contacto puede tener:
--   CLIENTE
--   PROVEEDOR
--
-- Por ejemplo:
--   Contacto 1 -> CLIENTE
--   Contacto 1 -> PROVEEDOR
--
-- Esto evita duplicar la misma persona/empresa.
-- ============================================================

CREATE TABLE contact_commercial_types (
    contact_id BIGINT NOT NULL,
    commercial_type VARCHAR(15) NOT NULL,

    CONSTRAINT pk_contact_commercial_types
        PRIMARY KEY (contact_id, commercial_type),

    CONSTRAINT fk_contact_commercial_types_contact
        FOREIGN KEY (contact_id)
        REFERENCES contacts(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX idx_contact_commercial_type
    ON contact_commercial_types(
        commercial_type,
        contact_id
    );


-- ============================================================
-- FIN DEL ESQUEMA
-- ============================================================
```