-- This is the second database, containing sales per product code with price information.

CREATE TABLE sales (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20),
    price       INTEGER
);

INSERT INTO sales(code, price) VALUES ('I001', 9);
INSERT INTO sales(code, price) VALUES ('I001', 10);
INSERT INTO sales(code, price) VALUES ('I002', 89);
INSERT INTO sales(code, price) VALUES ('I002', 99);

INSERT INTO sales(code, price) VALUES ('I004', 29);
INSERT INTO sales(code, price) VALUES ('I004', 39);
INSERT INTO sales(code, price) VALUES ('I005', 29);
INSERT INTO sales(code, price) VALUES ('I005', 29);
INSERT INTO sales(code, price) VALUES ('I005', 29);
INSERT INTO sales(code, price) VALUES ('I005', 29);
INSERT INTO sales(code, price) VALUES ('I006', 4);
