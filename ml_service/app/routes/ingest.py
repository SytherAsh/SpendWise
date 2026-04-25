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
@router.post("/api/data", response_model=IngestResponse)
async def ingest_single(payload: SmsPayload):
    """
    Receive a single SMS from the SpendWise Android app.
    Parses the body for financial data and optionally creates a transaction.
    """
    try:
        # Parse the body text for financial information
        parsed = parse_sms_body(payload.body, sender=payload.sender)

        # Build the raw record to store in Supabase
        raw_record = {
            "id": payload.id,
            "source": payload.source,
            "sender": payload.sender,
            "body": payload.body,
            "timestamp_ms": payload.timestamp_ms,
            "timestamp_human": payload.timestamp_human,
            "device_id": payload.device_id,
            "is_financial": parsed.is_financial,
            "parsed_amount": parsed.amount,
            "parsed_direction": parsed.direction,
            "parsed_bank": parsed.bank,
            "parsed_mode": parsed.transaction_mode,
            "parsed_upi_id": parsed.upi_id,
            "parsed_recipient": parsed.recipient_name,
            "parsed_account_suffix": parsed.account_suffix,
            "parsed_balance": parsed.balance_after,
        }

        # Insert raw record into Supabase
        supabase.table(RAW_TABLE).upsert(raw_record).execute()
        logger.info(f"Stored raw SMS: {payload.id} | financial={parsed.is_financial}")

        # If financial, also create a proper transaction in the transactions table
        transaction_id = None
        if parsed.is_financial and parsed.amount is not None:
            try:
                tx_data = {
                    "transaction_reference": payload.id,
                    "transaction_date": _timestamp_to_date(payload.timestamp_ms),
                    "amount": parsed.amount,
                    "debit": parsed.amount if parsed.direction == "DEBIT" else None,
                    "credit": parsed.amount if parsed.direction == "CREDIT" else None,
                    "balance": parsed.balance_after,
                    "transaction_mode": parsed.transaction_mode or "OTHER",
                    "dr_cr_indicator": parsed.direction or "UNKNOWN",
                    "note": payload.body[:200],
                    "recipient_name": parsed.recipient_name,
                    "bank": parsed.bank or "UNKNOWN",
                    "upi_id": parsed.upi_id,
                }
                created = create_single_transaction(tx_data)
                transaction_id = created.get("id")
                logger.info(f"Created transaction {transaction_id} from SMS {payload.id}")

                # Link the transaction ID back to the raw record
                supabase.table(RAW_TABLE).update(
                    {"transaction_id": transaction_id}
                ).eq("id", payload.id).execute()

            except Exception as e:
                logger.error(f"Failed to create transaction from SMS {payload.id}: {e}")

        return IngestResponse(
            status="ok",
            raw_id=payload.id,
            is_financial=parsed.is_financial,
            transaction_id=transaction_id,
            parsed=parsed,
        )

    except Exception as e:
        logger.error(f"Ingest failed for {payload.id}: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# -------------------------------------------------------
# POST /api/data/bulk — Batch ingest multiple records at once
# -------------------------------------------------------
@router.post("/api/data/bulk", response_model=BulkIngestResponse)
async def ingest_bulk(payloads: List[SmsPayload]):
    """
    Receive multiple SMS in one request.
    Useful for the Android app's retry-unsent batch operation.
    """
    financial_count = 0
    non_financial_count = 0
    tx_created = 0
    errors = []

    for payload in payloads:
        try:
            result = await ingest_single(payload)
            if result.is_financial:
                financial_count += 1
                if result.transaction_id:
                    tx_created += 1
            else:
                non_financial_count += 1
        except Exception as e:
            errors.append(f"{payload.id}: {str(e)}")

    return BulkIngestResponse(
        total=len(payloads),
        financial=financial_count,
        non_financial=non_financial_count,
        transactions_created=tx_created,
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
