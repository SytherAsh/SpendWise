from fastapi import FastAPI

from app.routes.bulk import router as bulk_router
from app.routes.transaction import router as transactions_router
from app.routes.categorize import router as categorize_router
app = FastAPI(title="Expense ML Service")


@app.get("/")
def health():
    return {"status": "running"}


app.include_router(bulk_router, tags=["bulk"])
app.include_router(transactions_router, tags=["transactions"])
app.include_router(categorize_router, tags=["categorization"])