package com.worldrunner.feature.standings.map

import android.content.res.Resources
import com.worldrunner.core.model.LatLng
import com.worldrunner.feature.standings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Land polygons, each an outer ring. Bundled with the app, so the map needs no network or map account. */
internal class LandOutlines(val rings: List<List<LatLng>>)

/** Parses `res/raw/world_land.txt`: `#` comment lines, then one ring per line as space-separated `lon,lat` pairs. */
internal fun parseLandOutlines(text: String): LandOutlines {
    val rings = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            line.split(' ').map { pair ->
                val (lon, lat) = pair.split(',').map { it.toDouble() }
                require(lon in -180.0..180.0 && lat in -90.0..90.0) { "Coordinate out of range: $pair" }
                LatLng(lat, lon)
            }.also { require(it.size >= 3) { "A land ring needs at least 3 points" } }
        }
        .toList()
    require(rings.isNotEmpty()) { "No land outlines" }
    return LandOutlines(rings)
}

internal suspend fun loadLandOutlines(resources: Resources): LandOutlines = withContext(Dispatchers.Default) {
    parseLandOutlines(resources.openRawResource(R.raw.world_land).bufferedReader().use { it.readText() })
}
