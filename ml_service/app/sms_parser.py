import re
import logging
from typing import Optional, Dict, Any

from app.schemas.transaction import ParsedTransaction

logger = logging.getLogger(__name__)

# -------------------------------------------------------
# Bank sender-ID mapping — maps SMS sender codes to bank names
# -------------------------------------------------------
SENDER_TO_BANK: Dict[str, str] = {
    "HDFCBK": "HDFC",
    "HDFCBN": "HDFC",
    "SBIINB": "SBI",
    "SBIPSG": "SBI",
    "SBIUPI": "SBI",
    "ICICIB": "ICICI",
    "ICICIS": "ICICI",
    "AXISBK": "AXIS",
    "PAYTM":  "PAYTM",
    "GPAY":   "GPAY",
    "PHONEPE": "PHONEPE",
    "KOTAKB": "KOTAK",
    "YESBNK": "YES",
    "INDBNK": "INDIAN",
    "BOIIND": "BOI",
    "PNBSMS": "PNB",
    "CENTBK": "CENTRAL",
    "CANBNK": "CANARA",
    "UBOI":   "UNION",
    "IDFCFB": "IDFC",
    "FEDBK":  "FEDERAL",
}

# -------------------------------------------------------
# Regex patterns for extracting financial data from SMS body
# -------------------------------------------------------

# Matches amounts like Rs.500, Rs 1,500.00, INR 2000, ₹3,000.50
AMOUNT_PATTERN = re.compile(
    r"(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)", re.IGNORECASE
)

# Matches available balance like "Avl Bal Rs.12,345.67" or "balance is INR 500"
BALANCE_PATTERN = re.compile(
    r"(?:(?:avl|avail(?:able)?|a/c)\s*(?:bal(?:ance)?|amt)[\s:]*|balance\s+(?:is\s+)?)"
    r"(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)",
    re.IGNORECASE,
)

# Matches account or card suffixes like "a/c XX1234", "acct *5678", "card ending 9012"
ACCOUNT_PATTERN = re.compile(
    r"(?:a/?c|acct|account|card)\s*(?:no\.?\s*)?(?:[Xx*]*\s*)?(\d{4,6})",
    re.IGNORECASE,
)

# Matches UPI IDs like "name@upi", "user@oksbi", "shop@ybl"
UPI_PATTERN = re.compile(
    r"([a-zA-Z0-9._-]+@[a-zA-Z]{2,})",
    re.IGNORECASE,
)

# Matches transaction modes like UPI, IMPS, NEFT, RTGS, POS etc.
MODE_PATTERN = re.compile(
    r"\b(UPI|IMPS|NEFT|RTGS|ATM|POS|NACH|ECS|CHQ|CHEQUE|ACH)\b",
    re.IGNORECASE,
)

# Keywords indicating debit transactions
DEBIT_KEYWORDS = re.compile(
    r"\b(debit(?:ed)?|spent|paid|sent|withdraw(?:n|al)?|purchase|charged|transferred)\b",
    re.IGNORECASE,
)

# Keywords indicating credit transactions
CREDIT_KEYWORDS = re.compile(
    r"\b(credit(?:ed)?|received|deposited|refund(?:ed)?|cashback|reversed|added)\b",
    re.IGNORECASE,
)

# Keywords that identify a message as financial — used for filtering
FINANCIAL_KEYWORDS = re.compile(
    r"(?:debit|credit|UPI|IMPS|NEFT|RTGS|Rs\.?|INR|₹|transaction|payment|"
    r"withdraw|a/?c\s*\w*\d{4}|balance|transfer|ATM|POS)",
    re.IGNORECASE,
)


def _parse_amount(text: str) -> float:
    """Remove commas from amount string and convert to float"""
    return float(text.replace(",", ""))


def _detect_bank_from_sender(sender: Optional[str]) -> Optional[str]:
    """Map an SMS sender ID to a known bank name"""
    if not sender:
        return None
    sender_upper = sender.upper().strip()
    # Direct match on known sender codes
    for code, bank in SENDER_TO_BANK.items():
        if code in sender_upper:
            return bank
    return None


