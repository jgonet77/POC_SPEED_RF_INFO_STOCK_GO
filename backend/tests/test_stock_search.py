import pytest
from models.stock import StockSearchRequest, StockSearchResponse, StockItem


def test_stock_search_request_validation():
    """Validate StockSearchRequest with at least one criterion."""
    # Valid request with article code
    req = StockSearchRequest(art_code="ABC123", stk_lieu=None, stk_nosu=None, act_code="BKS")
    assert req.art_code == "ABC123"

    # Valid request with all criteria
    req = StockSearchRequest(art_code="ABC123", stk_lieu="A-01", stk_nosu="SUP-001", act_code="BKS")
    assert req.art_code == "ABC123"

    # Invalid: all None
    with pytest.raises(ValueError):
        StockSearchRequest(art_code=None, stk_lieu=None, stk_nosu=None, act_code="BKS")


def test_stock_item_response():
    """Verify StockItem has all required fields."""
    item = StockItem(
        art_code="ABC123",
        stk_lieu="A-01",
        stk_nosu="SUP-001",
        qua_code="GOOD",
        stk_qte=100
    )
    assert item.art_code == "ABC123"
    assert item.stk_lieu == "A-01"
    assert item.stk_nosu == "SUP-001"
    assert item.qua_code == "GOOD"
    assert item.stk_qte == 100


def test_stock_search_response():
    """Verify StockSearchResponse structure."""
    items = [
        StockItem(art_code="ABC", stk_lieu="A-01", stk_nosu="S1", qua_code="GOOD", stk_qte=50),
        StockItem(art_code="DEF", stk_lieu="B-02", stk_nosu="S2", qua_code="OK", stk_qte=30),
    ]
    resp = StockSearchResponse(status="success", message="Found 2 items", items=items)
    assert resp.status == "success"
    assert len(resp.items) == 2
    assert resp.items[0].art_code == "ABC"
