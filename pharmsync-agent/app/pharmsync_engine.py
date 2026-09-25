"""PharmSync Clinical & Algorithmic Domain Engine.

Implements:
1. Days of Supply Remaining (D_i)
2. Anchor Date Selection (T_anchor = min D_i) & Zero-Day Gap Prevention
3. NCPDP Reject 79 / SCC 47 Overrides & Prorated Copay Calculation
4. Clinical Chronopharmacology Engine (Circadian kinetics, absorption conflicts, meal contexts)
5. Edge case handlers (Controlled substances lockout, non-syncable formulation float logic)
"""

from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Any, Dict, List, Optional
import math


@dataclass
class Prescription:
    rx_number: str
    drug_name: str
    strength: str
    dosage_form: str
    fill_date: str  # YYYY-MM-DD
    quantity_dispensed: int
    daily_dose_frequency: float  # e.g., 2.0 for BID, 1.0 for Daily
    standard_copay: float = 10.0
    schedule_class: str = "Legend"  # "Legend", "C-II", "C-III", "C-IV", etc.
    is_syncable: bool = True  # False for eye drops, inhalers, topical ointments, insulin pens
    directions: str = ""
    food_requirement: str = "None"  # "With Meal", "Empty Stomach", "None"
    special_timing: Optional[str] = None  # "Bedtime", "Morning", "Dinner"


@dataclass
class ChronoSchedule:
    time_window: str  # "Morning (8:00 AM)", "Dinner (7:00 PM)", "Bedtime (10:00 PM)"
    drug_name: str
    strength: str
    pill_geometry: str  # e.g. "White Oval ER", "Round Pink Tablet", "White Elliptical"
    context_chips: List[str]
    clinical_rationale: str


@dataclass
class SyncEvaluation:
    anchor_date: str
    anchor_drug: str
    trips_before: int
    trips_after: int
    annual_trips_saved: int
    audit_logs: List[str]
    medications: List[Dict[str, Any]]
    pill_deck: List[Dict[str, Any]]
    pharmacy_card: Dict[str, Any]


def calculate_days_remaining(fill_date_str: str, quantity: int, daily_freq: float, current_date: date) -> int:
    fill_dt = datetime.strptime(fill_date_str, "%Y-%m-%d").date()
    days_supplied = math.floor(quantity / daily_freq)
    depletion_date = fill_dt + timedelta(days=days_supplied)
    days_left = (depletion_date - current_date).days
    return max(0, days_left)


def evaluate_chronopharmacology(rx: Prescription) -> ChronoSchedule:
    name_lower = rx.drug_name.lower()
    
    if "atorvastatin" in name_lower or "simvastatin" in name_lower or "statin" in name_lower:
        return ChronoSchedule(
            time_window="Bedtime (10:00 PM)",
            drug_name=f"{rx.drug_name} {rx.strength}",
            strength=rx.strength,
            pill_geometry="White Elliptical Tablet",
            context_chips=["🌙 Lipid Peak Window", "💧 Full Glass of Water"],
            clinical_rationale="Hepatic cholesterol synthesis peaks overnight; bedtime dosing maximizes HMG-CoA reductase inhibition."
        )
    elif "metformin" in name_lower:
        return ChronoSchedule(
            time_window="Dinner (7:00 PM)",
            drug_name=f"{rx.drug_name} {rx.strength}",
            strength=rx.strength,
            pill_geometry="White Oval ER Tablet",
            context_chips=["🍽️ With Meal", "💧 Gastroprotection"],
            clinical_rationale="Extended-release biguanide administered with evening meal minimizes GI distress and optimizes postprandial glucose control."
        )
    elif "lisinopril" in name_lower or "amlodipine" in name_lower or "losartan" in name_lower:
        return ChronoSchedule(
            time_window="Morning (8:00 AM)",
            drug_name=f"{rx.drug_name} {rx.strength}",
            strength=rx.strength,
            pill_geometry="Round Pink Tablet",
            context_chips=["💧 Full Glass of Water", "⏱️ Morning Hemodynamic Surge"],
            clinical_rationale="ACE inhibitor administered in morning buffers early diurnal blood pressure surge."
        )
    elif "levothyroxine" in name_lower:
        return ChronoSchedule(
            time_window="Morning (7:00 AM)",
            drug_name=f"{rx.drug_name} {rx.strength}",
            strength=rx.strength,
            pill_geometry="Small Round Yellow Tablet",
            context_chips=["☕ 60-min Pre-Breakfast Stagger", "💧 Water Only"],
            clinical_rationale="Requires strict 60-minute absorption separation prior to dietary calcium or iron intake."
        )
    else:
        return ChronoSchedule(
            time_window="Morning (8:00 AM)",
            drug_name=f"{rx.drug_name} {rx.strength}",
            strength=rx.strength,
            pill_geometry="Standard White Capsule",
            context_chips=["💧 With Water"],
            clinical_rationale="Standard morning dosing protocol."
        )


