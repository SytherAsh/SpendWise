import logging
from datetime import datetime
from typing import List

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from app.schemas.transaction import SmsPayload, ParsedTransaction
from app.sms_parser import parse_sms_body
from app.supabase_client import supabase
from app.service import create_single_transaction, safe_value

logger = logging.getLogger(__name__)
router = APIRouter()

# -------------------------------------------------------
# Supabase table for raw incoming SMS
# -------------------------------------------------------
RAW_TABLE = "raw_sms"


class IngestResponse(BaseModel):
    """Response after ingesting a single SMS"""
    status: str
    raw_id: str
    is_financial: bool
    transaction_id: str | None = None
    parsed: ParsedTransaction | None = None


class BulkIngestResponse(BaseModel):
    """Response after ingesting a batch of SMS"""
    total: int
    financial: int
    non_financial: int
    transactions_created: int
    errors: list[str]


class RawSmsOut(BaseModel):
    """Shape returned when listing raw SMS records"""
    id: str
    source: str
    sender: str | None
    body: str
    timestamp_ms: int
    timestamp_human: str
    device_id: str
    is_financial: bool
    parsed_amount: float | None
    parsed_direction: str | None
    parsed_bank: str | None
    transaction_id: str | None
    created_at: str | None


# -------------------------------------------------------
# POST /api/data — Single notification/SMS ingest
# This is the endpoint the Android app POSTs to
# -------------------------------------------------------
import csv
import os

# Local CSV file for EDA
CSV_FILE = "captured_sms.csv"

def save_to_csv(data_dict):
    """Appends a single record to the local CSV file"""
    file_exists = os.path.isfile(CSV_FILE)
    with open(CSV_FILE, mode='a', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=data_dict.keys())
        if not file_exists:
            writer.writeheader()
        writer.writerow(data_dict)

