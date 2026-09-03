package com.deepkush.reprange.utils

import java.util.Locale
import kotlin.math.roundToLong

object Formatters {

    fun weight(kg: Double, unit: com.deepkush.reprange.constants.WeightUnit): String {
        val v = unit.fromKg(kg)
        val rounded = if (v >= 100) v.roundToLong().toString() else {
            val r = (v * 10).roundToLong() / 10.0
            if (r == r.toLong().toDouble()) r.toLong().toString() else String.format(Locale.US, "%.1f", r)
        }
        return "$rounded ${unit.symbol}"
    }

    fun weightShort(kg: Double, unit: com.deepkush.reprange.constants.WeightUnit): String =
        weight(kg, unit).replace(" ${unit.symbol}", "")

    fun volume(kg: Double, unit: com.deepkush.reprange.constants.WeightUnit): String {
        val v = unit.fromKg(kg).roundToLong()
        return when {
            v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000.0)
            v >= 10_000 -> String.format(Locale.US, "%.1fk", v / 1000.0)
            else -> String.format(Locale.US, "%,d", v)
        } + " ${unit.symbol}"
    }

    fun durationSeconds(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun restCountdown(seconds: Int): String = durationSeconds(seconds.toLong())

    fun date(tsMillis: Long): String =
        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
            .format(java.util.Date(tsMillis))

    fun relativeDay(tsMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diffDays = ((now - tsMillis) / 86_400_000L).toInt()
        return when (diffDays) {
            0 -> "Today"
            1 -> "Yesterday"
            in 2..6 -> "$diffDays days ago"
            else -> date(tsMillis)
        }
    }
}

object OneRmEstimator {
    /** Epley formula. Returns estimated one-rep max in same unit as input weight. */
    fun epley(weight: Double, reps: Int): Double =
        if (reps <= 0 || weight <= 0.0) 0.0
        else if (reps == 1) weight
        else weight * (1.0 + reps / 30.0)

    /**
     * Comparison score for personal records: primary = estimated 1RM,
     * tie-broken by total volume so equal-1RM heavier sets win.
     */
    fun prScore(weight: Double, reps: Int): Double =
        epley(weight, reps) * (1.0 + (weight * reps) / 100_000.0)
}
