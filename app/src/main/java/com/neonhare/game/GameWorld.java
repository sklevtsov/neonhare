package com.neonhare.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.InputDevice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class GameWorld {

    static final float FIXED_STEP_SECONDS = 1f / 60f;

    static final float TILE_SIZE = 36;

    static final float LOGICAL_HEIGHT = 720;

    static final float WORLD_OFFSET_Y = 70;

    volatile float viewWidth = 960;

    int widthInTiles = 168;

    int heightInTiles = 27;

    static final int BLASTER = 0;

    static final int PULSE = 1;

    static final int SCATTER = 2;

    static final int FROST = 3;

    static final int RICOCHET = 4;

    boolean[][] wall = new boolean[heightInTiles][widthInTiles];

    boolean[][] ledge = new boolean[heightInTiles][widthInTiles];

    final Player hero = new Player(110, 350);

    final List<Enemy> enemies = new ArrayList<>();

    final List<Bullet> bullets = new ArrayList<>();

    final List<Item> items = new ArrayList<>();

    final List<Spark> sparks = new ArrayList<>();

    final List<Spring> springs = new ArrayList<>();

    final List<Relay> relays = new ArrayList<>();

    final List<CacheWall> cacheWalls = new ArrayList<>();

    final List<Wave> waves = new ArrayList<>();

    final List<float[]> spikes = new ArrayList<>();

    final BossBrain boss = new BossBrain();

    int levelIndex;

    int loreMask;

    int secrets;

    boolean gateOpen;

    float camY;

    float messageTimer;

    float levelStartedAt;

    String messageTitle = "";

    String messageBody = "";

    final Random rng = new Random(12);

    Progression progress;

    LevelRecords records;

    ChapterMission mission;

    LevelDefinition definition;

    volatile UiScreen screen;

    volatile int menuSelection;

    volatile int chapterSelection;

    int levelScore;

    boolean upgradesFromMenu;

    int menuAxisX;

    int menuAxisY;

    final Set<String> collectedItems = new java.util.concurrent.ConcurrentSkipListSet<>();

    final List<float[]> vents = new ArrayList<>();

    final List<Shot> hostileShots = new ArrayList<>();

    boolean upgradeMenu;

    boolean heliHeld;

    boolean triggerHeli;

    boolean gliding;

    int selectedSkill;

    float fuel;

    float allyCooldown;

    final Set<Integer> seenVillains = new HashSet<>();

    int health = 3;

    int bossLives = 3;

    boolean encounterStarted;

    boolean bossEncounterFailed;

    boolean[][] indoors = new boolean[heightInTiles][widthInTiles];

    float[] checkpointX = new float[7];

    float[] checkpointY = new float[7];

    final List<Warp> warps = new ArrayList<>();

    boolean interactQueued;

    final SharedPreferences save;

    final SessionStore sessionStore;

    private boolean saveDirty;

    private long lastSaveNanos;

    volatile boolean left;

    volatile boolean right;

    volatile boolean jump;

    volatile boolean fire;

    volatile boolean jumpQueued;

    volatile boolean fireQueued;

    volatile boolean paused;

    volatile boolean won;

    volatile boolean gameOver;

    final ArrayDeque<GameCommand> actions = new ArrayDeque<>();

    final CommandMailbox<GameInputEvent> inputs = new CommandMailbox<>(512);

    int physicalWidth = 960, physicalHeight = 720;

    private Thread ownerThread;

    boolean showTouchControls = true;

    boolean triggerFire;

    float stick;

    float cam;

    float clock;

    float comboTimer;

    float shake;

    float fps;

    static final int START_LIVES = BuildConfig.DEBUG ? 99 : 5;

    int score;

    int best;

    int lives = START_LIVES;

    int combo = 1;

    int gun = BLASTER;

    float hatX;

    boolean hatJump;

    final Set<Integer> heldKeys = new java.util.concurrent.ConcurrentSkipListSet<>();

    int[] ammo = {999, 60, 30, 40, 50};

    int checkpoint;

    static final String[] LORE = {
        "Лев отвёл воду в убежище. В списках — дети всех кланов, даже врагов.",
        "Совет приказал осушить лес. Лев подписал приказ, но спрятал людей от Совета.",
        "Старый чертёж: обходные каналы сохранят и убежище, и живые корни леса.",
        "Лев отказался выдать раненых. За это Совет удерживает его семью у плотины.",
        "Прошлый взрыв Stu лишил квартал воды. Лев помнит то, о чём Stu молчит.",
        "Три обходных реле разгрузят узел. Уничтожать сердце водостока не требуется."
    };

    static class Warp {

        final float x, y, destX, destY;

        Warp(float x, float y, float destX, float destY) {
            this.x = x;
            this.y = y;
            this.destX = destX;
            this.destY = destY;
        }
    }

    static class Shot {

        float x, y, vx, vy, life = 4;

        Shot(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    static class Spring {

        final float x, y;

        Spring(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Relay {

        String id;

        final float x, y;

        boolean active;

        Relay(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    static class CacheWall {

        String id;

        final int x, y;

        boolean broken;

        CacheWall(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Wave {

        float x, y, vx, life = 3.5f;

        Wave(float x, float y, float vx) {
            this.x = x;
            this.y = y;
            this.vx = vx;
        }
    }

    static class Player {

        float x, y, vx, vy, inv, coyote, buffer, cool, stride, jumpFlash, jumpHold, springCool;
        int face = 1, jumps;

        boolean ground;

        Player(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Enemy {

        float x, y, vy, vx, phase, slow, attack, modeTime, homeY, homeX;
        int dir = -1, kind, hp, mode, questIndex = -1;

        boolean ground;

        boolean dead;

        Enemy(float x, float y, int k) {
            this.x = homeX = x;
            this.y = homeY = y;
            kind = k;
            hp =
                    k == 8
                            ? 4
                            : k == 7 || k == 10 || k == 11 || k == 12
                                    ? 2
                                    : k >= 3 ? 6 : k == 1 ? 3 : k == 2 ? 2 : 1;
        }
    }

    static class Bullet {

        float x, y, vx, vy, life = 1.35f, size;
        int damage, weapon, bounces;
        boolean obstructedAtSpawn;

        float left() {
            return x - size / 2;
        }

        float top() {
            return y - size / 2;
        }

        Bullet(float x, float y, float vx, float vy, float s, int d) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            size = s;
            damage = d;
        }
    }

    static class Item {

        String id;

        float x, y, phase;
        int kind, value;

        Item(float x, float y, int k, int v) {
            this.x = x;
            this.y = y;
            kind = k;
            value = v;
        }
    }

    static class Spark {

        float x, y, vx, vy, life = .65f, size;

        int color;

        Spark(float x, float y, float vx, float vy, int c, float s) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            color = c;
            size = s;
        }
    }

    static boolean isControllerSource(int sources) {
        return (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    static float logicalWidthFor(int width, int height) {
        return Math.max(960, 720f * Math.max(1, width) / Math.max(1, height));
    }

    int maximumHealth() {
        return 3 + (progress == null ? 0 : progress.ranks[2]);
    }

    boolean physicalAt(float x, float y) {
        return physics.physicalAt(x, y);
    }

    void stepEnemy(Enemy enemy, float dt) {
        enemySystem.stepEnemy(enemy, dt);
    }

    void enemyFire(Enemy enemy, int count) {
        enemySystem.enemyFire(enemy, count);
    }

    boolean enemyHittable(Enemy enemy) {
        return enemySystem.enemyHittable(enemy);
    }

    void updateEnemies(float dt) {
        enemySystem.updateEnemies(dt);
    }

    int maximumLives() {
        return BuildConfig.DEBUG ? 99 : 5 + (progress == null ? 0 : progress.ranks[2] * 2);
    }

    void updateFlight(float dt) {
        if (hero.ground) {
            fuel = Math.min(progress.maxFuel(), fuel + dt * (1.3f + progress.ranks[4] * .4f));
        }
        gliding = heliHeld && progress.heliUnlocked && !hero.ground && fuel > 0;
        if (gliding && !progress.flightTrained) {
            progress.flightTrained = true;
            store();
            announce(
                    "УШИ ВРАЩАЮТСЯ!",
                    "Удерживай L2 / H; приземлись для подзарядки. Потоки поднимают выше.",
                    6);
        }
        if (gliding) {
            fuel = Math.max(0, fuel - dt);
            hero.vy = approach(Math.min(hero.vy, 80), -55 - progress.ranks[4] * 18, 550 * dt);
            hero.jumpHold = 0;
        }
        for (float[] vent : vents) {
            if (hero.x + 15 > vent[0]
                    && hero.x + 15 < vent[1]
                    && hero.y > vent[2]
                    && hero.y < vent[3]) {
                hero.vy = Math.min(hero.vy, -510);
                fuel = Math.min(progress.maxFuel(), fuel + dt * .8f);
            }
        }
    }

    void updateEncounters(float dt) {
        enemySystem.updateEncounters(dt);
    }

    float allyStationX() {
        return definition.allyPostColumn * TILE_SIZE;
    }

    void updateAlly(float dt) {
        if (!progress.lionAlly || mission.nodesAreOptional() || boss.active) {
            return;
        }
        allyCooldown -= dt;
        float station = allyStationX();
        if (Math.abs(hero.x - station) < 80 && seenVillains.add(100 + levelIndex)) {
            announce(
                    "ЛЕВ · МАСТЕР ПОТОКА",
                    "Я держу обходной канал. Если нужен заряд и помощь — возвращайся к посту.",
                    5);
        }
        if (allyCooldown <= 0 && Math.abs(hero.x - station) < 120) {
            fuel = progress.maxFuel();
            health = Math.min(maximumHealth(), health + 1);
            allyCooldown = 12;
            burst(station, spawnFloor(station) - 35, Color.rgb(125, 235, 201), 8, 90);
        }
    }

    void toggleUpgrades() {
        upgradeMenu = !upgradeMenu;
        clearInput();
        if (upgradeMenu) {
            paused = false;
        } else if (upgradesFromMenu) {
            screen = UiScreen.MAIN_MENU;
            upgradesFromMenu = false;
        }
        store();
    }

    boolean buyUpgrade(int skill) {
        if (!progress.buy(skill)) {
            return false;
        }
        if (skill == 2) {
            health = Math.min(maximumHealth(), health + 1);
        }
        fuel = Math.min(progress.maxFuel(), fuel + .65f);
        lives = Math.min(maximumLives(), lives + 2);
        store();
        return true;
    }

    void setGate(boolean open) {
        gateOpen = open;
        for (int y = 0; y < heightInTiles; y++) {
            wall[y][definition.gateColumn] = !open;
            ledge[y][definition.gateColumn] = false;
        }
    }

    void announce(String title, String body, float time) {
        messageTitle = title;
        messageBody = body;
        messageTimer = time;
    }

    void loadLevel(int index) {
        diagnostic("LOAD_LEVEL target=" + index);
        levelIndex = Math.max(0, Math.min(Campaign.COUNT - 1, index));
        progress.currentLevel = levelIndex;
        if (Campaign.level(levelIndex).requiresFlight) {
            progress.befriendLion();
        }
        checkpoint = 0;
        levelScore = secrets = 0;
        screen = UiScreen.PLAY;
        cam = camY = 0;
        won = gameOver = paused = upgradeMenu = false;
        clearInput();
        fuel = progress.maxFuel();
        hero.x = 110;
        hero.y = 350;
        hero.vx = hero.vy = hero.coyote = hero.buffer = hero.jumpHold = hero.springCool = 0;
        hero.ground = false;
        hero.jumps = 0;
        hero.inv = 2;
        health = maximumHealth();
        upgradesFromMenu = false;
        buildLevel();
        store();
    }

    void previewBoss() {
        int chapter = levelIndex;
        while (chapter < Campaign.COUNT - 1 && Campaign.boss(chapter) < 0) {
            chapter++;
        }
        loadLevel(chapter);
        for (Relay relay : relays) {
            relay.active = true;
        }
        if (mission != null) {
            mission.mask = (1 << mission.nodes.length) - 1;
        }
        setGate(true);
        hero.x = 140 * TILE_SIZE;
        hero.y = 425.99f;
        hero.ground = true;
        hero.inv = 3;
        checkpoint = 6;
        cam = clamp(hero.x - viewWidth * .35f, 0, widthInTiles * TILE_SIZE - viewWidth);
        announce(
                "ОТЛАДКА · ЛЕВ АРДЕН",
                "Z — стреляй всегда. После промаха босса урон ×2. Рывок и волны перепрыгивай.",
                6);
    }

    void activateRelay(Relay relay) {
        if (relay.active) {
            return;
        }
        if (definition.missionKind == LevelDefinition.MissionKind.ORDERED_RELAYS) {
            int expected = 0;
            for (Relay r : relays) {
                if (r.active) {
                    expected++;
                }
            }
            if (relays.indexOf(relay) != expected) {
                announce(
                        "РЕЛЕ СРАБАТЫВАЮТ ПО ПОРЯДКУ",
                        "Сначала найди реле "
                                + (expected + 1)
                                + ". Верхний насос → нижний слив → клапан.",
                        4);
                return;
            }
        }
        diagnostic("RELAY x=" + relay.x + " y=" + relay.y);
        relay.active = true;
        add(600);
        burst(relay.x, relay.y, Color.CYAN, 24, 220);
        int count = 0;
        for (Relay r : relays) {
            if (r.active) {
                count++;
            }
        }
        if (count == relays.size()) {
            setGate(true);
            announce(
                    "ОБХОДНОЙ ПОТОК ВОССТАНОВЛЕН",
                    "Шлюз открыт. Вода идёт в убежище без осушения леса. Найди хранителя.",
                    6);
        } else {
            announce(
                    "ОБХОДНОЕ РЕЛЕ · " + count + " / " + relays.size(),
                    "Вода меняет путь. Остальные реле ищи на верхнем и нижнем маршрутах.",
                    4);
        }
    }

    void updateWorld(float dt) {
        for (Spring spring : springs) {
            if (hero.springCool <= 0
                    && hero.vy >= 0
                    && Math.abs(hero.x + 15 - spring.x) < 24
                    && Math.abs(hero.y + 42 - spring.y) < 10) {
                hero.y = spring.y - 42 - 24;
                hero.vy = -1360;
                hero.ground = false;
                hero.jumps = 1;
                hero.coyote = 0;
                hero.springCool = .45f;
                hero.jumpHold = .32f;
                burst(spring.x, spring.y, Color.rgb(114, 255, 186), 18, 240);
            }
        }
        for (Relay relay : relays) {
            if (!relay.active && hit(hero.x, hero.y, 30, 42, relay.x - 22, relay.y - 22, 44, 44)) {
                activateRelay(relay);
            }
        }
        if (interactQueued) {
            interactQueued = false;
            boolean usedWarp = false;
            for (Warp warp : warps) {
                if (Math.abs(hero.x + 15 - warp.x) < 55 && Math.abs(hero.y + 21 - warp.y) < 65) {
                    diagnostic("WARP targetX=" + warp.destX + " targetY=" + warp.destY);
                    usedWarp = true;
                    hero.x = warp.destX;
                    hero.y = warp.destY;
                    if (mission != null) {
                        mission.arrive(hero.x, hero.y);
                    }
                    hero.vx = hero.vy = 0;
                    hero.inv = 1;
                    cam = clamp(hero.x - viewWidth * .35f, 0, widthInTiles * TILE_SIZE - viewWidth);
                    camY = clamp(hero.y - 290, 0, heightInTiles * TILE_SIZE - 560);
                    announce(
                            mission.kind == LevelDefinition.MissionKind.MIRRORS
                                    ? "ЗЕРКАЛЬНЫЙ ПЕРЕХОД"
                                    : "СЛУЖЕБНЫЙ ОБХОД",
                            mission.kind == LevelDefinition.MissionKind.MIRRORS
                                    ? "Архив меняет порядок комнат. Ищи следующее зеркало."
                                    : "Старый канал выводит в уже знакомую комнату.",
                            3);
                    store();
                    break;
                }
            }
            if (!usedWarp && hero.ground && onLedge()) {
                hero.y += 7;
                hero.ground = false;
                hero.vy = 90;
            }
        }
        for (float[] spike : spikes) {
            if (hit(hero.x, hero.y, 30, 42, spike[0], spike[1] - 13, spike[2], 13)) {
                hurt(false, "spikes");
                break;
            }
        }
    }

    void beginBossWindup() {
        bossCombat.beginBossWindup();
    }

    void updateBoss(float dt) {
        bossCombat.updateBoss(dt);
    }

    void bossVolley() {
        bossCombat.bossVolley();
    }

    void damageBoss(int damage) {
        bossCombat.damageBoss(damage);
    }

    void tick(float dt) {
        assertOwner();
        clock += dt;
        messageTimer = Math.max(0, messageTimer - dt);
        hero.springCool = Math.max(0, hero.springCool - dt);
        float input = Math.abs(stick) > .18f ? stick : (right ? 1 : 0) - (left ? 1 : 0),
                target = input * 455;
        hero.vx =
                approach(
                        hero.vx,
                        target,
                        (hero.ground ? 2350 : 1350 * (1 + progress.ranks[0] * .15f)) * dt);
        if (input == 0 && hero.ground) {
            hero.vx = approach(hero.vx, 0, (Campaign.THEMES[levelIndex] == 6 ? 650 : 2900) * dt);
        }
        if (Math.abs(input) > .05f) {
            hero.face = input > 0 ? 1 : -1;
        }
        if (hero.ground) {
            hero.jumps = 0;
        }
        hero.coyote = hero.ground ? .13f : Math.max(0, hero.coyote - dt);
        if (jumpQueued) {
            hero.buffer = .14f;
            jumpQueued = false;
        } else {
            hero.buffer = Math.max(0, hero.buffer - dt);
        }
        if (hero.buffer > 0 && (hero.coyote > 0 || hero.jumps < 2)) {
            boolean air = hero.coyote <= 0;
            // Walking off a ledge leaves one air jump; every jump needs a fresh press.
            hero.jumps = air ? 2 : 1;
            hero.vy = air ? -840 - progress.ranks[0] * 20 : -920 - progress.ranks[0] * 25;
            hero.jumpHold = .10f;
            hero.ground = false;
            hero.buffer = hero.coyote = 0;
            hero.jumpFlash = air ? .22f : 0;
            burst(
                    hero.x + 15,
                    hero.y + 42,
                    air ? Color.rgb(137, 255, 100) : Color.CYAN,
                    air ? 16 : 7,
                    air ? 230 : 120);
        }
        hero.jumpFlash = Math.max(0, hero.jumpFlash - dt);
        hero.stride += Math.abs(hero.vx) * dt * .065f;
        hero.jumpHold = Math.max(0, hero.jumpHold - dt);
        if (!jump && hero.jumpHold <= 0 && hero.vy < -520) {
            hero.vy = -520;
        }
        hero.cool = Math.max(0, hero.cool - dt);
        if ((fireQueued || fire) && hero.cool <= 0) {
            shoot();
        }
        fireQueued = false;
        hero.vy = Math.min(hero.vy + 1650 * dt, 900);
        updateFlight(dt);
        if (definition.weather == LevelDefinition.Weather.GUSTS
                && !hero.ground
                && hero.x > 20 * TILE_SIZE
                && hero.x < 132 * TILE_SIZE) {
            hero.vx += (float) Math.sin((clock - levelStartedAt) * 1.7f) * 180 * dt;
        }
        if (definition.weather == LevelDefinition.Weather.SAND
                && mission != null
                && !mission.calm(clock - levelStartedAt)
                && !hero.ground
                && !physicalAt(hero.x + 15, hero.y - 36)) {
            hero.vx += 150 * dt;
        }
        move(hero.vx * dt, hero.vy * dt);
        if (hero.y > heightInTiles * TILE_SIZE + 90) {
            hurt(true);
        }
        updateBullets(dt);
        updateEnemies(dt);
        updateItems();
        updateWorld(dt);
        updateMission(dt);
        updateBoss(dt);
        updateEncounters(dt);
        updateAlly(dt);
        updateSparks(dt);
        if (comboTimer > 0) {
            comboTimer -= dt;
        } else {
            combo = 1;
        }
        if (hero.x > definition.exitColumn * TILE_SIZE
                && (Campaign.boss(levelIndex) < 0 || boss.defeated)) {
            finish();
        }
        for (int i = checkpoint + 1; i < checkpointX.length; i++) {
            if (hero.ground
                    && Math.abs(hero.x - checkpointX[i]) < 40
                    && Math.abs(hero.y + 42 - checkpointY[i]) < 12) {
                checkpoint = i;
                store();
            }
        }
        cam +=
                (clamp(hero.x - viewWidth * .35f, 0, widthInTiles * TILE_SIZE - viewWidth) - cam)
                        * .1f;
        camY += (clamp(hero.y - 290, 0, heightInTiles * TILE_SIZE - 560) - camY) * .12f;
        hero.inv = Math.max(0, hero.inv - dt);
        shake = Math.max(0, shake - 35 * dt);
    }

    PlayerPose playerPose() {
        return PlayerPose.current(this, assets);
    }

    void shoot() {
        weapons.shoot();
    }

    void updateBullets(float dt) {
        weapons.updateBullets(dt);
    }

    boolean touchesItem(Item item) {
        // Pickups follow the visible body (including head/shoulders), not the narrower terrain
        // collider.
        float bob = item.y + (float) Math.sin(item.phase) * 3;
        if (item.kind == 0) {
            return hit(hero.x - 13, hero.y - 28, 56, 70, item.x + 2, bob - 4, 21, 33);
        }
        return hit(hero.x - 9, hero.y - 20, 48, 62, item.x - 4, bob - 6, 33, 40);
    }

    void updateItems() {
        Iterator<Item> it = items.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            item.phase += .06f;
            if (!touchesItem(item)) {
                continue;
            }
            diagnostic(
                    "PICKUP kind="
                            + item.kind
                            + " value="
                            + item.value
                            + " itemX="
                            + item.x
                            + " itemY="
                            + item.y);
            if (item.kind == 0) {
                add(100);
            } else if (item.kind == 1) {
                gun = item.value;
                progress.weaponMask |= 1 << gun;
                ammo[gun] += gun == PULSE ? 80 : 35;
                add(500);
            } else if (item.kind == 2) {
                health = maximumHealth();
                lives = Math.min(maximumLives(), lives + 1);
                add(250);
            } else if (item.kind == 3) {
                loreMask |= 1 << item.value;
                add(750);
                announce("АРХИВ · " + (item.value + 1), LORE[item.value], 7);
            } else if (item.kind == 4 && !progress.hasTraining(item.value)) {
                progress.train(item.value);
                announce(
                        "МОДУЛЬ РАЗВИТИЯ",
                        "+1 очко. "
                                + (BuildConfig.DEBUG ? "Tab / Select" : "Select")
                                + " — прокачка.",
                        5);
            } else if (item.kind == 5 && progress.discover(item.value)) {
                add(750 + progress.ranks[3] * 150);
                announce(
                        "ПАСХАЛКА · " + Campaign.EGGS[item.value],
                        Campaign.EGG_NOTES[item.value],
                        6);
            }
            burst(item.x + 12, item.y + 12, item.kind == 1 ? Color.CYAN : Color.YELLOW, 12, 210);
            collectedItems.add(itemKey(item));
            it.remove();
            store();
        }
    }

    void updateSparks(float dt) {
        for (Spark s : sparks) {
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vy += 420 * dt;
            s.life -= dt;
        }
        for (Iterator<Spark> it = sparks.iterator(); it.hasNext(); ) {
            if (it.next().life <= 0) {
                it.remove();
            }
        }
    }

    void defeat(Enemy e) {
        enemySystem.defeat(e);
    }

    void add(int v) {
        score += v;
        levelScore += v;
        if (score > best) {
            best = score;
        }
    }

    void finish() {
        if (won) {
            return;
        }
        diagnostic("LEVEL_COMPLETE");
        won = true;
        upgradeMenu = false;
        clearInput();
        progress.complete(levelIndex);
        add(Math.max(0, 30000 - (int) ((clock - levelStartedAt) * 100)));
        records.complete(levelIndex, levelScore, clock - levelStartedAt, secrets);
        store();
    }

    void move(float dx, float dy) {
        physics.move(dx, dy);
    }

    void hurt(boolean fall) {
        hurt(fall, fall ? "fall" : "collision");
    }

    void hurt(boolean fall, String source) {
        if (!fall && hero.inv > 0) {
            return;
        }
        diagnostic("HURT reason=" + source);
        if (!fall) {
            health--;
            hero.inv = 1.25f + progress.ranks[2] * .2f;
            hero.vx = -hero.face * 260;
            hero.vy = -300;
            hero.ground = false;
            shake = 8;
            combo = 1;
            if (health > 0) {
                return;
            }
        }
        if (encounterStarted && !boss.defeated) {
            bossLives--;
            health = maximumHealth();
            waves.clear();
            hostileShots.clear();
            bullets.clear();
            if (bossLives <= 0) {
                boss.active = false;
                gameOver = true;
                bossEncounterFailed = true;
                store();
                return;
            }
            boss.retryPosition();
            hero.x = 140 * TILE_SIZE;
            hero.y = 13 * TILE_SIZE - 42 - .01f;
            hero.vx = hero.vy = 0;
            hero.ground = true;
            hero.inv = 2;
            hero.jumps = 0;
            cam = clamp(hero.x - viewWidth * .35f, 0, widthInTiles * TILE_SIZE - viewWidth);
            camY = 0;
            announce(
                    "ПОПЫТОК ОСТАЛОСЬ: " + bossLives,
                    "Здоровье босса сохранено. Продолжай бой.",
                    3);
            store();
            return;
        }
        lives--;
        health = maximumHealth();
        combo = 1;
        shake = 11;
        if (lives <= 0) {
            gameOver = true;
            store();
            return;
        }
        if (Campaign.boss(levelIndex) >= 0 && !boss.defeated) {
            boss.reset(Campaign.boss(levelIndex), levelIndex);
            waves.clear();
            hostileShots.clear();
        }
        diagnostic("RESPAWN checkpoint=" + checkpoint);
        announce("ВОЗВРАТ К КОНТРОЛЬНОЙ ТОЧКЕ", "Потеряна жизнь. Осталось: " + lives, 3);
        hero.x = checkpointSpawnX();
        hero.y = checkpointY[checkpoint] - 42 - .01f;
        camY = clamp(hero.y - 290, 0, heightInTiles * TILE_SIZE - 560);
        hero.vx = hero.vy = 0;
        hero.ground = true;
        hero.jumps = 0;
        hero.coyote = hero.buffer = hero.jumpHold = 0;
        hero.inv = 2.5f + progress.ranks[2] * .3f;
        cam = clamp(hero.x - viewWidth * .35f, 0, widthInTiles * TILE_SIZE - viewWidth);
        store();
    }

    float checkpointSpawnX() {
        return checkpointX[checkpoint];
    }

    float spawnFloor(float x) {
        return physics.spawnFloor(x);
    }

    boolean onLedge() {
        return physics.onLedge();
    }

    boolean blocked(float x, float y) {
        return physics.blocked(x, y);
    }

    boolean solid(float x, float y) {
        return physics.solid(x, y);
    }

    void burst(float x, float y, int color, int n, float speed) {
        if (sparks.size() > 280) {
            return;
        }
        for (int i = 0; i < n; i++) {
            float a = rng.nextFloat() * 6.283f, v = speed * (.35f + rng.nextFloat() * .65f);
            sparks.add(
                    new Spark(
                            x,
                            y,
                            (float) Math.cos(a) * v,
                            (float) Math.sin(a) * v,
                            color,
                            2 + rng.nextFloat() * 4));
        }
    }

    int gunColor() {
        return weapons.gunColor();
    }

    int weaponColor(int weapon) {
        return weapons.weaponColor(weapon);
    }

    String gunName() {
        return weapons.gunName();
    }

    void nextGun() {
        weapons.nextGun();
    }

    public void clearInput() {
        heldKeys.clear();
        left =
                right =
                        jump =
                                fire =
                                        jumpQueued =
                                                fireQueued =
                                                        hatJump =
                                                                triggerFire =
                                                                        heliHeld =
                                                                                triggerHeli = false;
        gliding = false;
        stick = hatX = 0;
        interactQueued = false;
    }

    void openMenu() {
        clearInput();
        paused = false;
        upgradeMenu = false;
        menuSelection = 0;
        screen = UiScreen.MAIN_MENU;
        store();
        diagnostic("MENU_OPEN");
    }

    void continueGame() {
        if (gameOver) {
            if (bossEncounterFailed) {
                previewBoss();
            } else {
                lives = maximumLives();
                loadLevel(levelIndex);
            }
        } else if (won && levelIndex < Campaign.COUNT - 1) {
            loadLevel(levelIndex + 1);
        } else if (won) {
            screen = UiScreen.CHAPTER_SELECT;
            chapterSelection = levelIndex;
            clearInput();
            return;
        }
        screen = UiScreen.PLAY;
        paused = false;
        clearInput();
    }

    String itemKey(Item item) {
        return item.id;
    }

    void updateMission(float dt) {
        if (mission == null || mission.nodesAreOptional()) {
            return;
        }
        int previous = mission.mask;
        mission.step(dt, hero.x, hero.y, hero.vx, gliding, hero.ground, clock - levelStartedAt);
        if (previous != mission.mask) {
            add(1000);
            announce(
                    ChapterMission.NAMES[levelIndex],
                    "Выполнено: " + mission.count() + " / " + mission.nodes.length,
                    3);
            store();
        }
        if (mission.done() && !gateOpen) {
            setGate(true);
            announce(
                    "МАРШРУТ ОТКРЫТ",
                    Campaign.boss(levelIndex) >= 0
                            ? "Задача выполнена. Хранитель ждёт в правом крыле."
                            : "Задача выполнена. Ищи выход справа.",
                    5);
            store();
        }
    }

    void restart() {
        diagnostic("NEW_CAMPAIGN");
        hero.x = 110;
        hero.y = 350;
        hero.vx =
                hero.vy = hero.inv = hero.coyote = hero.buffer = hero.jumpFlash = hero.jumpHold = 0;
        progress = new Progression();
        health = maximumHealth();
        fuel = progress.maxFuel();
        upgradeMenu = false;
        hero.ground = false;
        hero.jumps = 0;
        clearInput();
        score = 0;
        levelIndex = loreMask = secrets = levelScore = 0;
        screen = UiScreen.PLAY;
        cam = camY = 0;
        lives = START_LIVES;
        combo = 1;
        checkpoint = 0;
        clock = 0;
        won = gameOver = false;
        ammo = new int[] {999, 60, 30, 40, 50};
        gun = 0;
        buildLevel();
        store();
    }

    static boolean hit(
            float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    static float approach(float v, float t, float a) {
        return v < t ? Math.min(v + a, t) : Math.max(v - a, t);
    }

    void diagnostic(String event) {
        GameDiagnostics.event(
                event
                        + " chapter="
                        + (levelIndex + 1)
                        + " x="
                        + Math.round(hero.x)
                        + " y="
                        + Math.round(hero.y)
                        + " hp="
                        + health
                        + " lives="
                        + lives
                        + " checkpoint="
                        + checkpoint
                        + " bossLives="
                        + bossLives);
    }

    void buildLevel() {
        definition = Campaign.level(levelIndex);
        widthInTiles = definition.width;
        heightInTiles = definition.height;
        wall = new boolean[heightInTiles][widthInTiles];
        ledge = new boolean[heightInTiles][widthInTiles];
        indoors = new boolean[heightInTiles][widthInTiles];
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                char tile = definition.tiles.get(y).charAt(x);
                wall[y][x] = tile != '.';
                ledge[y][x] = tile == '=';
                indoors[y][x] = definition.interiors.get(y).charAt(x) == 'i';
            }
        }
        checkpointX = new float[definition.checkpoints.size()];
        checkpointY = new float[checkpointX.length];
        for (int i = 0; i < checkpointX.length; i++) {
            checkpointX[i] = definition.checkpoints.get(i).x;
            checkpointY[i] = definition.checkpoints.get(i).y;
        }
        collectedItems.clear();
        enemies.clear();
        bullets.clear();
        items.clear();
        sparks.clear();
        springs.clear();
        relays.clear();
        cacheWalls.clear();
        waves.clear();
        spikes.clear();
        vents.clear();
        hostileShots.clear();
        warps.clear();
        seenVillains.clear();
        bossLives = 3;
        encounterStarted = bossEncounterFailed = false;
        boss.reset(definition.boss, levelIndex);
        allyCooldown = 3;
        gliding = false;
        levelStartedAt = clock;
        mission = new ChapterMission(levelIndex);
        for (LevelDefinition.ObjectSpec object : definition.objects) {
            switch (object.group) {
                case "items":
                    if (object.type == 3 && (loreMask & (1 << object.value)) != 0) {
                        break;
                    }
                    if (object.type == 4 && progress.hasTraining(levelIndex)) {
                        break;
                    }
                    if (object.type == 5 && progress.hasEgg(levelIndex)) {
                        break;
                    }
                    Item item =
                            new Item(
                                    object.x,
                                    object.y,
                                    object.type,
                                    object.type == 4 || object.type == 5
                                            ? levelIndex
                                            : object.value);
                    item.id = object.id;
                    items.add(item);
                    break;
                case "enemies":
                    Enemy enemy = new Enemy(object.x, object.y, object.type);
                    enemy.hp = object.hp;
                    enemy.questIndex = object.objective;
                    enemies.add(enemy);
                    break;
                case "springs":
                    springs.add(new Spring(object.x, object.y));
                    break;
                case "relays":
                    Relay relay = new Relay(object.x, object.y);
                    relay.id = object.id;
                    relays.add(relay);
                    break;
                case "cacheWalls":
                    CacheWall cache = new CacheWall((int) object.x, (int) object.y);
                    cache.id = object.id;
                    cacheWalls.add(cache);
                    break;
                case "warps":
                    warps.add(new Warp(object.x, object.y, object.a, object.b));
                    break;
                case "spikes":
                    spikes.add(new float[] {object.x, object.y, object.a});
                    break;
                case "vents":
                    vents.add(new float[] {object.x, object.a, object.y, object.b});
                    break;
                default:
                    throw new IllegalStateException(
                            "Unknown validated object group " + object.group);
            }
        }
        setGate(definition.gateInitiallyOpen);
        hero.x = definition.spawnX;
        hero.y = definition.spawnY;
        hero.vx = hero.vy = 0;
        announce(definition.missionName, definition.help, 9);
    }

    private final GameAssets assets;

    private final Runnable exitCallback;

    final GameControls controls;

    GameWorld(Context c, boolean testProfile, GameAssets assets, Runnable exitCallback) {
        this.assets = assets;
        this.exitCallback = exitCallback;
        controls = new GameControls(this);
        sessionStore = new SessionStore(c, testProfile);
        save = sessionStore.save;
        progress = Progression.read(save);
        levelIndex = progress.currentLevel;
        loreMask = save.getInt("campaign.lore", 0);
        fuel = progress.maxFuel();
        health = maximumHealth();
        lives = maximumLives();
        best = save.getInt("high_score", 0);
        score = save.getInt("last_score", 0);
        records = new LevelRecords(save);
        buildLevel();
        restoreSession();
        chapterSelection = levelIndex;
        screen = testProfile ? UiScreen.PLAY : UiScreen.MAIN_MENU;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && isControllerSource(device.getSources())) {
                showTouchControls = false;
                break;
            }
        }
    }

    RectF bossHurtBox() {
        return assets.bossHurtBox(boss);
    }

    void enqueue(GameInputEvent event) {
        if (!inputs.offer(event)) {
            for (GameInputEvent discarded : inputs.clear()) {
                discarded.release();
            }
            inputs.offer(GameInputEvent.clear());
            if (!inputs.offer(event)) {
                event.release();
            }
            GameDiagnostics.event("INPUT_OVERFLOW reset held controls");
        }
    }

    void processInput(boolean applyActions) {
        assertOwner();
        GameInputEvent event;
        while ((event = inputs.poll()) != null) {
            try {
                event.apply(this);
                if (applyActions) {
                    applyPendingCommand();
                }
            } finally {
                event.release();
            }
        }
    }

    synchronized void claimOwnership() {
        if (ownerThread != null && ownerThread != Thread.currentThread()) {
            throw new IllegalStateException("World already has an owner");
        }
        ownerThread = Thread.currentThread();
    }

    synchronized void releaseOwnership() {
        if (ownerThread != Thread.currentThread()) {
            throw new IllegalStateException("Not the world owner");
        }
        ownerThread = null;
    }

    void assertOwner() {
        if (ownerThread != null && ownerThread != Thread.currentThread()) {
            throw new IllegalStateException("Gameplay mutation outside owner thread");
        }
    }

    void queueAction(GameCommand.Action action) {
        actions.addLast(new GameCommand(action));
    }

    void queueChapter(int chapter) {
        actions.addLast(new GameCommand(GameCommand.Action.LOAD_CHAPTER, chapter));
    }

    void store() {
        assertOwner();
        saveDirty = true;
    }

    void restoreSession() {
        sessionStore.restore(this);
    }

    boolean flushSave(boolean force) {
        assertOwner();
        if (saveDirty && (force || System.nanoTime() - lastSaveNanos >= 500_000_000L)) {
            lastSaveNanos = System.nanoTime();
            try {
                sessionStore.persist(this, force);
                saveDirty = false;
            } catch (RuntimeException error) {
                GameDiagnostics.event("SAVE_FAILED " + error.getClass().getSimpleName());
                announce(
                        "НЕ УДАЛОСЬ СОХРАНИТЬ",
                        "Проверь свободное место. Прогресс остаётся в текущей игре.",
                        5);
                return false;
            }
        }
        return true;
    }

    void applyPendingCommand() {
        GameCommand command;
        while ((command = actions.pollFirst()) != null) {
            diagnostic("COMMAND action=" + command.action);
            switch (command.action) {
                case NEW_CAMPAIGN:
                    restart();
                    break;
                case NEXT_CHAPTER:
                    loadLevel(levelIndex + 1);
                    break;
                case DEBUG_NEXT_CHAPTER:
                    loadLevel((levelIndex + 1) % Campaign.COUNT);
                    break;
                case PREVIEW_BOSS:
                    previewBoss();
                    break;
                case RETRY:
                    if (bossEncounterFailed) {
                        previewBoss();
                    } else {
                        lives = maximumLives();
                        loadLevel(levelIndex);
                    }
                    break;
                case DEBUG_PREVIOUS_CHAPTER:
                    loadLevel((levelIndex + Campaign.COUNT - 1) % Campaign.COUNT);
                    break;
                case OPEN_MENU:
                    openMenu();
                    break;
                case CONTINUE:
                    continueGame();
                    break;
                case CHAPTER_SELECT:
                    chapterSelection = levelIndex;
                    screen = UiScreen.CHAPTER_SELECT;
                    clearInput();
                    break;
                case UPGRADES:
                    upgradesFromMenu = true;
                    screen = UiScreen.PLAY;
                    upgradeMenu = true;
                    clearInput();
                    break;
                case SAVE_AND_EXIT:
                    store();
                    if (flushSave(true)) {
                        exitCallback.run();
                    }
                    break;
                case LOAD_CHAPTER:
                    if (command.chapter >= 0
                            && command.chapter < Campaign.COUNT
                            && progress.isUnlocked(command.chapter)) {
                        lives = maximumLives();
                        loadLevel(command.chapter);
                    }
                    break;
            }
        }
    }

    private final PhysicsSystem physics = new PhysicsSystem(this);

    private final EnemySystem enemySystem = new EnemySystem(this);

    private final WeaponSystem weapons = new WeaponSystem(this);

    private final BossCombat bossCombat = new BossCombat(this);
}
