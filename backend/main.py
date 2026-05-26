# backend/main.py
import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from routes.health import router as health_router
from routes.auth import router as auth_router
from routes.stock import router as stock_router
from config import settings

app = FastAPI(
    title="Stock API",
    description="API for stock management POC",
    version="0.1.0"
)

# Enable CORS for Android app
cors_origins = os.getenv("CORS_ORIGINS", "http://localhost:3000,http://10.0.2.2:3000").split(",")
app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Custom exception handler for HTTPException (401 Unauthorized)
from starlette.exceptions import HTTPException as StarletteHTTPException

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request, exc):
    """Handle HTTP exceptions with consistent response format."""
    if exc.status_code == 401:
        return JSONResponse(
            status_code=401,
            content={
                "status": "error",
                "message": "Unauthorized: invalid or missing token",
                "detail": "Please login first",
            }
        )
    # For other status codes, use default handling - just return as JSON
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail}
    )


# Include routers
app.include_router(health_router, prefix="/api", tags=["health"])
app.include_router(auth_router, prefix="/api", tags=["auth"])
app.include_router(stock_router, prefix="/api", tags=["stock"])

@app.get("/")
def read_root():
    """Root endpoint - check if API is running"""
    return {"message": "Stock API is running"}

if __name__ == "__main__":
    import uvicorn
    reload = os.getenv("API_RELOAD", "false").lower() == "true"
    uvicorn.run(
        app,
        host=settings.api_host,
        port=settings.api_port,
        reload=reload
    )
