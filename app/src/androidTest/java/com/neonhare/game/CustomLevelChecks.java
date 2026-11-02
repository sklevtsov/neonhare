package com.neonhare.game;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collections;

/** An independently authored size/spawn/gate is playable without a new branch in the engine. */
final class CustomLevelChecks {

    int verify(GameView view, android.content.Context context) throws Exception {
        JSONObject json =
                new JSONObject(
                        LevelCatalog.read(context.getAssets(), "levels/green-rift/level.json"));
        json.put("id", "custom-layout-contract")
                .put("width", 96)
                .put("height", 32)
                .put("exitColumn", 92)
                .put("spawn", new JSONObject().put("x", 200).put("y", 350))
                .put("gate", new JSONObject().put("column", 80).put("initiallyOpen", true));
        JSONArray tiles = new JSONArray(), inside = new JSONArray();
        for (int row = 0; row < 32; row++) {
            StringBuilder line = new StringBuilder(), room = new StringBuilder();
            for (int col = 0; col < 96; col++) {
                line.append(row == 20 || row == 21 ? '#' : '.');
                room.append('.');
            }
            tiles.put(line.toString());
            inside.put(room.toString());
        }
        json.put("tiles", tiles).put("interiors", inside);
        json.put(
                "checkpoints",
                new JSONArray()
                        .put(
                                new JSONObject()
                                        .put("id", "custom-entrance")
                                        .put("x", 200)
                                        .put("floorY", 720)));
        for (String group :
                new String[] {
                    "items",
                    "enemies",
                    "springs",
                    "relays",
                    "cacheWalls",
                    "warps",
                    "spikes",
                    "vents"
                }) {
            json.put(group, new JSONArray());
        }
        LevelDefinition custom = new LevelDefinition(json);
        Field field = Campaign.class.getDeclaredField("catalog");
        field.setAccessible(true);
        Object original = field.get(null);
        int count = Campaign.COUNT;
        GameWorld world = view.world;
        try {
            field.set(null, new LevelCatalog(Collections.singletonList(custom)));
            Campaign.COUNT = 1;
            world.progress = new Progression();
            world.loadLevel(0);
            require(
                    world.widthInTiles == 96
                            && world.heightInTiles == 32
                            && world.wall.length == 32,
                    "Custom map dimensions load from data");
            require(world.hero.x == 200 && world.hero.y == 350, "Custom spawn loads from data");
            world.hero.inv = 99;
            world.clearInput();
            for (int frame = 0; frame < 120; frame++) {
                world.tick(GameWorld.FIXED_STEP_SECONDS);
            }
            require(
                    world.hero.ground && Math.abs(world.hero.y - 677.99f) < .1f,
                    "Real physics lands on the custom map's floor");
            require(world.camY > 0, "Camera follows the taller custom map");
            world.setGate(false);
            world.hero.x = 80 * 36;
            world.hero.y = -150;
            world.move(100, 0);
            require(
                    world.hero.x < 80 * 36 - 26,
                    "Custom gate column cannot be bypassed above the map");
        } finally {
            field.set(null, original);
            Campaign.COUNT = count;
            world.restart();
        }
        return 5;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