def synchronize_regimen(prescriptions: List[Prescription], current_date: Optional[date] = None) -> SyncEvaluation:
    if current_date is None:
        current_date = date(2026, 9, 25)
    
    audit_logs: List[str] = []
    audit_logs.append(f"[MULTIMODAL INTAKE] Ingested {len(prescriptions)} prescription records.")
    
    # 1. Extraction & Normalization
    med_status = []
    for rx in prescriptions:
        d_i = calculate_days_remaining(rx.fill_date, rx.quantity_dispensed, rx.daily_dose_frequency, current_date)
        depletion_date = current_date + timedelta(days=d_i)
        med_status.append({
            "rx": rx,
            "days_remaining": d_i,
            "depletion_date": depletion_date
        })
        audit_logs.append(
            f"[EXTRACTION] {rx.drug_name} {rx.strength} (Rx #{rx.rx_number}): "
            f"{d_i} days supply remaining. Depletion date: {depletion_date.strftime('%b %d, %Y')}."
        )

    # 2. Chronopharmacology scheduling
    pill_deck = []
    for rx in prescriptions:
        chrono = evaluate_chronopharmacology(rx)
        pill_deck.append({
            "time_window": chrono.time_window,
            "drug_name": chrono.drug_name,
            "strength": chrono.strength,
            "pill_geometry": chrono.pill_geometry,
            "context_chips": chrono.context_chips,
            "clinical_rationale": chrono.clinical_rationale
        })
        audit_logs.append(f"[CHRONO] Scheduled {chrono.drug_name} at {chrono.time_window} ({', '.join(chrono.context_chips)}).")

    # 3. Anchor Date Selection (T_anchor = min D_i)
    syncable_meds = [m for m in med_status if m["rx"].is_syncable and m["rx"].schedule_class == "Legend"]
    if not syncable_meds:
        raise ValueError("No syncable legend medications found for synchronization.")

    min_item = min(syncable_meds, key=lambda m: m["days_remaining"])
    t_anchor_days = min_item["days_remaining"]
    anchor_date = min_item["depletion_date"]
    anchor_drug = min_item["rx"].drug_name

    audit_logs.append(
        f"[SYNC CHECK] Earliest depletion: {anchor_drug} runs out in {t_anchor_days} days on {anchor_date.strftime('%B %d')}."
    )
    audit_logs.append(
        f"[GAP RESILIENCE] Auto-Anchor Pullback: Setting T_anchor = {anchor_date.strftime('%B %d, %Y')} to prevent zero-day therapy gaps."
    )

    # 4. Alignment & NCPDP Overrides
    med_reports = []
    handoff_items = []

    for item in med_status:
        rx = item["rx"]
        d_i = item["days_remaining"]
        
        # Edge Case: Controlled substances
        if rx.schedule_class in ("C-II", "C-III", "C-IV"):
            audit_logs.append(
                f"[CONTROLLED SUBSTANCE LOCKOUT] {rx.drug_name} is Schedule {rx.schedule_class}. Excluded from automatic SCC 47 overrides by statutory regulation."
            )
            med_reports.append({
                "rx_number": rx.rx_number,
                "drug_name": f"{rx.drug_name} {rx.strength}",
                "days_remaining": d_i,
                "status": "EXCLUDED_CONTROLLED",
                "notes": f"Locked from auto-sync (DEA {rx.schedule_class}). Fill on exact depletion."
            })
            continue

        # Edge Case: Non-syncable formulation
        if not rx.is_syncable:
            audit_logs.append(
                f"[NON-SYNCABLE FLOAT] {rx.drug_name} ({rx.dosage_form}) designated as Floating Refill (pegged to nearest monthly cycle without dose split)."
            )
            med_reports.append({
                "rx_number": rx.rx_number,
                "drug_name": f"{rx.drug_name} {rx.strength}",
                "days_remaining": d_i,
                "status": "FLOATING_REFILL",
                "notes": "Unit of use / non-splittable package. Floated to anchor pickup."
            })
            continue

        delta_d = d_i - t_anchor_days
        if delta_d == 0:
            # Anchor drug: Full 30-day standard refill on anchor date
            med_reports.append({
                "rx_number": rx.rx_number,
                "drug_name": f"{rx.drug_name} {rx.strength}",
                "days_remaining": d_i,
                "action": "STANDARD_REFILL",
                "refill_date": anchor_date.strftime("%Y-%m-%d"),
                "quantity": int(30 * rx.daily_dose_frequency),
                "copay": rx.standard_copay,
                "ncpdp_scc": "NONE",
                "notes": "Anchor driver. Full standard 30-day cycle fill."
            })
            handoff_items.append({
                "rx_number": rx.rx_number,
                "drug_name": f"{rx.drug_name} {rx.strength}",
                "claim_type": "Standard 30-Day Cycle Fill",
                "action": "Process standard recurring cycle",
                "ncpdp_string": f"Rx#{rx.rx_number}: STANDARD 30D REFILL",
                "copay": f"${rx.standard_copay:.2f}"
            })
        else:
            # Requires early refill alignment with SCC 47
            bridge_days = 30 - delta_d
            bridge_qty = int(bridge_days * rx.daily_dose_frequency)
            prorated_copay = round(rx.standard_copay * (bridge_days / 30.0), 2)

            audit_logs.append(
                f"[OVERRIDE] {rx.drug_name} has {delta_d} surplus days. Attaching NCPDP Field 420-DK=47 (Med-Sync Short Fill). Bridge Qty: {bridge_qty}, Prorated Copay: ${prorated_copay:.2f}."
            )

            med_reports.append({
                "rx_number": rx.rx_number,
                "drug_name": f"{rx.drug_name} {rx.strength}",
                "days_remaining": d_i,
                "action": "MED_SYNC_OVERRIDE",
                "refill_date": anchor_date.strftime("%Y-%m-%d"),
                "delta_days": delta_d,
                "bridge_days": bridge_days,
                "quantity": bridge_qty,
                "copay": prorated_copay,
                "ncpdp_scc": "47",
                "notes": "NCPDP Reject 79 bypassed via Field 420-DK = 47. Prorated copay applied."
            })
            handoff_items.append({
                "rx_number": rx.rx_number,
                "drug_name": f"{rx.drug_name} {rx.strength}",
                "claim_type": "NCPDP SCC 47 (Med-Sync Alignment)",
                "action": f"Override Reject 79 with Field 420-DK=47; dispense {bridge_qty} units",
                "ncpdp_string": f"BIN:004336 PCN:ADV GRP:RXMED42 RX:{rx.rx_number} SCC:47 QTY:{bridge_qty} COPAY:${prorated_copay:.2f}",
                "copay": f"${prorated_copay:.2f}"
            })

    trips_before = len(prescriptions)
    trips_after = 1
    trips_saved_per_year = (trips_before - trips_after) * 12

    pharmacy_card = {
        "patient_name": "Elena Rostova",
        "dob": "1968-04-14",
        "anchor_date": anchor_date.strftime("%B %d, %Y"),
        "sync_frequency": "Monthly Recurring",
        "adjudication_items": handoff_items,
        "ncpdp_full_string": " | ".join([item["ncpdp_string"] for item in handoff_items])
    }

    return SyncEvaluation(
        anchor_date=anchor_date.strftime("%Y-%m-%d"),
        anchor_drug=anchor_drug,
        trips_before=trips_before,
        trips_after=trips_after,
        annual_trips_saved=trips_saved_per_year,
        audit_logs=audit_logs,
        medications=med_reports,
        pill_deck=pill_deck,
        pharmacy_card=pharmacy_card
    )


