import logging
import re
from enum import Enum
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field, validator
from typing import Optional, Dict, List, Tuple
from dataclasses import dataclass

# Configure logging
logger = logging.getLogger(__name__)

router = APIRouter()

# Constants
MODEL_VERSION = "v1.0-rules-engine"
MIN_CONFIDENCE = 0.0
MAX_CONFIDENCE = 1.0


class TransactionCategory(str, Enum):
    """Enumeration of transaction categories"""
    FOOD = "FOOD"
    TRAVEL = "TRAVEL"
    SHOPPING = "SHOPPING"
    BILLS = "BILLS"
    ENTERTAINMENT = "ENTERTAINMENT"
    HEALTH = "HEALTH"
    EDUCATION = "EDUCATION"
    UTILITIES = "UTILITIES"
    TRANSFER = "TRANSFER"
    INVESTMENT = "INVESTMENT"
    OTHERS = "OTHERS"


class CategorizeRequest(BaseModel):
    """Request model for transaction categorization"""
    description: Optional[str] = Field(None, min_length=0, max_length=500)
    amount: Optional[float] = Field(None, ge=0)
    transaction_mode: Optional[str] = Field(None, max_length=50)
    dr_cr_indicator: Optional[str] = Field(None, pattern="^(DR|CR)$")

    @validator("description")
    def validate_description(cls, v):
        if v:
            return v.strip()
        return v


class CategorizeResponse(BaseModel):
    """Response model for categorization"""
    category: TransactionCategory
    confidence: float = Field(..., ge=MIN_CONFIDENCE, le=MAX_CONFIDENCE)
    model_version: str
    reasoning: Optional[str] = None


@dataclass
class CategoryRule:
    """Represents a categorization rule"""
    category: TransactionCategory
    keywords: List[str]
    patterns: List[str]  # regex patterns
    base_confidence: float = 0.85
    
    def matches(self, text: str) -> Tuple[bool, float]:
        """
        Check if text matches this rule.
        
        Returns:
            Tuple of (matched: bool, confidence: float)
        """
        text_lower = text.lower()
        
        # Check keyword matches (word boundary)
        for keyword in self.keywords:
            if re.search(rf"\b{re.escape(keyword)}\b", text_lower):
                return True, self.base_confidence
        
        # Check regex patterns
        for pattern in self.patterns:
            if re.search(pattern, text_lower):
                return True, self.base_confidence
        
        return False, 0.0


class TransactionCategorizer:
    """Service for categorizing transactions"""
    
    def __init__(self):
        """Initialize categorizer with predefined rules"""
        self.rules: List[CategoryRule] = self._initialize_rules()
    
    def _initialize_rules(self) -> List[CategoryRule]:
        """Initialize categorization rules"""
        return [
            CategoryRule(
                category=TransactionCategory.FOOD,
                keywords=["swiggy", "zomato", "restaurant", "cafe", "food", "pizza", 
                         "burger", "dining", "hotel", "bakery", "kitchen"],
                patterns=[r".*food.*", r".*restaurant.*", r".*dining.*"],
                base_confidence=0.91
            ),
            CategoryRule(
                category=TransactionCategory.TRAVEL,
                keywords=["uber", "ola", "rapido", "fuel", "petrol", "diesel", "taxi",
                         "auto", "cab", "flight", "hotel", "booking", "parking"],
                patterns=[r".*travel.*", r".*transport.*", r".*fuel.*"],
                base_confidence=0.89
            ),
            CategoryRule(
                category=TransactionCategory.SHOPPING,
                keywords=["amazon", "flipkart", "myntra", "shopping", "mall", "store",
                         "retail", "shop", "apparel", "fashion", "shoes", "clothes"],
                patterns=[r".*shop.*", r".*retail.*"],
                base_confidence=0.86
            ),
            CategoryRule(
                category=TransactionCategory.BILLS,
                keywords=["rent", "electricity", "water", "internet", "bill", "utility",
                         "bsnl", "airtel", "jio", "gas", "phone", "postpaid"],
                patterns=[r".*bill.*", r".*utility.*"],
                base_confidence=0.84
            ),
            CategoryRule(
                category=TransactionCategory.ENTERTAINMENT,
                keywords=["netflix", "spotify", "amazon prime", "hotstar", "zee", "movie",
                         "cinema", "theater", "game", "gaming"],
                patterns=[r".*entertain.*", r".*stream.*"],
                base_confidence=0.88
            ),
            CategoryRule(
                category=TransactionCategory.HEALTH,
                keywords=["hospital", "clinic", "pharmacy", "doctor", "medical", "health",
                         "medicine", "drugstore", "lab", "diagnostic"],
                patterns=[r".*health.*", r".*medical.*", r".*pharmacy.*"],
                base_confidence=0.90
            ),
            CategoryRule(
                category=TransactionCategory.EDUCATION,
                keywords=["school", "college", "university", "course", "tuition", "fees",
                         "learning", "exam", "book", "educational"],
                patterns=[r".*education.*", r".*school.*", r".*course.*"],
                base_confidence=0.87
            ),
        ]
    
    def categorize(self, request: CategorizeRequest) -> CategorizeResponse:
        """
        Categorize a transaction based on its details.
        
        Args:
            request: CategorizeRequest containing transaction details
            
        Returns:
            CategorizeResponse with category, confidence, and reasoning
            
        Raises:
            ValueError: If description is empty or None
        """
        if not request.description or not request.description.strip():
            logger.warning("Empty description provided for categorization")
            return CategorizeResponse(
                category=TransactionCategory.OTHERS,
                confidence=0.5,
                model_version=MODEL_VERSION,
                reasoning="No description provided"
            )
        
        text = request.description.strip()
        best_match = None
        best_confidence = 0.0
        matched_rule = None
        
        # Find the best matching rule
        for rule in self.rules:
            matched, confidence = rule.matches(text)
            if matched and confidence > best_confidence:
                best_confidence = confidence
                best_match = rule.category
                matched_rule = rule
        
        # Fallback to OTHERS if no match
        if best_match is None:
            best_match = TransactionCategory.OTHERS
            best_confidence = 0.5
            reasoning = "No matching keywords found"
        else:
            reasoning = f"Matched with {matched_rule.category.value} keywords"
        
        logger.info(
            f"Categorized transaction: {text[:50]}... -> {best_match} "
            f"(confidence: {best_confidence})"
        )
        
        return CategorizeResponse(
            category=best_match,
            confidence=min(best_confidence, MAX_CONFIDENCE),
            model_version=MODEL_VERSION,
            reasoning=reasoning
        )


# Initialize categorizer
categorizer = TransactionCategorizer()


@router.post("/categorize", response_model=CategorizeResponse)
async def categorize(payload: CategorizeRequest) -> CategorizeResponse:
    """
    Categorize a financial transaction.
    
    Args:
        payload: Transaction details including description, amount, mode
        
    Returns:
        CategorizeResponse with predicted category and confidence score
        
    Example:
        POST /categorize
        {
            "description": "Swiggy food delivery",
            "amount": 500.0,
            "transaction_mode": "UPI",
            "dr_cr_indicator": "DR"
        }
    """
    try:
        result = categorizer.categorize(payload)
        return result
    except Exception as e:
        logger.error(f"Error categorizing transaction: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal server error during categorization")