-- Reference DDL for landing.cis_exception (AAI Override / Exception load target).
-- Align with existing AAI Postgres schema before applying in shared environments.

CREATE SCHEMA IF NOT EXISTS landing;

CREATE TABLE IF NOT EXISTS landing.cis_exception (
    id                      BIGSERIAL PRIMARY KEY,
    batch_id                UUID,
    coid                    SMALLINT NOT NULL,
    customer_number         BIGINT NOT NULL,
    linked_product_code     CHAR(3),
    linked_account_number   BIGINT,
    linked_sub_product_code CHAR(2),
    linked_cuac_code        CHAR(3),
    exc_acac                CHAR(3),
    std_acac                CHAR(3),
    create_date             DATE,
    change_date             DATE,
    deleted_flag            CHAR(1),
    loaded_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cis_exception_batch_id
    ON landing.cis_exception (batch_id);

CREATE INDEX IF NOT EXISTS idx_cis_exception_customer
    ON landing.cis_exception (customer_number);

CREATE INDEX IF NOT EXISTS idx_cis_exception_coid
    ON landing.cis_exception (coid);
