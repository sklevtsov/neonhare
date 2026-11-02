#!/usr/bin/env python3
"""Create a playable chapter draft beside existing content; append only when explicitly requested."""
import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'tools'))
from level_content import ROOT, validate, preview

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('id')
parser.add_argument('--title', required=True)
parser.add_argument('--root', type=Path, default=ROOT)
parser.add_argument('--append', action='store_true')
args = parser.parse_args()
if not re.fullmatch(r'[a-z][a-z0-9-]{0,63}', args.id):
    parser.error('id must be a stable lowercase name, e.g. moon-reservoir')
folder = args.root / args.id
if folder.exists():
    parser.error('level folder already exists; refusing to overwrite it')
width, height = 168, 27
level = dict(schemaVersion=1, id=args.id, revision=1, title=args.title, theme=0, accent='#ff97e889', style='open', requiresFlight=False, boss=None,
             width=width, height=height, spawn=dict(x=110, y=350), gate=dict(column=133, initiallyOpen=True), exitColumn=164, weather='none',
             egg=dict(title='СЕКРЕТ НОВОЙ ГЛАВЫ', note='Обсудить отсылку перед публикацией.'),
             mission=dict(kind='explore', name='Название задачи', help='Обсудить особенность маршрута и цель главы.', nodes=[]),
             tiles=['#'*width if row in (13, 14) else '.'*width for row in range(height)], interiors=['.'*width]*height,
             checkpoints=[dict(id='entrance', x=110, floorY=468), dict(id='middle', x=3000, floorY=468), dict(id='exit', x=5400, floorY=468)],
             items=[dict(id='seed-'+str(i), type='seed', x=200+i*72, y=432, value=0) for i in range(6)], enemies=[], springs=[], relays=[], cacheWalls=[], warps=[], spikes=[], vents=[])
validate(level)
folder.mkdir(parents=True)
(folder/'level.json').write_text(json.dumps(level, ensure_ascii=False, indent=2)+'\n')
(folder/'design.md').write_text(f'# {args.title}\n\nСтатус: черновик для обсуждения.\n\n## Идея\n\nЧем глава отличается от уже существующих?\n\n## Маршрут\n\nВход, ключевые сцены, развилки, возврат из секрета, выход.\n\n## Механика и обучение\n\nКакие умения нужны? Где безопасно научиться?\n\n## Бой и награды\n\nВраги, босс, оружие, пасхалка.\n\n## Приёмка\n\nПолный проход, все цели достижимы, каждый checkpoint безопасен, восстановление сессии работает.\n')
preview(level, folder/'preview.svg')
if args.append:
    index = args.root/'campaign.json'
    catalog = json.loads(index.read_text()) if index.exists() else dict(schemaVersion=1, levels=[])
    if args.id in catalog['levels']:
        parser.error('campaign already references this id')
    catalog['levels'].append(args.id)
    index.write_text(json.dumps(catalog, ensure_ascii=False, indent=2)+'\n')
print(f'Created draft: {folder}; '+('added to campaign' if args.append else 'not added to campaign yet'))
