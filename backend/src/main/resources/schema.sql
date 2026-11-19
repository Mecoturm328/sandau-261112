CREATE TABLE IF NOT EXISTS stores (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  short_name  TEXT NOT NULL,
  address     TEXT NOT NULL,
  accent      TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
  id          INTEGER PRIMARY KEY,
  name        TEXT NOT NULL,
  category    TEXT NOT NULL,
  price       REAL NOT NULL,
  sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS orders (
  id              INTEGER PRIMARY KEY,
  order_no        TEXT NOT NULL UNIQUE,
  store_id        INTEGER NOT NULL REFERENCES stores(id),
  ordered_at      TEXT NOT NULL,
  total_amount    REAL NOT NULL,
  item_count      INTEGER NOT NULL,
  payment_method  TEXT NOT NULL,
  is_member       INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_items (
  id          INTEGER PRIMARY KEY,
  order_id    INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id  INTEGER NOT NULL REFERENCES products(id),
  quantity    INTEGER NOT NULL,
  unit_price  REAL NOT NULL,
  subtotal    REAL NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_ordered_at ON orders(ordered_at);
CREATE INDEX IF NOT EXISTS idx_orders_store_time ON orders(store_id, ordered_at);
CREATE INDEX IF NOT EXISTS idx_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_items_product ON order_items(product_id);
