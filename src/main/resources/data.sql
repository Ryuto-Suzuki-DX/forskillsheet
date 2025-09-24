-- ============================================
-- 初期・追加データ 一括投入（idempotent）
-- 同じ名前カラムの衝突を避け、重複リテラルも削減
-- ============================================

WITH
ts AS (SELECT CURRENT_TIMESTAMP AS now),

-- ▼ マスタ系：コード基準のソース
users_src (username, name, password, role, delete_flag) AS (
  VALUES
    ('ryuto','Ryuto Suzuki','$2a$10$gY7a1E.92FyuaChr7xUl1eE5.Xxzg1i9GN5IER6T6eSTWamMlcxTW','ADMIN',false),
    ('general','General User','$2a$10$gY7a1E.92FyuaChr7xUl1eE5.Xxzg1i9GN5IER6T6eSTWamMlcxTW','GENERAL',false),
    ('admin1','Admin One','$2a$10$gY7a1E.92FyuaChr7xUl1eE5.Xxzg1i9GN5IER6T6eSTWamMlcxTW','ADMIN',false),
    ('admin2','Admin Two','$2a$10$gY7a1E.92FyuaChr7xUl1eE5.Xxzg1i9GN5IER6T6eSTWamMlcxTW','ADMIN',false),
    ('worker1','Worker One','$2a$10$gY7a1E.92FyuaChr7xUl1eE5.Xxzg1i9GN5IER6T6eSTWamMlcxTW','GENERAL',false),
    ('worker2','Worker Two','$2a$10$gY7a1E.92FyuaChr7xUl1eE5.Xxzg1i9GN5IER6T6eSTWamMlcxTW','GENERAL',false)
),
products_src (product_code, product_name, delete_flag) AS (
  VALUES
    ('PW66002','中国製ネジ5M',false),
    ('PX23444','3Φﾁｭｰﾌﾞ5m',false),
    ('PX10001','アルミパイプ 10m',false),
    ('PX10002','鉄板 3mm厚 1m×2m',false),
    ('PX10003','木ネジ 4×30mm',false),
    ('PX10004','電動ドリルセット',false),
    ('PX10005','耐熱ホース 5m',false)
),
parties_src (party_code, party_name, address, detail, attention, delete_flag) AS (
  VALUES
    ('A.inc','A株式会社','大阪府羽曳野市古市6-24-2','',                   '',               false),
    ('B.inc','B株式会社','大阪府羽曳野市西浦4-1-15','',                     '佐川での配送禁止', false),
    ('C.inc','C株式会社','大阪府西成区山王1-10-17 202号室','A株式会社の子会社','',               false),
    ('D.inc','D株式会社','東京都千代田区丸の内1-1-1','',                      '',               false),
    ('E.inc','E株式会社','大阪府大阪市北区梅田1-1-1','',                        '',               false),
    ('F.inc','F株式会社','京都府京都市中京区三条1-2-3','',                 '午前中配送希望',   false),
    ('G.inc','G株式会社','愛知県名古屋市中区錦1-2-3','',                    '',               false)
),
locations_src (location_name, delete_flag) AS (
  VALUES
    ('A001',false),('B-32',false),('C-10',false),('D-25',false),
    ('E-01',false),('E-02',false),('F-10',false),('Z-99',false)
),
categories_src (category_name, delete_flag) AS (
  VALUES
    ('ネジ',false),('チューブ',false),('工具',false),
    ('パイプ',false),('金属板',false),('木材',false),
    ('電動工具',false),('ホース',false)
),

