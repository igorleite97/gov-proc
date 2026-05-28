-- Flyway V4: Analise de viabilidade dos processos
-- 1:1 com procurement_processes (UNIQUE em process_id).
-- decision armazenada como STRING do enum AnalysisDecision.

CREATE TABLE process_analyses (
    id                  UUID         NOT NULL,
    process_id          UUID         NOT NULL,
    sample_required     BOOLEAN,
    allows_adhesion     BOOLEAN,
    proposal_guarantee  BOOLEAN,
    delivery_deadline   VARCHAR(200),
    decision            VARCHAR(20)  NOT NULL,
    rejection_reason    TEXT,
    observations        TEXT,
    analyzed_by         UUID         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_analyses          PRIMARY KEY (id),
    CONSTRAINT uq_analysis_process  UNIQUE (process_id),
    CONSTRAINT fk_analysis_process  FOREIGN KEY (process_id)
        REFERENCES procurement_processes (id)
);
