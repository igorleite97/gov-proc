-- Flyway V2: Tabela central de processos licitatorios
-- Sem FK para users: createdBy e UUID referenciado por convencao, nao por JPA join.
-- ddl-auto=validate vai conferir que este schema bate com o mapeamento JPA.

CREATE TABLE procurement_processes (
    id                  UUID         NOT NULL,
    process_number      VARCHAR(100) NOT NULL,
    uasg                VARCHAR(20)  NOT NULL,
    agency              VARCHAR(255) NOT NULL,
    portal              VARCHAR(100) NOT NULL,
    object_description  TEXT         NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    publication_date    DATE,
    opening_date        DATE,
    dispute_date        DATE,
    priority            VARCHAR(20)  NOT NULL,
    risk                VARCHAR(20)  NOT NULL,
    created_by          UUID         NOT NULL
        REFERENCES users (id),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_processes            PRIMARY KEY (id),
    CONSTRAINT uq_process_number_uasg  UNIQUE (process_number, uasg)
);

CREATE INDEX idx_processes_status     ON procurement_processes (status);
CREATE INDEX idx_processes_created_by ON procurement_processes (created_by);
