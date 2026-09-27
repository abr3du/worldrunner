# The world route

Route progress (see [`CONTEXT.md`](../CONTEXT.md)) shows each Team's total distance as a point along one shared, virtual route around the world. It is a visualisation only: it does not affect scoring, and a marker is never anyone's real location.

## Definition

- **Start:** Lisbon.
- **Direction:** east, through Europe and Asia, across the Pacific and North America, then over the Atlantic back to Lisbon.
- **Waypoints, in order:** Lisbon, Madrid, Barcelona, Marseille, Milan, Vienna, Budapest, Belgrade, Istanbul, Tehran, Delhi, Kolkata, Bangkok, Hanoi, Hong Kong, Shanghai, Seoul, Tokyo, Honolulu, San Francisco, Denver, Chicago, New York, Ponta Delgada (Azores), and back to Lisbon.
- **Legs:** great-circle arcs between consecutive Waypoints, on a sphere with the mean Earth radius (6,371,008.8 m).
- **Length of one circuit:** 36,084 km (the sum of the legs). `WorldRouteTest` pins it, because changing a Waypoint moves every Team.

The code is `WorldRoute` in `:core:model`. A distance maps to one deterministic point:

- 0 m is Lisbon.
- A distance past a Waypoint is on the next leg, interpolated along the great circle.
- A distance beyond one circuit wraps around and counts laps: 1 circuit + 500 km is drawn at the same point as 500 km, on lap 2.

## Map

The Standings screen draws the route and one marker per Team in the League on a world map:

- Land outlines are [Natural Earth](https://www.naturalearthdata.com) 1:110m land (public domain), bundled as `feature/standings/src/main/res/raw/world_land.txt`. Regenerate the file with `python3 tools/world_land.py`. The map needs no network, tile server, API key, or map account.
- The projection is equirectangular, cropped to latitudes 84° N to 58° S. The route is split where it crosses the antimeridian (between Tokyo and Honolulu).
- Markers show the Team's rank. Markers that would overlap on screen merge into one "+n" group; tapping a group zooms in on it. The selected Team always keeps its own marker.
- Pinch, drag, double-tap, the mouse wheel, and the zoom buttons move the map. Tapping a marker or a standings row shows that Team's rank, distance, lap, and position between Waypoints.
