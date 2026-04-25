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


class NotificationPayload(BaseModel):
    """Schema matching what the SpendWise Android app sends via POST /api/data"""
    id: str
    source: str  # "notification" or "sms"
    package_name: Optional[str] = None
    sender: Optional[str] = None
    title: Optional[str] = None
    body: str
    big_text: Optional[str] = None
    timestamp_ms: int
    timestamp_human: str
    device_id: str
    sent_to_backend: bool = False


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