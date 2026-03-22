import os
from dotenv import load_dotenv
import math

from app.supabase_client import supabase

load_dotenv()

ACCOUNT_ID = os.getenv("ACCOUNT_ID")

if ACCOUNT_ID is None:
    raise ValueError("ACCOUNT_ID not found in .env")


# -----------------------------
# GLOBAL SAFE CLEAN FUNCTION
# -----------------------------
def safe_value(v):
    if v is None:
        return None
    if isinstance(v, float) and (math.isnan(v) or math.isinf(v)):
        return None
    if str(v).lower() == "nan":
        return None
    return v

def get_or_create_account(bank_name):

    bank_name = safe_value(bank_name)

    if not bank_name:
        bank_name = "UNKNOWN_BANK"

    res = (
        supabase.table("accounts")
        .select("id")
        .eq("bank_name", bank_name)
        .execute()
    )

    if res.data:
        return res.data[0]["id"]

    new_account = (
        supabase.table("accounts")
        .insert({
            "bank_name": bank_name,
            "account_type": "SAVINGS"
        })
        .execute()
    )

    return new_account.data[0]["id"]

def get_or_create_recipient(name, upi_id, bank):

    name = safe_value(name) or "UNKNOWN"
    upi_id = safe_value(upi_id)
    bank = safe_value(bank) or "UNKNOWN_BANK"

    if upi_id:
        res = (
            supabase.table("recipients")
            .select("id")
            .eq("upi_id", upi_id)
            .execute()
        )

        if res.data:
            return res.data[0]["id"]

    new_recipient = (
        supabase.table("recipients")
        .insert({
            "name": name,
            "upi_id": upi_id,
            "bank_name": bank
        })
        .execute()
    )

    return new_recipient.data[0]["id"]


def insert_transaction(account_id, recipient_id, row):

    transaction_data = {
        "account_id": account_id,
        "recipient_id": recipient_id,
        "transaction_reference": str(row.get("transaction_reference")),
        "transaction_date": safe_value(row.get("transaction_date")),
        "amount": safe_value(row.get("amount")),
        "debit": safe_value(row.get("debit")),
        "credit": safe_value(row.get("credit")),
        "balance": safe_value(row.get("balance")),
        "transaction_mode": safe_value(row.get("transaction_mode")),
        "dr_cr_indicator": safe_value(row.get("dr_cr_indicator")),
        "note": safe_value(row.get("note")),
    }

    return (
        supabase
        .table("transactions")
        .insert(transaction_data)
        .execute()
    )


def create_single_transaction(row):
    bank = safe_value(row.get("bank"))
    upi_id = safe_value(row.get("upi_id"))
    name = safe_value(row.get("recipient_name"))

    account_id = get_or_create_account(bank)
    recipient_id = get_or_create_recipient(name, upi_id, bank)

    result = insert_transaction(account_id, recipient_id, row)
    if not result.data:
        raise ValueError("Transaction insert failed")

    return result.data[0]


def get_transaction_by_id(transaction_id):
    result = (
        supabase
        .table("transactions")
        .select("*")
        .eq("id", transaction_id)
        .limit(1)
        .execute()
    )

    if not result.data:
        return None

    return result.data[0]


def list_transactions(limit=50, offset=0):
    end = offset + limit - 1
    result = (
        supabase
        .table("transactions")
        .select("*")
        .order("id", desc=True)
        .range(offset, end)
        .execute()
    )

    return result.data or []


def get_transaction_logic(transaction):
    debit = safe_value(transaction.get("debit")) or 0
    credit = safe_value(transaction.get("credit")) or 0
    amount = safe_value(transaction.get("amount")) or 0

    if debit > 0:
        direction = "DEBIT"
        effective_amount = debit
    elif credit > 0:
        direction = "CREDIT"
        effective_amount = credit
    else:
        direction = safe_value(transaction.get("dr_cr_indicator")) or "UNKNOWN"
        effective_amount = amount

    if effective_amount >= 100000:
        bucket = "HIGH"
    elif effective_amount >= 10000:
        bucket = "MEDIUM"
    else:
        bucket = "LOW"

    return {
        "transaction_id": transaction.get("id"),
        "transaction_reference": transaction.get("transaction_reference"),
        "direction": direction,
        "effective_amount": effective_amount,
        "size_bucket": bucket,
        "transaction_mode": safe_value(transaction.get("transaction_mode")) or "UNKNOWN",
        "note": safe_value(transaction.get("note"))
    }