-- Flyway V5: Trilha de auditoria de alteracoes criticas
-- Registros imutaveis: nunca atualizados apos insercao.
-- Sem FK em entity_id: log polimórfico (cobre qualquer entidade do dominio).

CREATE TABLE audit_logs (
    id            UUID         NOT NULL,
    entity_name   VARCHAR(100) NOT NULL,
    entity_id     UUID         NOT NULL,
    field_name    VARCHAR(100) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    performed_by  UUID         NOT NULL,
    performed_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_entity       ON audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_performed_by ON audit_logs (performed_by);
CREATE INDEX idx_audit_performed_at ON audit_logs (performed_at DESC);
