# CampusTrade 画面遷移・URL設計書

## 1. 目的

本書は CampusTrade の画面遷移、URL、HTTPメソッド、認証要否を定義する。ワイヤーフレームではなく、Controller 実装時に必要なルーティングと遷移条件を整理する。

## 2. 認証区分

| 表記 | 意味 |
|---|---|
| 不要 | 未ログインでもアクセスできる |
| 必要 | ログイン済みユーザーだけアクセスできる |
| 購入者 | 対象商品の購入者または管理者だけアクセスできる |
| 出品者 | 対象商品の出品者または管理者だけアクセスできる |
| 管理者 | 管理者権限のユーザーだけアクセスできる |
| 関係者 | 対象商品の出品者、購入者、管理者だけアクセスできる |

## 3. 画面一覧

| 画面ID | 画面名 | URL | 認証 |
|---|---|---|---|
| `TOP` | トップ画面 | `/` | 不要 |
| `PRODUCT_LIST` | 商品検索結果画面 | `/products` | 不要 |
| `PRODUCT_DETAIL` | 商品詳細画面 | `/products/{id}` | 不要 |
| `PRODUCT_IMAGE` | 商品画像表示 | `/products/{productId}/images/{imageId}` | 不要 |
| `PRODUCT_NEW` | 商品出品画面 | `/products/new` | 必要 |
| `PRODUCT_EDIT` | 商品編集画面 | `/products/{id}/edit` | 必要 |
| `MYPAGE` | マイページ | `/mypage` | 必要 |
| `BUYER_PURCHASES` | 購入者ページ | `/mypage/purchases` | 必要 |
| `BUYER_TRANSACTION_DETAIL` | 購入者取引詳細 | `/mypage/purchases/{productId}` | 購入者 |
| `SELLER_SALES` | 出品者ページ | `/mypage/sales` | 必要 |
| `SELLER_TRANSACTION_DETAIL` | 出品者取引詳細 | `/mypage/sales/{productId}` | 出品者 |
| `LOGIN` | ログイン画面 | `/login` | 不要 |
| `REGISTER` | 会員登録画面 | `/register` | 不要 |
| `ADMIN_PRODUCTS` | 管理者商品一覧 | `/admin/products` | 管理者 |
| `ADMIN_CATEGORIES` | 管理者カテゴリ管理 | `/admin/categories` | 管理者 |

## 4. URL一覧

### 4.1 認証

| メソッド | URL | 認証 | 処理 | 正常時遷移 |
|---|---|---|---|---|
| GET | `/login` | 不要 | ログイン画面を表示 | `LOGIN` |
| POST | `/login` | 不要 | Spring Security によるログイン処理 | `/` |
| POST | `/logout` | 必要 | ログアウト処理 | `/login?logout` |
| GET | `/register` | 不要 | 会員登録画面を表示 | `REGISTER` |
| POST | `/register` | 不要 | 会員登録を実行 | `/login?registered` |

### 4.2 商品

| メソッド | URL | 認証 | 処理 | 正常時遷移 |
|---|---|---|---|---|
| GET | `/` | 不要 | 新着商品の一覧を表示 | `TOP` |
| GET | `/products` | 不要 | キーワード・カテゴリで検索 | `PRODUCT_LIST` |
| GET | `/products/{id}` | 不要 | 商品詳細を表示 | `PRODUCT_DETAIL` |
| GET | `/products/{productId}/images/{imageId}` | 不要 | DBから画像を返す | 画像レスポンス |
| GET | `/products/new` | 必要 | 商品出品画面を表示 | `PRODUCT_NEW` |
| POST | `/products` | 必要 | 商品と画像を登録 | `/products/{id}` |
| GET | `/products/{id}/edit` | 必要 | 商品編集画面を表示 | `PRODUCT_EDIT` |
| POST | `/products/{id}/edit` | 必要 | 商品情報を更新 | `/products/{id}` |
| POST | `/products/{id}/delete` | 必要 | 商品を論理削除 | `/mypage` |