-- ▼ INSERT（マスタ）
ins_users AS (
  INSERT INTO users (username, name, password, role, delete_flag, created_at, updated_at)
  SELECT s.username, s.name, s.password, s.role, s.delete_flag, ts.now, ts.now
  FROM users_src s CROSS JOIN ts
  ON CONFLICT (username) DO NOTHING
  RETURNING 1
),
ins_products AS (
  INSERT INTO products (product_code, name, delete_flag, created_at, updated_at)
  SELECT s.product_code, s.product_name, s.delete_flag, ts.now, ts.now
  FROM products_src s CROSS JOIN ts
  ON CONFLICT (product_code) DO NOTHING
  RETURNING 1
),
ins_parties AS (
  INSERT INTO parties (party_code, name, address, detail, attention, delete_flag, created_at, updated_at)
  SELECT s.party_code, s.party_name, s.address, s.detail, s.attention, s.delete_flag, ts.now, ts.now
  FROM parties_src s CROSS JOIN ts
  ON CONFLICT (party_code) DO NOTHING
  RETURNING 1
),
ins_locations AS (
  INSERT INTO locations (name, delete_flag, created_at, updated_at)
  SELECT s.location_name, s.delete_flag, ts.now, ts.now
  FROM locations_src s CROSS JOIN ts
  ON CONFLICT (name) DO NOTHING
  RETURNING 1
),
ins_categories AS (
  INSERT INTO categories (name, delete_flag, created_at, updated_at)
  SELECT s.category_name, s.delete_flag, ts.now, ts.now
  FROM categories_src s CROSS JOIN ts
  ON CONFLICT (name) DO NOTHING
  RETURNING 1
),

-- ▼ ルックアップ（ID引き当て用）
p_ids AS (
  SELECT product_code, id AS product_id
  FROM products
  WHERE product_code IN (SELECT product_code FROM products_src)
),
c_ids AS (
  SELECT name AS category_name, id AS category_id
  FROM categories
  WHERE name IN (SELECT category_name FROM categories_src)
),
l_ids AS (
  SELECT name AS location_name, id AS location_id
  FROM locations
  WHERE name IN (SELECT location_name FROM locations_src)
),
u_ids AS (
  SELECT username, id AS user_id
  FROM users
  WHERE username IN (SELECT username FROM users_src)
),
prt_ids AS (
  SELECT party_code, id AS party_id
  FROM parties
  WHERE party_code IN (SELECT party_code FROM parties_src)
),

-- ▼ 多対多：Product-Category
pc_src (product_code, category_name) AS (
  VALUES
    ('PW66002','ネジ'), ('PW66002','工具'),
    ('PX23444','チューブ'),
    ('PX10001','パイプ'), ('PX10001','金属板'),
    ('PX10002','金属板'),
    ('PX10003','ネジ'), ('PX10003','木材'),
    ('PX10004','電動工具'),
    ('PX10005','ホース'), ('PX10005','チューブ')
),
ins_pc AS (
  INSERT INTO product_categories (product_id, category_id, created_at, updated_at)
  SELECT p.product_id, c.category_id, ts.now, ts.now
  FROM pc_src s
  JOIN p_ids p ON p.product_code = s.product_code
  JOIN c_ids c ON c.category_name = s.category_name
  CROSS JOIN ts
  ON CONFLICT (product_id, category_id) DO NOTHING
  RETURNING 1
),

-- ▼ 多対多：Product-Location
pl_src (product_code, location_name) AS (
  VALUES
    ('PW66002','A001'),('PW66002','C-10'),('PW66002','D-25'),
    ('PX23444','B-32'),('PX23444','C-10'),
    ('PX10001','E-01'),('PX10001','E-02'),
    ('PX10002','F-10'),
    ('PX10003','Z-99'),('PX10003','A001')
),
ins_pl AS (
  INSERT INTO product_locations (product_id, location_id, created_at, updated_at)
  SELECT p.product_id, l.location_id, ts.now, ts.now
  FROM pl_src s
  JOIN p_ids p ON p.product_code   = s.product_code
  JOIN l_ids l ON l.location_name  = s.location_name
  CROSS JOIN ts
  ON CONFLICT (product_id, location_id) DO NOTHING
  RETURNING 1
),

