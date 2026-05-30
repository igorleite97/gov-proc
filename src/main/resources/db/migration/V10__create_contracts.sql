-- Flyway V10: Contrato decorrente do processo licitatorio
-- 1:1 com procurement_processes (UNIQUE em process_id).
-- Status proprio (ACTIVE/EXPIRED/TERMINATED/CLOSED) vive AQUI — o processo so
-- sabe que esta em CONTRACT_ACTIVE ou CLOSED.
-- remaining_balance nasce = contract_value (empenhos/medicoes sao roadmap futuro).
-- Padrao monetario: NUMERIC(19,4).

CREATE TABLE contracts (
    id                UUID          NOT NULL,
    process_id        UUID          NOT NULL,
    contract_number   VARCHAR(100)  NOT NULL,
    start_date        DATE          NOT NULL,
    end_date          DATE,
    contract_value    NUMERIC(19,4) NOT NULL,
    remaining_balance NUMERIC(19,4) NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    registered_by     UUID          NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_contracts        PRIMARY KEY (id),
    CONSTRAINT uq_contract_process UNIQUE (process_id),
    CONSTRAINT fk_contract_process FOREIGN KEY (process_id)
        REFERENCES procurement_processes (id)
);

CREATE INDEX idx_contracts_process ON contracts (process_id);
