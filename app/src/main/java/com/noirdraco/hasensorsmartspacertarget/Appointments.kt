package com.noirdraco.hasensorsmartspacertarget

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Ein Termin aus den Sensor-Attributen, z. B. 2026-10-16 → „Restmüll“. */
data class Appointment(val date: LocalDate, val label: String)

/**
 * Termine aus den Attributen eines Sensors. Waste Collection Schedule liefert seinen Kalender als
 * Attribute `"JJJJ-MM-TT": "Abfallart"`, der State nennt aber immer nur die *nächste* Abholung.
 * Sind heute und morgen Termine, fehlt im State also einer — hier werden deshalb alle passenden
 * Termine selbst beschriftet („Gelber Sack Heute“, „Restmüll Morgen“).
 *
 * Beschriftet wird zur Anzeigezeit relativ zum heutigen Datum, damit um Mitternacht aus „Morgen“
 * auch ohne neuen Abruf „Heute“ wird.
 *
 * Frei von Android-Ressourcen, damit die Logik als reiner Unit-Test läuft (wie [ShowRule]); die
 * lokalisierten Tagestexte reicht der Aufrufer als Funktion herein.
 */
object Appointments {

    private val DATE_KEY = Regex("""\d{4}-\d{2}-\d{2}""")

    /** Alle Attribute mit Datum als Schlüssel, nach Datum sortiert; leer, wenn es keine gibt. */
    fun fromAttributes(attributes: Map<String, String>): List<Appointment> =
        attributes.mapNotNull { (key, value) ->
            if (!DATE_KEY.matches(key) || value.isBlank()) return@mapNotNull null
            val date = try {
                LocalDate.parse(key)
            } catch (e: DateTimeParseException) {
                return@mapNotNull null
            }
            Appointment(date, value.trim())
        }.sortedBy { it.date }

    /**
     * Texte der Termine ab [today], die zum Anzeige-Filter [rule] passen, z. B.
     * „Gelber Sack Heute“. [dayText] bekommt den Abstand in Tagen (0 = heute).
     */
    fun texts(
        appointments: List<Appointment>,
        today: LocalDate,
        rule: String,
        dayText: (Long) -> String
    ): List<String> = appointments
        .filter { !it.date.isBefore(today) }
        .map { it.label + " " + dayText(ChronoUnit.DAYS.between(today, it.date)) }
        .filter { ShowRule.matches(it, rule) }
}
