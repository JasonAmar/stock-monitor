INSERT INTO users(name, email, password)
VALUES ('Demo User', 'demo@example.com', '$2a$10$demo.hash.placeholder');

INSERT INTO financial_instruments(symbol, type)
VALUES ('AAPL', 'STOCK'),
       ('TSLA', 'STOCK'),
       ('MSFT', 'STOCK');

INSERT INTO portfolios(user_id, name)
VALUES (1, 'Tech Portfolio');

INSERT INTO holdings(portfolio_id, financial_instrument_id, quantity)
VALUES (1, 1, 10),
       (1, 2, 5);

INSERT INTO stock_prices(financial_instrument_id, price, timestamp)
VALUES (1, 201.2500, CURRENT_TIMESTAMP),
       (2, 203, CURRENT_TIMESTAMP),
       (3, 245.1000, CURRENT_TIMESTAMP);

INSERT INTO alerts(user_id, financial_instrument_id, threshold, condition)
VALUES (1, 1, 200.00, 'ABOVE');
