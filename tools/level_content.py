#!/usr/bin/env python3
"""Dependency-free authoring checks. This checks body-space connectivity, not jump balance."""
import argparse
import collections
import html
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/levels'
GROUPS = ('items', 'enemies', 'springs', 'relays', 'cacheWalls', 'warps', 'spikes', 'vents')
KINDS = {'explore', 'ordered_relays', 'flight', 'perches', 'escort', 'ordered_keys', 'resonators', 'mirrors', 'sandstorm', 'captains', 'speedrun', 'rescue'}
GOALS = {'flight_ring', 'perch', 'cargo', 'ordered_key', 'crystal', 'mirror', 'obelisk', 'captain', 'speed_gate', 'rescue_console'}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def finite(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def validate(level):
    require(level['schemaVersion'] == 1, 'unsupported schemaVersion')
    width, height = level['width'], level['height']
    require(40 <= width <= 512 and 15 <= height <= 128, 'invalid dimensions')
    for name, symbols in [('tiles', '.#='), ('interiors', '.i')]:
        require(len(level[name]) == height, f'{name}: wrong row count')
        require(all(len(row) == width and set(row) <= set(symbols) for row in level[name]), f'{name}: invalid row')
    require(level['mission']['kind'] in KINDS, 'unknown mission kind')
    require(0 < level['gate']['column'] < level['exitColumn'] < width, 'invalid gate/exit')
    require(level['revision'] > 0, 'revision must be positive')
    for group in GROUPS:
        ids = [entry['id'] for entry in level[group]]
        require(len(ids) == len(set(ids)), f'{group}: duplicate IDs')
        for entry in level[group]:
            if 'x' in entry:
                require(finite(entry['x']) and finite(entry['y']), f'{group}: non-finite coordinate')
                require(0 <= entry['x'] < width * 36 and 0 <= entry['y'] < height * 36, f'{group}: outside world')
    goals = level['mission']['nodes']
    require(len(goals) <= 30, 'too many goals in one mission')
    require(len({goal['id'] for goal in goals}) == len(goals), 'duplicate goal IDs')
    for goal in goals:
        require(goal['type'] in GOALS, 'unknown goal behavior')
        require(goal['type'] != 'flight_ring' or level['requiresFlight'], 'flight ring requires flight ability')
    for enemy in level['enemies']:
        reference = enemy.get('objective', -1)
        require(reference == -1 or reference in {goal['id'] for goal in goals}, 'unknown enemy objective reference')
    def blocked(x, y):
        return any(col < 0 or col >= width or 0 <= row < height and level['tiles'][row][col] == '#'
                   for row in range(math.floor((y + 2) / 36), math.floor((y + 42) / 36) + 1)
                   for col in range(math.floor((x + 3) / 36), math.floor((x + 27) / 36) + 1))
    checkpoint_ids = [point['id'] for point in level['checkpoints']]
    require(checkpoint_ids and len(set(checkpoint_ids)) == len(checkpoint_ids), 'invalid checkpoint IDs')
    for point in level['checkpoints']:
        require(not blocked(point['x'], point['floorY'] - 42 - .01), f'checkpoint inside solid terrain: {point["id"]}')
    seen = set()
    spawn = level['spawn']
    queue = collections.deque([(int(spawn['x'] / 36), int(spawn['y'] / 36))])
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or x < 0 or x >= width or y < 0 or y + 1 >= height:
            continue
        if any(level['tiles'][row][x] == '#' for row in (y, y + 1)):
            continue
        seen.add((x, y))
        queue.extend([(x-1, y), (x+1, y), (x, y-1), (x, y+1)])
    for goal in goals:
        require((int(goal['x'] / 36), int((goal['y'] - 21) / 36)) in seen, f'body-space route missing to {goal["id"]}')


def preview(level, destination):
    scale = 8
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {level["width"] * scale} {level["height"] * scale + 28}">', '<rect width="100%" height="100%" fill="#071525"/>']
    for y, row in enumerate(level['tiles']):
        for x, tile in enumerate(row):
            if tile != '.':
                color = '#7196ad' if tile == '#' else '#77da93'
                parts.append(f'<rect x="{x*scale}" y="{y*scale+28}" width="8" height="{8 if tile == "#" else 2}" fill="{color}"/>')
    for index, goal in enumerate(level['mission']['nodes']):
        x, y = goal['x'] / 36 * scale, goal['y'] / 36 * scale + 28
        parts.append(f'<circle cx="{x}" cy="{y}" r="5" fill="#ffc36c"/><text x="{x+7}" y="{y+3}" fill="white" font-size="10">{index+1}</text>')
    parts.append(f'<text x="8" y="18" fill="white" font-size="13">{html.escape(level["title"])} — {html.escape(level["mission"]["name"])}</text></svg>')
    Path(destination).write_text('\n'.join(parts) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--preview-dir', type=Path)
    args = parser.parse_args()
    catalog = json.loads((args.root / 'campaign.json').read_text())
    require(catalog['schemaVersion'] == 1, 'unsupported campaign schema')
    require(catalog['levels'] and len(catalog['levels']) == len(set(catalog['levels'])), 'invalid campaign IDs')
    for identifier in catalog['levels']:
        require(identifier.replace('-', '').isalnum() and '/' not in identifier, 'invalid level path')
        level = json.loads((args.root / identifier / 'level.json').read_text())
        require(level['id'] == identifier, 'folder/id mismatch')
        validate(level)
        if args.preview_dir:
            args.preview_dir.mkdir(parents=True, exist_ok=True)
            preview(level, args.preview_dir / f'{identifier}.svg')
    print(f'PASS: {len(catalog["levels"])} level definitions; geometry, IDs, references and body-space routes checked')


if __name__ == '__main__':
    main()
