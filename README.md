# CampusTrade

CampusTrade は、学内で教科書、参考書、生活用品などを学生同士で出品・購入できるフリーマーケットシステムです。

一般学生は商品の出品、検索、購入申し込み、取引メッセージのやり取りを行います。管理者は不適切な出品の確認や非表示、カテゴリ管理を行います。

## ドキュメント

- [要件定義書](./CampusTrade.md)
- [設計書一覧](./docs/README.md)
- [基本設計書](./docs/basic-design.md)
- [DB設計書](./docs/db-design.md)
- [画面遷移・URL設計書](./docs/screen-url-design.md)
- [機能詳細設計書](./docs/feature-detail-design.md)

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
│   └── basic-design.md
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

テストを実行します。

```bash
./gradlew test
```

アプリケーションを起動します。

```bash
./gradlew bootRun
```

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
