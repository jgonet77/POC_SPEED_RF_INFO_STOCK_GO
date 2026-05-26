from pydantic import BaseModel, field_validator, model_validator, ConfigDict
from typing import Optional, List


class StockSearchRequest(BaseModel):
    """Request model for stock search."""
    art_code: Optional[str] = None
    stk_lieu: Optional[str] = None
    stk_nosu: Optional[str] = None
    act_code: str  # Activity code (required, filtered from Android)

    @field_validator('art_code', 'stk_lieu', 'stk_nosu', mode='before')
    @classmethod
    def trim_strings(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v

    @field_validator('act_code')
    @classmethod
    def act_code_required(cls, v):
        if not v or not v.strip():
            raise ValueError("Activity code (act_code) is required")
        return v.strip()

    @model_validator(mode='after')
    def validate_criteria(self):
        """Ensure at least one search criterion is provided."""
        has_any = bool(self.art_code or self.stk_lieu or self.stk_nosu)
        if not has_any:
            raise ValueError("At least one of art_code, stk_lieu, or stk_nosu must be provided")
        return self

    def has_criteria(self) -> bool:
        """Ensure at least one search criterion is provided."""
        has_any = bool(self.art_code or self.stk_lieu or self.stk_nosu)
        if not has_any:
            raise ValueError("At least one of art_code, stk_lieu, or stk_nosu must be provided")
        return True


class StockItem(BaseModel):
    """Single stock item in search results."""
    art_code: str
    stk_lieu: str
    stk_nosu: str
    qua_code: str
    stk_qte: int

    model_config = ConfigDict(
        json_schema_extra = {
            "example": {
                "art_code": "ARTICLE-001",
                "stk_lieu": "A-01-01",
                "stk_nosu": "SUPP-12345",
                "qua_code": "GOOD",
                "stk_qte": 150
            }
        }
    )


class StockSearchResponse(BaseModel):
    """Response model for stock search."""
    status: str  # "success" or "error"
    message: str
    items: List[StockItem] = []

    model_config = ConfigDict(
        json_schema_extra = {
            "example": {
                "status": "success",
                "message": "Found 3 matching items",
                "items": [
                    {
                        "art_code": "ARTICLE-001",
                        "stk_lieu": "A-01-01",
                        "stk_nosu": "SUPP-12345",
                        "qua_code": "GOOD",
                        "stk_qte": 150
                    }
                ]
            }
        }
    )
