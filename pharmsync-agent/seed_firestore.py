"""Seed script for PharmSync AI Firestore Database.

Populates the 'prescriptions' collection with initial chronic regimen data
for Elena Rostova (Metformin ER, Lisinopril, Atorvastatin) and additional patients.
Project ID is hardcoded as an exact string literal to prevent numeric ID lookup issues.
"""

import sys
from google.cloud import firestore

FIRESTORE_PROJECT_ID = "qwiklabs-gcp-04-99ca131a8220"
COLLECTION_NAME = "prescriptions"

SEEDED_PRESCRIPTIONS = [
    {
        "rx_number": "208491",
        "patient_name": "Elena Rostova",
        "patient_dob": "1968-04-14",
        "drug_name": "Metformin HCl ER",
        "strength": "500mg",
        "dosage_form": "Tablet Extended Release",
        "fill_date": "2026-08-30",
        "quantity_dispensed": 60,
        "daily_dose_frequency": 2.0,
        "standard_copay": 10.0,
        "schedule_class": "Legend",
        "is_syncable": True,
        "food_requirement": "With Meal",
        "sig_directions": "Take 1 tablet by mouth twice daily with breakfast and dinner.",
        "pms_bin": "004336",
        "pms_pcn": "ADV",
        "pms_group": "RXMED42"
    },
    {
        "rx_number": "301948",
        "patient_name": "Elena Rostova",
        "patient_dob": "1968-04-14",
        "drug_name": "Lisinopril",
        "strength": "10mg",
        "dosage_form": "Tablet",
        "fill_date": "2026-09-09",
        "quantity_dispensed": 30,
        "daily_dose_frequency": 1.0,
        "standard_copay": 8.0,
        "schedule_class": "Legend",
        "is_syncable": True,
        "food_requirement": "None",
        "sig_directions": "Take 1 tablet by mouth once daily in the morning with water.",
        "pms_bin": "004336",
        "pms_pcn": "ADV",
        "pms_group": "RXMED42"
    },
    {
        "rx_number": "415820",
        "patient_name": "Elena Rostova",
        "patient_dob": "1968-04-14",
        "drug_name": "Atorvastatin Calcium",
        "strength": "20mg",
        "dosage_form": "Tablet",
        "fill_date": "2026-09-15",
        "quantity_dispensed": 30,
        "daily_dose_frequency": 1.0,
        "standard_copay": 12.0,
        "schedule_class": "Legend",
        "is_syncable": True,
        "food_requirement": "None",
        "sig_directions": "Take 1 tablet by mouth at bedtime.",
        "pms_bin": "004336",
        "pms_pcn": "ADV",
        "pms_group": "RXMED42"
    },
    {
        "rx_number": "592104",
        "patient_name": "Marcus Chen",
        "patient_dob": "1975-11-20",
        "drug_name": "Amlodipine Besylate",
        "strength": "5mg",
        "dosage_form": "Tablet",
        "fill_date": "2026-09-01",
        "quantity_dispensed": 30,
        "daily_dose_frequency": 1.0,
        "standard_copay": 5.0,
        "schedule_class": "Legend",
        "is_syncable": True,
        "food_requirement": "None",
        "sig_directions": "Take 1 tablet daily by mouth.",
        "pms_bin": "004336",
        "pms_pcn": "ADV",
        "pms_group": "RXMED42"
    }
]


def seed_database():
    print(f"Connecting to Firestore with hardcoded project ID: '{FIRESTORE_PROJECT_ID}'...")
    db = firestore.Client(project=FIRESTORE_PROJECT_ID)
    
    for rx in SEEDED_PRESCRIPTIONS:
        rx_num = rx["rx_number"]
        print(f"Seeding Rx #{rx_num}: {rx['drug_name']} {rx['strength']} for {rx['patient_name']}...")
        db.collection(COLLECTION_NAME).document(rx_num).set(rx, merge=True)
    
    print("\n✅ Firestore successfully seeded!")
    print(f"Total records in '{COLLECTION_NAME}' collection: {len(SEEDED_PRESCRIPTIONS)}")


if __name__ == "__main__":
    seed_database()
