-- Flyway V6: Fornecedores
-- company_name = razao social. document = CNPJ sem mascara.
-- Snapshot (supplierNameSnapshot, supplierDocumentSnapshot) em Quotation: ROADMAP FUTURO.

CREATE TABLE suppliers (
    id           UUID         NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    trade_name   VARCHAR(255),
    document     VARCHAR(18)  NOT NULL,
    email        VARCHAR(255),
    phone        VARCHAR(20),
    segment      VARCHAR(100),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_suppliers         PRIMARY KEY (id),
    CONSTRAINT uq_supplier_document UNIQUE (document)
);

CREATE INDEX idx_suppliers_active ON suppliers (active);
