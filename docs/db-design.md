# CampusTrade DB設計書

## 1. 目的

本書は CampusTrade のデータベース構造を定義する。基本設計書で定めたユーザー、商品、商品画像、カテゴリ、メッセージを MySQL 上で管理するためのテーブル、制約、初期データ、検索条件を整理する。

## 2. 前提

| 項目 | 内容 |
|---|---|
| DBMS | MySQL |
| 文字コード | `utf8mb4` |
| 照合順序 | `utf8mb4_0900_ai_ci` |
| ID採番 | `BIGINT AUTO_INCREMENT` |
| 日時 | `TIMESTAMP` |
| 画像保存 | DB の `LONGBLOB` に保存 |

## 3. テーブル一覧

| テーブル | 用途 |
|---|---|
| `users` | 学生・管理者アカウント |
| `categories` | 商品カテゴリ |
| `products` | 出品商品と取引状態 |
| `product_images` | 商品画像データ |
| `messages` | 商品コメント・取引メッセージ |

## 4. 共通方針

- テーブル名とカラム名はスネークケースにする。
- パスワードはハッシュ化済み文字列だけを保存する。
- 商品は履歴保持のため物理削除しない。
- 管理者の非表示処理は `moderation_status` で表す。
- 出品者による非表示は `deleted_at` で表す。
- 商品画像はファイルパスではなく DB にバイナリとして保存する。

## 5. Enum値

### 5.1 users.role

| 値 | 意味 |
|---|---|
| `STUDENT` | 一般学生 |
| `ADMIN` | 管理者 |

### 5.2 products.trade_status

| 値 | 意味 |
|---|---|
| `OPEN` | 出品中 |
| `LOCKED` | 交渉中・取引中 |
| `CLOSED` | 取引完了 |

### 5.3 products.moderation_status

| 値 | 意味 |
|---|---|
| `ACTIVE` | 通常表示 |
| `PROHIBITED` | 管理者により禁止・非表示 |

### 5.4 messages.message_type

| 値 | 意味 |
|---|---|
| `COMMENT` | 商品詳細に表示する公開コメント |
| `TRANSACTION` | 出品者・購入者・管理者だけが閲覧できる取引メッセージ |

## 6. テーブル定義

### 6.1 users

| カラム | 型 | NULL | 制約 | 説明 |
|---|---|---|---|---|
| `id` | BIGINT | 不可 | PK, AUTO_INCREMENT | ユーザーID |
| `student_number` | VARCHAR(32) | 不可 | UNIQUE | 学生番号。ログインID |
| `password` | VARCHAR(255) | 不可 |  | ハッシュ化済みパスワード |
| `nickname` | VARCHAR(50) | 不可 |  | 画面表示名 |
| `role` | VARCHAR(20) | 不可 |  | `STUDENT` または `ADMIN` |
| `created_at` | TIMESTAMP | 不可 |  | 作成日時 |
| `updated_at` | TIMESTAMP | 不可 |  | 更新日時 |

### 6.2 categories

| カラム | 型 | NULL | 制約 | 説明 |
|---|---|---|---|---|
| `id` | BIGINT | 不可 | PK, AUTO_INCREMENT | カテゴリID |
| `name` | VARCHAR(50) | 不可 | UNIQUE | カテゴリ名 |
| `created_at` | TIMESTAMP | 不可 |  | 作成日時 |
| `updated_at` | TIMESTAMP | 不可 |  | 更新日時 |

### 6.3 products

| カラム | 型 | NULL | 制約 | 説明 |
|---|---|---|---|---|
| `id` | BIGINT | 不可 | PK, AUTO_INCREMENT | 商品ID |
| `seller_id` | BIGINT | 不可 | FK | 出品者ID |
| `buyer_id` | BIGINT | 可 | FK | 購入者ID。購入者未確定時はNULL |
| `category_id` | BIGINT | 不可 | FK | カテゴリID |
| `name` | VARCHAR(100) | 不可 |  | 商品名 |
| `description` | TEXT | 不可 |  | 商品説明 |
| `price` | INT | 不可 |  | 価格。0円以上 |
| `condition_label` | VARCHAR(50) | 不可 |  | 商品状態 |
| `trade_status` | VARCHAR(20) | 不可 |  | `OPEN`, `LOCKED`, `CLOSED` |
| `moderation_status` | VARCHAR(20) | 不可 |  | `ACTIVE`, `PROHIBITED` |
| `deleted_at` | TIMESTAMP | 可 |  | 出品者による非表示日時 |
| `created_at` | TIMESTAMP | 不可 |  | 作成日時 |
| `updated_at` | TIMESTAMP | 不可 |  | 更新日時 |

通常の商品一覧では、以下の条件を満たす商品だけを表示する。

```sql
trade_status = 'OPEN'
AND moderation_status = 'ACTIVE'
AND deleted_at IS NULL
```

### 6.4 product_images

