# Uniraj Guess Paper

Production Android + backend foundation for Rajasthan University guess papers.

## Architecture
- Android native app (Kotlin + Jetpack Compose)
- Node.js/Express API
- PostgreSQL-ready data model
- Razorpay-ready server-side order/payment verification
- Purchased-paper entitlement model
- Admin API foundation

## Required production secrets
Create environment variables on the backend; never commit them:
- `DATABASE_URL`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `JWT_SECRET`
- `PDF_STORAGE_BUCKET` / storage credentials

## Payment flow
1. App requests an order from backend.
2. Backend creates the Razorpay order.
3. App completes payment in Razorpay Checkout.
4. App sends payment identifiers to backend.
5. Backend verifies the signature using the secret key.
6. Backend records the purchase and grants access to the paper.

The repository is intentionally not shipped with real payment keys or private PDFs.
