package com.noirdraco.hasensorsmartspacertarget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Prüft [Appointments] (reine Logik, ohne Android-Ressourcen). Die Attribute entsprechen einer
 * Waste-Collection-Schedule-Entität („Nächste Abholung“), die ihren Kalender als
 * `"JJJJ-MM-TT": "Abfallart"` mitliefert.
 */
class AppointmentsTest {

    private val today = LocalDate.of(2026, 10, 15)

    // Stellvertretend für die lokalisierten Texte aus strings.xml
    private fun dayText(days: Long): String = when (days) {
        0L -> "Heute"
        1L -> "Morgen"
        else -> "in $days Tagen"
    }

    private fun texts(appointments: List<Appointment>, rule: String) =
        Appointments.texts(appointments, today, rule, ::dayText)

    @Test
    fun fromAttributes_takesOnlyDateKeys_sortedByDate() {
        val appointments = Appointments.fromAttributes(
            mapOf(
                "2026-10-16" to "Restmüll",
                "friendly_name" to "Nächste Abholung",
                "icon" to "mdi:trash-can",
                "2026-10-15" to "Gelber Sack",
                "attribution" to "Last update: 10/15/26 09:37:46"
            )
        )
        assertEquals(
            listOf(
                Appointment(LocalDate.of(2026, 10, 15), "Gelber Sack"),
                Appointment(LocalDate.of(2026, 10, 16), "Restmüll")
            ),
            appointments
        )
    }

    @Test
    fun fromAttributes_withoutDates_isEmpty() {
        assertTrue(Appointments.fromAttributes(mapOf("friendly_name" to "Temperatur")).isEmpty())
    }

    @Test
    fun fromAttributes_skipsInvalidDatesAndBlankLabels() {
        val appointments = Appointments.fromAttributes(
            mapOf("2026-13-40" to "Restmüll", "2026-10-16" to "  ")
        )
        assertTrue(appointments.isEmpty())
    }

    @Test
    fun todayAndTomorrow_bothShownWithFilter() {
        val appointments = listOf(
            Appointment(LocalDate.of(2026, 10, 15), "Gelber Sack"),
            Appointment(LocalDate.of(2026, 10, 16), "Restmüll"),
            Appointment(LocalDate.of(2026, 10, 19), "Bioabfall")
        )
        assertEquals(
            listOf("Gelber Sack Heute", "Restmüll Morgen"),
            texts(appointments, "Heute, Morgen")
        )
    }

    @Test
    fun pastAppointments_areDropped() {
        // Cache von gestern, noch kein neuer Abruf: aus „Morgen“ wird „Heute“, Gestriges fällt weg
        val appointments = listOf(
            Appointment(LocalDate.of(2026, 10, 14), "Bioabfall"),
            Appointment(LocalDate.of(2026, 10, 15), "Gelber Sack")
        )
        assertEquals(listOf("Gelber Sack Heute"), texts(appointments, "Heute, Morgen"))
    }

    @Test
    fun noMatch_isEmpty() {
        val appointments = listOf(Appointment(LocalDate.of(2026, 10, 19), "Bioabfall"))
        assertTrue(texts(appointments, "Heute, Morgen").isEmpty())
    }

    @Test
    fun filterMatchesLabelToo() {
        // Der Filter sieht den ganzen Text, also auch die Abfallart
        val appointments = listOf(
            Appointment(LocalDate.of(2026, 10, 19), "Bioabfall"),
            Appointment(LocalDate.of(2026, 10, 20), "Gelber Sack")
        )
        assertEquals(listOf("Gelber Sack in 5 Tagen"), texts(appointments, "gelber"))
    }

    @Test
    fun severalTypesOnOneDay_stayOneEntry() {
        val appointments = listOf(Appointment(LocalDate.of(2026, 10, 16), "Bioabfall, Schadstoffmobil"))
        assertEquals(listOf("Bioabfall, Schadstoffmobil Morgen"), texts(appointments, "Morgen"))
    }
}