-- ▼ Orders（2件）
orders_src (
  order_code, party_code, tracking_number, delivery_date,
  admin_note, warehouse_worker_note, quality_inspector_note,
  admin_username, worker_username, inspector_username,
  situation, location_name
) AS (
  VALUES
    ('OUT-1','sahosaho','SAGAWA123456'::text, DATE '2025-08-20',
     '至急出荷','棚A-01からピッキング','外装確認済',
     'admin1','worker1','worker2','作業中','A001'),
    ('IN-2','pispis','YAMATO987654'::text, DATE '2025-09-01',
     '通常納品','B-32棚','数量確認済',
     'admin2','worker2','worker1','完了','B-32')
),
ins_orders AS (
  INSERT INTO orders (
    order_code, party_id, tracking_number, delivery_date,
    admin_note, warehouse_worker_note, quality_inspector_note,
    admin_id, warehouse_worker_id, quality_inspector_id,
    situation, location_id, created_at, updated_at
  )
  SELECT
    s.order_code,
    prt.party_id,
    s.tracking_number,
    s.delivery_date,
    s.admin_note,
    s.warehouse_worker_note,
    s.quality_inspector_note,
    ua.user_id,
    uw.user_id,
    uq.user_id,
    s.situation,
    l.location_id,
    ts.now, ts.now
  FROM orders_src s
  JOIN prt_ids prt ON prt.party_code = s.party_code
  JOIN u_ids ua ON ua.username = s.admin_username
  JOIN u_ids uw ON uw.username = s.worker_username
  JOIN u_ids uq ON uq.username = s.inspector_username
  JOIN l_ids  l ON l.location_name = s.location_name
  CROSS JOIN ts
  ON CONFLICT (order_code) DO NOTHING
  RETURNING 1
),

-- ▼ Pictures
pics_src (file_name, file_type, file_size, file_path) AS (
  VALUES
    ('sample_image1.png','image/png',  12345,'uploads/sample_image1.png'),
    ('sample_image2.jpg','image/jpeg', 23456,'uploads/sample_image2.jpg')
),
ins_pics AS (
  INSERT INTO pictures (file_name, file_type, file_size, file_path, created_at, updated_at)
  SELECT s.file_name, s.file_type, s.file_size, s.file_path, ts.now, ts.now
  FROM pics_src s CROSS JOIN ts
  ON CONFLICT (file_name) DO NOTHING
  RETURNING 1
),

-- ▼ ルックアップ（orders/pictures/…）
o_ids AS (
  SELECT order_code, id AS order_id
  FROM orders
  WHERE order_code IN ('OUT-0001','IN-0002','OUT-1','IN-2')  -- 両系統を許容
),
pic_ids AS (
  SELECT file_name, id AS picture_id
  FROM pictures
  WHERE file_name IN (SELECT file_name FROM pics_src)
),

-- ▼ Order-Product（元の指定を踏襲：OUT-0001 / IN-0002）
op_src (order_code, product_code, quantity) AS (
  VALUES
    ('OUT-0001','PW66002',10),
    ('OUT-0001','PX23444', 5),
    ('IN-0002', 'PX10001', 3)
),
ins_op AS (
  INSERT INTO order_products (order_id, product_id, quantity, created_at, updated_at)
  SELECT o.order_id, p.product_id, s.quantity, ts.now, ts.now
  FROM op_src s
  JOIN o_ids o ON o.order_code = s.order_code
  JOIN p_ids p ON p.product_code = s.product_code
  CROSS JOIN ts
  ON CONFLICT (order_id, product_id) DO NOTHING
  RETURNING 1
),

-- ▼ Order-Picture（元の指定：ORD-0001 / ORD-0002）
opic_src (order_code, file_name) AS (
  VALUES
    ('ORD-0001','sample_image1.png'),
    ('ORD-0002','sample_image2.jpg')
)
INSERT INTO order_pictures (order_id, picture_id, created_at, updated_at)
SELECT o.order_id, pic.picture_id, ts.now, ts.now
FROM opic_src s
JOIN o_ids  o   ON o.order_code   = s.order_code
JOIN pic_ids pic ON pic.file_name = s.file_name
CROSS JOIN ts
ON CONFLICT (order_id, picture_id) DO NOTHING;
