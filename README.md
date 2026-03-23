# SpendWise

## Goal
Build a personal finance platform that cleans bank-statement data, stores it in Supabase, and exposes transaction + analysis APIs through FastAPI and Spring Boot. Also create a central layer to unify UPI transactions from providers like GPay and Paytm, then deliver accurate spending categorization.

## What is completed so far
- Data preprocessing notebooks are available in `ml_preprocessing` for extraction, cleaning, validation, and export.
- FastAPI service is implemented for:
  - single transaction create
  - list transactions (pagination)
  - get transaction by id
  - transaction logic view (`direction`, `effective_amount`, `size_bucket`)
  - bulk Excel load (`/load-excel`)
- Spring Boot backend is implemented for:
  - create transaction
  - get transaction by id
  - paginated transaction list
  - transaction logic endpoint
- Supabase/Postgres integration is active in both services.

## Current project structure
- `SpendWise_Backend/` → Java Spring Boot API (JPA + PostgreSQL)
- `ml_service/` → Python FastAPI service for ingestion and logic
- `ml_preprocessing/` → notebooks used for data cleaning and preparation

## API summary

### Spring Boot (`/api/transactions`)
- `POST /api/transactions`
- `GET /api/transactions/{id}`
- `GET /api/transactions?page=0&size=20`
- `GET /api/transactions/{id}/logic`

### FastAPI
- `GET /` health
- `POST /transactions`
- `GET /transactions?limit=50&offset=0`
- `GET /transactions/{transaction_id}`
- `GET /transactions/{transaction_id}/logic`
- `POST /load-excel`

## Supabase schema
Use this schema in your Supabase SQL editor:

```sql
create extension if not exists pgcrypto;

create table if not exists accounts (
  id uuid primary key default gen_random_uuid(),
  bank_name text not null,
  account_type text,
  created_at timestamptz not null default now()
);

create table if not exists recipients (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  upi_id text,
  bank_name text,
  created_at timestamptz not null default now()
);

create table if not exists transactions (
  id uuid primary key default gen_random_uuid(),
  account_id uuid not null references accounts(id),
  recipient_id uuid not null references recipients(id),
  transaction_reference text not null,
  transaction_date date,
  amount numeric(14,2),
  debit numeric(14,2),
  credit numeric(14,2),
  balance numeric(14,2),
  transaction_mode text,
  dr_cr_indicator text,
  note text,
  created_at timestamptz not null default now()
);

create index if not exists idx_accounts_bank_name on accounts(bank_name);
create index if not exists idx_recipients_upi_id on recipients(upi_id);
create index if not exists idx_transactions_account_id on transactions(account_id);
create index if not exists idx_transactions_recipient_id on transactions(recipient_id);
create index if not exists idx_transactions_created_at on transactions(created_at desc);
```

## Environment variables

### Spring Boot (`SpendWise_Backend/src/main/resources/application.properties`)
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT`
- `FASTAPI_BASE_URL`
- `FASTAPI_CONNECT_TIMEOUT_MS`
- `FASTAPI_READ_TIMEOUT_MS`

### FastAPI (`ml_service/.env`)
- `SUPABASE_URL`
- `SUPABASE_KEY`
- `ACCOUNT_ID`

## Run locally

### Spring Boot backend
```bash
cd SpendWise_Backend
mvnw.cmd spring-boot:run
```

### FastAPI service
```bash
cd ml_service
uvicorn app.main:app --reload
```

## Notes
- Keep credentials only in environment variables.
- `ml_service/app/excel_loader.py` currently loads first 10 rows (`df.iloc[:10]`) for testing.