package com.neonhare.game;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable, validated content. A chapter never contains executable Java or platform callbacks. */
final class LevelDefinition {

    enum Style {
        OPEN,
        CISTERN,
        TOWER,
        CAVERN,
        PYRAMID
    }

    enum MissionKind {
        EXPLORE,
        ORDERED_RELAYS,
        FLIGHT,
        PERCHES,
        ESCORT,
        ORDERED_KEYS,
        RESONATORS,
        MIRRORS,
        SANDSTORM,
        CAPTAINS,
        SPEEDRUN,
        RESCUE
    }

    enum Weather {
        NONE,
        GUSTS,
        SAND
    }

    static final String[] ITEM_TYPES = {
        "seed", "weapon", "health", "archive", "training", "easter_egg"
    };

    static final String[] ENEMY_TYPES = {
        "scout",
        "rammer",
        "flier",
        "rook_guard",
        "vespera_guard",
        "morrow_guard",
        "registrar_guard",
        "crawler",
        "shield",
        "hover",
        "hopper",
        "sentry",
        "worm"
    };

    static final String[] NODE_TYPES = {
        "flight_ring",
        "perch",
        "cargo",
        "ordered_key",
        "crystal",
        "mirror",
        "obelisk",
        "captain",
        "speed_gate",
        "rescue_console"
    };

    static final String[] BOSS_TYPES = {"lion", "vespera", "rook", "morrow", "registrar"};

    final String id, title, eggTitle, eggNote, missionName, help;
    final int revision, width, height, theme, accent, boss, gateColumn, exitColumn;
    final boolean requiresFlight, gateInitiallyOpen;

    final Style style;

    final MissionKind missionKind;

    final Weather weather;

    final float spawnX, spawnY;

    final float allyPostColumn;

    final float cartStartX,
            cartFloorY,
            cartSpeed,
            escortRadius,
            holdSeconds,
            minSpeed,
            calmSeconds,
            stormSeconds;
    final List<String> tiles, interiors;

    final List<Point> checkpoints;

    final List<ObjectSpec> objects;

    final List<Goal> goals;

    static final class Point {

        final String id;

        final float x, y;

