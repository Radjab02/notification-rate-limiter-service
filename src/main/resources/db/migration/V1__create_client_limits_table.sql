CREATE TABLE client_limit_config (
                     client_id VARCHAR(255) PRIMARY KEY,
                     monthly_limit BIGINT NOT NULL,
                     window_capacity INT NOT NULL,
                     window_duration_seconds INT NOT NULL
);

INSERT INTO client_limit_config (client_id, monthly_limit, window_capacity, window_duration_seconds) VALUES
                         ('client-x', 100, 8, 5),
                         ('client-y', 5, 10, 60),
                         ('client-z', 1000, 1000, 300);
