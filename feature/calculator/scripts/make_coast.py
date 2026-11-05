"""Builds the coastlines of the Place map from Natural Earth (public domain).

Downloads the GeoJSON files, cuts land minus lakes into 10° tiles and writes them to
composeResources/files/place as coast_low.dat and coast_high.dat, read by CoastTiles in
PlaceCoast.kt. land_fill.webp is land.webp (Trail Sense's land map, kept next to this script) with its colors
spread over the sea, so that coasts sharper than its pixels never show an empty edge.

    pip install shapely pillow numpy scipy
    python make_coast.py <folder for downloads>

File: 649 little endian uint32 offsets of the 36 × 18 tiles (row 0 is the north), then the tiles.
Tile: varint number of rings, each one a varint number of points and zigzag varint steps in units
of 1/2000°, the first step from the corner of the tile. Rings are filled even-odd.
"""
import json, os, sys, struct, urllib.request
import numpy as np
from PIL import Image
from scipy import ndimage
from shapely import make_valid, set_precision
from shapely.geometry import box, shape
from shapely.ops import unary_union
from shapely.strtree import STRtree

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src', 'commonMain', 'composeResources', 'files', 'place')
URL = 'https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/'
TILE = 10
UNITS = 2000
BLUR = 2.5


def load(folder, name):
    path = os.path.join(folder, name + '.geojson')
    if not os.path.exists(path):
        urllib.request.urlretrieve(URL + name + '.geojson', path)
    with open(path, encoding='utf-8') as f:
        return [make_valid(shape(x['geometry'])) for x in json.load(f)['features']]


def varint(v, out):
    while v >= 0x80:
        out.append((v & 0x7F) | 0x80)
        v >>= 7
    out.append(v)


def zigzag(v):
    return v * 2 if v >= 0 else -v * 2 - 1


def polygons(g):
    if g.geom_type == 'Polygon':
        return [] if g.is_empty else [g]
    return [p for part in getattr(g, 'geoms', []) for p in polygons(part)]


def encode(geometry, x0, y0):
    rings = []
    for polygon in polygons(geometry):
        for ring in [polygon.exterior, *polygon.interiors]:
            points = []
            for x, y in ring.coords[:-1]:
                p = (round((x - x0) * UNITS), round((y0 - y) * UNITS))
                if not points or p != points[-1]:
                    points.append(p)
            if len(points) >= 3:
                rings.append(points)
    out = bytearray()
    varint(len(rings), out)
    for points in rings:
        varint(len(points), out)
        px, py = 0, 0
        for x, y in points:
            varint(zigzag(x - px), out)
            varint(zigzag(y - py), out)
            px, py = x, y
    return out, sum(len(r) for r in rings)


def build(land, lakes, tolerance, name):
    land_tree, lake_tree = STRtree(land), STRtree(lakes)
    blobs, total = [], 0
    for row in range(18):
        for col in range(36):
            x0, y0 = col * TILE - 180, 90 - row * TILE
            cell = box(x0, y0 - TILE, x0 + TILE, y0)
            parts = [land[i].intersection(cell) for i in land_tree.query(cell)]
            area = unary_union(parts) if parts else cell.difference(cell)
            water = [lakes[i].intersection(cell) for i in lake_tree.query(cell)]
            if water:
                area = area.difference(unary_union(water))
            if tolerance:
                area = area.simplify(tolerance, preserve_topology=True)
            area = make_valid(set_precision(area, 1 / UNITS))
            blob, points = encode(area, x0, y0)
            blobs.append(blob)
            total += points
    offset = 4 * 649
    table = bytearray()
    for blob in blobs:
        table += struct.pack('<I', offset)
        offset += len(blob)
    table += struct.pack('<I', offset)
    with open(os.path.join(OUT, name), 'wb') as f:
        f.write(table)
        for blob in blobs:
            f.write(blob)
    print(name, offset // 1024, 'KB', total, 'points', max(len(b) for b in blobs) // 1024, 'KB biggest tile')


def fill_colors():
    image = np.array(Image.open(os.path.join(HERE, 'land.webp')).convert('RGBA').resize((1900, 950), Image.NEAREST))
    rgb, alpha = image[..., :3], image[..., 3] > 128
    # Lakes are drawn as shapes now, their gray would show around them
    lake = (np.abs(rgb.astype(int) - rgb[..., [0]]).sum(axis=2) < 40) & (rgb[..., 0] < 170)
    known = alpha & ~lake
    nearest = ndimage.distance_transform_edt(~known, return_distances=False, return_indices=True)
    # Blurred, as the steps of its pixels would show on sharp coasts when zoomed in
    filled = ndimage.gaussian_filter(rgb[nearest[0], nearest[1]].astype(float), sigma=(BLUR, BLUR, 0), mode=('nearest', 'wrap', 'nearest'))
    Image.fromarray(filled.round().astype(np.uint8)).save(os.path.join(OUT, 'land_fill.webp'), quality=85, method=6)
    print('land_fill.webp', os.path.getsize(os.path.join(OUT, 'land_fill.webp')) // 1024, 'KB')


def main(folder):
    os.makedirs(folder, exist_ok=True)
    build(load(folder, 'ne_50m_land'), load(folder, 'ne_50m_lakes'), 0.02, 'coast_low.dat')
    build(load(folder, 'ne_10m_land') + load(folder, 'ne_10m_minor_islands'), load(folder, 'ne_10m_lakes'), 0.002, 'coast_high.dat')
    fill_colors()


if __name__ == '__main__':
    main(sys.argv[1])