        Point(String id, float x, float y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    static final class Goal {

        final String id;

        final int type;

        final float x, y;

        Goal(String id, int type, float x, float y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    static final class ObjectSpec {

        final String group, id;
        final int type, value, hp, objective;
        final float x, y, a, b;

        ObjectSpec(
                String group,
                String id,
                int type,
                int value,
                int hp,
                int objective,
                float x,
                float y,
                float a,
                float b) {
            this.group = group;
            this.id = id;
            this.type = type;
            this.value = value;
            this.hp = hp;
            this.objective = objective;
            this.x = x;
            this.y = y;
            this.a = a;
            this.b = b;
        }
    }

    LevelDefinition(JSONObject json) throws JSONException {
        require(json.getInt("schemaVersion") == 1, "Unsupported level schema");
        knownKeys(
                json,
                "schemaVersion id revision title theme accent style requiresFlight boss width"
                        + " height spawn gate exitColumn weather egg mission tiles interiors"
                        + " checkpoints items enemies springs relays cacheWalls warps spikes vents"
                        + " allyPostColumn");
        id = json.getString("id");
        require(id.matches("[a-z][a-z0-9-]{0,63}"), "Invalid stable level id");
        revision = positive(json.getInt("revision"), "revision");
        title = json.getString("title");
        require(!title.trim().isEmpty(), "Empty title");
        width = json.getInt("width");
        height = json.getInt("height");
        require(
                width >= 40 && width <= 512 && height >= 15 && height <= 128,
                "Level dimensions out of range");
        theme = json.getInt("theme");
        require(theme >= 0 && theme <= 6, "Unknown theme");
        String color = json.getString("accent");
        require(color.matches("#[0-9a-fA-F]{8}"), "Expected #AARRGGBB accent");
        accent = (int) Long.parseLong(color.substring(1), 16);
        style = Style.valueOf(json.getString("style").toUpperCase(java.util.Locale.ROOT));
        missionKind =
                MissionKind.valueOf(
                        json.getJSONObject("mission")
                                .getString("kind")
                                .toUpperCase(java.util.Locale.ROOT));
        weather = Weather.valueOf(json.getString("weather").toUpperCase(java.util.Locale.ROOT));
        requiresFlight = json.getBoolean("requiresFlight");
        allyPostColumn = json.has("allyPostColumn") ? number(json, "allyPostColumn") : 18;
        boss = json.isNull("boss") ? -1 : type(BOSS_TYPES, json.getString("boss"));
        require(boss < 0 || width >= 168, "Existing boss arenas need width >= 168");
        JSONObject spawn = json.getJSONObject("spawn");
        spawnX = number(spawn, "x");
        spawnY = number(spawn, "y");
        inWorld(spawnX, spawnY, "spawn");
        JSONObject gate = json.getJSONObject("gate");
        gateColumn = gate.getInt("column");
        gateInitiallyOpen = gate.getBoolean("initiallyOpen");
        exitColumn = json.getInt("exitColumn");
        require(
                gateColumn > 0 && gateColumn < exitColumn && exitColumn < width,
                "Invalid gate/exit ordering");
        tiles = rows(json.getJSONArray("tiles"), ".#=");
        interiors = rows(json.getJSONArray("interiors"), ".i");
        JSONObject egg = json.getJSONObject("egg");
        eggTitle = egg.getString("title");
        eggNote = egg.getString("note");
        JSONObject mission = json.getJSONObject("mission");
        knownKeys(mission, "kind name help nodes parameters");
        missionName = mission.getString("name");
        help = mission.getString("help");
        JSONObject parameters = mission.optJSONObject("parameters");
        if (parameters == null) {
            parameters = new JSONObject();
        }
        cartStartX = parameter(parameters, "cartStartX", 864);
        cartFloorY = parameter(parameters, "cartFloorY", 468);
        cartSpeed = parameter(parameters, "cartSpeed", 76);
        escortRadius = parameter(parameters, "escortRadius", 125);
        holdSeconds = parameter(parameters, "holdSeconds", 1);
        minSpeed = parameter(parameters, "minSpeed", 310);
        calmSeconds = parameter(parameters, "calmSeconds", 5);
        stormSeconds = parameter(parameters, "stormSeconds", 5);
        List<Goal> parsedGoals = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        JSONArray nodes = mission.getJSONArray("nodes");
        require(nodes.length() <= 30, "At most 30 mission nodes are supported");
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            knownKeys(n, "id type x y");
            String goalId = n.getString("id");
            require(ids.add(goalId), "Duplicate goal id");
            float x = number(n, "x"), y = number(n, "y");
            inWorld(x, y, "goal");
            parsedGoals.add(new Goal(goalId, type(NODE_TYPES, n.getString("type")), x, y));
        }
        goals = Collections.unmodifiableList(parsedGoals);
        for (Goal goal : goals) {
            require(
                    goal.type != ChapterMission.RING || requiresFlight,
                    "Flight objectives require the flight ability");
        }
        if (missionKind == MissionKind.ESCORT) {
            require(
                    goals.size() == 1 && goals.get(0).type == ChapterMission.CARGO,
                    "Escort mission requires one cargo destination");
            require(cartStartX < goals.get(0).x && cartFloorY < height * 36, "Invalid escort path");
        }
        if (missionKind == MissionKind.EXPLORE) {
            require(gateInitiallyOpen, "Exploration needs an initially open exit");
        }
        require(
                (missionKind == MissionKind.EXPLORE || missionKind == MissionKind.ORDERED_RELAYS)
                        || !goals.isEmpty(),
                "Mission requires goals");
        List<Point> points = new ArrayList<>();
        ids.clear();
        JSONArray cp = json.getJSONArray("checkpoints");
        require(cp.length() > 0 && cp.length() <= 32, "Invalid checkpoint count");
        for (int i = 0; i < cp.length(); i++) {
            JSONObject p = cp.getJSONObject(i);
            String pointId = p.getString("id");
            require(ids.add(pointId), "Duplicate checkpoint id");
            float x = number(p, "x"), y = number(p, "floorY");
            inWorld(x, y - 42, "checkpoint");
            require(!blocked(x, y - 42 - .01f), "Checkpoint inside solid terrain: " + pointId);
            points.add(new Point(pointId, x, y));
        }
        checkpoints = Collections.unmodifiableList(points);
        List<ObjectSpec> parsedObjects = new ArrayList<>();
        ids.clear();
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
            JSONArray objects = json.getJSONArray(group);
            require(objects.length() <= 2048, "Too many objects");
            for (int i = 0; i < objects.length(); i++) {
                JSONObject o = objects.getJSONObject(i);
                String objectId = group + ":" + o.getString("id");
                require(ids.add(objectId), "Duplicate object id: " + objectId);
                int objectType =
                        group.equals("items")
                                ? type(ITEM_TYPES, o.getString("type"))
                                : group.equals("enemies")
                                        ? type(ENEMY_TYPES, o.getString("type"))
                                        : 0;
                int value = o.optInt("value", 0),
                        hp = o.optInt("hp", 1),
                        objective = o.optInt("objective", -1);
                if (o.opt("objective") instanceof String) {
                    objective = -1;
                    String reference = o.getString("objective");
                    for (int goalIndex = 0; goalIndex < goals.size(); goalIndex++) {
                        if (goals.get(goalIndex).id.equals(reference)) {
                            objective = goalIndex;
                        }
                    }
                    require(objective >= 0, "Enemy references unknown goal ID");
                }
                float x, y, a = 0, b = 0;
                if (group.equals("cacheWalls")) {
                    x = o.getInt("column");
                    y = o.getInt("row");
                    require(x >= 0 && x < width && y >= 0 && y < height, "Cache outside map");
                } else if (group.equals("vents")) {
                    x = number(o, "left");
                    y = number(o, "top");
                    a = number(o, "right");
                    b = number(o, "bottom");
                    require(
                            x < a
                                    && y < b
                                    && x >= 0
                                    && a <= width * 36
                                    && y >= 0
                                    && b <= height * 36,
                            "Invalid vent bounds");
                } else {
                    x = number(o, "x");
                    y = number(o, "y");
                    inWorld(x, y, group);
                    if (group.equals("warps")) {
                        a = number(o, "destX");
                        b = number(o, "destY");
                        inWorld(a, b, "warp destination");
                    }
                    if (group.equals("spikes")) {
                        a = number(o, "width");
                        require(a > 0 && x + a <= width * 36, "Invalid spike width");
                    }
                }
                if (group.equals("items")) {
                    require(value >= 0, "Negative item value");
                    if (objectType == 1) {
                        require(value < 5, "Unknown weapon");
                    }
                    if (objectType == 3) {
                        require(value < 6, "Unknown archive");
                    }
                }
                if (group.equals("enemies")) {
                    require(hp > 0 && hp <= 100, "Invalid enemy health");
                    require(
                            objective >= -1 && objective < goals.size(),
                            "Enemy references missing objective");
                }
                parsedObjects.add(
                        new ObjectSpec(
                                group, objectId, objectType, value, hp, objective, x, y, a, b));
            }
        }
        objects = Collections.unmodifiableList(parsedObjects);
    }

    private List<String> rows(JSONArray data, String allowed) throws JSONException {
        require(data.length() == height, "Wrong map height");
        List<String> result = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            String row = data.getString(y);
            require(row.length() == width, "Wrong map width at row " + y);
            for (int x = 0; x < width; x++) {
                require(allowed.indexOf(row.charAt(x)) >= 0, "Unknown tile symbol");
            }
            result.add(row);
        }
        return Collections.unmodifiableList(result);
    }

