# backend/main.py
import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes.health import router as health_router
from routes.auth import router as auth_router
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

# Include routers
app.include_router(health_router, prefix="/api", tags=["health"])
app.include_router(auth_router, prefix="/api", tags=["auth"])

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
