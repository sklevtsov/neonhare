package com.neonhare.game;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Device checks for the actual game physics and handheld input, without test dependencies. */
public final class GameChecks extends Instrumentation {

    private int checks;

    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getSharedPreferences("neon_hare_checks", 0).edit().clear().commit();
            Activity activity =
                    startActivitySync(
                            new Intent()
                                    .setClassName(getTargetContext(), MainActivity.class.getName())
                                    .putExtra("test_profile", true)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final Throwable[] failure = {null};
            runOnMainSync(
                    () -> {
                        try {
                            verify(activity);
                        } catch (Throwable e) {
                            failure[0] = e;
                        }
                    });
            if (failure[0] != null) {
                throw failure[0];
            }
            result.putString("stream", "\nPASS: " + checks + " gameplay checks\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable e) {
            result.putString("stream", "\nFAIL: " + e + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private Object field(Object object, String name) throws Exception {
        if (object instanceof GameView) {
            if (name.equals("menuPage")) {
                return ((GameView) object).world.screen.ordinal();
            }
            GameView view = (GameView) object;
            view.world.processInput(false);
            for (Object target : new Object[] {view, view.world, view.renderer, view.assets}) {
                try {
                    Field f = target.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw new NoSuchFieldException(name);
        }
        Field f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }

    private void set(Object object, String name, Object value) throws Exception {
        if (object instanceof GameView) {
            GameView view = (GameView) object;
            view.world.processInput(false);
            for (Object target : new Object[] {view.world, view.renderer, view.assets, view}) {
                try {
                    Field f = target.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw new NoSuchFieldException(name);
        }
        Field f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(object, value);
    }

    private Object call(Object object, String name, Class<?>[] types, Object... args)
            throws Exception {
        if (object instanceof GameView) {
            GameView view = (GameView) object;
            view.world.processInput(false);
            for (Object target : new Object[] {view.world, view.renderer, view.assets, view}) {
                try {
                    Method method = target.getClass().getDeclaredMethod(name, types);
                    method.setAccessible(true);
                    Object result = method.invoke(target, args);
                    if (name.equals("store")) {
                        view.world.flushSave(true);
                    }
                    return result;
                } catch (NoSuchMethodException ignored) {
                }
            }
            throw new NoSuchMethodException(name);
        }
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(object, args);
    }

    private void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private float number(Object o, String name) throws Exception {
        return ((Number) field(o, name)).floatValue();
    }

    private void move(GameView v, float dx, float dy) throws Exception {
        call(v, "move", new Class<?>[] {float.class, float.class}, dx, dy);
    }

    private void tick(GameView v) throws Exception {
        call(v, "tick", new Class<?>[] {float.class}, 1f / 60);
    }

    private void press(GameView v, int key) {
        v.onKeyDown(key, new KeyEvent(KeyEvent.ACTION_DOWN, key));
        v.world.processInput(false);
    }

    private void release(GameView v, int key) {
        v.onKeyUp(key, new KeyEvent(KeyEvent.ACTION_UP, key));
        v.world.processInput(false);
    }

    private boolean roomReachable(
            boolean[][] wall, boolean[][] ledge, int sx, int sy, int gx, int gy) {
        boolean[][] seen = new boolean[wall.length][wall[0].length];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[] {sx, sy});
        while (!queue.isEmpty()) {
            int[] point = queue.remove();
            int x = point[0], y = point[1];
            if (x < 0
                    || x >= wall[0].length
                    || y < 0
                    || y + 1 >= wall.length
                    || seen[y][x]
                    || (wall[y][x] && !ledge[y][x])
                    || (wall[y + 1][x] && !ledge[y + 1][x])) {
                continue;
            }
            seen[y][x] = true;
            if (x == gx && y == gy) {
                return true;
            }
            queue.add(new int[] {x + 1, y});
            queue.add(new int[] {x - 1, y});
            queue.add(new int[] {x, y + 1});
            queue.add(new int[] {x, y - 1});
        }
        return false;
    }

    private void verify(Activity a) throws Exception {
        GameView v = (GameView) field(a, "gameView");
        v.pause();
        checks += new CatalogChecks().verify(getTargetContext(), getContext());
        checks += new SaveMigrationChecks().verify(getTargetContext(), v.world);
        checks += new CustomLevelChecks().verify(v, getTargetContext());
        checks += new ConcurrencyChecks().verify(v);
        checks += new WeaponOriginChecks().verify(v);
        v.world.restart();
        v.clearInput();
        Object h = field(v, "hero");
        check(number(v, "lives") == 99, "Debug builds need 99 lives");
        ((List<?>) field(v, "enemies")).clear();
        // A tile can lie between the old corner probes: the complete body must collide.
        check(
                (Boolean)
                        call(v, "blocked", new Class<?>[] {float.class, float.class}, 1875f, 321f),
                "Middle-of-body platform collision");
        set(h, "x", 230f);
        set(h, "y", 290f);
        set(h, "vy", 900f);
        move(v, 0, 90);
        check(
                Math.abs(number(h, "y") - 317.99f) < .1f,
                "Land exactly on platform without penetration");
        check((Boolean) field(h, "ground"), "Landing restores grounded state");
        for (int i = 0; i < 60; i++) {
            move(v, 0, .5f);
        }
        check(
                Math.abs(number(h, "y") - 317.99f) < .1f,
                "Standing on platform must not sink or jitter");
        // Spawn underneath row 10; colliding with its underside must stop upward motion.
        set(h, "x", 230f);
        set(h, "y", 410f);
        set(h, "vy", -800f);
        move(v, 0, -65);
        check(
                number(h, "y") < 394f && number(h, "vy") < 0,
                "Elevated platforms allow jumping through from below");
        set(h, "y", 550f);
        set(h, "vy", -800f);
        move(v, 0, -65);
        check(
                number(h, "y") >= 538f && number(h, "vy") == 0,
                "Solid terrain blocks upward movement");
        // Pillar 52 blocks fast lateral motion from an unobstructed starting position.
        set(h, "x", 1810f);
        set(h, "y", 350f);
        set(h, "vx", 455f);
        move(v, 150, 0);
        check(
                number(h, "x") < 1845.1f && number(h, "vx") == 0,
                "Cannot tunnel sideways through pillar");
        set(h, "x", 110f);
        set(h, "y", 425.99f);
        set(h, "ground", true);
        set(h, "vy", 0f);
        set(h, "jumps", 0);
        press(v, KeyEvent.KEYCODE_DPAD_UP);
        tick(v);
        check(number(h, "vy") < 0 && number(h, "jumps") == 1, "D-pad UP starts first jump");
        release(v, KeyEvent.KEYCODE_DPAD_UP);
        tick(v);
        press(v, KeyEvent.KEYCODE_DPAD_UP);
        tick(v);
        check(
                number(h, "vy") < -600 && number(h, "jumps") == 2,
                "Fresh UP press performs air jump");
        release(v, KeyEvent.KEYCODE_DPAD_UP);
        tick(v);
        float beforeThird = number(h, "vy");
        press(v, KeyEvent.KEYCODE_DPAD_UP);
        tick(v);
        check(number(h, "vy") > beforeThird && number(h, "jumps") == 2, "Third jump is rejected");
        v.clearInput();
        press(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        tick(v);
        check(number(h, "vx") > 0, "D-pad RIGHT moves the rabbit");
        release(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        check(
                v.onKeyDown(KeyEvent.KEYCODE_BUTTON_B, new KeyEvent(0, KeyEvent.KEYCODE_BUTTON_B)),
                "B is consumed");
        check(
                v.onKeyUp(KeyEvent.KEYCODE_BACK, new KeyEvent(1, KeyEvent.KEYCODE_BACK)),
                "BACK cannot close the game");
        check(
                a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)),
                "Activity consumes handheld BACK mapping");
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        check(!a.isFinishing(), "BACK leaves the activity running");
        v.clearInput();
        MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
        pp.id = 0;
        MotionEvent.PointerCoords pc = new MotionEvent.PointerCoords();
        pc.setAxisValue(MotionEvent.AXIS_HAT_X, -1);
        pc.setAxisValue(MotionEvent.AXIS_HAT_Y, -1);
        MotionEvent event =
                MotionEvent.obtain(
                        0,
                        0,
                        MotionEvent.ACTION_MOVE,
                        1,
                        new MotionEvent.PointerProperties[] {pp},
                        new MotionEvent.PointerCoords[] {pc},
                        0,
                        0,
                        1,
                        1,
                        0,
                        0,
                        InputDevice.SOURCE_JOYSTICK,
                        0);
        v.onGenericMotionEvent(event);
        event.recycle();
        check(
                number(v, "stick") == -1 && (Boolean) field(v, "jump"),
                "Hat-axis D-pad supports move and jump together");
        // Reproduce the reported unreachable first platform with a one-frame tap.
        v.clearInput();
        set(h, "x", 230f);
        set(h, "y", 425.99f);
        set(h, "ground", true);
        set(h, "vx", 0f);
        set(h, "vy", 0f);
        set(h, "jumps", 0);
        set(h, "buffer", 0f);
        press(v, KeyEvent.KEYCODE_DPAD_UP);
        tick(v);
        release(v, KeyEvent.KEYCODE_DPAD_UP);
        for (int i = 0; i < 90; i++) {
            tick(v);
        }
        check(
                Math.abs(number(h, "y") - 317.99f) < .1f && (Boolean) field(h, "ground"),
                "A quick tap reaches and lands on the first platform");
        // The second ledge is five tiles above the ground and is reachable with a held jump.
        set(h, "x", 518f);
        set(h, "y", 425.99f);
        set(h, "ground", true);
        set(h, "vy", 0f);
        set(h, "jumps", 0);
        set(h, "buffer", 0f);
        press(v, KeyEvent.KEYCODE_SPACE);
        for (int i = 0; i < 90; i++) {
            tick(v);
        }
        release(v, KeyEvent.KEYCODE_SPACE);
        check(
                Math.abs(number(h, "y") - 245.99f) < .1f,
                "A held jump reaches a platform 180 pixels above ground");
        set(h, "x", 3101f);
        set(h, "y", 425.99f);
        set(h, "ground", true);
        set(h, "vy", 0f);
        set(h, "jumps", 0);
        set(h, "buffer", 0f);
        press(v, KeyEvent.KEYCODE_SPACE);
        for (int i = 0; i < 12; i++) {
            tick(v);
        }
        release(v, KeyEvent.KEYCODE_SPACE);
        tick(v);
        press(v, KeyEvent.KEYCODE_SPACE);
        tick(v);
        release(v, KeyEvent.KEYCODE_SPACE);
        for (int i = 0; i < 100; i++) {
            tick(v);
        }
        check(
                Math.abs(number(h, "y") - 137.99f) < .1f,
                "Double jump reaches the high platform 288 pixels above ground");
        set(h, "x", 110f);
        set(h, "y", 425.99f);
        set(h, "ground", true);
        set(h, "vx", 0f);
        ((List<?>) field(v, "bullets")).clear();
        press(v, KeyEvent.KEYCODE_Z);
        for (int i = 0; i < 40; i++) {
            tick(v);
        }
        release(v, KeyEvent.KEYCODE_Z);
        check(
                ((List<?>) field(v, "bullets")).size() >= 2,
                "Holding Z repeatedly fires the blaster");
        set(h, "vx", 300f);
        set(h, "ground", true);
        int initial = (Integer) call(v, "runFrameIndex", new Class<?>[] {});
        press(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        for (int i = 0; i < 8; i++) {
            tick(v);
        }
        release(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        int later = (Integer) call(v, "runFrameIndex", new Class<?>[] {});
        check(initial != later, "Running advances leg animation frames");
        call(v, "restart", new Class<?>[] {});
        ((List<?>) field(v, "enemies")).clear();
        boolean[][] world = (boolean[][]) field(v, "wall");
        check(
                world.length == 27 && world[0].length == 168,
                "Campaign maps support stacked routes and a longer exploration space");
        List<?> exits = (List<?>) field(v, "springs");
        boolean allPocketsHaveExit = true;
        for (int start = 14; start <= 148; ) {
            while (start <= 148 && world[23][start]) {
                start++;
            }
            if (start > 148) {
                break;
            }
            int end = start;
            while (end < 148 && !world[23][end + 1]) {
                end++;
            }
            boolean hasExit = false;
            for (Object spring : exits) {
                if (number(spring, "y") == 24 * 36f
                        && number(spring, "x") >= start * 36f
                        && number(spring, "x") < (end + 1) * 36f) {
                    hasExit = true;
                }
            }
            allPocketsHaveExit &= hasExit;
            start = end + 1;
        }
        check(allPocketsHaveExit, "Every enclosed lower-route chamber has a spring exit");
        set(h, "x", 17 * 36f - 15);
        set(h, "y", 24 * 36f - 42);
        set(h, "vy", 0f);
        set(h, "springCool", 0f);
        call(v, "updateWorld", new Class<?>[] {float.class}, 1f / 60);
        check(
                number(h, "vy") < -1300,
                "Lower-route spring launches high enough to escape the watercourse");
        set(h, "vx", 0f);
        set(h, "inv", 99f);
        v.clearInput();
        for (int i = 0; i < 28; i++) {
            tick(v);
        }
        press(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        for (int i = 0; i < 30; i++) {
            tick(v);
        }
        release(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        for (int i = 0; i < 60; i++) {
            tick(v);
        }
        check(
                (Boolean) field(h, "ground") && number(h, "y") < 500,
                "Spring route returns from the watercourse to the surface without teleporting");
        set(h, "springCool", 1f);
        set(h, "y", 790f);
        set(h, "vy", 0f);
        set(h, "inv", 99f);
        tick(v);
        check(number(v, "camY") > 0, "Camera follows the lower route vertically");
        List<?> items = (List<?>) field(v, "items");
        Object archive = null;
        for (Object item : items) {
            if (number(item, "kind") == 3) {
                archive = item;
                break;
            }
        }
        check(archive != null, "Exploration route contains a story archive");
        set(h, "x", number(archive, "x"));
        set(h, "y", number(archive, "y"));
        call(v, "updateItems", new Class<?>[] {});
        int savedLore = (Integer) field(v, "loreMask");
        check(savedLore != 0, "Picking up an archive reveals and remembers its story");
        set(v, "gun", 0);
        set(h, "face", 1);
        set(h, "x", 47 * 36f - 50);
        set(h, "y", 17 * 36f + 8 - (v.world.playerPose().muzzleY(v.world.hero) - v.world.hero.y));
        call(v, "shoot", new Class<?>[] {});
        call(v, "updateBullets", new Class<?>[] {float.class}, .025f);
        check(!world[17][47], "Shooting the cracked masonry opens a secret passage");
        call(v, "finish", new Class<?>[] {});
        press(v, KeyEvent.KEYCODE_ENTER);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_ENTER);
        check(
                number(v, "levelIndex") == 1 && !(Boolean) field(v, "won"),
                "Finishing the garden advances to the waterworks");
        check(number(v, "loreMask") == savedLore, "Archives carry across chapter transitions");
        ((List<?>) field(v, "enemies")).clear();
        check(!(Boolean) field(v, "gateOpen"), "Waterworks gate starts locked");
        set(h, "x", 132 * 36f);
        set(h, "y", -200f);
        move(v, 200, 0);
        check(number(h, "x") < 133 * 36f - 26, "Gate cannot be bypassed by jumping above the map");
        List<?> relays = (List<?>) field(v, "relays");
        Object firstRelay = relays.get(0);
        set(h, "x", number(firstRelay, "x") - 50);
        set(
                h,
                "y",
                number(firstRelay, "y")
                        - (v.world.playerPose().muzzleY(v.world.hero) - v.world.hero.y));
        set(h, "face", 1);
        call(v, "shoot", new Class<?>[] {});
        call(v, "updateBullets", new Class<?>[] {float.class}, .025f);
        check((Boolean) field(firstRelay, "active"), "Relays can be activated by shooting them");
        for (Object relay : relays) {
            call(v, "activateRelay", new Class<?>[] {relay.getClass()}, relay);
        }
        check((Boolean) field(v, "gateOpen"), "All three exploration relays open the gate");
        world = (boolean[][]) field(v, "wall");
        check(
                !world[0][133] && !world[26][133],
                "Opening the gate removes its entire collision column");
        call(v, "previewBoss", new Class<?>[] {});
        call(v, "updateBoss", new Class<?>[] {float.class}, .016f);
        Object boss = field(v, "boss");
        check(
                (Boolean) field(boss, "active"),
                "Lion encounter begins when the player enters the arena");
        call(v, "damageBoss", new Class<?>[] {int.class}, 5);
        check(number(boss, "hp") == 43, "All visible boss states take normal damage");
        set(boss, "state", 3);
        call(v, "damageBoss", new Class<?>[] {int.class}, 5);
        check(
                number(boss, "hp") == 33,
                "Recovery doubles damage rather than being the only hittable state");
        set(boss, "state", 1);
        set(boss, "timer", .01f);
        set(boss, "cycle", 0);
        call(v, "updateBoss", new Class<?>[] {float.class}, .02f);
        check(number(boss, "state") == 2, "Telegraphed attack transitions into a charge");
        set(boss, "timer", .01f);
        call(v, "updateBoss", new Class<?>[] {float.class}, .02f);
        check(number(boss, "state") == 3, "Charge ends with a punishable recovery window");
        set(boss, "state", 1);
        set(boss, "plan", BossBrain.GROUND_SLAM);
        set(boss, "timer", .01f);
        set(boss, "cycle", 1);
        set(boss, "hp", 20);
        call(v, "updateBoss", new Class<?>[] {float.class}, .02f);
        check(
                ((List<?>) field(v, "waves")).size() == 2,
                "Second attack sends two jumpable shockwaves");
        call(v, "hurt", new Class<?>[] {boolean.class}, true);
        check(
                number(boss, "hp") == 20
                        && number(v, "bossLives") == 2
                        && !(Boolean) field(boss, "active"),
                "A lost boss life preserves inflicted damage and consumes one of three attempts");
        check(
                (Boolean) field(v, "gateOpen") && number(v, "loreMask") == savedLore,
                "Death preserves relay and archive exploration progress");
        call(v, "previewBoss", new Class<?>[] {});
        set(h, "x", 167 * 36f);
        set(h, "inv", 99f);
        tick(v);
        check(
                !(Boolean) field(v, "won"),
                "The chapter cannot end while the guardian is undefeated");
        set(boss, "active", true);
        set(boss, "state", 3);
        call(v, "damageBoss", new Class<?>[] {int.class}, 100);
        check(
                (Boolean) field(boss, "defeated") && !(Boolean) field(boss, "active"),
                "The defeated guardian yields instead of disappearing");
        set(h, "x", 165 * 36f);
        set(h, "y", 425.99f);
        set(h, "vy", 0f);
        tick(v);
        check((Boolean) field(v, "won"), "The exit becomes available after the lion yields");
        call(v, "restart", new Class<?>[] {});
        Progression progression = (Progression) field(v, "progress");
        check(
                Campaign.COUNT >= LegacySaveIds.LEVELS.length
                        && new java.util.HashSet<String>(java.util.Arrays.asList(Campaign.TITLES))
                                        .size()
                                == Campaign.COUNT,
                "Campaign contains uniquely named chapters");
        java.util.Set<Integer> mapShapes = new java.util.HashSet<>(),
                themes = new java.util.HashSet<>();
        boolean chapterObjects = true;
        for (int chapter = 0; chapter < Campaign.COUNT; chapter++) {
            call(v, "loadLevel", new Class<?>[] {int.class}, chapter);
            boolean[][] layout = (boolean[][]) field(v, "wall");
            if (LegacySaveIds.index(Campaign.level(chapter).id) >= 0) {
                mapShapes.add(java.util.Arrays.deepHashCode(layout));
                themes.add(Campaign.THEMES[chapter]);
                chapterObjects &=
                        layout[13][160]
                                && ((List<?>) field(v, "springs")).size() > 0
                                && ((List<?>) field(v, "items")).size() > 0;
            }
            check(
                    ((List<?>) field(v, "relays")).size() == Campaign.RELAYS[chapter],
                    "Relay objectives match chapter " + chapter);
        }
        check(
                mapShapes.size() == LegacySaveIds.LEVELS.length,
                "Every chapter has distinct collision geometry");
        check(
                themes.size() == 7,
                "Campaign uses forest plus six independently illustrated environments");
        check(
                chapterObjects,
                "All chapters have a supported exit, routes, pickups and spring returns");
        call(v, "restart", new Class<?>[] {});
        progression = (Progression) field(v, "progress");
        check(!progression.heliUnlocked, "Helicopter ability starts story-locked");
        set(h, "ground", false);
        set(h, "vy", 700f);
        set(v, "fuel", 1f);
        press(v, KeyEvent.KEYCODE_H);
        call(v, "updateFlight", new Class<?>[] {float.class}, .25f);
        check(
                !(Boolean) field(v, "gliding") && number(v, "fuel") == 1,
                "Locked flight cannot consume energy or alter the jump");
        release(v, KeyEvent.KEYCODE_H);
        call(v, "previewBoss", new Class<?>[] {});
        boss = field(v, "boss");
        set(v, "loreMask", 0);
        set(boss, "active", true);
        set(boss, "state", 3);
        call(v, "damageBoss", new Class<?>[] {int.class}, 100);
        check(
                progression.lionAlly && progression.heliUnlocked,
                "Lion always becomes a good ally and gifts flight, even with zero archives");
        set(h, "ground", false);
        set(h, "vy", 700f);
        set(v, "fuel", 1f);
        press(v, KeyEvent.KEYCODE_H);
        call(v, "updateFlight", new Class<?>[] {float.class}, .25f);
        check(
                (Boolean) field(v, "gliding") && number(h, "vy") < 100 && number(v, "fuel") < 1,
                "Helicopter ears slow descent while consuming fuel");
        for (int i = 0; i < 10; i++) {
            call(v, "updateFlight", new Class<?>[] {float.class}, .25f);
        }
        check(
                number(v, "fuel") == 0 && !(Boolean) field(v, "gliding"),
                "Flight stops when the energy reserve is depleted");
        release(v, KeyEvent.KEYCODE_H);
        set(h, "ground", true);
        call(v, "updateFlight", new Class<?>[] {float.class}, .5f);
        check(number(v, "fuel") > 0, "Flight energy recharges on the ground");
        progression.points = 2;
        float originalFuel = progression.maxFuel();
        check(
                (Boolean) call(v, "buyUpgrade", new Class<?>[] {int.class}, 0)
                        && progression.ranks[0] == 1
                        && progression.points == 1,
                "A rank-one upgrade spends one skill point");
        check(
                !(Boolean) call(v, "buyUpgrade", new Class<?>[] {int.class}, 0)
                        && progression.points == 1,
                "Insufficient points cannot purchase the next rank");
        check(
                progression.maxFuel() > originalFuel,
                "Pilot upgrades increase helicopter flight duration");
        progression.points = 40;
        for (int skill = 0; skill < 5; skill++) {
            while (progression.ranks[skill] < 3) {
                call(v, "buyUpgrade", new Class<?>[] {int.class}, skill);
            }
        }
        check(!progression.buy(0) && progression.ranks[0] == 3, "Skill ranks stop at three");
        int beforeReward = progression.points;
        progression.complete(4);
        progression.complete(4);
        check(
                progression.points == beforeReward + 2,
                "A completed chapter cannot be farmed for repeated skill rewards");
        beforeReward = progression.points;
        progression.discover(7);
        progression.discover(7);
        check(
                progression.points == beforeReward + 1,
                "An Easter egg awards its discovery point only once");
        call(v, "loadLevel", new Class<?>[] {int.class}, 6);
        ((List<?>) field(v, "enemies")).clear();
        Object frostCache = null;
        for (Object item : (List<?>) field(v, "items")) {
            if (number(item, "kind") == 1 && number(item, "value") == 3) {
                frostCache = item;
                break;
            }
        }
        check(frostCache != null, "Crystal caves contain a concealed frost weapon cache");
        set(h, "x", number(frostCache, "x"));
        set(h, "y", number(frostCache, "y"));
        call(v, "updateItems", new Class<?>[] {});
        check(
                (progression.weaponMask & (1 << 3)) != 0 && number(v, "gun") == 3,
                "Finding the frost cache permanently unlocks and equips it");
        call(v, "loadLevel", new Class<?>[] {int.class}, 8);
        Object ricochetCache = null;
        for (Object item : (List<?>) field(v, "items")) {
            if (number(item, "kind") == 1 && number(item, "value") == 4) {
                ricochetCache = item;
                break;
            }
        }
        check(ricochetCache != null, "Desert ruins contain a concealed ricochet weapon cache");
        set(h, "x", number(ricochetCache, "x"));
        set(h, "y", number(ricochetCache, "y"));
        call(v, "updateItems", new Class<?>[] {});
        check(
                (progression.weaponMask & (1 << 4)) != 0,
                "Finding the ricochet cache permanently unlocks it");
        ((List<?>) field(v, "bullets")).clear();
        ((List<?>) field(v, "enemies")).clear();
        set(v, "gun", 4);
        set(h, "x", 133 * 36f - 60);
        set(h, "y", 300f);
        set(h, "face", 1);
        call(v, "shoot", new Class<?>[] {});
        call(v, "updateBullets", new Class<?>[] {float.class}, .05f);
        List<?> reflected = (List<?>) field(v, "bullets");
        check(
                !reflected.isEmpty()
                        && number(reflected.get(0), "vx") < 0
                        && number(reflected.get(0), "bounces") == 1,
                "Ricochet rounds reflect from solid gates");
        call(v, "loadLevel", new Class<?>[] {int.class}, 6);
        Object frozenEnemy = ((List<?>) field(v, "enemies")).get(0);
        ((List<?>) field(v, "bullets")).clear();
        set(v, "gun", 3);
        set(h, "x", number(frozenEnemy, "x") - 60);
        set(h, "y", number(frozenEnemy, "y") - 2);
        set(h, "face", 1);
        call(v, "shoot", new Class<?>[] {});
        call(v, "updateBullets", new Class<?>[] {float.class}, .05f);
        check(number(frozenEnemy, "slow") > 2, "Frost rounds apply a real enemy movement slow");
        call(v, "store", new Class<?>[] {});
        Progression loaded = Progression.read((android.content.SharedPreferences) field(v, "save"));
        check(
                loaded.currentLevel == 6
                        && loaded.ranks[0] == 3
                        && loaded.lionAlly
                        && loaded.heliUnlocked
                        && loaded.weaponMask == progression.weaponMask
                        && loaded.points == progression.points,
                "Campaign chapter, skills, weapons and redeemed Lion survive a save/reload");
        set(v, "lives", 1);
        call(v, "hurt", new Class<?>[] {boolean.class}, true);
        press(v, KeyEvent.KEYCODE_ENTER);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_ENTER);
        check(
                number(v, "levelIndex") == 6
                        && number(v, "lives") == 99
                        && ((Progression) field(v, "progress")).ranks[0] == 3,
                "Game over retries the current chapter without wiping earned upgrades");
        call(v, "loadLevel", new Class<?>[] {int.class}, 11);
        boss = field(v, "boss");
        check(
                number(boss, "type") == 4 && number(boss, "maxHp") > 48,
                "Final chapter contains the Registrar instead of another Lion fight");
        press(v, KeyEvent.KEYCODE_TAB);
        release(v, KeyEvent.KEYCODE_TAB);
        check((Boolean) field(v, "upgradeMenu"), "Tab opens the workshop");
        press(v, KeyEvent.KEYCODE_TAB);
        release(v, KeyEvent.KEYCODE_TAB);
        check(
                !(Boolean) field(v, "upgradeMenu"),
                "Tab closes the workshop without changing jump bindings");
        call(v, "restart", new Class<?>[] {});
        call(v, "loadLevel", new Class<?>[] {int.class}, 1);
        call(v, "previewBoss", new Class<?>[] {});
        call(v, "updateBoss", new Class<?>[] {float.class}, .016f);
        boss = field(v, "boss");
        ((List<?>) field(v, "enemies")).clear();
        android.graphics.RectF upper =
                (android.graphics.RectF) call(v, "bossHurtBox", new Class<?>[] {});
        check(
                upper.top < number(boss, "y") - 20,
                "Boss hurtbox includes the visible head and mane above the old body rectangle");
        int previousHp = (Integer) field(boss, "hp");
        set(v, "gun", 0);
        set(h, "face", 1);
        set(h, "x", number(boss, "x") + 48 - 41);
        set(h, "y", upper.top + 8 - (v.world.playerPose().muzzleY(v.world.hero) - v.world.hero.y));
        call(v, "shoot", new Class<?>[] {});
        call(v, "updateBullets", new Class<?>[] {float.class}, .016f);
        check(
                number(boss, "hp") < previousHp,
                "A real blaster projectile hitting the upper sprite deals damage");
        set(h, "x", 140 * 36f);
        set(h, "y", 425.99f);
        set(h, "inv", 0f);
        set(v, "health", 3);
        set(boss, "hp", 23);
        float beforeHitX = number(h, "x");
        int normalLives = (Integer) field(v, "lives");
        call(v, "hurt", new Class<?>[] {boolean.class}, false);
        check(
                number(v, "health") == 2
                        && number(h, "x") == beforeHitX
                        && number(v, "lives") == normalLives
                        && number(boss, "hp") == 23,
                "A normal hit causes health loss and knockback without teleporting or healing the"
                        + " boss");
        call(v, "hurt", new Class<?>[] {boolean.class}, false);
        check(
                number(v, "health") == 2,
                "Invulnerability prevents repeated contact damage during knockback");
        for (int expected = 2; expected >= 0; expected--) {
            set(v, "health", 1);
            set(h, "inv", 0f);
            call(v, "hurt", new Class<?>[] {boolean.class}, false);
            check(
                    number(v, "bossLives") == expected && number(boss, "hp") == 23,
                    "Boss attempt budget is exact and preserves damage at "
                            + expected
                            + " attempts");
            if (expected > 0) {
                call(v, "updateBoss", new Class<?>[] {float.class}, .016f);
            }
        }
        check(
                (Boolean) field(v, "gameOver") && number(v, "lives") == normalLives,
                "Three failed boss lives end the encounter even with 99 debug retries");
        press(v, KeyEvent.KEYCODE_ENTER);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_ENTER);
        check(
                number(v, "bossLives") == 3
                        && number(boss, "hp") == 48
                        && !(Boolean) field(v, "gameOver")
                        && (Boolean) field(v, "gateOpen"),
                "Retry after the third defeat explicitly starts a fresh boss fight without"
                        + " replaying relay puzzles");
        call(v, "loadLevel", new Class<?>[] {int.class}, 2);
        Progression friend = (Progression) field(v, "progress");
        friend.befriendLion();
        float post = ((Number) call(v, "allyStationX", new Class<?>[] {})).floatValue();
        set(h, "x", 2100f);
        check(
                ((Number) call(v, "allyStationX", new Class<?>[] {})).floatValue() == post,
                "Lion stays at an independent support post instead of following Nova's"
                        + " coordinates");
        set(v, "health", 1);
        set(v, "fuel", 0f);
        set(v, "allyCooldown", 0f);
        call(v, "updateAlly", new Class<?>[] {float.class}, .016f);
        check(
                number(v, "health") == 1 && number(v, "fuel") == 0,
                "Distant Lion does not give invisible automatic combat or healing");
        set(h, "x", post);
        call(v, "updateAlly", new Class<?>[] {float.class}, .016f);
        check(
                number(v, "health") == 2 && number(v, "fuel") > 0,
                "Returning to Lion's support post provides visible-purpose healing and recharge");
        call(v, "restart", new Class<?>[] {});
        press(v, KeyEvent.KEYCODE_2);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_2);
        check(number(v, "levelIndex") == 1, "Digit 2 replaces F2 for next chapter");
        press(v, KeyEvent.KEYCODE_NUMPAD_2);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_NUMPAD_2);
        check(number(v, "levelIndex") == 2, "Numpad digits work as well as the number row");
        press(v, KeyEvent.KEYCODE_3);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_3);
        check(
                number(v, "levelIndex") == 3 && number(field(v, "boss"), "type") == 1,
                "Digit 3 opens the current or next boss instead of always teleporting to Lion");
        press(v, KeyEvent.KEYCODE_4);
        release(v, KeyEvent.KEYCODE_4);
        int workshopPoints = ((Progression) field(v, "progress")).points;
        check((Boolean) field(v, "upgradeMenu"), "Digit 4 replaces F4 for the workshop");
        press(v, KeyEvent.KEYCODE_4);
        release(v, KeyEvent.KEYCODE_4);
        check(
                number(v, "selectedSkill") == 3
                        && ((Progression) field(v, "progress")).points == workshopPoints,
                "Inside the workshop digit 4 selects a skill and does not invoke debug rewards"
                        + " again");
        press(v, KeyEvent.KEYCODE_TAB);
        release(v, KeyEvent.KEYCODE_TAB);
        for (int[] screen :
                new int[][] {
                    {960, 720, 960},
                    {1920, 1080, 1280},
                    {2340, 1080, 1560},
                    {2400, 1080, 1600},
                    {2560, 1440, 1280}
                }) {
            check(
                    Math.abs(GameView.logicalWidthFor(screen[0], screen[1]) - screen[2]) < .1f,
                    "Viewport adapts to "
                            + screen[0]
                            + "x"
                            + screen[1]
                            + " without stretching the actors");
        }
        check(
                a.getApplicationInfo().icon != 0
                        && a.getPackageManager().getApplicationIcon(a.getApplicationInfo()) != null,
                "APK declares and loads the illustrated Nova launcher icon");
        for (int stage : new int[] {1, 3, 4, 5, 6, 7, 8, 10, 11}) {
            call(v, "loadLevel", new Class<?>[] {int.class}, stage);
            boolean[][] masonry = (boolean[][]) field(v, "wall"),
                    chambers = (boolean[][]) field(v, "indoors");
            check(
                    masonry[0][60] || chambers[0][60],
                    "Chapter " + stage + " has built architecture above its corridors");
            check(
                    chambers[22][31] && ((List<?>) field(v, "warps")).size() > 0,
                    "Chapter " + stage + " has a connected secret basement with a return passage");
            for (Object relay : (List<?>) field(v, "relays")) {
                int targetX = (int) (number(relay, "x") / 36),
                        targetY = (int) ((number(relay, "y") - 32) / 36);
                check(
                        roomReachable(
                                masonry, (boolean[][]) field(v, "ledge"), 19, 11, targetX, targetY),
                        "Room network connects the entrance to relay at "
                                + targetX
                                + " in chapter "
                                + stage);
            }
        }
        // Complete actual fights using the normal projectile collision path, not direct damage
        // calls.
        for (int stage : new int[] {1, 3, 5, 7, 11}) {
            call(v, "restart", new Class<?>[] {});
            call(v, "loadLevel", new Class<?>[] {int.class}, stage);
            call(v, "previewBoss", new Class<?>[] {});
            ((List<?>) field(v, "enemies")).clear();
            set(h, "inv", 999f);
            set(v, "gun", 0);
            press(v, KeyEvent.KEYCODE_Z);
            boss = field(v, "boss");
            for (int frame = 0; frame < 2400 && !(Boolean) field(boss, "defeated"); frame++) {
                set(h, "face", number(boss, "x") + 48 > number(h, "x") + 15 ? 1 : -1);
                tick(v);
            }
            release(v, KeyEvent.KEYCODE_Z);
            check(
                    (Boolean) field(boss, "defeated"),
                    "Boss in chapter "
                            + stage
                            + " can be defeated by ordinary continuous blaster fire");
        }
        for (int identity = 7; identity <= 12; identity++) {
            call(v, "restart", new Class<?>[] {});
            Object actor = null;
            for (Object candidate : (List<?>) field(v, "enemies")) {
                if (number(candidate, "kind") == identity) {
                    actor = candidate;
                    break;
                }
            }
            check(actor != null, "Ordinary enemy roster includes behavior " + identity);
            set(h, "x", number(actor, "x") - 100);
            set(h, "y", number(actor, "y"));
            set(h, "inv", 999f);
            if (identity == 7) {
                float old = number(actor, "x");
                call(v, "updateEnemies", new Class<?>[] {float.class}, .1f);
                check(number(actor, "x") != old, "Scrap beetles patrol instead of standing still");
            }
            if (identity == 8) {
                call(v, "updateEnemies", new Class<?>[] {float.class}, .016f);
                check(number(actor, "mode") == 1, "Shield sentry visibly prepares before charging");
                call(v, "updateEnemies", new Class<?>[] {float.class}, .55f);
                call(v, "updateEnemies", new Class<?>[] {float.class}, .1f);
                check(
                        number(actor, "mode") == 2 && Math.abs(number(actor, "vx")) > 100,
                        "Sentry preparation leads into a moving charge");
            }
            if (identity == 9 || identity == 11) {
                ((List<?>) field(v, "hostileShots")).clear();
                call(v, "updateEnemies", new Class<?>[] {float.class}, .016f);
                call(v, "updateEnemies", new Class<?>[] {float.class}, .6f);
                check(
                        ((List<?>) field(v, "hostileShots")).size() >= (identity == 11 ? 3 : 1),
                        identity == 9
                                ? "Firefly drones fire after their warning"
                                : "Rooted plants fire a warned seed fan");
            }
            if (identity == 10) {
                set(actor, "ground", true);
                call(v, "updateEnemies", new Class<?>[] {float.class}, .016f);
                check(number(actor, "vy") < -500, "Spring toads launch a real ballistic jump");
            }
            if (identity == 12) {
                check(
                        !(Boolean)
                                call(v, "enemyHittable", new Class<?>[] {actor.getClass()}, actor),
                        "Buried worms cannot invisibly damage or receive shots");
                call(v, "updateEnemies", new Class<?>[] {float.class}, .016f);
                call(v, "updateEnemies", new Class<?>[] {float.class}, .45f);
                check(
                        (Boolean)
                                call(v, "enemyHittable", new Class<?>[] {actor.getClass()}, actor),
                        "An emerged worm has an active visible hitbox");
            }
        }
        for (int type = 0; type < 5; type++) {
            BossBrain brain = new BossBrain();
            brain.reset(type, type == 0 ? 1 : type == 1 ? 3 : type == 2 ? 5 : type == 3 ? 7 : 11);
            brain.activate(140 * 36);
            float min = brain.x, max = brain.x;
            java.util.Set<Integer> states = new java.util.HashSet<>();
            for (int frame = 0; frame < 1800; frame++) {
                brain.step(1f / 60, 140 * 36 + (float) Math.sin(frame * .01f) * 100, 0);
                min = Math.min(min, brain.x);
                max = Math.max(max, brain.x);
                states.add(brain.state);
            }
            check(
                    max - min > 120
                            && states.contains(BossBrain.WINDUP)
                            && states.contains(BossBrain.RECOVER),
                    "Boss brain "
                            + type
                            + " moves, telegraphs attacks and recovers over a full combat cycle");
        }
        check(
                !GameView.isControllerSource(InputDevice.SOURCE_KEYBOARD),
                "Phone system keys must not hide touch controls as a false gamepad");
        check(
                !GameView.isControllerSource(InputDevice.SOURCE_TOUCHSCREEN)
                        && GameView.isControllerSource(
                                InputDevice.SOURCE_GAMEPAD | InputDevice.SOURCE_JOYSTICK),
                "Full input source masks distinguish touchscreens from real controllers");
        call(v, "restart", new Class<?>[] {});
        set(v, "showTouchControls", false);
        MotionEvent touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 60, 550, 0);
        v.onTouchEvent(touchEvent);
        touchEvent.recycle();
        check(
                (Boolean) field(v, "showTouchControls") && (Boolean) field(v, "left"),
                "A phone touch restores the touch overlay and activates the left control");
        v.clearInput();
        // The first tokens intersect Stu's visible head while his terrain hitbox passes below.
        for (boolean backwards : new boolean[] {false, true}) {
            call(v, "loadLevel", new Class<?>[] {int.class}, 0);
            ((List<?>) field(v, "enemies")).clear();
            List<?> tokens = (List<?>) field(v, "items");
            Object token = tokens.get(0);
            check(number(token, "kind") == 0, "Running regression targets a real token");
            tokens.subList(1, tokens.size()).clear();
            float tokenX = number(token, "x");
            set(h, "x", backwards ? tokenX + 85 : tokenX - 85);
            set(h, "y", 425.99f);
            set(h, "vx", 0f);
            set(h, "vy", 0f);
            set(h, "ground", true);
            set(token, "phase", backwards ? 1.57f : 4.71f);
            int earned = (int) number(v, "score");
            press(v, backwards ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
            for (int frame = 0; frame < 30 && !tokens.isEmpty(); frame++) {
                tick(v);
            }
            v.clearInput();
            check(
                    tokens.isEmpty() && number(v, "score") == earned + 100,
                    "Running collects visible token without jumping, direction=" + backwards);
        }
        // Touch every authored pickup in every chapter using the actual collision path.
        for (int stage = 0; stage < Campaign.COUNT; stage++) {
            call(v, "loadLevel", new Class<?>[] {int.class}, stage);
            List<?> pickups = (List<?>) field(v, "items");
            int authoredPickups = pickups.size();
            while (!pickups.isEmpty()) {
                Object pickup = pickups.get(0);
                set(h, "x", number(pickup, "x"));
                set(h, "y", number(pickup, "y"));
                int count = pickups.size();
                call(v, "updateItems", new Class<?>[] {});
                check(
                        pickups.size() < count,
                        "Pickup collision consumes the touched object in chapter " + stage);
                check(
                        number(v, "levelIndex") == stage && !(Boolean) field(v, "gameOver"),
                        "Pickup does not restart or fail chapter " + stage);
            }
            if (LegacySaveIds.index(Campaign.level(stage).id) >= 0)
                check(authoredPickups > 0, "Legacy chapter has collectible objects");
        }
        call(v, "restart", new Class<?>[] {});
        press(v, KeyEvent.KEYCODE_BUTTON_SELECT);
        release(v, KeyEvent.KEYCODE_BUTTON_SELECT);
        check((Boolean) field(v, "upgradeMenu"), "Anbernic Select opens upgrades");
        press(v, KeyEvent.KEYCODE_BUTTON_SELECT);
        release(v, KeyEvent.KEYCODE_BUTTON_SELECT);
        check(!(Boolean) field(v, "upgradeMenu"), "Anbernic Select closes upgrades");
        // Every mission target has an unobstructed body-sized connection to the entrance.
        java.util.Set<Integer> missionTypes = new java.util.HashSet<>();
        for (int stage = 2; stage < Campaign.COUNT; stage++) {
            call(v, "loadLevel", new Class<?>[] {int.class}, stage);
            ChapterMission task = (ChapterMission) field(v, "mission");
            boolean[][] structure = (boolean[][]) field(v, "wall"),
                    platforms = (boolean[][]) field(v, "ledge");
            for (ChapterMission.Node node : task.nodes) {
                missionTypes.add(node.mode);
                int column = (int) (node.x / 36), row = (int) ((node.y - 21) / 36);
                check(
                        roomReachable(
                                structure,
                                platforms,
                                (int) (Campaign.level(stage).spawnX / 36),
                                (int) (Campaign.level(stage).spawnY / 36),
                                column,
                                row),
                        "Body-sized route reaches chapter "
                                + stage
                                + " objective "
                                + node.mode
                                + " at "
                                + column
                                + ","
                                + row);
            }
            for (int point = 0; point < ((float[]) field(v, "checkpointX")).length; point++) {
                float px = ((float[]) field(v, "checkpointX"))[point],
                        py = ((float[]) field(v, "checkpointY"))[point] - 42 - .01f;
                check(
                        !(Boolean)
                                call(
                                        v,
                                        "blocked",
                                        new Class<?>[] {float.class, float.class},
                                        px,
                                        py),
                        "Chapter "
                                + stage
                                + " checkpoint "
                                + point
                                + " never restores Stu inside masonry");
            }
        }
        check(
                missionTypes.size() == 10,
                "Chapter objectives use ten different activation mechanics");
        ChapterMission flightTask = new ChapterMission(2);
        ChapterMission.Node ring = flightTask.nodes[0];
        flightTask.step(.1f, ring.x - 15, ring.y - 21, 0, false, false, 0);
        check(flightTask.mask == 0, "Walking or jumping through a flight ring is insufficient");
        flightTask.step(.1f, ring.x - 15, ring.y - 21, 0, true, false, 0);
        check(flightTask.lit(0), "Rotating ears activate an aerial ring");
        ChapterMission perchTask = new ChapterMission(3);
        ChapterMission.Node perch = perchTask.nodes[0];
        perchTask.step(.5f, perch.x - 15, perch.y - 21, 0, false, true, 0);
        check(!perchTask.lit(0), "Storm shelter needs a full stable landing");
        perchTask.step(.6f, perch.x - 15, perch.y - 21, 0, false, true, 0);
        check(perchTask.lit(0), "Resting in a storm shelter activates it");
        ChapterMission cargoTask = new ChapterMission(4);
        float cartStart = cargoTask.cartX;
        cargoTask.step(1, 0, 0, 0, false, true, 0);
        check(cargoTask.cartX == cartStart, "Cargo waits when Stu leaves it behind");
        cargoTask.step(1, cartStart - 15, 13 * 36 - 42, 0, false, true, 0);
        check(cargoTask.cartX > cartStart, "Cargo advances while Stu escorts it");
        ChapterMission towerTask = new ChapterMission(5);
        ChapterMission.Node wrongKey = towerTask.nodes[2];
        towerTask.step(.1f, wrongKey.x - 15, wrongKey.y - 21, 0, false, true, 0);
        check(towerTask.mask == 0, "Tower keys cannot be collected in the wrong order");
        for (ChapterMission.Node key : towerTask.nodes) {
            towerTask.step(.1f, key.x - 15, key.y - 21, 0, false, true, 0);
        }
        check(towerTask.done(), "Basement, right tower, then sky bridge completes the key route");
        ChapterMission crystals = new ChapterMission(6);
        for (ChapterMission.Node crystal : crystals.nodes) {
            crystals.step(.1f, crystal.x - 15, crystal.y - 21, 0, false, true, 0);
            check(!crystals.done(), "Touching crystals does not replace firing");
            check(
                    crystals.shoot(crystal.x, crystal.y, 10),
                    "Blaster hits resonate with each crystal");
        }
        check(crystals.done(), "All resonators open the geode exit");
        ChapterMission mirrors = new ChapterMission(7);
        ChapterMission.Node mirror = mirrors.nodes[0];
        mirrors.step(.1f, mirror.x - 15, mirror.y - 21, 0, false, true, 0);
        check(mirrors.mask == 0, "Walking into an archive room does not count as using its mirror");
        for (ChapterMission.Node entry : mirrors.nodes) {
            mirrors.arrive(entry.x - 15, entry.y - 21);
        }
        check(mirrors.done(), "Correct mirror arrivals solve the nonlinear archive route");
        ChapterMission sandTask = new ChapterMission(8);
        ChapterMission.Node obelisk = sandTask.nodes[0];
        sandTask.step(.1f, obelisk.x - 15, obelisk.y - 21, 0, false, true, 7);
        check(sandTask.mask == 0, "Sandstorm blocks obelisk activation");
        sandTask.step(.1f, obelisk.x - 15, obelisk.y - 21, 0, false, true, 2);
        check(sandTask.lit(0), "Obelisks activate during the calm window");
        ChapterMission ambush = new ChapterMission(9);
        ambush.captain(0);
        ambush.captain(1);
        check(!ambush.done(), "All three convoy captains are required");
        ambush.captain(2);
        check(ambush.done(), "Defeating the captains opens the Rook approach");
        ChapterMission iceTask = new ChapterMission(10);
        ChapterMission.Node speedGate = iceTask.nodes[0];
        iceTask.step(.1f, speedGate.x - 15, speedGate.y - 21, 100, false, true, 0);
        check(iceTask.mask == 0, "Ice gate rejects slow walking");
        iceTask.step(.1f, speedGate.x - 15, speedGate.y - 21, 400, false, true, 0);
        check(iceTask.lit(0), "Ice gate accepts a fast pass");
        ChapterMission citadel = new ChapterMission(11);
        ChapterMission.Node aerial = citadel.nodes[0],
                seal = citadel.nodes[1],
                console = citadel.nodes[2];
        citadel.step(.1f, aerial.x - 15, aerial.y - 21, 0, true, false, 0);
        citadel.shoot(seal.x, seal.y, 10);
        check(
                !citadel.done(),
                "Citadel rescue is still required after flight and combat objectives");
        citadel.step(1.1f, console.x - 15, console.y - 21, 0, false, true, 0);
        check(citadel.done(), "Citadel combines flight, ranged combat and rescuing prisoners");
        // Exercise mission conditions through actual physics, bullets, warp input and enemy defeat.
        call(v, "loadLevel", new Class<?>[] {int.class}, 2);
        ((List<?>) field(v, "enemies")).clear();
        ChapterMission flying = (ChapterMission) field(v, "mission");
        set(h, "x", flying.nodes[0].x - 15);
        set(h, "y", 24 * 36 - 42f);
        set(h, "vy", 0f);
        set(h, "ground", true);
        set(h, "inv", 99f);
        set(v, "fuel", 2f);
        press(v, KeyEvent.KEYCODE_BUTTON_L2);
        for (int frame = 0; frame < 180 && !flying.lit(0); frame++) {
            tick(v);
        }
        release(v, KeyEvent.KEYCODE_BUTTON_L2);
        check(flying.lit(0), "Real L2 flight through an updraft collects the aerial ring");
        check(
                ((Progression) field(v, "progress")).flightTrained,
                "First successful ear flight records the tutorial as learned");
        call(v, "loadLevel", new Class<?>[] {int.class}, 3);
        ((List<?>) field(v, "enemies")).clear();
        ChapterMission shelter = (ChapterMission) field(v, "mission");
        ChapterMission.Node landing = shelter.nodes[0];
        set(h, "x", landing.x - 15);
        set(h, "y", landing.y + 24 - 42 - .01f);
        set(h, "vx", 0f);
        set(h, "vy", 0f);
        set(h, "ground", true);
        set(h, "inv", 99f);
        v.clearInput();
        for (int frame = 0; frame < 65 && !shelter.lit(0); frame++) {
            tick(v);
        }
        check(
                shelter.lit(0),
                "Storm perch can be activated by a real grounded character without being lifted"
                        + " away");
        call(v, "loadLevel", new Class<?>[] {int.class}, 6);
        ((List<?>) field(v, "enemies")).clear();
        ChapterMission resonators = (ChapterMission) field(v, "mission");
        ChapterMission.Node resonance = resonators.nodes[0];
        set(h, "x", resonance.x - 50);
        set(h, "y", resonance.y - (v.world.playerPose().muzzleY(v.world.hero) - v.world.hero.y));
        set(h, "face", 1);
        set(v, "gun", 0);
        call(v, "shoot", new Class<?>[] {});
        call(v, "updateBullets", new Class<?>[] {float.class}, .025f);
        check(resonators.lit(0), "Actual blaster projectile lights a crystal resonator");
        call(v, "loadLevel", new Class<?>[] {int.class}, 7);
        ChapterMission mirrorRoute = (ChapterMission) field(v, "mission");
        List<?> doors = (List<?>) field(v, "warps");
        for (int door = 0; door < 3; door++) {
            Object passage = doors.get(door);
            set(h, "x", number(passage, "x") - 15);
            set(h, "y", number(passage, "y") - 21);
            press(v, KeyEvent.KEYCODE_DPAD_DOWN);
            call(v, "updateWorld", new Class<?>[] {float.class}, .016f);
            release(v, KeyEvent.KEYCODE_DPAD_DOWN);
            check(mirrorRoute.lit(door), "Actual Down input completes mirror transition " + door);
        }
        call(v, "updateMission", new Class<?>[] {float.class}, .016f);
        check(
                (Boolean) field(v, "gateOpen"),
                "Mirror sequence opens the exit through its normal world event path");
        call(v, "store", new Class<?>[] {});
        call(v, "buildLevel", new Class<?>[] {});
        call(v, "restoreSession", new Class<?>[] {});
        check(
                ((ChapterMission) field(v, "mission")).done() && (Boolean) field(v, "gateOpen"),
                "Saved mission objectives and open gate survive a session reload");
        call(v, "loadLevel", new Class<?>[] {int.class}, 9);
        java.util.ArrayList<Object> captains = new java.util.ArrayList<>();
        for (Object foe : (List<?>) field(v, "enemies")) {
            if (number(foe, "questIndex") >= 0) {
                captains.add(foe);
            }
        }
        check(captains.size() == 3, "Caravan spawns three real captain enemies");
        for (Object captain : captains) {
            call(v, "defeat", new Class<?>[] {captain.getClass()}, captain);
        }
        call(v, "updateMission", new Class<?>[] {float.class}, .016f);
        check(
                ((ChapterMission) field(v, "mission")).done() && (Boolean) field(v, "gateOpen"),
                "Defeating the actual captains unlocks the caravan arena");
        // Menu navigation uses the real input dispatcher and cannot enter a locked chapter.
        call(v, "restart", new Class<?>[] {});
        press(v, KeyEvent.KEYCODE_BUTTON_START);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_BUTTON_START);
        check(number(v, "menuPage") == 1, "Start opens the main menu");
        press(v, KeyEvent.KEYCODE_DPAD_DOWN);
        release(v, KeyEvent.KEYCODE_DPAD_DOWN);
        press(v, KeyEvent.KEYCODE_BUTTON_A);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_BUTTON_A);
        check(number(v, "menuPage") == 2, "Gamepad opens the chapter selector");
        press(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        release(v, KeyEvent.KEYCODE_DPAD_RIGHT);
        press(v, KeyEvent.KEYCODE_BUTTON_A);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_BUTTON_A);
        check(
                number(v, "levelIndex") == 0 && number(v, "menuPage") == 2,
                "Locked chapters cannot be loaded");
        ((Progression) field(v, "progress")).complete(0);
        press(v, KeyEvent.KEYCODE_BUTTON_A);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_BUTTON_A);
        check(
                number(v, "levelIndex") == 1 && number(v, "menuPage") == 0,
                "Completing a chapter unlocks the next selection");
        call(v, "openMenu", new Class<?>[] {});
        set(v, "menuSelection", 2);
        press(v, KeyEvent.KEYCODE_BUTTON_A);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_BUTTON_A);
        check((Boolean) field(v, "upgradeMenu"), "Main menu opens the workshop");
        press(v, KeyEvent.KEYCODE_BUTTON_SELECT);
        release(v, KeyEvent.KEYCODE_BUTTON_SELECT);
        check(
                number(v, "menuPage") == 1 && !(Boolean) field(v, "upgradeMenu"),
                "Select returns from the workshop to the menu");
        press(v, KeyEvent.KEYCODE_BUTTON_START);
        call(v, "applyPendingCommand", new Class<?>[] {});
        release(v, KeyEvent.KEYCODE_BUTTON_START);
        check(number(v, "menuPage") == 0, "Start resumes gameplay from the menu");
        // Resume preserves points and removes already collected items rather than awarding them
        // twice.
        call(v, "loadLevel", new Class<?>[] {int.class}, 0);
        Object savedItem = ((List<?>) field(v, "items")).get(0);
        set(h, "x", number(savedItem, "x"));
        set(h, "y", number(savedItem, "y"));
        call(v, "updateItems", new Class<?>[] {});
        java.util.Set<?> collected =
                new java.util.HashSet<>((java.util.Set<?>) field(v, "collectedItems"));
        int savedLevelScore = (int) number(v, "levelScore");
        set(v, "checkpoint", 1);
        set(v, "clock", 43f);
        set(v, "levelStartedAt", 10f);
        call(v, "store", new Class<?>[] {});
        call(v, "buildLevel", new Class<?>[] {});
        set(v, "checkpoint", 0);
        set(v, "levelScore", 0);
        call(v, "restoreSession", new Class<?>[] {});
        check(
                number(v, "checkpoint") == 1 && number(v, "levelScore") == savedLevelScore,
                "Session restores checkpoint and chapter score");
        check(
                Math.abs(number(h, "x") - ((float[]) field(v, "checkpointX"))[1]) < .01f,
                "Continue spawns at the saved checkpoint");
        check(
                Math.abs(number(v, "clock") - number(v, "levelStartedAt") - 33) < .01f,
                "Resume preserves elapsed chapter time");
        for (Object remaining : (List<?>) field(v, "items")) {
            check(
                    !collected.contains(
                            call(v, "itemKey", new Class<?>[] {remaining.getClass()}, remaining)),
                    "Saved pickups do not respawn after loading");
        }
        check(
                ((java.util.Set<?>) field(v, "collectedItems")).containsAll(collected),
                "Collected item IDs survive session restoration");
        // Per-chapter records are independent and survive recreating their storage model.
        android.content.SharedPreferences recordPrefs =
                getTargetContext().getSharedPreferences("neon_hare_record_checks", 0);
        recordPrefs.edit().clear().commit();
        LevelRecords scores = new LevelRecords(recordPrefs);
        scores.complete(0, 1200, 45.5f, 2);
        scores.complete(0, 900, 38.2f, 1);
        scores.complete(1, 700, 90f, 3);
        check(
                scores.bestScore[0] == 1200
                        && scores.bestMillis[0] == 38200
                        && scores.bestSecrets[0] == 2
                        && scores.clears[0] == 2,
                "Score, time and secrets retain independent personal bests");
        android.content.SharedPreferences.Editor recordEdit = recordPrefs.edit();
        scores.write(recordEdit);
        recordEdit.commit();
        LevelRecords restoredRecords = new LevelRecords(recordPrefs);
        check(
                restoredRecords.bestScore[0] == 1200
                        && restoredRecords.bestScore[1] == 700
                        && restoredRecords.clears[0] == 2,
                "Records survive recreation and remain separated by chapter");
        call(v, "loadLevel", new Class<?>[] {int.class}, 0);
        set(v, "levelScore", 500);
        set(v, "clock", 60f);
        set(v, "levelStartedAt", 0f);
        LevelRecords realRecords = (LevelRecords) field(v, "records");
        int previousClears = realRecords.clears[0];
        call(v, "finish", new Class<?>[] {});
        call(v, "finish", new Class<?>[] {});
        check(
                realRecords.clears[0] == previousClears + 1 && realRecords.bestScore[0] >= 24500,
                "Normal completion records the chapter bonus exactly once");
        call(v, "restart", new Class<?>[] {});
        check(number(v, "lives") == 99, "Restart restores debugging lives");
        v.resume();
    }
}
