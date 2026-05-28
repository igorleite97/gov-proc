-- Flyway V7: Cotacoes por fornecedor/processo
-- Regra: apenas 1 selected=true por process_id (garantido pelo service via clearSelectedByProcess).
-- Roadmap: partial unique index (WHERE selected = true) pode reforcar no banco futuramente.
-- Padrao monetario: NUMERIC(19,4) — scale=4 para precisao de centavos fracionados.

CREATE TABLE quotations (
    id                  UUID          NOT NULL,
    process_id          UUID          NOT NULL,
    supplier_id         UUID          NOT NULL,
    product_description TEXT          NOT NULL,
    unit_cost           NUMERIC(19,4) NOT NULL,
    shipping_cost       NUMERIC(19,4) NOT NULL DEFAULT 0,
    quantity            INTEGER       NOT NULL,
    total_cost          NUMERIC(19,4) NOT NULL,
    manufacturer        VARCHAR(255),
    brand               VARCHAR(100),
    delivery_days       VARCHAR(100),
    selected            BOOLEAN       NOT NULL DEFAULT FALSE,
    observations        TEXT,
    registered_by       UUID          NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_quotations    PRIMARY KEY (id),
    CONSTRAINT fk_quot_process  FOREIGN KEY (process_id)  REFERENCES procurement_processes (id),
    CONSTRAINT fk_quot_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

CREATE INDEX idx_quotations_process  ON quotations (process_id);
CREATE INDEX idx_quotations_selected ON quotations (process_id, selected);
