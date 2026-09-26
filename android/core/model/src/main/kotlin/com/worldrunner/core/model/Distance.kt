package com.worldrunner.core.model

import java.util.Locale
import kotlin.math.roundToLong

/** A distance in whole metres. Never a floating-point kilometre value. */
@JvmInline
value class Distance(val metres: Long) : Comparable<Distance> {
    init {
        require(metres >= 0) { "Distance cannot be negative: $metres" }
    }

    operator fun plus(other: Distance) = Distance(metres + other.metres)

    override fun compareTo(other: Distance) = metres.compareTo(other.metres)

    /** Formats in [unit] with one decimal place, e.g. "5.2 km". */
    fun format(unit: DistanceUnit, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f %s", metres.toDouble() / unit.metresPerUnit, unit.symbol)

    companion object {
        val Zero = Distance(0)

        fun kilometres(km: Double) = of(km, DistanceUnit.Kilometres)

        /** Converts a value the Runner typed in [unit] to whole metres. */
        fun of(value: Double, unit: DistanceUnit) = Distance((value * unit.metresPerUnit).roundToLong())
    }
}

fun Iterable<Distance>.sum(): Distance = fold(Distance.Zero, Distance::plus)

enum class DistanceUnit(val metresPerUnit: Double, val symbol: String) {
    Kilometres(1_000.0, "km"),
    Miles(1_609.344, "mi"),
}
