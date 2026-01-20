-- users: 通知ON/OFF
alter table public.users
add column if not exists wishlist_notify_enabled boolean not null default true;

-- wishlist: 24時間前通知の送信済み管理
alter table public.wishlist
add column if not exists notify_24h_sent_at timestamptz null;

-- 推奨: expiryをtimestamptzに統一できるなら（既にtextならこの行は実行しないでください）
-- alter table public.wishlist alter column expiry type timestamptz using expiry::timestamptz;

