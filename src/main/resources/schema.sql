-- ============================================================
-- schema.sql  （PostgreSQL想定）
-- 画像は一時保存(temp_pictures) → 本保存(pictures)
-- 製品・注文との紐付けは product_pictures / order_pictures で管理
-- ============================================================

-- 既存の依存関係に注意して、参照する側から順に DROP
DROP TABLE IF EXISTS 
  product_pictures,
  order_pictures,
  order_products,
  stock,
  product_locations,
  product_categories,
  orders,
  pictures,
  temp_pictures,
  locations,
  parties,
  categories,
  products,
  users;

-- ============================================================
-- 親テーブル（参照される側）
-- ============================================================

  CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username      VARCHAR(25)  NOT NULL UNIQUE,
    name          VARCHAR(25)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(25)  NOT NULL DEFAULT 'GENERAL',
    delete_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

CREATE TABLE IF NOT EXISTS products (
  id SERIAL PRIMARY KEY,
  product_code  VARCHAR(255) NOT NULL UNIQUE,
  name          VARCHAR(255) NOT NULL,
  delete_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
  id SERIAL PRIMARY KEY,
  name          VARCHAR(255) NOT NULL UNIQUE,
  delete_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parties (
  id SERIAL PRIMARY KEY,
  party_code    VARCHAR(255) NOT NULL UNIQUE,
  name          VARCHAR(255) NOT NULL,
  address       VARCHAR(255) NOT NULL,
  detail        TEXT,
  attention     TEXT,
  delete_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS locations (
  id SERIAL PRIMARY KEY,
  name          VARCHAR(255) NOT NULL UNIQUE,
  delete_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 本保存された画像
CREATE TABLE IF NOT EXISTS pictures (
  id SERIAL PRIMARY KEY,
  file_name     VARCHAR(255) NOT NULL UNIQUE,   -- 実ファイル名（ユニーク）
  file_type     VARCHAR(50)  NOT NULL,          -- MIME type
  file_size     BIGINT       NOT NULL,          -- bytes
  file_path     VARCHAR(255) NOT NULL,          -- サーバ上の絶対/相対パス or 配信パス
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 一時保存画像（画面編集中のアップロード置き場）
CREATE TABLE IF NOT EXISTS temp_pictures (
  id SERIAL PRIMARY KEY,
  name           VARCHAR(255) NOT NULL,         -- 一時ファイル名
  file_type      VARCHAR(50)  NOT NULL,
  file_size      BIGINT       NOT NULL,
  file_path      VARCHAR(255) NOT NULL,         -- /files/temp/{draft_key}/{name} 等
  draft_key      VARCHAR(100) NOT NULL,         -- 画面編集セッション等を特定するキー
  uploader_user_id INT,                         -- アップローダー（users.id）
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 子テーブル（参照する側）
-- ============================================================

CREATE TABLE IF NOT EXISTS stock (
  id SERIAL PRIMARY KEY,
  product_id   INT NOT NULL REFERENCES products(id),
  quantity     INT NOT NULL DEFAULT 0,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (product_id) 
);

CREATE TABLE IF NOT EXISTS orders (
  id SERIAL PRIMARY KEY,
  order_code            TEXT        NOT NULL UNIQUE,
  party_id              INT         NOT NULL REFERENCES parties(id),
  tracking_number       VARCHAR(255),
  delivery_date         DATE        NOT NULL,
  admin_note            TEXT,
  warehouse_worker_note TEXT,
  quality_inspector_note TEXT,
  admin_id              INT         NOT NULL REFERENCES users(id),
  warehouse_worker_id   INT         REFERENCES users(id),
  quality_inspector_id  INT         REFERENCES users(id),
  situation             VARCHAR(50) NOT NULL,
  location_id           INT         REFERENCES locations(id),
  how_csv                BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_products (
  id SERIAL PRIMARY KEY,
  order_id    INT NOT NULL REFERENCES orders(id),
  product_id  INT NOT NULL REFERENCES products(id),
  quantity    INT NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (order_id, product_id)
);

-- 注文と画像の紐付け（注文票用の添付）
CREATE TABLE IF NOT EXISTS order_pictures (
  id SERIAL PRIMARY KEY,
  order_id    INT NOT NULL REFERENCES orders(id),
  picture_id  INT NOT NULL REFERENCES pictures(id),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (order_id, picture_id)
);

-- 製品とカテゴリの紐付け
CREATE TABLE IF NOT EXISTS product_categories (
  id SERIAL PRIMARY KEY,
  product_id  INT NOT NULL REFERENCES products(id),
  category_id INT NOT NULL REFERENCES categories(id),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (product_id, category_id)
);

-- 製品と保管場所の紐付け
CREATE TABLE IF NOT EXISTS product_locations (
  id SERIAL PRIMARY KEY,
  product_id  INT NOT NULL REFERENCES products(id),
  location_id INT NOT NULL REFERENCES locations(id),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (product_id, location_id)
);

--  製品と画像の紐付け（製品画像ギャラリー）
CREATE TABLE IF NOT EXISTS product_pictures (
  id SERIAL PRIMARY KEY,
  product_id  INT NOT NULL REFERENCES products(id),
  picture_id  INT NOT NULL REFERENCES pictures(id),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (product_id, picture_id)
);

-- ============================================================
-- 推奨インデックス
-- ============================================================

-- 外部キー参照先に合わせて検索高速化
CREATE INDEX IF NOT EXISTS idx_stock_product       ON stock(product_id);

CREATE INDEX IF NOT EXISTS idx_orders_party        ON orders(party_id);
CREATE INDEX IF NOT EXISTS idx_orders_admin        ON orders(admin_id);
CREATE INDEX IF NOT EXISTS idx_orders_worker       ON orders(warehouse_worker_id);
CREATE INDEX IF NOT EXISTS idx_orders_inspector    ON orders(quality_inspector_id);
CREATE INDEX IF NOT EXISTS idx_orders_location     ON orders(location_id);

CREATE INDEX IF NOT EXISTS idx_order_products_order   ON order_products(order_id);
CREATE INDEX IF NOT EXISTS idx_order_products_product ON order_products(product_id);

CREATE INDEX IF NOT EXISTS idx_order_pictures_order   ON order_pictures(order_id);
CREATE INDEX IF NOT EXISTS idx_order_pictures_picture ON order_pictures(picture_id);

CREATE INDEX IF NOT EXISTS idx_product_categories_product  ON product_categories(product_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category ON product_categories(category_id);

CREATE INDEX IF NOT EXISTS idx_product_locations_product   ON product_locations(product_id);
CREATE INDEX IF NOT EXISTS idx_product_locations_location  ON product_locations(location_id);

CREATE INDEX IF NOT EXISTS idx_product_pictures_product    ON product_pictures(product_id);
CREATE INDEX IF NOT EXISTS idx_product_pictures_picture    ON product_pictures(picture_id);

-- 一時画像はドラフト単位で引くことが多い
CREATE INDEX IF NOT EXISTS idx_temp_pictures_draft   ON temp_pictures(draft_key);
CREATE INDEX IF NOT EXISTS idx_temp_pictures_user    ON temp_pictures(uploader_user_id);
