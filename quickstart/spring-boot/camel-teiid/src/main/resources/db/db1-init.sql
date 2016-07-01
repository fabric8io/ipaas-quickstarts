-- This is the first database, containing products and reference countries.

CREATE TABLE inventory (
    code        VARCHAR(20) PRIMARY KEY,
    product     VARCHAR(50),
    country     VARCHAR(3)
);

INSERT INTO inventory(code, product, country) VALUES ('I001', 'Ball', 'US');
INSERT INTO inventory(code, product, country) VALUES ('I002', 'Shoes', 'IT');
INSERT INTO inventory(code, product, country) VALUES ('I003', 'Hat', 'UK');
INSERT INTO inventory(code, product, country) VALUES ('I004', 'Shorts', 'US');
INSERT INTO inventory(code, product, country) VALUES ('I005', 'T-Shirt', 'IT');
INSERT INTO inventory(code, product, country) VALUES ('I006', 'Socks', 'IT');
