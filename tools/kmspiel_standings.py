#!/usr/bin/env python3
"""Turns the text of a kmspiel league page into the standings snapshot the test app bundles.

kmspiel league pages are only readable when signed in (logged-out visitors get a human check), so there is no
automatic refresh. To refresh the snapshot:

1. Sign in to kmspiel.de in a browser and open the league page, e.g. https://www.kmspiel.de/2018/km_liga.php?liga=4
2. Select the whole page (Ctrl+A), copy it, and save the text to a file, e.g. liga4.txt.
3. Run:
   python3 tools/kmspiel_standings.py liga4.txt --my-team "LG Albatros Kiel" \\
       > android/core/data/src/main/resources/kmspiel/liga-4.tsv

Only team-level fields are kept: rank, team name, season distance, and number of runners. Distances are copied
verbatim in kmspiel's German format ("4.138" is 4,138 km); the app parses them. The script never sees cookies or
credentials. Permission to use this data is recorded in docs/adr/0003-kmspiel-standings-prototype.md.
"""
import argparse
import datetime
import re
import sys

ROW = re.compile(
    r"^(?P<rank>\d+)\.\s*(?:\(R\d+\)\s*)?[▲▼-]\s+(?P<title>.+?)\s+(?P<distance>\d{1,3}(?:\.\d{3})*(?:,\d+)?) km\s*\n"
    r"#\s*(?P<runners>\d+)\b",
    re.MULTILINE,
)
# kmspiel appends a recruiting note to small teams, e.g. "TuS Oedt needs 2p in -61d".
RECRUITING = re.compile(r"\s+needs \d+p in -?\d+d$")


def team_name(title):
    """The title is "<name> - <motto>"; a name in German quotes may itself contain " - "."""
    if title.startswith("„"):
        end = title.index('"', 1)
        return title[: end + 1]
    return RECRUITING.sub("", title.split(" - ", 1)[0].strip())


def parse(text):
    # A rank on its own line is followed by a relegation note such as "(R3)"; join them so each row is one line.
    text = re.sub(r"^(\d+)\.\n(\(R\d+\))", r"\1. \2", text, flags=re.MULTILINE)
    rows = [
        (int(m["rank"]), team_name(m["title"]), m["distance"], int(m["runners"]))
        for m in ROW.finditer(text)
    ]
    ranks = [r[0] for r in rows]
    if not rows or ranks != list(range(1, len(rows) + 1)):
        sys.exit(f"Expected ranks 1..n, found {ranks}. Did the page layout change?")
    return rows


def field(pattern, text, what):
    match = re.search(pattern, text, re.MULTILINE)
    if not match:
        sys.exit(f"Could not find the {what} on the page.")
    return match.group(1)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("page_text", help="text copied from a signed-in kmspiel league page")
    parser.add_argument("--my-team", required=True, help="the team to highlight as the Runner's own")
    parser.add_argument("--url", default="https://www.kmspiel.de/2018/km_liga.php?liga=4")
    parser.add_argument("--retrieved-at", help="UTC time the page was read, ISO 8601 (default: now)")
    args = parser.parse_args()

    with open(args.page_text, encoding="utf-8") as f:
        text = f.read()
    rows = parse(text)
    if args.my_team not in [r[1] for r in rows]:
        sys.exit(f"--my-team {args.my_team!r} is not in this league.")
    league = field(r"[?&]liga=(\d+)", args.url, "league number in --url") + ". Liga"
    retrieved = args.retrieved_at or datetime.datetime.now(datetime.UTC).strftime("%Y-%m-%dT%H:%M:%SZ")

    print("# kmspiel standings snapshot. Regenerate with tools/kmspiel_standings.py; do not edit by hand.")
    print(f"# competition\tkmspiel {league}")
    print(f"# season\t{field(r'Aktuelle Saison: (\S+)', text, 'season')}")
    print(f"# as_of\t{field(r'^(\d{2}\.\d{2}\.\d{4}), der', text, 'page date')}")
    print(f"# source_url\t{args.url}")
    print(f"# retrieved_at\t{retrieved}")
    print("# unit\tkm")
    print(f"# my_team\t{args.my_team}")
    print("rank\tteam\tdistance\trunners")
    for rank, name, distance, runners in rows:
        print(f"{rank}\t{name}\t{distance}\t{runners}")


if __name__ == "__main__":
    main()
