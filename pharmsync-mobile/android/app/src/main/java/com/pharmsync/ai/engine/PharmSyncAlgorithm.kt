package com.pharmsync.ai.engine

import com.pharmsync.ai.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object PharmSyncAlgorithm {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd")

    fun getSampleElenaPrescriptions(): List<Prescription> {
        return listOf(
            Prescription(
                rxNumber = "208491",
                drugName = "Metformin HCl ER",
                strength = "500mg",
                dosageForm = "Tablet Extended Release",
                fillDate = LocalDate.of(2026, 8, 30),
                quantityDispensed = 60,
                dailyDoseFrequency = 2.0, // 30 days total -> Depleted Sept 29
                standardCopay = 10.0,
                scheduleClass = ScheduleClass.LEGEND,
                isSyncableFormulation = true,
                sigDirections = "Take 1 tablet by mouth twice daily with morning and evening meals.",
                pillGeometry = PillGeometry.WHITE_OVAL_ER,
                foodRequirement = "With Meal"
            ),
            Prescription(
                rxNumber = "301948",
                drugName = "Lisinopril",
                strength = "10mg",
                dosageForm = "Tablet",
                fillDate = LocalDate.of(2026, 9, 9),
                quantityDispensed = 30,
                dailyDoseFrequency = 1.0, // 30 days total -> Depleted Oct 9
                standardCopay = 8.0,
                scheduleClass = ScheduleClass.LEGEND,
                isSyncableFormulation = true,
                sigDirections = "Take 1 tablet by mouth once daily in the morning with water.",
                pillGeometry = PillGeometry.ROUND_PINK,
                foodRequirement = "None"
            ),
            Prescription(
                rxNumber = "415820",
                drugName = "Atorvastatin Calcium",
                strength = "20mg",
                dosageForm = "Tablet",
                fillDate = LocalDate.of(2026, 9, 15),
                quantityDispensed = 30,
                dailyDoseFrequency = 1.0, // 30 days total -> Depleted Oct 15
                standardCopay = 12.0,
                scheduleClass = ScheduleClass.LEGEND,
                isSyncableFormulation = true,
                sigDirections = "Take 1 tablet by mouth at bedtime.",
                pillGeometry = PillGeometry.WHITE_ELLIPTICAL,
                foodRequirement = "None"
            )
        )
    }

    fun optimizeRegimen(
        prescriptions: List<Prescription>,
        currentDate: LocalDate = LocalDate.of(2026, 9, 25),
        requestedAnchorDate: LocalDate? = null
    ): SyncPlan {
        val auditLogs = mutableListOf<String>()
        auditLogs.add("[OCR INTAKE] Parsed ${prescriptions.size} active Rx label records.")

        // 1. Calculate remaining days of supply (D_i)
        val medRemaining = prescriptions.map { rx ->
            val d_i = rx.calculateDaysRemaining(currentDate)
            val depDate = rx.calculateDepletionDate()
            auditLogs.add("[ANALYSIS] ${rx.drugName} ${rx.strength}: $d_i days left. Depletion date: ${depDate.format(DATE_FORMATTER)}.")
            Pair(rx, d_i)
        }

        // 2. Earliest depletion date: T_empty = min(D_i)
        val syncableMeds = medRemaining.filter { it.first.isSyncableFormulation && it.first.scheduleClass == ScheduleClass.LEGEND }
        val earliestDepletionPair = syncableMeds.minByOrNull { it.second }
            ?: throw IllegalStateException("No syncable medications available.")

        val tEmptyDays = earliestDepletionPair.second
        val earliestDepletionDate = earliestDepletionPair.first.calculateDepletionDate()
        val anchorDrug = earliestDepletionPair.first.drugName

        // 3. Gap-Prevention: Auto-Anchor Pullback logic
        val resolvedAnchorDate = if (requestedAnchorDate != null && requestedAnchorDate.isAfter(earliestDepletionDate)) {
            val gapDays = java.time.temporal.ChronoUnit.DAYS.between(earliestDepletionDate, requestedAnchorDate)
            auditLogs.add("[GAP ALERT] Requested date ${requestedAnchorDate.format(DATE_FORMATTER)} causes a ${gapDays}-day therapy gap for $anchorDrug! Auto-clamping anchor to ${earliestDepletionDate.format(DATE_FORMATTER)}.")
            earliestDepletionDate
        } else {
            earliestDepletionDate
        }

        auditLogs.add("[ANCHOR SET] T_anchor clamped to ${resolvedAnchorDate.format(DATE_FORMATTER)} driven by $anchorDrug depletion.")

        // 4. NCPDP Field 420-DK SCC 47 & Copay calculations
        val overrides = mutableListOf<NCPDPOverride>()
        var totalCopay = 0.0

        for ((rx, d_i) in medRemaining) {
            // Controlled substance lockout
            if (rx.scheduleClass != ScheduleClass.LEGEND) {
                auditLogs.add("[EXCLUSION] ${rx.drugName} locked from auto-sync (Controlled Schedule ${rx.scheduleClass}). Refill strictly on depletion date.")
                continue
            }

            // Non-syncable formulation
            if (!rx.isSyncableFormulation) {
                auditLogs.add("[FLOAT LOGIC] ${rx.drugName} (${rx.dosageForm}) pegged as Floating Refill without dose split.")
                continue
            }

            val deltaDays = d_i - tEmptyDays
            if (deltaDays == 0) {
                // Anchor drug: Full 30-day standard refill
                totalCopay += rx.standardCopay
                overrides.add(
                    NCPDPOverride(
                        rxNumber = rx.rxNumber,
                        drugName = "${rx.drugName} ${rx.strength}",
                        actionType = "STANDARD_REFILL",
                        surplusDays = 0,
                        bridgeDays = 30,
                        bridgeQuantity = (30 * rx.dailyDoseFrequency).toInt(),
                        proratedCopay = rx.standardCopay,
                        ncpdpField420DK = "NONE",
                        billingClaimString = "BIN:004336 PCN:ADV RX:${rx.rxNumber} QTY:${(30 * rx.dailyDoseFrequency).toInt()} COPAY:$${String.format("%.2f", rx.standardCopay)}",
                        rationale = "Anchor Driver. Standard 30-day recurring cycle fill."
                    )
                )
            } else {
                // Surplus medication: Requires SCC 47 Med-Sync Short Fill
                val bridgeDays = 30 - deltaDays
                val bridgeQty = (bridgeDays * rx.dailyDoseFrequency).toInt()
                val proratedCopay = Math.round((rx.standardCopay * (bridgeDays / 30.0)) * 100.0) / 100.0
                totalCopay += proratedCopay

                val claimString = "BIN:004336 PCN:ADV GRP:RXMED42 RX:${rx.rxNumber} SCC:47 QTY:${bridgeQty} COPAY:$${String.format("%.2f", proratedCopay)}"
                auditLogs.add("[BILLING OVERRIDE] ${rx.drugName}: Surplus ${deltaDays}d. Triggered Field 420-DK=47. Bridge: ${bridgeQty} units. Prorated Copay: $${String.format("%.2f", proratedCopay)}.")

                overrides.add(
                    NCPDPOverride(
                        rxNumber = rx.rxNumber,
                        drugName = "${rx.drugName} ${rx.strength}",
                        actionType = "MED_SYNC_OVERRIDE",
                        surplusDays = deltaDays,
                        bridgeDays = bridgeDays,
                        bridgeQuantity = bridgeQty,
                        proratedCopay = proratedCopay,
                        ncpdpField420DK = "47",
                        billingClaimString = claimString,
                        rationale = "NCPDP Reject 79 (Refill Too Soon) bypassed via Field 420-DK = 47. Short-fill alignment."
                    )
                )
            }
        }

        // 5. Clinical Chronopharmacology Regimen Builder
        val pillDeck = buildChronopharmacologyDeck(prescriptions)

        return SyncPlan(
            currentDate = currentDate,
            anchorDate = resolvedAnchorDate,
            anchorDrug = anchorDrug,
            tripsBeforePerMonth = prescriptions.size,
            tripsAfterPerMonth = 1,
            annualTripsSaved = (prescriptions.size - 1) * 12,
            totalCopay = Math.round(totalCopay * 100.0) / 100.0,
            overrides = overrides,
            dailyPillDeck = pillDeck,
            auditLogs = auditLogs
        )
    }

    private fun buildChronopharmacologyDeck(prescriptions: List<Prescription>): List<DosingScheduleItem> {
        val items = mutableListOf<DosingScheduleItem>()

        for (rx in prescriptions) {
            val name = rx.drugName.lowercase()
            when {
                name.contains("lisinopril") -> {
                    items.add(
                        DosingScheduleItem(
                            id = "item_lisinopril",
                            window = TimeOfDayWindow.MORNING,
                            drugName = rx.drugName,
                            strength = rx.strength,
                            geometry = PillGeometry.ROUND_PINK,
                            contextChips = listOf("💧 Full Glass of Water", "⏱️ Morning Hemodynamic Surge"),
                            clinicalRationale = "ACE inhibitor taken at 8:00 AM buffers early diurnal blood pressure spikes upon waking."
                        )
                    )
                }
                name.contains("metformin") -> {
                    // BID: Morning with breakfast and Evening with dinner
                    items.add(
                        DosingScheduleItem(
                            id = "item_metformin_am",
                            window = TimeOfDayWindow.MORNING,
                            drugName = "${rx.drugName} (Dose 1/2)",
                            strength = rx.strength,
                            geometry = PillGeometry.WHITE_OVAL_ER,
                            contextChips = listOf("🍽️ With Breakfast", "💧 Gastroprotection"),
                            clinicalRationale = "Administer with morning meal to mitigate nausea and establish glycemic baseline."
                        )
                    )
                    items.add(
                        DosingScheduleItem(
                            id = "item_metformin_pm",
                            window = TimeOfDayWindow.DINNER,
                            drugName = "${rx.drugName} (Dose 2/2)",
                            strength = rx.strength,
                            geometry = PillGeometry.WHITE_OVAL_ER,
                            contextChips = listOf("🍽️ With Dinner", "🌙 Overnight Glucose Regulation"),
                            clinicalRationale = "Extended-release formulation controls postprandial evening glucose and curbs overnight dawn phenomenon."
                        )
                    )
                }
                name.contains("atorvastatin") || name.contains("statin") -> {
                    items.add(
                        DosingScheduleItem(
                            id = "item_atorvastatin",
                            window = TimeOfDayWindow.BEDTIME,
                            drugName = rx.drugName,
                            strength = rx.strength,
                            geometry = PillGeometry.WHITE_ELLIPTICAL,
                            contextChips = listOf("🌙 Lipid Peak Window", "💧 Full Glass of Water"),
                            clinicalRationale = "Hepatic cholesterol synthesis peaks overnight; bedtime dosing maximizes HMG-CoA reductase inhibition."
                        )
                    )
                }
            }
        }
        return items
    }
}
