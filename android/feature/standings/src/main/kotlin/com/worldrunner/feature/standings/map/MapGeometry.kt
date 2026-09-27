package com.worldrunner.feature.standings.map

import com.worldrunner.core.model.LatLng
import com.worldrunner.core.model.RoutePosition
import com.worldrunner.core.model.Standing
import com.worldrunner.core.model.WorldRoute
import kotlin.math.hypot

/** Latitudes outside this band (mostly Antarctica) are cropped: nothing on the Route goes there. */
internal const val LAT_NORTH = 84.0
internal const val LAT_SOUTH = -58.0
internal const val MIN_ZOOM = 1f
internal const val MAX_ZOOM = 24f

internal data class MapPoint(val x: Float, val y: Float)

/** Equirectangular projection where the whole world is [worldWidth] wide at zoom 1. */
internal class Projection(val worldWidth: Float) {
    val worldHeight: Float = worldWidth * ((LAT_NORTH - LAT_SOUTH) / 360.0).toFloat()

    fun project(p: LatLng): MapPoint = MapPoint(
        ((p.longitude + 180.0) / 360.0 * worldWidth).toFloat(),
        ((LAT_NORTH - p.latitude) / (LAT_NORTH - LAT_SOUTH) * worldHeight).toFloat(),
    )
}

/** Maps world pixels to screen pixels: screen = world * zoom + offset. */
internal data class Camera(val zoom: Float = MIN_ZOOM, val offsetX: Float = 0f, val offsetY: Float = 0f) {
    fun toScreen(p: MapPoint) = MapPoint(p.x * zoom + offsetX, p.y * zoom + offsetY)

    /** Zooms by [factor] keeping the screen point ([focusX], [focusY]) fixed. */
    fun zoomBy(factor: Float, focusX: Float, focusY: Float): Camera {
        val z = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val applied = z / zoom
        return Camera(z, focusX - (focusX - offsetX) * applied, focusY - (focusY - offsetY) * applied)
    }

    fun panBy(dx: Float, dy: Float) = copy(offsetX = offsetX + dx, offsetY = offsetY + dy)

    /** Puts world point [p] in the middle of the viewport. */
    fun centeredOn(p: MapPoint, viewportWidth: Float, viewportHeight: Float, zoom: Float = this.zoom) =
        Camera(zoom, viewportWidth / 2 - p.x * zoom, viewportHeight / 2 - p.y * zoom)

    /** Keeps the world covering the viewport where it is big enough, and centred on an axis where it is not. */
    fun clamped(projection: Projection, viewportWidth: Float, viewportHeight: Float): Camera {
        fun axis(offset: Float, world: Float, viewport: Float) =
            if (world <= viewport) (viewport - world) / 2 else offset.coerceIn(viewport - world, 0f)
        return Camera(
            zoom,
            axis(offsetX, projection.worldWidth * zoom, viewportWidth),
            axis(offsetY, projection.worldHeight * zoom, viewportHeight),
        )
    }

    fun isVisible(p: MapPoint, viewportWidth: Float, viewportHeight: Float): Boolean =
        toScreen(p).let { it.x in 0f..viewportWidth && it.y in 0f..viewportHeight }
}

/** A Team at its Route position. [world] is in unzoomed map pixels. */
internal data class PlacedTeam(val standing: Standing, val route: RoutePosition, val world: MapPoint)

internal fun placeTeams(standings: List<Standing>, projection: Projection): List<PlacedTeam> = standings.map {
    val route = WorldRoute.positionAt(it.totalDistance)
    PlacedTeam(it, route, projection.project(route.position))
}

/** One or more Teams drawn as a single marker at screen position ([x], [y]). Teams are in rank order. */
internal data class MarkerCluster(val teams: List<PlacedTeam>, val x: Float, val y: Float) {
    val isSingle get() = teams.size == 1
}

/**
 * Groups Teams whose markers would overlap on screen, so each marker stays readable. Better-ranked Teams seed
 * clusters, and a cluster sits at its first Team. The [selectedTeamId] is never merged, so it is always visible.
 */
internal fun clusterMarkers(
    teams: List<PlacedTeam>,
    camera: Camera,
    radiusPx: Float,
    selectedTeamId: String?,
): List<MarkerCluster> {
    val clusters = mutableListOf<MutableList<Pair<PlacedTeam, MapPoint>>>()
    val selected = mutableListOf<MarkerCluster>()
    for (team in teams.sortedBy { it.standing.rank }) {
        val screen = camera.toScreen(team.world)
        if (team.standing.team.id == selectedTeamId) {
            selected += MarkerCluster(listOf(team), screen.x, screen.y)
            continue
        }
        val home = clusters.firstOrNull { c -> c.first().second.let { hypot(it.x - screen.x, it.y - screen.y) } < radiusPx }
        if (home != null) home += team to screen else clusters += mutableListOf(team to screen)
    }
    // Selected last, so it draws on top.
    return clusters.map { c -> MarkerCluster(c.map { it.first }, c.first().second.x, c.first().second.y) } + selected
}

/** The marker under a tap, preferring the nearest; null when the tap misses every marker. */
internal fun hitTest(clusters: List<MarkerCluster>, x: Float, y: Float, radiusPx: Float): MarkerCluster? =
    clusters.map { it to hypot(it.x - x, it.y - y) }
        .filter { it.second <= radiusPx }
        .minByOrNull { it.second }
        ?.first
