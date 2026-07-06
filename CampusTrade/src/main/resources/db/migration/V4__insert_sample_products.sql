INSERT INTO users (student_number, password, nickname, role, created_at, updated_at) VALUES
('s1001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '佐藤なつ', 'STUDENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s1002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '田中ゆうき', 'STUDENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s1003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '山田あおい', 'STUDENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s1004', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '鈴木みなと', 'STUDENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (
    seller_id,
    buyer_id,
    category_id,
    name,
    description,
    price,
    condition_label,
    trade_status,
    moderation_status,
    deleted_at,
    created_at,
    updated_at
) VALUES
((SELECT id FROM users WHERE student_number = 's1001'), NULL, 110, '統計学入門 第3版', '前期の授業で使用しました。重要箇所に数ページだけマーカーがあります。', 1200, '書き込み少なめ', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1002'), NULL, 120, 'TOEIC対策 公式問題集', 'リスニング音声はオンライン版を利用していました。表紙に少し折れがあります。', 800, '使用感あり', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1003'), NULL, 130, '文房具セット', '未使用ノート2冊、ルーズリーフ、ボールペンのセットです。', 0, '未使用品あり', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1004'), NULL, 140, 'MacBook Air 13インチ', '授業用に使っていました。初期化して受け渡します。充電器付きです。', 58000, '動作確認済み', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1001'), NULL, 310, '電気ケトル 0.8L', '一人暮らしで半年ほど使用しました。動作確認済みです。', 1500, '良好', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1002'), NULL, 320, 'キッチン用品セット', 'マグカップ、皿、軽量カップのセットです。引っ越し整理のため出品します。', 700, '使用感あり', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1003'), NULL, 330, '収納ボックス 3個セット', 'クローゼット整理に使える折りたたみ式収納ボックスです。', 500, '良好', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1004'), NULL, 410, 'マンガ全巻セット', '全10巻セットです。日焼けは少なめです。', 2200, '良好', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1001'), NULL, 420, 'ゲームコントローラー', 'PC接続で動作確認済みです。箱はありません。', 1800, '動作確認済み', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1002'), NULL, 510, 'ハンドメイドキーホルダー', '学園祭用に作った余りです。複数個あります。', 300, '新品', 'OPEN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1002'), (SELECT id FROM users WHERE student_number = 's1001'), 330, 'デスクライト', '明るさ調整ができるLEDライトです。図書館前で受け渡し希望です。', 0, '動作確認済み', 'LOCKED', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1003'), (SELECT id FROM users WHERE student_number = 's1004'), 230, '就活用バッグ', '面接で数回使いました。A4書類が入ります。', 1000, '良好', 'CLOSED', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1004'), NULL, 190, '管理確認用の商品', '管理者の禁止表示確認用の商品です。一般一覧には表示されません。', 9999, '確認用', 'OPEN', 'PROHIBITED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE student_number = 's1001'), NULL, 120, '非表示確認用の参考書', '出品者による非表示状態の確認用データです。', 400, '確認用', 'OPEN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO messages (product_id, sender_id, receiver_id, message_type, content, read_flag, created_at) VALUES
((SELECT id FROM products WHERE name = '統計学入門 第3版'), (SELECT id FROM users WHERE student_number = 's1002'), NULL, 'COMMENT', '授業の第何回くらいまで使いましたか？', FALSE, CURRENT_TIMESTAMP),
((SELECT id FROM products WHERE name = '統計学入門 第3版'), (SELECT id FROM users WHERE student_number = 's1001'), NULL, 'COMMENT', '第12回まで使いました。章末問題も確認できます。', FALSE, CURRENT_TIMESTAMP),
((SELECT id FROM products WHERE name = 'デスクライト'), (SELECT id FROM users WHERE student_number = 's1001'), (SELECT id FROM users WHERE student_number = 's1002'), 'TRANSACTION', '本日17時に図書館前で受け取れます。', FALSE, CURRENT_TIMESTAMP),
((SELECT id FROM products WHERE name = 'デスクライト'), (SELECT id FROM users WHERE student_number = 's1002'), (SELECT id FROM users WHERE student_number = 's1001'), 'TRANSACTION', '承知しました。入口横の掲示板付近で待っています。', FALSE, CURRENT_TIMESTAMP);
