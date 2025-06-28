DROP TABLE IF EXISTS transactions;
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50),
    amount DECIMAL(19, 2) NOT NULL,
    transaction_date DATETIME NOT NULL,
    transaction_description VARCHAR(255) NOT NULL,
    debit_account VARCHAR(50) NOT NULL,
    credit_account VARCHAR(50) NOT NULL,
    status VARCHAR(50),
    last_updated DATETIME,
    currency VARCHAR(10),
    submitted_by VARCHAR(50),
    submitted_at DATETIME,
    approved_by VARCHAR(50),
    approved_at DATETIME
);