### 4.3 取引

| メソッド | URL | 認証 | 処理 | 正常時遷移 |
|---|---|---|---|---|
| POST | `/products/{id}/purchase` | 必要 | 購入申し込み | `/mypage/purchases/{id}` |
| GET | `/mypage/purchases/{productId}` | 購入者 | 購入者向け取引詳細を表示 | `BUYER_TRANSACTION_DETAIL` |
| GET | `/mypage/sales/{productId}` | 出品者 | 出品者向け取引詳細を表示 | `SELLER_TRANSACTION_DETAIL` |
| POST | `/products/{id}/cancel` | 関係者 | 取引キャンセル | 実行元に応じた取引一覧または取引詳細 |
| POST | `/products/{id}/close` | 関係者 | 取引完了 | 実行元に応じた取引詳細 |

### 4.4 メッセージ

| メソッド | URL | 認証 | 処理 | 正常時遷移 |
|---|---|---|---|---|
| POST | `/products/{id}/comments` | 必要 | 商品コメントを投稿 | `/products/{id}` |
| POST | `/mypage/purchases/{productId}/messages` | 購入者 | 購入者側から取引メッセージを投稿 | `/mypage/purchases/{productId}` |
| POST | `/mypage/sales/{productId}/messages` | 出品者 | 出品者側から取引メッセージを投稿 | `/mypage/sales/{productId}` |

### 4.5 マイページ

| メソッド | URL | 認証 | 処理 | 正常時遷移 |
|---|---|---|---|---|
| GET | `/mypage` | 必要 | 購入者ページ、出品者ページ、取引中商品への入口を表示 | `MYPAGE` |
| GET | `/mypage/purchases` | 必要 | 自分が購入者になっている取引を表示 | `BUYER_PURCHASES` |
| GET | `/mypage/sales` | 必要 | 自分が出品した商品と成立済み取引を表示 | `SELLER_SALES` |

### 4.6 管理者

| メソッド | URL | 認証 | 処理 | 正常時遷移 |
|---|---|---|---|---|
| GET | `/admin/products` | 管理者 | 管理者向け商品一覧を表示 | `ADMIN_PRODUCTS` |
| POST | `/admin/products/{id}/prohibit` | 管理者 | 商品を禁止表示にする | `/admin/products` |
| POST | `/admin/products/{id}/activate` | 管理者 | 禁止表示を解除する | `/admin/products` |
| GET | `/admin/categories` | 管理者 | カテゴリ一覧を表示 | `ADMIN_CATEGORIES` |
| POST | `/admin/categories` | 管理者 | カテゴリを追加 | `/admin/categories` |
| POST | `/admin/categories/{id}/edit` | 管理者 | カテゴリ名を更新 | `/admin/categories` |
| POST | `/admin/categories/{id}/delete` | 管理者 | カテゴリを削除 | `/admin/categories` |

## 5. 主要画面遷移

### 5.1 商品閲覧から購入まで

```mermaid
flowchart TD
    A["トップ画面"] --> B["商品検索結果画面"]
    A --> C["商品詳細画面"]
    B --> C
    C --> D{"ログイン済み?"}
    D -->|いいえ| E["ログイン画面"]
    D -->|はい| F["購入申し込み"]
    F --> G["購入者取引詳細"]
```

### 5.2 出品

```mermaid
flowchart TD
    A["ログイン済みユーザー"] --> B["商品出品画面"]
    B --> C{"入力エラーあり?"}
    C -->|はい| B
    C -->|いいえ| D["商品登録"]
    D --> E["商品詳細画面"]
```

### 5.3 取引

```mermaid
flowchart TD
    A["商品詳細画面"] --> B["購入申し込み"]
    B --> C["購入者取引詳細"]
    S["出品者ページ"] --> T["出品者取引詳細"]
    C --> D["取引メッセージ"]
    T --> D
    C --> E["取引キャンセル"]
    T --> E
    C --> F["取引完了"]
    T --> F
    E --> G["OPEN"]
    F --> H["CLOSED"]
```

