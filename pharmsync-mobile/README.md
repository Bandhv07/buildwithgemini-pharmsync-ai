# 📱 PharmSync AI: The Smart Regimen & Refill Synchronization Guardian (Mobile)

A production-grade, clinical-grade mobile application for Android (Kotlin + Jetpack Compose) and Cross-Platform Mobile architectures.

---

## 🏗️ Architecture & Component Hierarchy

```text
pharmsync-mobile/
├── android/app/src/main/java/com/pharmsync/ai/
│   ├── model/
│   │   └── PharmSyncModels.kt          # Prescription, DosingScheduleItem, NCPDPOverride, SyncPlan
│   ├── engine/
│   │   └── PharmSyncAlgorithm.kt       # Gap-prevention pullback, Days of supply (D_i), SCC 47 overrides
│   ├── ui/
│   │   ├── components/
│   │   │   ├── SynchronizationWheel.kt # Animated Jetpack Compose concentric SVG/Canvas arcs
│   │   │   ├── DailyPillDeck.kt        # Morning/Dinner/Bedtime schedule with tactile pill shapes
│   │   │   ├── PharmacyHandoffCard.kt  # Digital counter voucher with copyable NCPDP code & prorated copay
│   │   │   └── AgentThoughtDrawer.kt   # Collapsible terminal drawer with live reasoning logs
│   │   ├── screens/
│   │   │   └── PharmSyncDashboardScreen.kt # Primary mobile screen coordinating all components
│   │   └── viewmodel/
│   │       └── PharmSyncViewModel.kt   # Reactive StateFlow / Coroutines state manager
```

---

## 🧮 Crucial Mathematical Modeling & Business Logic

1. **Days of Supply Remaining ($D_i$)**:
   $$D_i = \left(\text{Fill Date}_i + \frac{\text{Quantity Dispensed}_i}{\text{Daily Dose Frequency}_i}\right) - \text{Current Date}$$

2. **Anchor Date Selection ($T_{\text{anchor}}$)**:
   $$T_{\text{anchor}} = \min_{i} (D_i)$$
   - **Auto-Anchor Pullback**: If any target date exceeds the earliest depletion date ($T_{\text{empty}}$), it flags a critical therapy gap warning and automatically clamps $T_{\text{anchor}} = \text{Date}(D_{\text{min}} = 0)$ to protect continuity of care.

3. **NCPDP Reject 79 Bypass (Field 420-DK = 47)**:
   - For medications where $D_i > T_{\text{anchor}}$, submitting standard 30-day refills triggers Reject 79 ("Refill Too Soon").
   - The engine automatically triggers Submission Clarification Code `47` (Med-Sync Short Fill).
   - Computes bridge quantity:
     $$Q_{\text{bridge}} = (30 - \Delta D_i) \times \text{Daily Dose Frequency}_i$$
   - Computes statutory prorated copay:
     $$\text{Copay}_{\text{prorated}} = \text{Copay}_{\text{standard}} \times \left(\frac{30 - \Delta D_i}{30}\right)$$

---

## 🌟 Visual UI Showcase

1. **Interactive Synchronization Wheel**:
   - Concentric circular arcs for Metformin (Emerald), Lisinopril (Sky Blue), and Atorvastatin (Radiant Pink).
   - Smooth `animateFloatAsState` spring transitions that snap from fragmented angles (90°, 200°, 290°) into a synchronized 360° illuminated ring on September 29.
   - Dynamic metric chip: *"Trips reduced from 3/month to 1/month (24 trips saved/year)"*.

2. **Daily Smart Pill-Deck**:
   - Organizes medications into Morning (8:00 AM), Dinner (7:00 PM), and Bedtime (10:00 PM).
   - Visual pill geometries: Round Pink (Lisinopril), White Oval ER (Metformin), and White Elliptical (Atorvastatin).
   - Contextual clinical badges (`🍽️ With Meal`, `💧 Full Glass of Water`, `🌙 Lipid Peak Window`).

3. **Pharmacy Counter Handoff Card**:
   - Digital voucher displaying Patient Name (Elena Rostova), DOB, and Anchor Date.
   - Exact override instructions for the technician.
   - One-tap button to copy the complete NCPDP billing claim string.
