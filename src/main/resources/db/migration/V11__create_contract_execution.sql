-- Flyway V11: Execucao do contrato — empenhos, faturas e aditivos.
-- Membros do agregado Contract: referenciam contracts(id) por FK (integridade),
-- mas a aplicacao mantem o invariante de saldo na raiz Contract.
-- Empenho consome saldo; fatura NAO; aditivo altera capacidade. Padrao: NUMERIC(19,4).

CREATE TABLE commitments (
    id                UUID          NOT NULL,
    contract_id       UUID          NOT NULL,
    commitment_number VARCHAR(100)  NOT NULL,
    amount            NUMERIC(19,4) NOT NULL,
    issue_date        DATE          NOT NULL,
    notes             TEXT,
    registered_by     UUID          NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_commitments          PRIMARY KEY (id),
    CONSTRAINT fk_commitment_contract  FOREIGN KEY (contract_id) REFERENCES contracts (id)
);

CREATE TABLE invoices (
    id             UUID          NOT NULL,
    contract_id    UUID          NOT NULL,
    invoice_number VARCHAR(100)  NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    issued_at      DATE          NOT NULL,
    registered_by  UUID          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_invoices          PRIMARY KEY (id),
    CONSTRAINT fk_invoice_contract  FOREIGN KEY (contract_id) REFERENCES contracts (id)
);

CREATE TABLE addenda (
    id              UUID          NOT NULL,
    contract_id     UUID          NOT NULL,
    addendum_number VARCHAR(100)  NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    value_change    NUMERIC(19,4),
    new_end_date    DATE,
    reason          TEXT,
    signed_at       DATE,
    registered_by   UUID          NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_addenda           PRIMARY KEY (id),
    CONSTRAINT fk_addendum_contract FOREIGN KEY (contract_id) REFERENCES contracts (id)
);

CREATE INDEX idx_commitments_contract ON commitments (contract_id);
CREATE INDEX idx_invoices_contract    ON invoices (contract_id);
CREATE INDEX idx_addenda_contract     ON addenda (contract_id);
