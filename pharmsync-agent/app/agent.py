"""PharmSync AI Agent: The Smart Regimen & Refill Synchronization Guardian.

Built with Google ADK & agents-cli.
Features:
- Multimodal Intake & Extraction: Prescriptions & Rx Claims
- Clinical Chronopharmacology Engine: Morning / Evening / Bedtime alignment
- Optimization Sandbox: Gap prevention, Anchor Date calculation, NCPDP SCC 47 overrides
- Real-time Audit Stream and Counter-ready Pharmacy Handoff generator
"""

import json
from datetime import date
from typing import Any, Dict, List, Optional

from google.adk.agents import Agent
from google.adk.apps import App
from google.adk.models import Gemini
from google.genai import types

from app.pharmsync_engine import (
    Prescription,
    get_elena_sample_regimen,
    synchronize_regimen,
)

MODEL = "gemini-3.6-flash"


def run_pharmsync_optimization(
    patient_name: str = "Elena Rostova",
    custom_rx_json: Optional[str] = None
) -> str:
    """Runs the PharmSync Optimization & Chronopharmacology Sandbox.
    
    Calculates exact depletion dates (D_i), assigns optimal circadian timing windows,
    identifies the earliest depletion date (T_anchor) to prevent therapy gaps,
    resolves NCPDP Reject 79 via Field 420-DK SCC 47 overrides, computes statutory
    prorated copays, and produces the complete synchronized plan.

    Args:
        patient_name: Name of the patient (defaults to Elena Rostova).
        custom_rx_json: Optional JSON string with a list of prescription objects.
                        If omitted, Elena's clinical case (Metformin, Lisinopril, Atorvastatin) is evaluated.

    Returns:
        JSON string containing the comprehensive synchronization plan, audit trail,
        pill deck, and pharmacy counter handoff card.
    """
    if custom_rx_json:
        try:
            raw_list = json.loads(custom_rx_json)
            prescriptions = []
            for item in raw_list:
                prescriptions.append(Prescription(
                    rx_number=item.get("rx_number", "RX000"),
                    drug_name=item["drug_name"],
                    strength=item.get("strength", ""),
                    dosage_form=item.get("dosage_form", "Tablet"),
                    fill_date=item.get("fill_date", "2026-08-30"),
                    quantity_dispensed=int(item.get("quantity_dispensed", 30)),
                    daily_dose_frequency=float(item.get("daily_dose_frequency", 1.0)),
                    standard_copay=float(item.get("standard_copay", 10.0)),
                    schedule_class=item.get("schedule_class", "Legend"),
                    is_syncable=item.get("is_syncable", True),
                    directions=item.get("directions", ""),
                    food_requirement=item.get("food_requirement", "None"),
                    special_timing=item.get("special_timing")
                ))
        except Exception as e:
            return json.dumps({"error": f"Failed to parse custom prescriptions: {e}"})
    else:
        prescriptions = get_elena_sample_regimen()

    evaluation = synchronize_regimen(prescriptions, current_date=date(2026, 9, 25))

    result = {
        "status": "SUCCESS",
        "patient": patient_name,
        "anchor_date": evaluation.anchor_date,
        "anchor_driver": evaluation.anchor_drug,
        "trips_before": evaluation.trips_before,
        "trips_after": evaluation.trips_after,
        "annual_trips_saved": evaluation.annual_trips_saved,
        "audit_logs": evaluation.audit_logs,
        "medications": evaluation.medications,
        "pill_deck": evaluation.pill_deck,
        "pharmacy_card": evaluation.pharmacy_card
    }
    return json.dumps(result, indent=2)


def get_ncpdp_adjudication_string(rx_number: str) -> str:
    """Generates the exact NCPDP Field 420-DK SCC 47 adjudication string for pharmacy counter entry.

    Args:
        rx_number: The prescription number to lookup.

    Returns:
        The ready-to-copy NCPDP billing claim string with statutory Med-Sync SCC 47 override.
    """
    prescriptions = get_elena_sample_regimen()
    evaluation = synchronize_regimen(prescriptions, current_date=date(2026, 9, 25))
    for item in evaluation.pharmacy_card.get("adjudication_items", []):
        if rx_number in item["rx_number"]:
            return json.dumps(item, indent=2)
    return json.dumps({
        "error": f"Rx #{rx_number} not found in active regimen.",
        "active_rx_numbers": [rx.rx_number for rx in prescriptions]
    })


SYSTEM_INSTRUCTION = """You are PharmSync AI: The Smart Regimen & Refill Synchronization Guardian.

Your mission is to eliminate medication therapy gaps, protect patients from erratic pharmacy trips, and resolve pharmacy counter rejections (NCPDP Reject 79: Refill Too Soon) before they happen.

Follow this clinical and optimization workflow:
1. Multimodal Intake & Extraction:
   - Identify drug name, strength, fill date, quantity, daily dose frequency, and directions.
2. Clinical Chronopharmacology Engine:
   - Morning (8:00 AM): ACE inhibitors (Lisinopril) for morning hemodynamic surge.
   - Dinner (7:00 PM): Metformin ER with evening meal for gastroprotection and glucose control.
   - Bedtime (10:00 PM): Statins (Atorvastatin) matching nocturnal hepatic cholesterol peak.
   - Absorption Antagonism (Levothyroxine vs Iron/Calcium): enforce strict 60-minute stagger.
3. Optimization Sandbox & Zero-Day Gap Prevention:
   - Calculate Days of Supply Remaining (D_i).
   - Clamp T_anchor = min(D_i). Point out that delaying the anchor creates a dangerous zero-day therapy gap!
   - Apply statutory NCPDP Field 420-DK value 47 (Med-Sync Short Fill) to align other medications.
   - Calculate bridge quantity: Q_bridge = (30 - Delta_D) * daily_frequency.
   - Calculate prorated copay: Copay_standard * ((30 - Delta_D) / 30).
4. Edge Case Resilience:
   - Controlled Substances (C-II to C-IV): Lock out of auto-sync overrides due to statutory restrictions.
   - Non-Syncable Formulations (inhalers, eye drops): Apply Floating Refill logic pegged to nearest cycle.

When interacting with patients or pharmacists:
- Use your tools `run_pharmsync_optimization` and `get_ncpdp_adjudication_string`.
- Format clinical schedules clearly into Morning, Dinner, and Bedtime pill decks.
- Provide the exact NCPDP SCC 47 billing string and prorated copays so the patient walks out with all medications in one visit.
"""

root_agent = Agent(
    name="pharmsync_guardian",
    model=Gemini(
        model=MODEL,
        retry_options=types.HttpRetryOptions(attempts=3),
    ),
    instruction=SYSTEM_INSTRUCTION,
    tools=[run_pharmsync_optimization, get_ncpdp_adjudication_string],
)

app = App(
    root_agent=root_agent,
    name="app",
)