| カラム | 型 | NULL | 制約 | 説明 |
|---|---|---|---|---|
| `id` | BIGINT | 不可 | PK, AUTO_INCREMENT | 商品画像ID |
| `product_id` | BIGINT | 不可 | FK | 対象商品ID |
| `original_filename` | VARCHAR(255) | 不可 |  | アップロード時のファイル名 |
| `content_type` | VARCHAR(100) | 不可 |  | `image/jpeg`, `image/png`, `image/webp` |
| `image_data` | LONGBLOB | 不可 |  | 画像バイナリ |
| `display_order` | INT | 不可 |  | 商品詳細での表示順 |
| `primary_flag` | BOOLEAN | 不可 |  | 一覧表示用の代表画像かどうか |
| `created_at` | TIMESTAMP | 不可 |  | 作成日時 |

画像登録ルール:

- 1商品につき最大5枚まで登録できる。
- 1枚あたりの上限は5MBとする。
- 許可する形式は JPEG、PNG、WebP とする。
- 1商品につき `primary_flag = true` は1件だけにする。
- 商品削除時は商品を論理削除するため、画像も履歴として残す。

### 6.5 messages

| カラム | 型 | NULL | 制約 | 説明 |
|---|---|---|---|---|
| `id` | BIGINT | 不可 | PK, AUTO_INCREMENT | メッセージID |
| `product_id` | BIGINT | 不可 | FK | 対象商品ID |
| `sender_id` | BIGINT | 不可 | FK | 送信者ID |
| `receiver_id` | BIGINT | 可 | FK | 受信者ID。商品コメントの場合はNULL |
| `message_type` | VARCHAR(20) | 不可 |  | `COMMENT` または `TRANSACTION` |
| `content` | TEXT | 不可 |  | メッセージ本文 |
| `read_flag` | BOOLEAN | 不可 |  | 既読フラグ |
| `created_at` | TIMESTAMP | 不可 |  | 送信日時 |

メッセージ表示ルール:

- `COMMENT` は商品詳細画面で公開表示する。
- `TRANSACTION` は出品者、購入者、管理者だけが閲覧できる。
- `TRANSACTION` では `receiver_id` を必須扱いにする。
- 投稿順は `created_at ASC` とする。

## 7. リレーション

| 親 | 子 | 関係 |
|---|---|---|
| `users.id` | `products.seller_id` | 1人のユーザーは複数の商品を出品できる |
| `users.id` | `products.buyer_id` | 1人のユーザーは複数の商品を購入できる |
| `categories.id` | `products.category_id` | 1カテゴリに複数の商品が属する |
| `products.id` | `product_images.product_id` | 1商品に複数画像が属する |
| `products.id` | `messages.product_id` | 1商品に複数メッセージが属する |
| `users.id` | `messages.sender_id` | 1ユーザーは複数メッセージを送信できる |
| `users.id` | `messages.receiver_id` | 1ユーザーは複数メッセージを受信できる |

## 8. インデックス設計

| テーブル | インデックス | 目的 |
|---|---|---|
| `users` | `UNIQUE(student_number)` | ログインIDの重複防止・検索 |
| `categories` | `UNIQUE(name)` | カテゴリ名の重複防止 |
| `products` | `INDEX(category_id, trade_status, moderation_status, deleted_at)` | 一覧・カテゴリ検索 |
| `products` | `INDEX(seller_id, created_at)` | 出品履歴表示 |
| `products` | `INDEX(buyer_id, created_at)` | 購入履歴表示 |
| `product_images` | `INDEX(product_id, display_order)` | 商品詳細画像表示 |
| `messages` | `INDEX(product_id, message_type, created_at)` | 商品コメント・取引メッセージ表示 |

## 9. 初期データ

### 9.1 categories

| name |
|---|
| 教科書 |
| 参考書 |
| 家具 |
| 家電 |
| 生活用品 |
| その他 |

### 9.2 管理者ユーザー

管理者ユーザーは初期データとして1件作成する。初期パスワードは環境ごとに変更し、平文では保存しない。

| student_number | nickname | role |
|---|---|---|
| `admin` | 管理者 | `ADMIN` |

## 10. DDL案

実装時のたたき台として以下を使用する。Spring Data JPA の自動生成を使う場合も、この定義と差異が出ないように確認する。

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_number VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_products_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_products_price CHECK (price >= 0)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    image_data LONGBLOB NOT NULL,
    display_order INT NOT NULL,
    primary_flag BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    read_flag BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
```

## 11. JPA実装メモ

- `User` と `Product` は、出品者と購入者で2つの `ManyToOne` を持つ。
- `Product` と `ProductImage` は `OneToMany` と `ManyToOne` で紐づける。
- `Product` と `Message` は `OneToMany` と `ManyToOne` で紐づける。
- Enum値は Java の enum として定義し、DB には文字列で保存する。
- 画像の `image_data` は `@Lob` を使用する。
- 一覧画面では画像本体をまとめて読み込まず、代表画像の取得処理を分ける。

