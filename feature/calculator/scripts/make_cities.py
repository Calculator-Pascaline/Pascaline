"""Adds the cities of GeoNames (CC BY 4.0) to cities.tsv of the Place map.

Natural Earth's cities stay, with their names in every language. GeoNames adds the towns it doesn't
have, under one name, for the search and to show when the map is zoomed in far enough.

    pip install fonttools
    python make_cities.py cities15000.txt      (from download.geonames.org/export/dump)
"""
import glob, math, os, sys, unicodedata
from fontTools.ttLib import TTFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..', '..')
FILE = os.path.join(HERE, '..', 'src', 'commonMain', 'composeResources', 'files', 'place', 'cities.tsv')
HEADER = '# Natural Earth populated places (public domain) and GeoNames (CC BY 4.0): '
LANGUAGES = 13
# Same name this close is the same city
SAME_CITY_KM = 25
CELL_DEGREES = 0.5
# Zoom of the map where towns appear, by population
ZOOMS = ((1_000_000, 4.5), (300_000, 5.5), (100_000, 6.5), (50_000, 7.5), (30_000, 8.0))
SMALL_TOWN_ZOOM = 8.5


def plain(name):
    return ''.join(c for c in unicodedata.normalize('NFD', name) if not unicodedata.combining(c)).casefold()


def distance_km(a, b):
    x = (a[1] - b[1]) * math.cos(math.radians((a[0] + b[0]) / 2))
    return math.hypot(x, a[0] - b[0]) * 111.2


def cell(lat, lon):
    return (int(lat / CELL_DEGREES), int(lon / CELL_DEGREES))


def font_characters():
    characters = set()
    for path in glob.glob(os.path.join(ROOT, 'sharedApp', 'src', 'wasmJsMain', 'composeResources', 'font', '*')):
        characters |= set(TTFont(path, fontNumber=0).getBestCmap())
    return characters


def main(source):
    lines = open(FILE, encoding='utf-8').read().split('\n')
    header = lines[0] if lines[0].startswith('#') else None
    rows = [l.split('\t') for l in lines if l and not l.startswith('#')]
    rows = [r for r in rows if len(r) > 6 or r[2] not in {str(z) for _, z in ZOOMS} | {str(SMALL_TOWN_ZOOM)}]
    known = {}
    for r in rows:
        known.setdefault(cell(float(r[0]), float(r[1])), []).append(((float(r[0]), float(r[1])), {plain(n) for n in r[5:] if n}))
    fonts = font_characters()
    added = 0
    for line in open(source, encoding='utf-8'):
        c = line.rstrip('\n').split('\t')
        name, ascii_name, lat, lon, population, zone = c[1], c[2], float(c[4]), float(c[5]), int(c[14] or 0), c[17]
        here = cell(lat, lon)
        nearby = (e for x in (-1, 0, 1) for y in (-1, 0, 1) for e in known.get((here[0] + x, here[1] + y), []))
        wanted = {plain(name), plain(ascii_name)}
        if any(names & wanted and distance_km(point, (lat, lon)) < SAME_CITY_KM for point, names in nearby):
            continue
        shown = name if all(ord(ch) < 0x80 or ord(ch) in fonts for ch in name) else ascii_name
        if not shown or '\t' in shown:
            continue
        zoom = next((z for size, z in ZOOMS if population >= size), SMALL_TOWN_ZOOM)
        rows.append([f'{lat:.4f}', f'{lon:.4f}', str(zoom), str(population), zone, shown])
        known.setdefault(here, []).append(((lat, lon), wanted))
        added += 1
    rows.sort(key=lambda r: -int(r[3] or 0))
    with open(FILE, 'w', encoding='utf-8', newline='\n') as f:
        if header:
            f.write(HEADER + header.split(': ', 1)[-1] + '\n')
        for r in rows:
            f.write('\t'.join(r).rstrip('\t') + '\n')
    print('added', added, 'total', len(rows), os.path.getsize(FILE) // 1024, 'KB')


main(sys.argv[1])
