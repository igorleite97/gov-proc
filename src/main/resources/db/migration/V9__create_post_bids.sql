-- Flyway V9: Fase pos-disputa (homologacao → adjudicacao → conclusao)
-- 1:1 com procurement_processes (UNIQUE em process_id), como analysis e dispute.
-- Estados juridicos (PENDING/HOMOLOGATED/ADJUDICATED/COMPLETED) vivem AQUI,
-- fora do ProcessStatus — o processo so sabe que esta em POST_BID.

CREATE TABLE post_bids (
    id                  UUID         NOT NULL,
    process_id          UUID         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    homologation_number VARCHAR(100),
    homologation_date   DATE,
    adjudication_number VARCHAR(100),
    adjudication_date   DATE,
    notes               TEXT,
    registered_by       UUID         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_post_bids         PRIMARY KEY (id),
    CONSTRAINT uq_post_bid_process  UNIQUE (process_id),
    CONSTRAINT fk_post_bid_process  FOREIGN KEY (process_id)
        REFERENCES procurement_processes (id)
);

CREATE INDEX idx_post_bids_process ON post_bids (process_id);
