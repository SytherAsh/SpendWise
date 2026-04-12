from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()


class CategorizeRequest(BaseModel):
    description: Optional[str] = None
    amount: Optional[float] = None
    transaction_mode: Optional[str] = None
    dr_cr_indicator: Optional[str] = None


@router.post("/categorize")
def categorize(payload: CategorizeRequest):
    text = (payload.description or "").lower()

    if any(k in text for k in ["swiggy", "zomato", "restaurant", "cafe", "food"]):
        category = "FOOD"
        confidence = 0.91
    elif any(k in text for k in ["uber", "ola", "rapido", "fuel", "petrol", "diesel"]):
        category = "TRAVEL"
        confidence = 0.89
    elif any(k in text for k in ["amazon", "flipkart", "myntra", "shopping"]):
        category = "SHOPPING"
        confidence = 0.86
    elif any(k in text for k in ["rent", "electricity", "water", "internet", "bill"]):
        category = "BILLS"
        confidence = 0.84
    else:
        category = "OTHERS"
        confidence = 0.70

    return {
        "category": category,
        "confidence": confidence,
        "model_version": "dummy-v1"
    }