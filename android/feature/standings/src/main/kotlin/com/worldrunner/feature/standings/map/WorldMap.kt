package com.worldrunner.feature.standings.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldrunner.core.model.DistanceUnit
import com.worldrunner.core.model.Standing
import com.worldrunner.core.model.WorldRoute
import com.worldrunner.feature.standings.R

internal sealed interface LandState {
    data object Loading : LandState
    data class Ready(val land: LandOutlines) : LandState
    data object Failed : LandState
}

/**
 * Every Team in the League placed on the [WorldRoute] by its total distance. This visualises accumulated distance;
 * it is not where anyone is. Tapping a marker selects its Team; tapping a group of overlapping Teams zooms in.
 */
@Composable
fun WorldMapCard(
    standings: List<Standing>,
    unit: DistanceUnit,
    selectedTeamId: String?,
    onSelectTeam: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    var attempt by remember { mutableIntStateOf(0) }
    var land by remember { mutableStateOf<LandState>(LandState.Loading) }
    LaunchedEffect(attempt) {
        land = LandState.Loading
        land = runCatching { loadLandOutlines(resources) }.fold({ LandState.Ready(it) }, { LandState.Failed })
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.map_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.map_disclaimer, WorldRoute.start.name, WorldRoute.length.format(unit)),
                style = MaterialTheme.typography.bodySmall,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .aspectRatio(2f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                when (val state = land) {
                    LandState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.map_loading), style = MaterialTheme.typography.bodySmall)
                    }
                    LandState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.map_error), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.map_retry)) }
                    }
                    is LandState.Ready -> {
                        MapCanvas(state.land, standings, selectedTeamId, onSelectTeam)
                        if (standings.isEmpty()) {
                            Text(stringResource(R.string.map_empty), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            SelectedTeamInfo(standings, selectedTeamId, unit)
            MapLegend()
            Text(stringResource(R.string.map_attribution), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MapCanvas(
    land: LandOutlines,
    standings: List<Standing>,
    selectedTeamId: String?,
    onSelectTeam: (String) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val ocean = if (dark) Color(0xFF15232C) else Color(0xFFDCEAF3)
    val landColor = if (dark) Color(0xFF34423A) else Color(0xFFEAE6D6)
    val colors = MaterialTheme.colorScheme
    val routeColor = colors.onSurfaceVariant
    val density = LocalDensity.current
    val markerRadius = with(density) { 12.dp.toPx() }
    val hitRadius = with(density) { 24.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val markerText = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)

    var size by remember { mutableStateOf(IntSize.Zero) }
    val projection = remember(size.width) { Projection(size.width.toFloat().coerceAtLeast(1f)) }
    var camera by remember { mutableStateOf(Camera()) }
    fun update(next: Camera) {
        camera = next.clamped(projection, size.width.toFloat(), size.height.toFloat())
    }
    LaunchedEffect(size) { update(Camera()) }

    val landPath = remember(projection, land) {
        Path().apply {
            land.rings.forEach { ring ->
                ring.forEachIndexed { i, p ->
                    val m = projection.project(p)
                    if (i == 0) moveTo(m.x, m.y) else lineTo(m.x, m.y)
                }
                close()
            }
        }
    }
    val routeLines = remember(projection) { WorldRoute.polylines().map { line -> line.map(projection::project) } }
    val start = remember(projection) { projection.project(WorldRoute.start.position) }
    val placed = remember(projection, standings) { placeTeams(standings, projection) }
    val clusters = clusterMarkers(placed, camera, markerRadius * 1.6f, selectedTeamId)
    val currentClusters by rememberUpdatedState(clusters)
    val currentSelect by rememberUpdatedState(onSelectTeam)

    // A Team selected from the list is brought into view if its marker is off screen.
    LaunchedEffect(selectedTeamId, projection) {
        val target = placed.firstOrNull { it.standing.team.id == selectedTeamId } ?: return@LaunchedEffect
        if (size.width > 0 && !camera.isVisible(target.world, size.width.toFloat(), size.height.toFloat())) {
            update(camera.centeredOn(target.world, size.width.toFloat(), size.height.toFloat()))
        }
    }

    val description = stringResource(R.string.map_content_description, standings.size)
    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .semantics { contentDescription = description }
                .pointerInput(projection, size) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        update(camera.zoomBy(zoom, centroid.x, centroid.y).panBy(pan.x, pan.y))
                    }
                }
                .pointerInput(projection, size) {
                    detectTapGestures(
                        onDoubleTap = { update(camera.zoomBy(2f, it.x, it.y)) },
                        onTap = { tap ->
                            val hit = hitTest(currentClusters, tap.x, tap.y, hitRadius) ?: return@detectTapGestures
                            if (hit.isSingle || camera.zoom >= MAX_ZOOM) {
                                currentSelect(hit.teams.first().standing.team.id)
                            } else {
                                update(camera.zoomBy(3f, hit.x, hit.y))
                            }
                        },
                    )
                }
                .pointerInput(projection, size) {
                    // Mouse wheel and trackpad zoom on ChromeOS and desktop-style windows.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.first()
                                val factor = if (change.scrollDelta.y < 0) 1.25f else 0.8f
                                update(camera.zoomBy(factor, change.position.x, change.position.y))
                                change.consume()
                            }
                        }
                    }
                },
        ) {
            drawRect(ocean)
            withTransform({
                translate(camera.offsetX, camera.offsetY)
                scale(camera.zoom, camera.zoom, pivot = Offset.Zero)
            }) {
                drawPath(landPath, landColor)
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            routeLines.forEach { line ->
                val path = Path()
                line.forEachIndexed { i, p ->
                    val s = camera.toScreen(p)
                    if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y)
                }
                drawPath(path, routeColor, style = Stroke(width = 2.dp.toPx(), pathEffect = dash))
            }
            val s = camera.toScreen(start)
            val half = 5.dp.toPx()
            drawRect(colors.onSurface, topLeft = Offset(s.x - half, s.y - half), size = Size(half * 2, half * 2))

            clusters.forEach { cluster ->
                val team = cluster.teams.first().standing
                val selected = cluster.isSingle && team.team.id == selectedTeamId
                val fill = when {
                    !cluster.isSingle -> colors.tertiary
                    team.team.isMine -> colors.secondary
                    else -> colors.primary
                }
                val onFill = when {
                    !cluster.isSingle -> colors.onTertiary
                    team.team.isMine -> colors.onSecondary
                    else -> colors.onPrimary
                }
                val r = if (selected) markerRadius * 1.3f else markerRadius
                val center = Offset(cluster.x, cluster.y)
                if (selected) drawCircle(colors.onSurface, r + 3.dp.toPx(), center)
                drawCircle(Color.White, r + 1.dp.toPx(), center)
                drawCircle(fill, r, center)
                val label = if (cluster.isSingle) "${team.rank}" else "+${cluster.teams.size}"
                drawCentredText(textMeasurer, label, center, markerText.copy(color = onFill))
            }
        }
        Column(
            Modifier.align(Alignment.TopEnd).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            MapButton(stringResource(R.string.map_zoom_in), onClick = { update(camera.zoomBy(2f, w / 2, h / 2)) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
            MapButton(stringResource(R.string.map_zoom_out), onClick = { update(camera.zoomBy(0.5f, w / 2, h / 2)) }) {
                Text("\u2212", style = MaterialTheme.typography.titleLarge)
            }
            MapButton(stringResource(R.string.map_reset), onClick = { update(Camera()) }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MapButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(36.dp).semantics { contentDescription = label }) {
        icon()
    }
}

/** Names the selected Team with its rank and distance, since a marker alone only shows the rank. */
@Composable
private fun SelectedTeamInfo(standings: List<Standing>, selectedTeamId: String?, unit: DistanceUnit) {
    val standing = standings.firstOrNull { it.team.id == selectedTeamId }
    if (standing == null) {
        Text(stringResource(R.string.map_select_hint), style = MaterialTheme.typography.bodySmall)
        return
    }
    val route = WorldRoute.positionAt(standing.totalDistance)
    val place = if (route.distanceIntoLap.metres == 0L) {
        stringResource(R.string.map_at_place, route.previous.name)
    } else {
        stringResource(R.string.map_between_places, route.previous.name, route.next.name)
    }
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(R.string.map_selected_team, standing.rank, standing.team.name),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.map_selected_detail, standing.totalDistance.format(unit), route.lap + 1, place),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapLegend() {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendItem(stringResource(R.string.legend_route)) {
            drawLine(colors.onSurfaceVariant, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
        }
        LegendItem(stringResource(R.string.legend_start, WorldRoute.start.name)) {
            val half = 5.dp.toPx()
            drawRect(colors.onSurface, Offset(center.x - half, center.y - half), Size(half * 2, half * 2))
        }
        LegendItem(stringResource(R.string.legend_team)) { drawCircle(colors.primary, size.minDimension / 2) }
        LegendItem(stringResource(R.string.legend_your_team)) { drawCircle(colors.secondary, size.minDimension / 2) }
        LegendItem(stringResource(R.string.legend_group)) { drawCircle(colors.tertiary, size.minDimension / 2) }
    }
}

@Composable
private fun LegendItem(label: String, swatch: DrawScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp), onDraw = swatch)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun DrawScope.drawCentredText(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    center: Offset,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
