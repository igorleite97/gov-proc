-- Flyway V8: Disputa — estrategia comercial do processo licitatorio
-- 1:1 com procurement_processes (UNIQUE em process_id), como process_analyses.
-- quoted_cost = snapshot do total_cost da cotacao selecionada (custo nasce na Cotacao).
-- expected_profit = target_sale_price - quoted_cost (calculado e persistido).
-- Resultado (WINNER/LOSER) NAO vive aqui: pertence ao processo. Status local: OPEN/CONCLUDED.
-- Padrao monetario: NUMERIC(19,4).

CREATE TABLE disputes (
    id                 UUID          NOT NULL,
    process_id         UUID          NOT NULL,
    quotation_id       UUID          NOT NULL,
    quoted_cost        NUMERIC(19,4) NOT NULL,
    target_margin      NUMERIC(19,4),
    target_sale_price  NUMERIC(19,4),
    minimum_sale_price NUMERIC(19,4),
    expected_profit    NUMERIC(19,4),
    bid_strategy       VARCHAR(20)   NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    strategy_notes     TEXT,
    registered_by      UUID          NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_disputes          PRIMARY KEY (id),
    CONSTRAINT uq_dispute_process   UNIQUE (process_id),
    CONSTRAINT fk_dispute_process   FOREIGN KEY (process_id)   REFERENCES procurement_processes (id),
    CONSTRAINT fk_dispute_quotation FOREIGN KEY (quotation_id) REFERENCES quotations (id)
);

CREATE INDEX idx_disputes_process ON disputes (process_id);
