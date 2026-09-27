package com.worldrunner.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WorldRouteTest {
    private val route = WorldRoute

    private fun assertNear(expected: LatLng, actual: LatLng, toleranceDeg: Double = 1e-6) {
        assertEquals(expected.latitude, actual.latitude, toleranceDeg)
        assertEquals(expected.longitude, actual.longitude, toleranceDeg)
    }

    @Test
    fun circuitLengthIsTheSumOfItsGreatCircleLegs() {
        // Lisbon eastbound and back: 36,084 km. Changing a Waypoint moves every Team, so this is pinned.
        assertEquals(36_084.0, route.length.metres / 1_000.0, 1.0)
        assertEquals(502.4, route.distanceTo(1).metres / 1_000.0, 0.1) // Lisbon → Madrid
    }

    @Test
    fun zeroDistanceIsAtTheStart() {
        val p = route.positionAt(Distance.Zero)
        assertNear(route.start.position, p.position)
        assertEquals(0, p.lap)
        assertEquals("Lisbon", p.previous.name)
        assertEquals("Madrid", p.next.name)
    }

    @Test
    fun exactlyAtAWaypointIsThatWaypoint() {
        val madrid = route.positionAt(route.distanceTo(1))
        assertNear(route.waypoints[1].position, madrid.position)
        assertEquals("Madrid", madrid.previous.name)
        assertEquals("Barcelona", madrid.next.name)
    }

    @Test
    fun oneMetreBeforeAWaypointIsStillOnTheLegTowardsIt() {
        val p = route.positionAt(Distance(route.distanceTo(1).metres - 1))
        assertEquals("Lisbon", p.previous.name)
        assertEquals("Madrid", p.next.name)
        assertNear(route.waypoints[1].position, p.position, toleranceDeg = 1e-3)
    }

    @Test
    fun halfwayAlongALegIsEquidistantFromBothEnds() {
        val from = route.distanceTo(9) // Tehran
        val to = route.distanceTo(10) // Delhi
        val mid = route.positionAt(Distance((from.metres + to.metres) / 2)).position
        val a = route.greatCircleMetres(route.waypoints[9].position, mid)
        val b = route.greatCircleMetres(mid, route.waypoints[10].position)
        assertTrue("$a vs $b", abs(a - b) <= 2)
    }

    @Test
    fun lastLegLeadsBackToTheStart() {
        val p = route.positionAt(Distance(route.length.metres - 1))
        assertEquals(0, p.lap)
        assertEquals("Ponta Delgada", p.previous.name)
        assertEquals("Lisbon", p.next.name)
    }

    @Test
    fun aFullCircuitIsBackAtTheStartOnTheNextLap() {
        val p = route.positionAt(route.length)
        assertNear(route.start.position, p.position)
        assertEquals(1, p.lap)
        assertEquals(Distance.Zero, p.distanceIntoLap)
    }

    @Test
    fun distanceBeyondACircuitWrapsAround() {
        val extra = Distance.kilometres(1_000.0)
        val wrapped = route.positionAt(route.length + route.length + extra)
        val firstLap = route.positionAt(extra)
        assertEquals(2, wrapped.lap)
        assertEquals(extra, wrapped.distanceIntoLap)
        assertNear(firstLap.position, wrapped.position)
    }

    @Test
    fun crossingThePacificIsSplitAtTheAntimeridian() {
        val lines = route.polylines()
        assertEquals(2, lines.size)
        // No segment may jump more than 180° of longitude, which a flat map would draw across the world.
        lines.forEach { line ->
            line.zipWithNext().forEach { (a, b) -> assertTrue(abs(a.longitude - b.longitude) < 180) }
        }
        assertEquals(180.0, abs(lines[0].last().longitude), 1e-9)
        assertEquals(lines[0].last().latitude, lines[1].first().latitude, 1e-9)
        assertNear(route.start.position, lines[0].first())
        assertNear(route.start.position, lines[1].last())
    }

    @Test
    fun theRouteHeadsEastFromLisbon() {
        val soon = route.positionAt(Distance.kilometres(100.0)).position
        assertTrue(soon.longitude > route.start.position.longitude)
    }
}
