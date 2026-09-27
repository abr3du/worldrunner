package com.worldrunner.feature.standings.map

import com.worldrunner.core.model.Distance
import com.worldrunner.core.model.LatLng
import com.worldrunner.core.model.Standing
import com.worldrunner.core.model.Team
import com.worldrunner.core.model.WorldRoute
import com.worldrunner.core.model.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapGeometryTest {
    private val projection = Projection(worldWidth = 360f)

    private fun standing(rank: Int, km: Double, id: String = "t$rank") =
        Standing(rank, Team(id, "Team $rank", isMine = false), Distance.kilometres(km), activeRunners = 3, zone = Zone.Safe)

    @Test
    fun projectsCornersAndTheStart() {
        val northWest = projection.project(LatLng(LAT_NORTH, -180.0))
        val southEast = projection.project(LatLng(LAT_SOUTH, 180.0))
        assertEquals(0f, northWest.x, 0.001f)
        assertEquals(0f, northWest.y, 0.001f)
        assertEquals(360f, southEast.x, 0.001f)
        assertEquals(142f, southEast.y, 0.001f)
        val lisbon = projection.project(WorldRoute.start.position)
        assertEquals(180 - 9.1393f, lisbon.x, 0.01f)
    }

    @Test
    fun zoomKeepsTheFocusPointFixedAndIsBounded() {
        val zoomed = Camera().zoomBy(4f, 100f, 50f)
        val before = Camera().toScreen(MapPoint(100f, 50f))
        assertEquals(before, zoomed.toScreen(MapPoint(100f, 50f)))
        assertEquals(MAX_ZOOM, Camera().zoomBy(1_000f, 0f, 0f).zoom)
        assertEquals(MIN_ZOOM, Camera().zoomBy(0.001f, 0f, 0f).zoom)
    }

    @Test
    fun clampCentresASmallWorldAndStopsPanningPastTheEdge() {
        // Zoom 1 in a 360×180 viewport: the 142-high world is letterboxed and centred vertically.
        val fit = Camera().panBy(500f, 500f).clamped(projection, 360f, 180f)
        assertEquals(0f, fit.offsetX)
        assertEquals(19f, fit.offsetY, 0.001f)
        // Zoomed in, the world cannot be dragged away from the viewport edge.
        val zoomed = Camera(zoom = 4f, offsetX = 50f, offsetY = -10_000f).clamped(projection, 360f, 180f)
        assertEquals(0f, zoomed.offsetX)
        assertEquals(180f - 142f * 4, zoomed.offsetY, 0.01f)
    }

    @Test
    fun zeroDistanceTeamsSitAtTheStart() {
        val placed = placeTeams(listOf(standing(1, 0.0)), projection).single()
        assertEquals(projection.project(WorldRoute.start.position), placed.world)
        assertEquals(0, placed.route.lap)
    }

    @Test
    fun teamsBeyondACircuitShareThePositionOfTheirRemainderButCountTheLap() {
        val lapKm = WorldRoute.length.metres / 1_000.0
        val (first, second) = placeTeams(listOf(standing(1, lapKm + 500), standing(2, 500.0)), projection)
        assertEquals(second.world.x, first.world.x, 0.001f)
        assertEquals(second.world.y, first.world.y, 0.001f)
        assertEquals(1, first.route.lap)
        assertEquals(0, second.route.lap)
    }

    @Test
    fun overlappingMarkersClusterAndSeparateWhenZoomedIn() {
        // 1,400 and 1,356 km apart by 44 km: one marker for the whole world, two when zoomed right in.
        val teams = placeTeams(listOf(standing(1, 1_400.0), standing(2, 1_356.0), standing(3, 12_000.0)), projection)
        val world = clusterMarkers(teams, Camera(), radiusPx = 10f, selectedTeamId = null)
        assertEquals(listOf(listOf(1, 2), listOf(3)), world.map { c -> c.teams.map { it.standing.rank } })
        val focus = Camera().toScreen(teams[0].world)
        val close = clusterMarkers(teams, Camera().zoomBy(MAX_ZOOM, focus.x, focus.y), radiusPx = 5f, selectedTeamId = null)
        assertTrue(close.all { it.isSingle })
    }

    @Test
    fun theSelectedTeamIsNeverHiddenInACluster() {
        val teams = placeTeams(listOf(standing(1, 1_400.0), standing(2, 1_399.0), standing(3, 1_398.0)), projection)
        val clusters = clusterMarkers(teams, Camera(), radiusPx = 10f, selectedTeamId = "t2")
        assertEquals(listOf(listOf(1, 3), listOf(2)), clusters.map { c -> c.teams.map { it.standing.rank } })
        // Drawn last, so it is on top.
        assertEquals("t2", clusters.last().teams.single().standing.team.id)
    }

    @Test
    fun hitTestPicksTheNearestMarkerWithinReach() {
        val teams = placeTeams(listOf(standing(1, 0.0), standing(2, 12_000.0)), projection)
        val clusters = clusterMarkers(teams, Camera(), radiusPx = 10f, selectedTeamId = null)
        val far = Camera().toScreen(teams[1].world)
        assertEquals(2, hitTest(clusters, far.x + 3, far.y, radiusPx = 12f)?.teams?.single()?.standing?.rank)
        assertNull(hitTest(clusters, far.x + 30, far.y, radiusPx = 12f))
    }

    @Test
    fun parsesTheBundledLandFormatAndRejectsBrokenInput() {
        val land = parseLandOutlines("# comment\n-10,40 0,40 0,50 -10,40\n\n170,-10 180,-10 175,0\n")
        assertEquals(2, land.rings.size)
        assertEquals(LatLng(40.0, -10.0), land.rings[0][0])
        listOf("", "# only comments", "0,0 1,1", "200,0 1,1 2,2", "a,b c,d e,f").forEach { bad ->
            assertTrue(bad, runCatching { parseLandOutlines(bad) }.isFailure)
        }
    }
}
