package com.worldrunner.core.model

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on the Earth in degrees. Longitude is in [-180, 180]. */
data class LatLng(val latitude: Double, val longitude: Double)

data class Waypoint(val name: String, val position: LatLng)

/**
 * Where a distance lands on the [WorldRoute]. [lap] counts completed circuits, so a Team that has travelled
 * more than one circuit keeps going round with a higher lap. [previous] and [next] are the Waypoints either side.
 */
data class RoutePosition(
    val position: LatLng,
    val lap: Int,
    val distanceIntoLap: Distance,
    val previous: Waypoint,
    val next: Waypoint,
)

/**
 * The one shared virtual route every Team follows, used only to visualise Route progress. It starts in Lisbon and
 * heads east through Europe, Asia, across the Pacific and North America, then back over the Atlantic to Lisbon.
 * Legs are great-circle arcs between consecutive Waypoints, so a distance maps to one deterministic point.
 */
object WorldRoute {
    /** Mean Earth radius (IUGG), in metres. */
    private const val EARTH_RADIUS_M = 6_371_008.8

    /** In travel order. The circuit closes by returning from the last Waypoint to the first. */
    val waypoints: List<Waypoint> = listOf(
        "Lisbon" to LatLng(38.7223, -9.1393),
        "Madrid" to LatLng(40.4168, -3.7038),
        "Barcelona" to LatLng(41.3874, 2.1686),
        "Marseille" to LatLng(43.2965, 5.3698),
        "Milan" to LatLng(45.4642, 9.1900),
        "Vienna" to LatLng(48.2082, 16.3738),
        "Budapest" to LatLng(47.4979, 19.0402),
        "Belgrade" to LatLng(44.7866, 20.4489),
        "Istanbul" to LatLng(41.0082, 28.9784),
        "Tehran" to LatLng(35.6892, 51.3890),
        "Delhi" to LatLng(28.6139, 77.2090),
        "Kolkata" to LatLng(22.5726, 88.3639),
        "Bangkok" to LatLng(13.7563, 100.5018),
        "Hanoi" to LatLng(21.0278, 105.8342),
        "Hong Kong" to LatLng(22.3193, 114.1694),
        "Shanghai" to LatLng(31.2304, 121.4737),
        "Seoul" to LatLng(37.5665, 126.9780),
        "Tokyo" to LatLng(35.6762, 139.6503),
        "Honolulu" to LatLng(21.3099, -157.8581),
        "San Francisco" to LatLng(37.7749, -122.4194),
        "Denver" to LatLng(39.7392, -104.9903),
        "Chicago" to LatLng(41.8781, -87.6298),
        "New York" to LatLng(40.7128, -74.0060),
        "Ponta Delgada" to LatLng(37.7412, -25.6756),
    ).map { (name, position) -> Waypoint(name, position) }

    val start: Waypoint get() = waypoints.first()

    /** Distance from the start to each Waypoint, ending with the full circuit back at the start. */
    private val cumulative: List<Long> = buildList {
        var total = 0L
        add(0L)
        for (i in waypoints.indices) {
            total += greatCircleMetres(waypoints[i].position, waypoints[(i + 1) % waypoints.size].position)
            add(total)
        }
    }

    /** One full circuit. */
    val length: Distance = Distance(cumulative.last())

    /** Distance from the start to Waypoint [index] on the first lap. */
    fun distanceTo(index: Int): Distance = Distance(cumulative[index])

    /** The point [travelled] reaches. Zero is the start; a whole number of circuits is back at the start. */
    fun positionAt(travelled: Distance): RoutePosition {
        val lap = (travelled.metres / length.metres).toInt()
        val into = travelled.metres % length.metres
        // Last leg whose start is at or before [into]; there is always one because cumulative[0] == 0.
        val leg = cumulative.indexOfLast { it <= into }.coerceAtMost(waypoints.size - 1)
        val from = waypoints[leg]
        val to = waypoints[(leg + 1) % waypoints.size]
        val legLength = cumulative[leg + 1] - cumulative[leg]
        val fraction = if (legLength == 0L) 0.0 else (into - cumulative[leg]).toDouble() / legLength
        return RoutePosition(interpolate(from.position, to.position, fraction), lap, Distance(into), from, to)
    }

    /**
     * The route as polylines for drawing, sampled every [stepKm] along each great-circle leg. Lines are split
     * where they cross the antimeridian, so a flat map never draws a stroke across the whole world.
     */
    fun polylines(stepKm: Double = 100.0): List<List<LatLng>> {
        val points = buildList {
            for (i in waypoints.indices) {
                val a = waypoints[i].position
                val b = waypoints[(i + 1) % waypoints.size].position
                val steps = (greatCircleMetres(a, b) / (stepKm * 1_000)).toInt().coerceAtLeast(1)
                for (s in 0 until steps) add(interpolate(a, b, s.toDouble() / steps))
            }
            add(start.position)
        }
        return splitAtAntimeridian(points)
    }

    internal fun splitAtAntimeridian(points: List<LatLng>): List<List<LatLng>> {
        val lines = mutableListOf(mutableListOf<LatLng>())
        for (p in points) {
            val current = lines.last()
            val last = current.lastOrNull()
            if (last != null && kotlin.math.abs(p.longitude - last.longitude) > 180) {
                // Cross at ±180, at the latitude interpolated between the two sides.
                val east = if (last.longitude > 0) 180.0 else -180.0
                val unwrapped = p.longitude + if (last.longitude > 0) 360 else -360
                val t = (east - last.longitude) / (unwrapped - last.longitude)
                val lat = last.latitude + t * (p.latitude - last.latitude)
                current.add(LatLng(lat, east))
                lines.add(mutableListOf(LatLng(lat, -east)))
            }
            lines.last().add(p)
        }
        return lines
    }

    fun greatCircleMetres(a: LatLng, b: LatLng): Long {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return (2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))).roundToLong()
    }

    /** The point [fraction] of the way from [a] to [b] along the great circle. */
    fun interpolate(a: LatLng, b: LatLng, fraction: Double): LatLng {
        if (fraction <= 0.0) return a
        if (fraction >= 1.0) return b
        val lat1 = Math.toRadians(a.latitude)
        val lon1 = Math.toRadians(a.longitude)
        val lat2 = Math.toRadians(b.latitude)
        val lon2 = Math.toRadians(b.longitude)
        val d = greatCircleMetres(a, b) / EARTH_RADIUS_M
        if (d == 0.0) return a
        val s = sin(d)
        val wa = sin((1 - fraction) * d) / s
        val wb = sin(fraction * d) / s
        val x = wa * cos(lat1) * cos(lon1) + wb * cos(lat2) * cos(lon2)
        val y = wa * cos(lat1) * sin(lon1) + wb * cos(lat2) * sin(lon2)
        val z = wa * sin(lat1) + wb * sin(lat2)
        return LatLng(Math.toDegrees(atan2(z, sqrt(x * x + y * y))), Math.toDegrees(atan2(y, x)))
    }
}
