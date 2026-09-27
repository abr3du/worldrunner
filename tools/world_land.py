#!/usr/bin/env python3
"""Regenerates android/feature/standings/src/main/res/raw/world_land.txt from Natural Earth.

Usage: python3 tools/world_land.py > android/feature/standings/src/main/res/raw/world_land.txt

Natural Earth is public domain (https://www.naturalearthdata.com/about/terms-of-use/). Only outer rings are kept and
coordinates are rounded to 0.1 degree, which is plenty for a world view and keeps the file around 55 KB.
"""
import json
import urllib.request

SOURCE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_land.geojson"


def main():
    with urllib.request.urlopen(SOURCE) as response:
        data = json.load(response)
    print("# Land outlines: Natural Earth 1:110m land (public domain), https://www.naturalearthdata.com")
    print("# One outer ring per line as lon,lat pairs separated by spaces. Regenerate with tools/world_land.py.")
    for feature in data["features"]:
        geometry = feature["geometry"]
        polygons = [geometry["coordinates"]] if geometry["type"] == "Polygon" else geometry["coordinates"]
        for polygon in polygons:
            ring = []
            for lon, lat in polygon[0]:
                point = (round(lon, 1), round(lat, 1))
                if not ring or ring[-1] != point:
                    ring.append(point)
            if len(ring) >= 4:
                print(" ".join(f"{lon:g},{lat:g}" for lon, lat in ring))


if __name__ == "__main__":
    main()
