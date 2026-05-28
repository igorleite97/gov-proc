-- Flyway V1: Tabela de usuários do sistema
-- Role armazenada como STRING (enum Java). Sem enum nativo no banco: facilita evolução.

CREATE TABLE users (
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT  pk_users       PRIMARY KEY (id),
    CONSTRAINT  uq_users_email UNIQUE      (email)
);
