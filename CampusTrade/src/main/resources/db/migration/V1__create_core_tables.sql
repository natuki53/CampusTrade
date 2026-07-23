CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_number VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    buyer_id BIGINT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    price INT NOT NULL,
    condition_label VARCHAR(50) NOT NULL,
    trade_status VARCHAR(20) NOT NULL,
    moderation_status VARCHAR(20) NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_products_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_products_price CHECK (price >= 0)
);

CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    image_data LONGBLOB NOT NULL,
    display_order INT NOT NULL,
    primary_flag BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    read_flag BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_messages_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE INDEX idx_products_search
    ON products(category_id, trade_status, moderation_status, deleted_at);

CREATE INDEX idx_products_seller
    ON products(seller_id, created_at);

CREATE INDEX idx_products_buyer
    ON products(buyer_id, created_at);

CREATE INDEX idx_product_images_product
    ON product_images(product_id, display_order);

CREATE INDEX idx_messages_product
    ON messages(product_id, message_type, created_at);
