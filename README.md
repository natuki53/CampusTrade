# CampusTrade

CampusTrade は、学内で教科書、参考書、生活用品などを学生同士で出品・購入できるフリーマーケットシステムです。

一般学生は商品の出品、検索、購入申し込み、取引メッセージのやり取りを行います。管理者は不適切な出品の確認や非表示、カテゴリ管理を行います。

## ドキュメント

- [要件定義書](./CampusTrade.md)
- [設計書一覧](./docs/README.md)
- [基本設計書](./docs/basic-design.md)
- [DB設計書](./docs/db-design.md)
- [カテゴリ設計書](./docs/category-design.md)
- [画面遷移・URL設計書](./docs/screen-url-design.md)
- [機能詳細設計書](./docs/feature-detail-design.md)
- [サンプルデータ画像の出典](./docs/sample-data-sources.md)
- [UIプロトタイプ](./prototype/index.html)

## 技術構成

| 区分 | 技術 |
|---|---|
| アプリケーション | Spring Boot |
| 画面テンプレート | Thymeleaf |
| 認証・認可 | Spring Security |
| データアクセス | Spring Data JPA |
| データベース | MySQL |
| ビルド | Gradle |
| Java | Java 21 |

## ディレクトリ構成

```text
.
├── CampusTrade.md
├── README.md
├── docs
│   ├── README.md
│   ├── basic-design.md
│   ├── db-design.md
│   ├── screen-url-design.md
│   └── feature-detail-design.md
├── prototype
│   ├── README.md
│   ├── index.html
│   ├── styles.css
│   └── app.js
└── CampusTrade
    ├── build.gradle
    ├── settings.gradle
    ├── gradlew
    └── src
```

## 開発環境

- Java 21
- MySQL
- Git

Gradle はプロジェクト同梱の Gradle Wrapper を使用します。

## セットアップ

リポジトリを取得します。

```bash
git clone https://github.com/natuki53/CampusTrade.git
cd CampusTrade
```

Spring Boot プロジェクトのディレクトリへ移動します。

```bash
cd CampusTrade
```

MySQL に開発用データベースとユーザーを作成します。

```sql
CREATE DATABASE campustrade CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'campustrade'@'localhost' IDENTIFIED BY 'campustrade';
GRANT ALL PRIVILEGES ON campustrade.* TO 'campustrade'@'localhost';
FLUSH PRIVILEGES;
```

接続情報を変える場合は、以下の環境変数を設定します。

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/campustrade?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo'
export SPRING_DATASOURCE_USERNAME='campustrade'
export SPRING_DATASOURCE_PASSWORD='campustrade'
```

テストを実行します。

```bash
./gradlew test
```

アプリケーションを起動します。

```bash
./gradlew bootRun
```

初回起動時に Flyway がテーブルと初期カテゴリを作成します。初期管理者は以下です。

| 項目 | 値 |
|---|---|
| 学生番号 | `admin` |
| パスワード | `password` |

商品テストデータとして、出品中・取引中・取引完了・管理確認用の商品が登録されます。サンプル学生ユーザーのパスワードはいずれも `password` です。

| 学生番号 | ニックネーム |
|---|---|
| `s1001` | 佐藤なつ |
| `s1002` | 田中ゆうき |
| `s1003` | 山田あおい |
| `s1004` | 鈴木みなと |

商品画像の出典は [サンプルデータ画像の出典](./docs/sample-data-sources.md) に記載しています。

## 主な機能

- ユーザー登録・ログイン
- プロフィール編集
- 商品出品・編集・非表示
- 商品検索・一覧表示
- 商品詳細表示
- 購入申し込み
- 取引ステータス管理
- 商品コメント
- 取引メッセージ
- 管理者による商品・カテゴリ管理

## 取引ステータス

| ステータス | 説明 |
|---|---|
| `OPEN` | 出品中 |
| `LOCKED` | 交渉中・取引中 |
| `CLOSED` | 取引完了 |

## 備考

このリポジトリでは、要件定義と設計資料をルートおよび `docs/` に置き、実装コードは `CampusTrade/` 配下で管理します。

## 手動確認シナリオ

1. `/register` で学生ユーザーを登録し、`/login` からログインする。
2. `/products/new` で商品を出品し、トップ画面と `/products` に表示されることを確認する。
3. 別ユーザーでログインし、商品詳細から購入申し込みを行う。
4. `/mypage/purchases/{id}` と `/mypage/sales/{id}` で取引メッセージを送受信する。
5. 取引完了を実行し、商品が完了状態になることを確認する。
6. `admin` でログインし、`/admin/products` から商品を禁止表示・解除する。
7. `/admin/categories` でカテゴリを追加・編集し、商品が紐づくカテゴリは削除できないことを確認する。
