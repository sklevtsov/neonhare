# World and running animation assets

Both assets were produced with the built-in imagegen tool and copied into the project.
The approved standing sprite remains `app/src/main/res/drawable-nodpi/nova_green.png`.

- Terrain: `app/src/main/res/drawable-nodpi/jungle_terrain.png`.
- Run cycle: `app/src/main/res/drawable-nodpi/nova_run.png`.

## Final terrain prompt

Asset type: detailed 2D side-scrolling alien-jungle platformer terrain TILE ATLAS, exactly 3 equal columns by 2 equal rows, square cells, no padding, no borders, no text. Orthographic SIDE VIEW, not isometric. Match richly detailed polished 1990s pixel-art action game: dark blue-grey stone strata, brown soil, dense vivid green moss, tiny fern fronds, fine tangled roots, chipped rock, occasional faint cyan bioluminescent crystals. Top row: THREE variations of solid platform SURFACE tiles. Each top tile has a perfectly FLAT bright green walkable moss top surface at the very top edge, solid rock and soil filling below, moss trailing down the front; SAME surface height across all three cells and matching side edges for horizontal tiling. Bottom row: THREE variations of solid UNDERGROUND rock and root tiles without grassy surface, filling entire cell edge to edge, compatible adjacent edges. Every cell fully opaque and filled with terrain; no sky, no holes, no objects sticking out above cell top. Detailed natural hand-painted pixel-art texture with crisp small highlights, deep crevice shading, readable strata and varied material texture, not simple geometric blocks or icons. Rich detail at 108px in-game scale. Rectangular atlas overall aspect ratio 3:2.

## Final run-cycle prompt

Reference: approved `nova_green.png`. Transparent background enabled.

Create EIGHT running animation poses of THIS EXACT detailed green rabbit hero as a perfectly regular 4-column x 2-row sprite sheet on transparent background. CRITICAL ENGINE REQUIREMENT: each sprite INCLUDING PISTOL AND LONG EARS must be SMALL and centered completely within the inner 70 percent of its own grid cell, with LARGE EMPTY TRANSPARENT GUTTERS on all four sides. No part of a sprite may cross a cell boundary. Overall sheet 1536x1024; each cell is 384x512, character maximum width 260px, maximum height 330px, feet baseline at cell y=420; so x ranges for sprites are 62..322, 446..706, 830..1090, 1214..1474. Preserve exact identity, detailed pixel art, green fur, red jacket, black shorts, white-red boots, cyan pistol. All face right with upper body and aim stable. Run cycle eight poses: contact, recoil, passing, high-knee, opposite-contact, opposite-recoil, opposite-passing, opposite-high-knee; visibly alternate both legs. Same scale in all cells. No text or drawn grid, no background. Genuine transparent alpha.


## Lion guardian

Built-in imagegen; project asset: `app/src/main/res/drawable-nodpi/lev_arden.png`.
`nova_green.png` was used only as a rendering-style reference.

Final prompt:

Asset type: production 2D platformer BOSS CHARACTER SPRITE. Reference image is STYLE REFERENCE ONLY for richly detailed polished pixel-art rendering, not the subject. Create an original powerful anthropomorphic male LION guardian named LEV ARDEN. Single FULL-BODY three-quarter SIDE VIEW facing LEFT, combat-ready pose, entire character centered with generous transparent margins. Muscular imposing broad shoulders, huge layered dark auburn and golden mane, weathered golden fur, intelligent amber eyes, small old scar across eyebrow, stern conflicted protective expression rather than mindless rage. Wears battered dark-teal/bronze waterworks guardian armor with engraved water channels and restrained turquoise power cores, heavy gauntlets, practical boots, a small blue cloth keepsake tied around his forearm. One massive armored fist forward, other hand open protectively. Lion face clearly recognizable with muzzle, ears, whiskers, thick mane and tail. Beautiful hand-painted pixel-art detail matching reference rabbit's dense shading and crisp highlights; boss should look majestic and formidable at 170px high. Genuine transparent background, no floor, no scenery, no lettering, no health bar, no grid. Preserve full visible feet, tail and mane. No crown, no sword, no cape covering the legs.

## Expanded campaign assets

Built-in imagegen. All four outputs are copied into Android resources:

| Asset | Saved path | Use |
|---|---|---|
| Six backgrounds | `app/src/main/res/drawable-nodpi/campaign_worlds.png` | Two columns × three rows: waterworks, sky, neon, crystal, desert, aurora |
| Six terrain materials | `app/src/main/res/drawable-nodpi/campaign_terrain.png` | Matching two-column × three-row atlas |
| Four villains | `app/src/main/res/drawable-nodpi/campaign_villains.png` | Rook, Vesper, Morrow and Registrar |
| Helicopter ears | `app/src/main/res/drawable-nodpi/nova_heli.png` | Approved rabbit in a flying pose with ears as rotor blades |

