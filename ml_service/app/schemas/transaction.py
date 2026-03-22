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