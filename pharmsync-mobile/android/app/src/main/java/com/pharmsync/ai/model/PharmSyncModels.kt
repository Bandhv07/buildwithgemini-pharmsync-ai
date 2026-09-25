package com.pharmsync.ai.model

import java.time.LocalDate

enum class ScheduleClass {
    LEGEND,
    C_II,
    C_III,
    C_IV
}

enum class TimeOfDayWindow(val title: String, val defaultTime: String) {
    MORNING("Morning", "8:00 AM"),
    DINNER("Dinner", "7:00 PM"),
    BEDTIME("Bedtime", "10:00 PM")
}

enum class PillGeometry(val shapeLabel: String, val colorHex: Long) {
    ROUND_PINK("Round Pink Tablet", 0xFFF472B6),
    WHITE_OVAL_ER("White Oval ER Tablet", 0xFFF8FAFC),
    WHITE_ELLIPTICAL("White Elliptical Tablet", 0xFFE2E8F0),
    YELLOW_ROUND("Yellow Round Tablet", 0xFFFBBF24)
}

data class Prescription(
    val rxNumber: String,
    val drugName: String,
    val strength: String,
    val dosageForm: String,
    val fillDate: LocalDate,
    val quantityDispensed: Int,
    val dailyDoseFrequency: Double, // e.g., 2.0 for BID, 1.0 for QD
    val standardCopay: Double = 10.0,
    val scheduleClass: ScheduleClass = ScheduleClass.LEGEND,
    val isSyncableFormulation: Boolean = true,
    val sigDirections: String = "",
    val pillGeometry: PillGeometry = PillGeometry.WHITE_OVAL_ER,
    val foodRequirement: String = "None"
) {
    fun calculateDaysSupply(): Int = (quantityDispensed / dailyDoseFrequency).toInt()

    fun calculateDepletionDate(): LocalDate = fillDate.plusDays(calculateDaysSupply().toLong())

    fun calculateDaysRemaining(currentDate: LocalDate): Int {
        val days = java.time.temporal.ChronoUnit.DAYS.between(currentDate, calculateDepletionDate()).toInt()
        return maxOf(0, days)
    }
}

data class DosingScheduleItem(
    val id: String,
    val window: TimeOfDayWindow,
    val drugName: String,
    val strength: String,
    val geometry: PillGeometry,
    val contextChips: List<String>,
    val clinicalRationale: String,
    var isTaken: Boolean = false
)

data class NCPDPOverride(
    val rxNumber: String,
    val drugName: String,
    val actionType: String, // "STANDARD_REFILL" or "MED_SYNC_OVERRIDE"
    val surplusDays: Int,
    val bridgeDays: Int,
    val bridgeQuantity: Int,
    val proratedCopay: Double,
    val ncpdpField420DK: String, // "47"
    val billingClaimString: String,
    val rationale: String
)

data class SyncPlan(
    val patientName: String = "Elena Rostova",
    val patientDob: String = "1968-04-14",
    val currentDate: LocalDate,
    val anchorDate: LocalDate,
    val anchorDrug: String,
    val tripsBeforePerMonth: Int = 3,
    val tripsAfterPerMonth: Int = 1,
    val annualTripsSaved: Int = 24,
    val totalCopay: Double,
    val overrides: List<NCPDPOverride>,
    val dailyPillDeck: List<DosingScheduleItem>,
    val auditLogs: List<String>
)
