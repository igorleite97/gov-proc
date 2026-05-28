-- Flyway V3: Timeline operacional dos processos
-- Registros imutaveis — nunca sao atualizados apos insercao.
-- process_id sem FK intencional: timeline e um log; joins sao feitos pela aplicacao.

CREATE TABLE process_timeline_events (
    id           UUID        NOT NULL,
    process_id   UUID        NOT NULL,
    event_type   VARCHAR(80) NOT NULL,
    description  TEXT,
    performed_by UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_timeline PRIMARY KEY (id)
);

CREATE INDEX idx_timeline_process_id    ON process_timeline_events (process_id);
CREATE INDEX idx_timeline_process_event ON process_timeline_events (process_id, event_type);