@router.post("/api/data", response_model=IngestResponse)
async def ingest_single(payload: SmsPayload):
    """
    Receive a single SMS and save it to a LOCAL CSV for EDA.
    Supabase logic is currently disabled.
    """
    try:
        # Parse the body text for financial information
        parsed = parse_sms_body(payload.body, sender=payload.sender)

        # Build the record
        record = {
            "id": payload.id,
            "sender": payload.sender,
            "body": payload.body,
            "timestamp_human": payload.timestamp_human,
            "is_financial": parsed.is_financial,
            "amount": parsed.amount,
            "direction": parsed.direction,
            "bank": parsed.bank,
            "upi_id": parsed.upi_id,
            "recipient": parsed.recipient_name,
        }

        # SAVE TO LOCAL CSV
        save_to_csv(record)
        logger.info(f"✅ Saved to CSV: {payload.id} | {payload.sender}")

        return IngestResponse(
            status="ok",
            raw_id=payload.id,
            is_financial=parsed.is_financial,
            parsed=parsed,
        )

    except Exception as e:
        logger.error(f"Ingest failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# -------------------------------------------------------
# POST /api/data/bulk — Batch ingest multiple records at once
# -------------------------------------------------------
@router.post("/api/data/bulk", response_model=BulkIngestResponse)
async def ingest_bulk(payloads: List[SmsPayload]):
    """
    Receive multiple SMS and save them to CSV in a single batch operation.
    """
    financial_count = 0
    non_financial_count = 0
    errors = []
    records_to_save = []

    for payload in payloads:
        try:
            parsed = parse_sms_body(payload.body, sender=payload.sender)
            if parsed.is_financial:
                financial_count += 1
            else:
                non_financial_count += 1
            
            records_to_save.append({
                "id": payload.id,
                "sender": payload.sender,
                "body": payload.body,
                "timestamp_ms": payload.timestamp_ms,
                "timestamp_human": payload.timestamp_human,
                "device_id": payload.device_id,
                "is_financial": parsed.is_financial,
                "amount": parsed.amount,
                "direction": parsed.direction,
                "bank": parsed.bank,
                "upi_id": parsed.upi_id,
                "recipient": parsed.recipient_name,
            })
        except Exception as e:
            errors.append(f"{payload.id}: {str(e)}")

    # BATCH WRITE TO CSV
    if records_to_save:
        try:
            file_exists = os.path.isfile(CSV_FILE)
            with open(CSV_FILE, mode='a', newline='', encoding='utf-8') as f:
                writer = csv.DictWriter(f, fieldnames=records_to_save[0].keys())
                if not file_exists:
                    writer.writeheader()
                writer.writerows(records_to_save)
            logger.info(f"📁 Batch saved: {len(records_to_save)} records added to CSV")
        except Exception as e:
            logger.error(f"Failed to write batch to CSV: {e}")
            errors.append(f"CSV_WRITE_ERROR: {str(e)}")

    return BulkIngestResponse(
        total=len(payloads),
        financial=financial_count,
        non_financial=non_financial_count,
        transactions_created=0, # Supabase disabled
        errors=errors,
    )


# -------------------------------------------------------
# GET /api/data — List raw notifications/SMS with pagination
# -------------------------------------------------------
@router.get("/api/data")
async def list_raw(
    limit: int = Query(default=50, ge=1, le=500),
    offset: int = Query(default=0, ge=0),
    financial_only: bool = Query(default=False, description="Return only financial messages"),
    device_id: str | None = Query(default=None, description="Filter by device"),
):
    """List raw captured SMS with optional filters"""
    query = supabase.table(RAW_TABLE).select("*")

    if financial_only:
        query = query.eq("is_financial", True)
    if device_id:
        query = query.eq("device_id", device_id)

    end = offset + limit - 1
    result = query.order("timestamp_ms", desc=True).range(offset, end).execute()

    return {
        "count": len(result.data or []),
        "offset": offset,
        "items": result.data or [],
    }


# -------------------------------------------------------
# GET /api/data/{record_id} — Get a single raw record by ID
# -------------------------------------------------------
@router.get("/api/data/{record_id}")
async def get_raw(record_id: str):
    """Retrieve a single raw notification/SMS record by its UUID"""
    result = (
        supabase.table(RAW_TABLE)
        .select("*")
        .eq("id", record_id)
        .limit(1)
        .execute()
    )
    if not result.data:
        raise HTTPException(status_code=404, detail="Record not found")
    return result.data[0]


# -------------------------------------------------------
# GET /api/data/stats/summary — Dashboard stats for the app
# -------------------------------------------------------
@router.get("/api/data/stats/summary")
async def get_stats(device_id: str | None = Query(default=None)):
    """
    Returns summary stats for SMS records.
    """
    base_query = supabase.table(RAW_TABLE)

    # Total count
    total_q = base_query.select("id", count="exact")
    if device_id:
        total_q = total_q.eq("device_id", device_id)
    total_result = total_q.execute()
    total = total_result.count or 0

    # Financial count
    fin_q = base_query.select("id", count="exact").eq("is_financial", True)
    if device_id:
        fin_q = fin_q.eq("device_id", device_id)
    fin_result = fin_q.execute()
    financial = fin_result.count or 0

    # Last 5 financial records
    recent_q = (
        base_query.select("*")
        .eq("is_financial", True)
        .order("timestamp_ms", desc=True)
        .limit(5)
    )
    if device_id:
        recent_q = recent_q.eq("device_id", device_id)
    recent_result = recent_q.execute()

    return {
        "total_sms": total,
        "financial_sms": financial,
        "non_financial_sms": total - financial,
        "recent_financial": recent_result.data or [],
    }


# -------------------------------------------------------
# POST /api/data/{record_id}/reparse — Re-parse a raw record
# -------------------------------------------------------
@router.post("/api/data/{record_id}/reparse")
async def reparse_record(record_id: str):
    """
    Re-parse an existing raw record. Useful after improving the parser
    to retroactively extract better data from old messages.
    """
    # Fetch the raw record
    result = (
        supabase.table(RAW_TABLE)
        .select("*")
        .eq("id", record_id)
        .limit(1)
        .execute()
    )
    if not result.data:
        raise HTTPException(status_code=404, detail="Record not found")

    record = result.data[0]
    full_text = record.get("body", "")
    sender = record.get("sender")

    # Re-parse with the latest parser logic
    parsed = parse_sms_body(full_text, sender=sender)

    # Update the raw record with new parsed fields
    supabase.table(RAW_TABLE).update({
        "is_financial": parsed.is_financial,
        "parsed_amount": parsed.amount,
        "parsed_direction": parsed.direction,
        "parsed_bank": parsed.bank,
        "parsed_mode": parsed.transaction_mode,
        "parsed_upi_id": parsed.upi_id,
        "parsed_recipient": parsed.recipient_name,
        "parsed_account_suffix": parsed.account_suffix,
        "parsed_balance": parsed.balance_after,
    }).eq("id", record_id).execute()

    return {
        "status": "reparsed",
        "record_id": record_id,
        "parsed": parsed,
    }


# -------------------------------------------------------
# Helper functions
# -------------------------------------------------------
def _timestamp_to_date(timestamp_ms: int) -> str:
    """Convert Unix milliseconds timestamp to YYYY-MM-DD date string"""
    return datetime.fromtimestamp(timestamp_ms / 1000).strftime("%Y-%m-%d")
