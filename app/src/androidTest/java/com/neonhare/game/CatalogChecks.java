package com.neonhare.game;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

/** Content contracts and migration parity, independent of renderer implementation details. */
final class CatalogChecks {

    private int checks;

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    int verify(Context target, Context tests) throws Exception {
        JSONArray legacy =
                new JSONArray(LevelCatalog.read(tests.getAssets(), "legacy-levels.json"));
        check(legacy.length() <= Campaign.COUNT, "All existing chapters migrated");
        for (int legacyIndex = 0; legacyIndex < legacy.length(); legacyIndex++) {
            int chapter = Campaign.indexOf(LegacySaveIds.at(legacyIndex));
            check(chapter >= 0, "Published legacy chapter remains available");
            LevelDefinition level = Campaign.level(chapter);
            JSONObject old = legacy.getJSONObject(legacyIndex);
            if (level.revision > 1) continue;
            check(level.title.equals(old.getString("title")), "Chapter title parity: " + level.id);
            for (int row = 0; row < level.height; row++) {
                check(
                        level.tiles.get(row).equals(old.getJSONArray("tiles").getString(row)),
                        "Collision grid parity: " + level.id + " row " + row);
                check(
                        level.interiors
                                .get(row)
                                .equals(old.getJSONArray("interiors").getString(row)),
                        "Interior grid parity: " + level.id + " row " + row);
            }
            check(
                    level.checkpoints.size() == old.getJSONArray("checkpoints").length(),
                    "Checkpoint count parity");
            for (int i = 0; i < level.checkpoints.size(); i++) {
                JSONArray point = old.getJSONArray("checkpoints").getJSONArray(i);
                check(
                        level.checkpoints.get(i).x == point.getDouble(0)
                                && level.checkpoints.get(i).y == point.getDouble(1),
                        "Checkpoint location parity: " + level.id);
            }
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
                java.util.List<LevelDefinition.ObjectSpec> actual = new java.util.ArrayList<>();
                for (LevelDefinition.ObjectSpec object : level.objects) {
                    if (object.group.equals(group)) {
                        actual.add(object);
                    }
                }
                JSONArray expected = old.getJSONArray(group);
                check(
                        actual.size() == expected.length(),
                        "Object count parity: " + level.id + " " + group);
                for (int i = 0; i < actual.size(); i++) {
                    LevelDefinition.ObjectSpec object = actual.get(i);
                    JSONArray values = expected.getJSONArray(i);
                    float[] tuple =
                            group.equals("items")
                                    ? new float[] {object.x, object.y, object.type, object.value}
                                    : group.equals("enemies")
                                            ? new float[] {
                                                object.x,
                                                object.y,
                                                object.type,
                                                object.hp,
                                                object.objective
                                            }
                                            : group.equals("warps")
                                                    ? new float[] {
                                                        object.x, object.y, object.a, object.b
                                                    }
                                                    : group.equals("spikes")
                                                            ? new float[] {
                                                                object.x, object.y, object.a
                                                            }
                                                            : group.equals("vents")
                                                                    ? new float[] {
                                                                        object.x, object.a,
                                                                        object.y, object.b
                                                                    }
                                                                    : new float[] {
                                                                        object.x, object.y
                                                                    };
                    for (int field = 0; field < tuple.length; field++) {
                        check(
                                tuple[field] == (float) values.getDouble(field),
                                "Object value parity: " + level.id + " " + group + " #" + i);
                    }
                }
            }
        }
        JSONObject sample =
                new JSONObject(
                        LevelCatalog.read(target.getAssets(), "levels/green-rift/level.json"));
        rejected(
                new JSONObject(sample.toString()).put("schemaVersion", 99),
                "Unknown schema rejected");
        rejected(
                new JSONObject(sample.toString()).put("width", 99999),
                "Unbounded geometry rejected");
        JSONObject broken = new JSONObject(sample.toString());
        broken.getJSONArray("items").getJSONObject(0).put("type", "unknown-power");
        rejected(broken, "Unknown item behavior rejected");
        broken = new JSONObject(sample.toString());
        broken.getJSONArray("checkpoints").getJSONObject(0).put("x", -100);
        rejected(broken, "Unsafe checkpoint rejected");
        broken = new JSONObject(sample.toString());
        broken.getJSONArray("items").put(broken.getJSONArray("items").getJSONObject(0));
        rejected(broken, "Duplicate entity id rejected");
        return checks;
    }

    private void rejected(JSONObject input, String message) throws Exception {
        boolean rejected = false;
        try {
            new LevelDefinition(input);
        } catch (org.json.JSONException | IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, message);
    }
}