    private boolean blocked(float x, float y) {
        for (int row = (int) Math.floor((y + 2) / 36);
                row <= (int) Math.floor((y + 42) / 36);
                row++) {
            for (int col = (int) Math.floor((x + 3) / 36);
                    col <= (int) Math.floor((x + 27) / 36);
                    col++) {
                if (col < 0
                        || col >= width
                        || row >= 0 && row < height && tiles.get(row).charAt(col) == '#') {
                    return true;
                }
            }
        }
        return false;
    }

    private void inWorld(float x, float y, String field) throws JSONException {
        require(x >= 0 && x < width * 36 && y >= 0 && y < height * 36, field + " outside map");
    }

    private static float parameter(JSONObject object, String key, float fallback)
            throws JSONException {
        float value = object.has(key) ? number(object, key) : fallback;
        require(value > 0, "Mission parameter must be positive: " + key);
        return value;
    }

    private static void knownKeys(JSONObject object, String permitted) throws JSONException {
        Set<String> allowed = new HashSet<>(java.util.Arrays.asList(permitted.split(" ")));
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            require(allowed.contains(key), "Unknown content field: " + key);
        }
    }

    static float number(JSONObject object, String key) throws JSONException {
        double n = object.getDouble(key);
        require(
                !Double.isNaN(n) && !Double.isInfinite(n) && Math.abs(n) < 1000000,
                "Non-finite coordinate");
        return (float) n;
    }

    static int type(String[] names, String value) throws JSONException {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(value)) {
                return i;
            }
        }
        throw new JSONException("Unknown content type: " + value);
    }

    private static int positive(int value, String field) throws JSONException {
        require(value > 0, "Invalid " + field);
        return value;
    }

    static void require(boolean condition, String message) throws JSONException {
        if (!condition) {
            throw new JSONException(message);
        }
    }
}
