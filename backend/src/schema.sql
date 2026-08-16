CREATE TABLE IF NOT EXISTS users (
 id BIGSERIAL PRIMARY KEY,
 name TEXT NOT NULL,
 email TEXT UNIQUE NOT NULL,
 password_hash TEXT NOT NULL,
 role TEXT NOT NULL DEFAULT 'user' CHECK(role IN ('user','admin')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS papers (
 id BIGSERIAL PRIMARY KEY,
 title TEXT NOT NULL,
 subject TEXT NOT NULL,
 semester TEXT,
 price_paise INTEGER NOT NULL CHECK(price_paise >= 0),
 pdf_url TEXT,
 active BOOLEAN NOT NULL DEFAULT true,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS orders (
 id BIGSERIAL PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id),
 paper_id BIGINT NOT NULL REFERENCES papers(id),
 razorpay_order_id TEXT UNIQUE,
 amount_paise INTEGER NOT NULL,
 status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','paid','rejected')),
 payment_ref TEXT UNIQUE,
 upi_txn_id TEXT UNIQUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 approved_at TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS purchases (
 id BIGSERIAL PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id),
 paper_id BIGINT NOT NULL REFERENCES papers(id),
 razorpay_payment_id TEXT UNIQUE,
 payment_ref TEXT UNIQUE,
 purchased_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(user_id,paper_id)
);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'user';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS razorpay_order_id TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_ref TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS upi_txn_id TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS payment_ref TEXT;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS razorpay_payment_id TEXT;
INSERT INTO papers(title,subject,semester,price_paise) SELECT 'B.Sc. Chemistry Guess Paper','Chemistry','1st Semester',4900 WHERE NOT EXISTS (SELECT 1 FROM papers WHERE title='B.Sc. Chemistry Guess Paper');
INSERT INTO papers(title,subject,semester,price_paise) SELECT 'B.Sc. Mathematics Guess Paper','Mathematics','1st Semester',4900 WHERE NOT EXISTS (SELECT 1 FROM papers WHERE title='B.Sc. Mathematics Guess Paper');
INSERT INTO papers(title,subject,semester,price_paise) SELECT 'B.Sc. Physics Guess Paper','Physics','1st Semester',4900 WHERE NOT EXISTS (SELECT 1 FROM papers WHERE title='B.Sc. Physics Guess Paper');
INSERT INTO papers(title,subject,semester,price_paise) SELECT 'B.A. Hindi Guess Paper','Hindi','1st Semester',500 WHERE NOT EXISTS (SELECT 1 FROM papers WHERE title='B.A. Hindi Guess Paper');
