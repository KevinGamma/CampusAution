CREATE DATABASE IF NOT EXISTS campus_auction DEFAULT CHARACTER SET utf8mb4;
USE campus_auction;

CREATE TABLE IF NOT EXISTS users (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)    NOT NULL UNIQUE,
    password VARCHAR(255)   NOT NULL,
    balance  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    role     ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT'
);

-- Migration: add role to an existing users table that was created without it.
-- ADD COLUMN IF NOT EXISTS is safe to run repeatedly (MySQL 8.0.3+).
ALTER TABLE users
    ADD COLUMN role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT';

CREATE TABLE IF NOT EXISTS auctions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200)   NOT NULL,
    description   TEXT,
    start_price   DECIMAL(10, 2) NOT NULL,
    current_price DECIMAL(10, 2) NOT NULL,
    end_time      DATETIME       NOT NULL,
    status        ENUM('ACTIVE', 'FINISHED', 'SOLD', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    creator_id    BIGINT         NOT NULL,
    quantity      INT            NOT NULL DEFAULT 1,
    category      VARCHAR(50)    NOT NULL DEFAULT 'Others',
    sale_type     ENUM('AUCTION','DIRECT') NOT NULL DEFAULT 'AUCTION',
    FOREIGN KEY (creator_id) REFERENCES users (id)
);

-- Migrations: add columns to existing auctions table (safe to re-run on MySQL 8.0.3+).
ALTER TABLE auctions ADD COLUMN quantity  INT                      NOT NULL DEFAULT 1;
ALTER TABLE auctions ADD COLUMN category  VARCHAR(50)              NOT NULL DEFAULT 'Others';
ALTER TABLE auctions ADD COLUMN sale_type ENUM('AUCTION','DIRECT') NOT NULL DEFAULT 'AUCTION';

CREATE TABLE IF NOT EXISTS bids (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT         NOT NULL,
    bidder_id  BIGINT         NOT NULL,
    amount     DECIMAL(10, 2) NOT NULL,
    timestamp  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions (id),
    FOREIGN KEY (bidder_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS orders (
    id         BIGINT         AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT         NOT NULL,
    bid_id     BIGINT         NULL,        -- NULL for direct-sale purchases (no bid involved)
    buyer_id   BIGINT         NOT NULL,
    seller_id  BIGINT         NOT NULL,
    amount     DECIMAL(10,2)  NOT NULL,
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions (id),
    FOREIGN KEY (bid_id)     REFERENCES bids (id),
    FOREIGN KEY (buyer_id)   REFERENCES users (id),
    FOREIGN KEY (seller_id)  REFERENCES users (id)
);

-- Migration: allow bid_id to be NULL for direct-sale orders.
ALTER TABLE orders MODIFY COLUMN bid_id BIGINT NULL;

-- Image / profile migrations (safe to re-run on MySQL 8.0.3+)
ALTER TABLE users    ADD COLUMN avatar_url VARCHAR(500) NULL;
ALTER TABLE users    ADD COLUMN bio        VARCHAR(200) NULL;
ALTER TABLE auctions ADD COLUMN image_urls TEXT         NULL;

-- Seller-profile / review system migrations
ALTER TABLE users ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT   AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT   NOT NULL,
    reviewer_id BIGINT   NOT NULL,
    reviewee_id BIGINT   NOT NULL,
    rating      TINYINT  NOT NULL,
    comment     TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_reviewer (order_id, reviewer_id),
    FOREIGN KEY (order_id)    REFERENCES orders (id),
    FOREIGN KEY (reviewer_id) REFERENCES users  (id),
    FOREIGN KEY (reviewee_id) REFERENCES users  (id)
);

-- Default administrator account (INSERT IGNORE is idempotent on the UNIQUE username column)
INSERT IGNORE INTO users (username, password, role, balance)
VALUES ('admin', 'admin123', 'ADMIN', 999999.00);