def _detect_bank_from_body(body: str) -> Optional[str]:
    """Try to extract bank name from the message body itself"""
    body_upper = body.upper()
    bank_names = [
        ("HDFC", "HDFC"), ("SBI", "SBI"), ("ICICI", "ICICI"),
        ("AXIS", "AXIS"), ("KOTAK", "KOTAK"), ("YES BANK", "YES"),
        ("PNB", "PNB"), ("BOI", "BOI"), ("CANARA", "CANARA"),
        ("UNION", "UNION"), ("IDFC", "IDFC"), ("FEDERAL", "FEDERAL"),
        ("PAYTM", "PAYTM"), ("PHONEPE", "PHONEPE"), ("GPAY", "GPAY"),
    ]
    for keyword, bank in bank_names:
        if keyword in body_upper:
            return bank
    return None


def _extract_recipient_from_body(body: str) -> Optional[str]:
    """Extract recipient/payee name from common SMS patterns"""
    # Patterns like "to NAME" or "to VPA name@upi"
    patterns = [
        re.compile(r"(?:to|paid to|sent to|transferred to)\s+([A-Za-z][A-Za-z0-9 _.]{2,30})", re.IGNORECASE),
        re.compile(r"(?:from|received from|credited by)\s+([A-Za-z][A-Za-z0-9 _.]{2,30})", re.IGNORECASE),
    ]
    for pat in patterns:
        match = pat.search(body)
        if match:
            name = match.group(1).strip()
            # Stop at common terminators like "on", "ref", "via"
            name = re.split(r"\s+(?:on|ref|via|txn|at|for)\b", name, flags=re.IGNORECASE)[0].strip()
            if len(name) >= 2:
                return name
    return None


def parse_sms_body(body: str, sender: Optional[str] = None) -> ParsedTransaction:
    """
    Parse a raw SMS or notification body into structured transaction fields.
    Returns a ParsedTransaction with all extracted data.
    """
    result = ParsedTransaction()

    # Check if this is a financial message at all
    if not FINANCIAL_KEYWORDS.search(body):
        result.is_financial = False
        return result

    result.is_financial = True

    # Extract amount — take the first match (usually the transaction amount)
    amounts = AMOUNT_PATTERN.findall(body)
    if amounts:
        try:
            result.amount = _parse_amount(amounts[0])
        except ValueError:
            pass

    # Determine debit or credit direction
    if DEBIT_KEYWORDS.search(body):
        result.direction = "DEBIT"
    elif CREDIT_KEYWORDS.search(body):
        result.direction = "CREDIT"

    # Extract bank name from sender first, then body as fallback
    result.bank = _detect_bank_from_sender(sender) or _detect_bank_from_body(body)

    # Extract UPI ID if present
    upi_match = UPI_PATTERN.search(body)
    if upi_match:
        upi_id = upi_match.group(1)
        # Filter out email-like false positives
        if not upi_id.endswith((".com", ".org", ".net", ".in", ".co")):
            result.upi_id = upi_id

    # Extract transaction mode (UPI, IMPS, NEFT etc.)
    mode_match = MODE_PATTERN.search(body)
    if mode_match:
        result.transaction_mode = mode_match.group(1).upper()

    # Extract account suffix (last 4-6 digits)
    acct_match = ACCOUNT_PATTERN.search(body)
    if acct_match:
        result.account_suffix = acct_match.group(1)

    # Extract balance after transaction
    bal_match = BALANCE_PATTERN.search(body)
    if bal_match:
        try:
            result.balance_after = _parse_amount(bal_match.group(1))
        except ValueError:
            pass

    # Extract recipient name
    result.recipient_name = _extract_recipient_from_body(body)

    logger.info(
        f"Parsed: amt={result.amount}, dir={result.direction}, "
        f"bank={result.bank}, mode={result.transaction_mode}, "
        f"financial={result.is_financial}"
    )

    return result
