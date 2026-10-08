-- candle: daily OHLCV
CREATE TABLE IF NOT EXISTS candle (
    id          BIGSERIAL PRIMARY KEY,
    ticker      VARCHAR(20)  NOT NULL,
    trade_date  DATE         NOT NULL,
    open        NUMERIC(18, 6) NOT NULL,
    high        NUMERIC(18, 6) NOT NULL,
    low         NUMERIC(18, 6) NOT NULL,
    close       NUMERIC(18, 6) NOT NULL,
    volume      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_candle_ticker_date UNIQUE (ticker, trade_date)
);

CREATE INDEX IF NOT EXISTS idx_candle_ticker_date ON candle (ticker, trade_date);

-- ticker metadata (sectors, names, etc.)
CREATE TABLE IF NOT EXISTS ticker_metadata (
    ticker      VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    sector      VARCHAR(50),
    currency    VARCHAR(10)  DEFAULT 'RUB',
    board       VARCHAR(20)  DEFAULT 'TQBR',
    isin        VARCHAR(20),
    updated_at  TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ticker_metadata_sector ON ticker_metadata (sector);

-- report metadata (for async report generation) — matches ReportMetadata entity
CREATE TABLE IF NOT EXISTS report_metadata (
    report_id      VARCHAR(36)  PRIMARY KEY,
    status         VARCHAR(20)  NOT NULL,
    file_path      VARCHAR(512),
    error_message  VARCHAR(1000),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ,
    request_json   TEXT
);
