CREATE TABLE IF NOT EXISTS users (id BIGSERIAL PRIMARY KEY,email TEXT UNIQUE NOT NULL,password_hash TEXT NOT NULL,created_at TIMESTAMPTZ DEFAULT now());
CREATE TABLE IF NOT EXISTS papers (id BIGSERIAL PRIMARY KEY,title TEXT NOT NULL,subject TEXT NOT NULL,price_inr INTEGER NOT NULL CHECK(price_inr>=0),pdf_url TEXT,created_at TIMESTAMPTZ DEFAULT now());
CREATE TABLE IF NOT EXISTS orders (id BIGSERIAL PRIMARY KEY,user_id BIGINT REFERENCES users(id),paper_id BIGINT REFERENCES papers(id) NOT NULL,provider_order_id TEXT UNIQUE NOT NULL,provider_payment_id TEXT,amount_inr INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'created',created_at TIMESTAMPTZ DEFAULT now());
CREATE INDEX IF NOT EXISTS orders_user_idx ON orders(user_id);
CREATE TABLE IF NOT EXISTS entitlements (user_id BIGINT REFERENCES users(id),paper_id BIGINT REFERENCES papers(id),order_id BIGINT REFERENCES orders(id),granted_at TIMESTAMPTZ DEFAULT now(),PRIMARY KEY(user_id,paper_id));