### Background prompt

Production 2D platformer BACKGROUND ATLAS, exactly TWO equal columns by THREE equal rows, six rectangular landscape panels with NO borders, NO text, NO foreground floor, NO characters. Rich detailed polished hand-painted PIXEL ART like a beautiful 1990s platformer. Each panel independent side-view landscape with distant/midground parallax scenery and playable open space in center. Panel1 top-left: ancient WATERWORKS, turquoise canals, bronze pipes, cyan waterfalls, lush green roots and glowing amber lamps. Panel2 top-right: floating SKY ISLANDS above immense turquoise and peach cloud sea, white limestone arches, giant pink flowers, saturated azure sky. Panel3 middle-left: NEON CITY at night, raspberry magenta skyscrapers, electric cyan signs WITHOUT LETTERS, golden cables, deep indigo sky. Panel4 middle-right: CRYSTAL CAVERNS, enormous violet amethyst and bright cyan crystal formations, glowing gold veins, deep blue underground lake, luminous plants. Panel5 bottom-left: SUNSET DESERT, warm coral and terracotta mesas, turquoise ancient ruins, pink and gold evening sky, rich dramatic shadows. Panel6 bottom-right: AURORA ICE CITADEL, indigo and cobalt fortress towers, turquoise ice, mint aurora, violet snowy mountains, gold windows. All six equal panels edge-to-edge, consistent pixel art detail and camera, vivid distinct color palettes, no black gutters. Intended to crop each equal cell separately as a horizontally scrolling game background.

### Terrain prompt

Production detailed PIXEL ART platform game TERRAIN ATLAS. Exactly TWO equal columns by THREE equal rows of six square tiles, edge to edge no gutters no text. Each tile is solid front-facing ground occupying entire cell with perfectly FLAT walkable surface at the top edge, no isometric perspective, no sky. Same detailed 1990s polished hand-painted pixel art. Tile1 top-left waterworks: carved turquoise stone, bronze pipes, water stains and green moss. Tile2 top-right sky island: white limestone with pink flowers and grassy teal top, intricate pale roots below. Tile3 middle-left neon city: deep indigo metal panels, magenta energy circuits and cyan luminous top lip, bolts and conduits. Tile4 middle-right crystal cave: dark violet rock embedded with dense cyan and golden crystal veins. Tile5 bottom-left desert: carved red sandstone, turquoise geometric ancient inlays, gold dust and warm coral top edge. Tile6 bottom-right ice citadel: cobalt blue stone, fractured mint ice and icicles, fine golden metal filigree. Tiles fully opaque and natural detailed texture; no characters or items or labels. Each cell suitable for horizontal repeating platforms.

### Villain prompt

Style reference: `nova_green.png`. Transparent background enabled.

Production 2D platformer VILLAIN SPRITE SHEET, exactly TWO equal columns by TWO equal rows of FOUR full-body characters. Reference rabbit is STYLE ONLY: match detailed polished pixel-art shading and crisp highlights. All characters face LEFT in menacing side-view combat pose. Genuine transparent background. Each full character must fit entirely inside central 75% of its own cell, generous transparent gutters, no overlap, no text. Top-left: ROOK, sleek powerful anthropomorphic black RAVEN debt collector, sharp gold beak, elegant magenta-black armored coat, cyan mechanical crossbow, folded dark wings, sly calculating expression. Top-right: VESPER, beautiful dangerous giant orchid MANTIS alchemist, emerald and violet organic armor, rose petal wings, luminous golden eyes and elegant razor forearms, intelligent calm expression. Bottom-left: MORROW, silver-white FOX illusionist and mapmaker, dark indigo explorer coat with turquoise chronometer, floating angular amethyst mirror shards, pale blue eyes, cunning composed face. Bottom-right: THE REGISTRAR, imposing NAUTILUS-like clockwork armored tyrant, large gold spiral shell, ivory mask with single cyan eye, dark violet mechanical robes and four articulated brass arms, an intimidating original final villain. Preserve same scale and fine detail, independent well-separated sprites, no scenery or floor.

### Flight prompt

Edit target: `nova_green.png`. Transparent background enabled.

Precise character animation pose edit. Preserve EXACT approved detailed green rabbit hero identity, pixel art, green fur, red jacket, black shorts, red-white shoes, face and cyan pistol. Single complete full-body sprite facing RIGHT, flying in midair with legs tucked slightly beneath body. His two long rabbit EARS extend opposite each other horizontally above his head as spinning HELICOPTER ROTOR BLADES, visibly part of his body, not a mechanical propeller and no hat. Suggest rotation with two small subtle translucent green motion arcs around the ears only. Maintain original attractive detailed rendering, anatomy and outfit. Entire ears, pistol, tail and boots visible with generous empty transparent margins, genuine transparent RGBA background, no landscape, no lettering, no other characters.
