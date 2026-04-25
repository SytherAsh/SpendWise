from pydantic import BaseModel
from typing import Optional


class TransactionCreate(BaseModel):
    transaction_reference: str
    transaction_date: Optional[str] = None
    amount: Optional[float] = None
    debit: Optional[float] = None
    credit: Optional[float] = None
    balance: Optional[float] = None
    transaction_mode: Optional[str] = None
    dr_cr_indicator: Optional[str] = None
    note: Optional[str] = None
    recipient_name: Optional[str] = None
    bank: Optional[str] = None
    upi_id: Optional[str] = None


class SmsPayload(BaseModel):
    """Schema for incoming SMS data from the mobile app"""
    id: str                                  # Unique record ID generated on phone
    source: str                              # Should always be "sms" now
    sender: Optional[str] = None             # SMS sender ID (e.g. HDFCBK)
    body: str                                # The full SMS text
    timestamp_ms: int                        # Unix timestamp in ms
    timestamp_human: str                     # Human readable time
    device_id: str                           # Unique phone identifier
    sent_to_backend: bool = False            # Local sync status


class ParsedTransaction(BaseModel):
    """Result of parsing a raw notification/SMS body into structured transaction fields"""
    amount: Optional[float] = None
    direction: Optional[str] = None  # "DEBIT" or "CREDIT"
    bank: Optional[str] = None
    upi_id: Optional[str] = None
    recipient_name: Optional[str] = None
    transaction_mode: Optional[str] = None  # UPI, IMPS, NEFT, RTGS, etc.
    account_suffix: Optional[str] = None  # last 4 digits of account/card
    balance_after: Optional[float] = None
    is_financial: bool = False  # whether the message was identified as financial