### 5.4 管理者確認

```mermaid
flowchart TD
    A["管理者商品一覧"] --> B["商品詳細確認"]
    B --> C{"違反あり?"}
    C -->|はい| D["禁止表示"]
    C -->|いいえ| A
    D --> A
```

## 6. 画面別表示条件

### 6.1 商品一覧・検索結果

表示対象:

- `trade_status = OPEN`
- `moderation_status = ACTIVE`
- `deleted_at IS NULL`

表示項目:

- 代表画像
- 商品名
- 価格
- カテゴリ
- 商品状態
- 出品日時

検索条件:

- キーワード
- カテゴリID
- 販売中のみ

### 6.2 商品詳細

表示項目:

- 商品画像一覧
- 商品名
- 説明
- 価格
- カテゴリ
- 商品状態
- 出品者ニックネーム
- 取引ステータス
- 商品コメント

ボタン表示:

| 条件 | 表示する操作 |
|---|---|
| 未ログイン | ログインへの導線 |
| ログイン済み・他人の商品・`OPEN` | 購入申し込み |
| 出品者本人・`OPEN` | 編集、非表示 |
| 出品者本人または購入者・`LOCKED` | 取引詳細への導線 |
| `CLOSED` | 取引完了表示のみ |
| `PROHIBITED` | 一般画面では非表示 |

### 6.3 マイページ

表示項目:

- 購入者ページへの導線
- 出品者ページへの導線
- 進行中取引のサマリー
- 取引完了商品のサマリー

### 6.4 購入者ページ

表示対象:

- `buyer_id = ログインユーザーID`
- `trade_status IN (LOCKED, CLOSED)`

表示項目:

- 購入した商品
- 出品者ニックネーム
- 取引ステータス
- 最終メッセージ日時
- 購入者取引詳細への導線

### 6.5 出品者ページ

表示対象:

- `seller_id = ログインユーザーID`
- `deleted_at IS NULL`

表示項目:

- 自分の出品商品
- 購入者ニックネーム（成立済み取引のみ）
- 取引ステータス
- 最終メッセージ日時（成立済み取引のみ）
- 出品者取引詳細への導線（`LOCKED` または `CLOSED` のみ）

### 6.6 取引詳細

購入者取引詳細と出品者取引詳細は、URL と表示する補助情報を分けるが、同じ商品取引のメッセージスレッドを参照する。

取引詳細は `trade_status = LOCKED` または `CLOSED` の商品だけ表示する。

共通表示項目:

- 商品概要
- 取引ステータス
- 取引メッセージ
- メッセージ入力フォーム
- 取引キャンセル
- 取引完了

購入者向け表示項目:

- 出品者ニックネーム
- 受け渡し調整のメッセージ

出品者向け表示項目:

- 購入者ニックネーム
- 発送または受け渡し対応のメッセージ

## 7. エラー時遷移

| ケース | 遷移 |
|---|---|
| 未ログインで認証必須URLにアクセス | `/login` |
| 権限のない商品編集 | 403ページ |
| 存在しない商品ID | 404ページ |
| 入力チェックエラー | 元の入力画面 |
| 購入できない商品への購入申し込み | 商品詳細画面にエラー表示 |
| 画像形式・サイズエラー | 商品出品または編集画面にエラー表示 |

## 8. Controller配置案

| Controller | 担当URL |
|---|---|
| `AuthController` | `/login`, `/register` |
| `ProductController` | `/`, `/products/**` |
| `ProductImageController` | `/products/{productId}/images/{imageId}` |
| `TransactionController` | `/products/{id}/purchase`, `/products/{id}/cancel`, `/products/{id}/close`, `/mypage/purchases/{productId}`, `/mypage/sales/{productId}` |
| `MessageController` | `/products/{id}/comments`, `/mypage/purchases/{productId}/messages`, `/mypage/sales/{productId}/messages` |
| `MypageController` | `/mypage`, `/mypage/purchases`, `/mypage/sales` |
| `AdminController` | `/admin/**` |
