"""FastAPI Backend Server serving the PharmSync AI Interactive Canvas UI, Mobile Simulator, and Agent API."""

import os
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from app.pharmsync_engine import (
    Prescription,
    get_elena_sample_regimen,
    synchronize_regimen,
)

app = FastAPI(title="PharmSync AI Guardian")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

STATIC_DIR = os.path.join(os.path.dirname(__file__), "static")
SIMULATOR_DIR = "/config/Desktop/BuildWithGemini/pharmsync-mobile/simulator"

app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

@app.get("/", response_class=HTMLResponse)
async def get_index():
    with open(os.path.join(STATIC_DIR, "index.html"), "r") as f:
        return f.read()

@app.get("/mobile", response_class=HTMLResponse)
async def get_mobile():
    with open(os.path.join(SIMULATOR_DIR, "index.html"), "r") as f:
        return f.read()

@app.post("/api/sync")
async def run_sync_api(request: Request):
    try:
        body = await request.json()
    except Exception:
        body = {}

    patient_name = body.get("patient_name", "Elena Rostova")
    evaluation = synchronize_regimen(get_elena_sample_regimen())

    return JSONResponse({
        "status": "SUCCESS",
        "patient": patient_name,
        "anchor_date": evaluation.anchor_date,
        "anchor_drug": evaluation.anchor_drug,
        "trips_before": evaluation.trips_before,
        "trips_after": evaluation.trips_after,
        "annual_trips_saved": evaluation.annual_trips_saved,
        "audit_logs": evaluation.audit_logs,
        "medications": evaluation.medications,
        "pill_deck": evaluation.pill_deck,
        "pharmacy_card": evaluation.pharmacy_card
    })

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
