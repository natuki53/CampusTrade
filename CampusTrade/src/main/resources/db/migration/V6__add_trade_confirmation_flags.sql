ALTER TABLE products
    ADD COLUMN buyer_close_requested BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products
    ADD COLUMN seller_close_requested BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products
    ADD COLUMN buyer_cancel_requested BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products
    ADD COLUMN seller_cancel_requested BOOLEAN NOT NULL DEFAULT FALSE;
