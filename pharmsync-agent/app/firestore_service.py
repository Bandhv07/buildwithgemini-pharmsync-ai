"""Firestore Client and Helper Service for PharmSync AI.

IMPORTANT: PROJECT_ID is hardcoded as an exact string literal to prevent
issues where google.auth.default() or GOOGLE_CLOUD_PROJECT returns the numeric
project ID on Agent Platform runtime.
"""

from typing import Any, Dict, List, Optional
from google.cloud import firestore

FIRESTORE_PROJECT_ID: str = "qwiklabs-gcp-04-99ca131a8220"
PRESCRIPTIONS_COLLECTION: str = "prescriptions"

_client: Optional[firestore.Client] = None


def get_firestore_client() -> firestore.Client:
    """Returns a singleton Firestore client with hardcoded project ID string."""
    global _client
    if _client is None:
        _client = firestore.Client(project=FIRESTORE_PROJECT_ID)
    return _client


def get_patient_prescriptions(patient_name: str) -> List[Dict[str, Any]]:
    """Fetches all active prescriptions for a given patient from Firestore."""
    db = get_firestore_client()
    query = db.collection(PRESCRIPTIONS_COLLECTION).where("patient_name", "==", patient_name)
    docs = query.stream()
    results = []
    for doc in docs:
        data = doc.to_dict()
        data["id"] = doc.id
        results.append(data)
    return results


def get_prescription_by_rx(rx_number: str) -> Optional[Dict[str, Any]]:
    """Looks up a single prescription by its Rx number."""
    db = get_firestore_client()
    doc_ref = db.collection(PRESCRIPTIONS_COLLECTION).document(rx_number)
    doc = doc_ref.get()
    if doc.exists:
        data = doc.to_dict()
        data["id"] = doc.id
        return data
    return None


def upsert_prescription(
    rx_number: str,
    patient_name: str,
    drug_name: str,
    strength: str,
    dosage_form: str,
    fill_date: str,
    quantity_dispensed: int,
    daily_dose_frequency: float,
    standard_copay: float,
    sig_directions: str,
    schedule_class: str = "Legend",
    is_syncable: bool = True,
    food_requirement: str = "None"
) -> Dict[str, Any]:
    """Saves or updates a prescription record in Firestore."""
    db = get_firestore_client()
    doc_ref = db.collection(PRESCRIPTIONS_COLLECTION).document(rx_number)
    data = {
        "rx_number": rx_number,
        "patient_name": patient_name,
        "drug_name": drug_name,
        "strength": strength,
        "dosage_form": dosage_form,
        "fill_date": fill_date,
        "quantity_dispensed": quantity_dispensed,
        "daily_dose_frequency": daily_dose_frequency,
        "standard_copay": standard_copay,
        "sig_directions": sig_directions,
        "schedule_class": schedule_class,
        "is_syncable": is_syncable,
        "food_requirement": food_requirement,
        "updated_at": firestore.SERVER_TIMESTAMP
    }
    doc_ref.set(data, merge=True)
    return data
