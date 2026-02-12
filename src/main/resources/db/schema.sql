CREATE TABLE portfolios (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

CREATE TABLE holdings (
                          id SERIAL PRIMARY KEY,
                          portfolio_id INTEGER REFERENCES portfolios(id),
                          symbol VARCHAR(10),
                          quantity DOUBLE PRECISION
);

CREATE TABLE stock_prices (
                              id SERIAL PRIMARY KEY,
                              symbol VARCHAR(10),
                              price DOUBLE PRECISION,
                              timestamp TIMESTAMP
);

CREATE TABLE alerts (
                        id SERIAL PRIMARY KEY,
                        symbol VARCHAR(10),
                        threshold DOUBLE PRECISION,
                        condition VARCHAR(10)
);
