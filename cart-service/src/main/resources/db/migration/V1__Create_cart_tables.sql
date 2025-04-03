CREATE TABLE carts (
  id VARCHAR(255) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  total_price NUMERIC(10, 2) NOT NULL DEFAULT 0,
  total_items INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE cart_items (
  id BIGSERIAL PRIMARY KEY,
  cart_id VARCHAR(255) NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  product_image VARCHAR(255),
  price NUMERIC(10, 2) NOT NULL,
  quantity INTEGER NOT NULL,
  CONSTRAINT fk_cart FOREIGN KEY (cart_id) REFERENCES carts(id)
);