def get_elena_sample_regimen() -> List[Prescription]:
    return [
        Prescription(
            rx_number="208491",
            drug_name="Metformin HCl ER",
            strength="500mg",
            dosage_form="Tablet Extended Release",
            fill_date="2026-08-30",
            quantity_dispensed=60,
            daily_dose_frequency=2.0,  # 30 days supply, expires Sep 29
            standard_copay=10.0,
            schedule_class="Legend",
            is_syncable=True,
            directions="Take 1 tablet by mouth twice daily with morning and evening meals.",
            food_requirement="With Meal",
            special_timing="Dinner"
        ),
        Prescription(
            rx_number="301948",
            drug_name="Lisinopril",
            strength="10mg",
            dosage_form="Tablet",
            fill_date="2026-09-09",
            quantity_dispensed=30,
            daily_dose_frequency=1.0,  # 30 days supply, expires Oct 9
            standard_copay=8.0,
            schedule_class="Legend",
            is_syncable=True,
            directions="Take 1 tablet by mouth every morning with water.",
            food_requirement="None",
            special_timing="Morning"
        ),
        Prescription(
            rx_number="415820",
            drug_name="Atorvastatin Calcium",
            strength="20mg",
            dosage_form="Tablet",
            fill_date="2026-09-15",
            quantity_dispensed=30,
            daily_dose_frequency=1.0,  # 30 days supply, expires Oct 15
            standard_copay=12.0,
            schedule_class="Legend",
            is_syncable=True,
            directions="Take 1 tablet by mouth at bedtime.",
            food_requirement="None",
            special_timing="Bedtime"
        ),
    ]
