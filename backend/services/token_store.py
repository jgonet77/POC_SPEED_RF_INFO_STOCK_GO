# backend/services/token_store.py
"""
Centralized token store to avoid circular imports between routes and middleware.
"""

# In-memory token store — maps token string to login
active_tokens: dict[str, str] = {}
