package com.neonhare.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.SurfaceHolder;

import com.neonhare.game.GameWorld.Bullet;
import com.neonhare.game.GameWorld.CacheWall;
import com.neonhare.game.GameWorld.Enemy;
import com.neonhare.game.GameWorld.Item;
import com.neonhare.game.GameWorld.Relay;
import com.neonhare.game.GameWorld.Shot;
import com.neonhare.game.GameWorld.Spark;
import com.neonhare.game.GameWorld.Spring;
import com.neonhare.game.GameWorld.Warp;
import com.neonhare.game.GameWorld.Wave;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

final class GameRenderer {

    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    final Paint font = new Paint(Paint.ANTI_ALIAS_FLAG);

    final Rect tileSource = new Rect();

    final RectF tileTarget = new RectF();

    final Paint backgroundPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    final Paint terrainPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    final Paint outlinePaint = new Paint();

    final Map<Bitmap, Bitmap> silhouettes = new IdentityHashMap<>();

    void drawInterior(Canvas c) {
        int first = Math.max(0, (int) (world.cam / world.TILE_SIZE) - 1),
                last =
                        Math.min(
                                world.widthInTiles - 1,
                                (int) ((world.cam + world.viewWidth) / world.TILE_SIZE) + 1);
        for (int row = 0; row < world.heightInTiles; row++) {
            for (int x = first; x <= last; ) {
                if (!world.indoors[row][x]) {
                    x++;
                    continue;
                }
                int end = x;
                while (end < last && world.indoors[row][end + 1]) {
                    end++;
                }
                paint.setColor(Color.argb(240, 12, 21, 34));
                c.drawRect(
                        x * world.TILE_SIZE,
                        row * world.TILE_SIZE,
                        (end + 1) * world.TILE_SIZE,
                        (row + 1) * world.TILE_SIZE,
                        paint);
                x = end + 1;
            }
        }
        for (Warp warp : world.warps) {
            paint.setColor(Color.argb(70, 92, 219, 215));
            c.drawOval(warp.x - 22, warp.y - 29, warp.x + 22, warp.y + 29, paint);
            paint.setColor(Color.rgb(96, 202, 193));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            c.drawOval(warp.x - 22, warp.y - 29, warp.x + 22, warp.y + 29, paint);
            paint.setStyle(Paint.Style.FILL);
            if (Math.abs(world.hero.x - warp.x) < 100) {
                hudText(c, "E / ↓ · ОБХОД", warp.x - 42, warp.y - 40, 11, Color.rgb(170, 230, 221));
            }
        }
        if (LevelArchitecture.style(world.levelIndex) != LevelArchitecture.OPEN) {
            paint.setColor(Color.rgb(65, 54, 43));
            c.drawRoundRect(
                    28 * world.TILE_SIZE,
                    18 * world.TILE_SIZE + 4,
                    33 * world.TILE_SIZE,
                    18 * world.TILE_SIZE + 33,
                    3,
                    3,
                    paint);
            hudText(
                    c,
                    "RABBITS STINK",
                    28 * world.TILE_SIZE + 9,
                    18 * world.TILE_SIZE + 24,
                    12,
                    Color.rgb(190, 183, 148));
        }
    }

    void drawReadable(Canvas c, Bitmap sprite, float x, float y) {
        Bitmap alpha = silhouettes.get(sprite);
        if (alpha == null) {
            alpha = sprite.extractAlpha();
            silhouettes.put(sprite, alpha);
        }
        outlinePaint.setAlpha(235);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox != 0 || oy != 0) {
                    c.drawBitmap(alpha, x + ox * 1.4f, y + oy * 1.4f, outlinePaint);
                }
            }
        }
        paint.setColor(Color.WHITE);
        paint.setAlpha(world.hero.inv > 0 && ((int) (world.hero.inv * 15) & 1) == 0 ? 175 : 255);
        c.drawBitmap(sprite, x, y, paint);
        paint.setAlpha(255);
    }

    void drawDevice(Canvas c, int index, float x, float bottom, float scale) {
        c.save();
        c.translate(x, bottom);
        c.scale(scale, scale);
        paint.setColor(Color.WHITE);
        c.drawBitmap(
                assets.devices[index],
                -assets.devices[index].getWidth() / 2f,
                -assets.devices[index].getHeight(),
                paint);
        c.restore();
    }

    void drawMinion(Canvas c, Enemy enemy) {
        Bitmap sprite = assets.minionSprites[enemy.kind - 7];
        float floor = enemy.y + 40,
                bob = enemy.ground ? (float) Math.sin(enemy.phase * 12) * 1.2f : 0;
        c.save();
        c.translate(enemy.x + 17, floor + bob);
        if (enemy.dir > 0) {
            c.scale(-1, 1);
        }
        if (enemy.kind == 12 && enemy.mode == 0) {
            c.scale(1, .22f);
        } else if (enemy.kind == 12 && enemy.mode == 1) {
            c.scale(1, .35f + .65f * (1 - Math.max(0, enemy.modeTime) / .4f));
        }
        if (enemy.kind == 9) {
            c.rotate((float) Math.sin(enemy.phase * 6) * 5);
        }
        paint.setColor(Color.WHITE);
        if (enemy.slow > 0) {
            paint.setAlpha(175);
        }
        c.drawBitmap(sprite, -sprite.getWidth() / 2f, -sprite.getHeight(), paint);
        paint.setAlpha(255);
        c.restore();
        if (enemy.mode == 1 && enemy.kind != 12) {
            paint.setColor(Color.rgb(255, 111, 87));
            c.drawCircle(enemy.x + 17, enemy.y - 12, 4 + (float) Math.sin(world.clock * 16), paint);
        }
        if (enemy.kind == 8 && enemy.mode == 3) {
            paint.setColor(Color.rgb(123, 235, 190));
            c.drawCircle(enemy.x + 17, enemy.y + 11, 4, paint);
        }
    }

    void expeditionObjects(Canvas c) {
        for (float[] vent : world.vents) {
            paint.setColor(
                    Color.argb(38, Campaign.THEMES[world.levelIndex] == 5 ? 255 : 130, 220, 205));
            c.drawRect(vent[0], vent[2], vent[1], vent[3], paint);
            paint.setColor(Campaign.ACCENTS[world.levelIndex]);
            paint.setStrokeWidth(1);
            for (int i = 0; i < 7; i++) {
                float x = vent[0] + 12 + i * 12,
                        y = vent[3] - (world.clock * 120 + i * 77) % (vent[3] - vent[2]);
                c.drawLine(x, y, x + 4, y - 20, paint);
            }
        }
        for (Shot shot : world.hostileShots) {
            paint.setColor(Color.rgb(29, 12, 27));
            c.drawCircle(shot.x, shot.y, 9, paint);
            paint.setColor(Color.rgb(255, 112, 102));
            c.drawCircle(shot.x, shot.y, 6, paint);
            paint.setColor(Color.rgb(255, 228, 173));
            c.drawCircle(shot.x, shot.y, 2, paint);
        }
        if (world.progress.ranks[3] > 0) {
            for (CacheWall cache : world.cacheWalls) {
                if (!cache.broken
                        && Math.abs(world.hero.x - cache.x * world.TILE_SIZE)
                                < 160 + world.progress.ranks[3] * 90) {
                    star(
                            c,
                            cache.x * world.TILE_SIZE + 18,
                            cache.y * world.TILE_SIZE - 14,
                            7,
                            Campaign.ACCENTS[world.levelIndex]);
                }
            }
        }
    }

    void drawAlly(Canvas c) {
        if (!world.progress.lionAlly || world.mission.nodesAreOptional()) {
            return;
        }
        float x = world.allyStationX(), y = world.spawnFloor(x);
        c.save();
        c.translate(x, y);
        c.scale(.58f, .58f);
        paint.setColor(Color.WHITE);
        Bitmap pose = assets.lionFrames[0];
        c.drawBitmap(pose, -pose.getWidth() / 2f, -166, paint);
        c.restore();
        hudText(c, "ЛЕВ · ПОСТ ПОМОЩИ", x - 52, y - 104, 10, Color.rgb(146, 228, 205));
    }

    void drawUpgrades(Canvas c) {
        paint.setColor(Color.argb(247, 6, 13, 29));
        c.drawRect(0, 0, world.viewWidth, world.LOGICAL_HEIGHT, paint);
        c.save();
        c.translate((world.viewWidth - 960) / 2, 0);
        hudText(c, "МАСТЕРСКАЯ STU", 72, 70, 32, Campaign.ACCENTS[world.levelIndex]);
        hudText(
                c,
                "Stu · Очки: "
                        + world.progress.points
                        + (BuildConfig.DEBUG
                                ? " · 1–5 / ↑↓: выбор · Enter / A: улучшить · Tab / Select: назад"
                                : " · ↑↓: выбор · A: улучшить · Select: назад"),
                72,
                105,
                16,
                Color.rgb(190, 214, 226));
        for (int i = 0; i < 5; i++) {
            float y = 132 + i * 96;
            paint.setColor(
                    i == world.selectedSkill ? Color.rgb(27, 62, 79) : Color.rgb(15, 30, 48));
            c.drawRoundRect(60, y, 900, y + 82, 12, 12, paint);
            star(c, 88, y + 29, 10, Campaign.ACCENTS[world.levelIndex]);
            hudText(c, (i + 1) + " · " + Progression.SKILLS[i], 112, y + 32, 21, Color.WHITE);
            hudText(c, Progression.EFFECTS[i], 112, y + 61, 14, Color.rgb(180, 207, 216));
            for (int rank = 0; rank < 3; rank++) {
                paint.setColor(
                        rank < world.progress.ranks[i]
                                ? Campaign.ACCENTS[world.levelIndex]
                                : Color.rgb(52, 73, 88));
                c.drawRoundRect(647 + rank * 29, y + 19, 669 + rank * 29, y + 33, 3, 3, paint);
            }
            hudText(
                    c,
                    world.progress.ranks[i] >= 3
                            ? "МАКС."
                            : "Цена: " + (world.progress.ranks[i] + 1),
                    766,
                    y + 32,
                    17,
                    Color.rgb(255, 221, 147));
        }
        hudText(
                c,
                world.progress.heliUnlocked
                        ? "Полёт на ушах: H / L2 · запас "
                                + String.format(Locale.US, "%.1f", world.progress.maxFuel())
                                + " с"
                        : "Уши: победи Льва в главе 02. Потом прыжок + удерживай L2 / H.",
                72,
                655,
                17,
                Color.rgb(159, 239, 209));
        hudText(
                c,
                "2 очка за новую главу · модули и пасхалки дают дополнительные очки",
                72,
                689,
                14,
                Color.rgb(155, 177, 194));
        c.restore();
    }

    void worldObjects(Canvas c) {
        for (Spring spring : world.springs) {
            drawDevice(c, 2, spring.x, spring.y + 5, .86f);
        }
        for (Relay relay : world.relays) {
            drawDevice(c, relay.active ? 1 : 0, relay.x, relay.y + 31, 1);
            if (world.definition.missionKind == LevelDefinition.MissionKind.ORDERED_RELAYS) {
                hudText(
                        c,
                        String.valueOf(world.relays.indexOf(relay) + 1),
                        relay.x - 5,
                        relay.y - 48,
                        18,
                        Color.WHITE);
            }
            if (Math.abs(world.hero.x - relay.x) < 130 && !relay.active) {
                hudText(c, "Z / КАСАНИЕ", relay.x - 34, relay.y - 42, 10, Color.rgb(230, 214, 173));
            }
        }
        for (int i = 1; i < world.checkpointX.length; i++) {
            float x = world.checkpointX[i] + 15;
            if (x > world.cam - 60 && x < world.cam + world.viewWidth + 60) {
                drawDevice(c, i <= world.checkpoint ? 1 : 0, x, world.checkpointY[i], .47f);
            }
        }
        for (CacheWall cache : world.cacheWalls) {
            if (!cache.broken) {
                float x = cache.x * world.TILE_SIZE, y = cache.y * world.TILE_SIZE;
                paint.setColor(Color.rgb(177, 210, 166));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                Path crack = new Path();
                crack.moveTo(x + 20, y);
                crack.lineTo(x + 13, y + 12);
                crack.lineTo(x + 23, y + 20);
                crack.lineTo(x + 12, y + 36);
                c.drawPath(crack, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        for (float[] spike : world.spikes) {
            for (float x = spike[0]; x < spike[0] + spike[2]; x += 12) {
                paint.setColor(Color.rgb(231, 104, 99));
                Path thorn = new Path();
                thorn.moveTo(x, spike[1]);
                thorn.lineTo(x + 5, spike[1] - 14);
                thorn.lineTo(x + 11, spike[1]);
                thorn.close();
                c.drawPath(thorn, paint);
            }
        }
        if (!world.gateOpen) {
            paint.setColor(Color.argb(170, 42, 181, 211));
            for (int y = 0; y < world.heightInTiles; y++) {
                c.drawRect(
                        world.definition.gateColumn * world.TILE_SIZE + 10,
                        y * world.TILE_SIZE,
                        world.definition.gateColumn * world.TILE_SIZE + 25,
                        y * world.TILE_SIZE + 27,
                        paint);
            }
            hudText(
                    c,
                    !world.mission.nodesAreOptional()
                            ? ChapterMission.NAMES[world.levelIndex]
                            : "ШЛЮЗ: " + world.relays.size() + " РЕЛЕ",
                    129 * world.TILE_SIZE,
                    9 * world.TILE_SIZE,
                    16,
                    Color.rgb(181, 236, 245));
        }
        for (Wave wave : world.waves) {
            paint.setColor(Color.argb(75, 255, 108, 80));
            c.drawOval(wave.x - 21, wave.y - 28, wave.x + 21, wave.y + 22, paint);
            paint.setColor(Color.rgb(255, 149, 108));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            c.drawArc(wave.x - 14, wave.y - 18, wave.x + 14, wave.y + 18, 90, 180, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    void drawArchive(Canvas c, Item item) {
        drawDevice(c, 3, item.x + 12, item.y + 28 + (float) Math.sin(item.phase) * 3, .62f);
    }

    void drawBoss(Canvas c) {
        if (Campaign.boss(world.levelIndex) < 0 || (!world.boss.active && !world.boss.defeated)) {
            return;
        }
        if (world.boss.active) {
            drawBossWarning(c);
        }
        paint.setColor(Color.argb(90, 2, 10, 20));
        c.drawOval(
                world.boss.x - 8,
                13 * world.TILE_SIZE - 8,
                world.boss.x + 104,
                13 * world.TILE_SIZE + 5,
                paint);
        Bitmap art = assets.bossSprite(world.boss);
        c.save();
        c.translate(world.boss.x + 48, world.boss.y + 110);
        if (world.boss.dir > 0) {
            c.scale(-1, 1);
        }
        if (world.boss.state == BossBrain.CHARGE) {
            c.rotate(-5);
        }
        if (world.boss.defeated) {
            c.scale(1, .82f);
        }
        paint.setColor(Color.WHITE);
        paint.setAlpha(world.boss.defeated ? 180 : 255);
        c.drawBitmap(art, -art.getWidth() / 2f, -166, paint);
        paint.setAlpha(255);
        c.restore();
        if (world.boss.active && (world.boss.state == BossBrain.RECOVER || world.boss.flash > 0)) {
            paint.setColor(
                    world.boss.flash > 0 ? Color.rgb(255, 225, 178) : Color.rgb(136, 246, 205));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            c.drawCircle(
                    world.boss.x + 48,
                    world.boss.y + 47,
                    12 + (float) Math.sin(world.clock * 13) * 2,
                    paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    void drawBossWarning(Canvas c) {
        boolean tell = world.boss.state == BossBrain.WINDUP;
        int alpha = 90 + (int) (25 * Math.sin(world.clock * 18));
        if (tell && world.boss.plan == BossBrain.DASH) {
            float start = world.boss.x + 48,
                    end =
                            world.clamp(
                                    world.boss.targetX + world.boss.dir * 85,
                                    139 * world.TILE_SIZE,
                                    world.definition.exitColumn * world.TILE_SIZE);
            paint.setColor(Color.argb(alpha, 255, 95, 72));
            c.drawRect(
                    Math.min(start, end),
                    13 * world.TILE_SIZE - 8,
                    Math.max(start, end),
                    13 * world.TILE_SIZE - 2,
                    paint);
            paint.setStrokeWidth(2);
            for (float x = Math.min(start, end) + 18; x < Math.max(start, end); x += 38) {
                c.drawLine(
                        x - world.boss.dir * 6,
                        13 * world.TILE_SIZE - 19,
                        x,
                        13 * world.TILE_SIZE - 14,
                        paint);
                c.drawLine(
                        x,
                        13 * world.TILE_SIZE - 14,
                        x - world.boss.dir * 6,
                        13 * world.TILE_SIZE - 9,
                        paint);
            }
        }
        if ((tell && world.boss.plan == BossBrain.POUNCE) || world.boss.state == BossBrain.LEAP) {
            paint.setColor(Color.argb(70, 255, 101, 75));
            c.drawOval(
                    world.boss.targetX - 47,
                    13 * world.TILE_SIZE - 13,
                    world.boss.targetX + 47,
                    13 * world.TILE_SIZE + 7,
                    paint);
            paint.setColor(Color.rgb(255, 144, 106));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            c.drawOval(
                    world.boss.targetX - 47,
                    13 * world.TILE_SIZE - 13,
                    world.boss.targetX + 47,
                    13 * world.TILE_SIZE + 7,
                    paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (tell && world.boss.plan == BossBrain.GROUND_SLAM) {
            paint.setColor(Color.argb(alpha, 255, 102, 84));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            c.drawArc(
                    world.boss.x - 100,
                    13 * world.TILE_SIZE - 15,
                    world.boss.x + 195,
                    13 * world.TILE_SIZE + 15,
                    180,
                    180,
                    false,
                    paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if ((tell && world.boss.plan == BossBrain.TELEPORT)
                || world.boss.state == BossBrain.MIRROR) {
            Bitmap ghost = assets.villainFrames[2];
            paint.setColor(Color.WHITE);
            paint.setAlpha(65);
            c.drawBitmap(
                    ghost,
                    world.boss.teleportX + 48 - ghost.getWidth() / 2f,
                    13 * world.TILE_SIZE - 166,
                    paint);
            paint.setAlpha(255);
            paint.setColor(Color.rgb(255, 129, 111));
            c.drawRect(
                    world.boss.teleportX,
                    13 * world.TILE_SIZE - 4,
                    world.boss.teleportX + 96,
                    13 * world.TILE_SIZE,
                    paint);
        }
    }

    void campaignHud(Canvas c) {
        if (world.showTouchControls) {
            hudText(c, "☰ МЕНЮ", world.viewWidth - 126, 108, 16, Color.WHITE);
        }
        paint.setColor(Color.argb(205, 5, 20, 30));
        c.drawRoundRect(18, 12, 590, 70, 10, 10, paint);
        hudText(
                c,
                String.format(
                        Locale.US,
                        "%02d · %s",
                        world.levelIndex + 1,
                        Campaign.TITLES[world.levelIndex]),
                32,
                35,
                17,
                Color.rgb(157, 237, 184));
        int active = 0;
        for (Relay relay : world.relays) {
            if (relay.active) {
                active++;
            }
        }
        String objective =
                !world.gateOpen
                        ? "Обходные реле " + active + " / " + world.relays.size()
                        : world.boss.defeated
                                ? "Путь открыт · выход справа"
                                : Campaign.boss(world.levelIndex) >= 0
                                        ? "Найди "
                                                + Campaign.BOSS_NAMES[
                                                        Campaign.boss(world.levelIndex)]
                                        : (BuildConfig.DEBUG
                                                ? "Исследуй верх / низ · Tab: прокачка · H: полёт"
                                                : "Исследуй верх / низ · Select: навыки · L2:"
                                                        + " полёт");
        if (world.mission != null && !world.mission.nodesAreOptional() && !world.mission.done()) {
            objective =
                    ChapterMission.NAMES[world.levelIndex]
                            + " · "
                            + world.mission.count()
                            + " / "
                            + world.mission.nodes.length;
        }
        hudText(c, objective, 32, 57, 13, Color.rgb(191, 214, 214));
        if (world.mission != null && !world.mission.nodesAreOptional() && !world.mission.done()) {
            ChapterMission.Node target = null;
            float distance = Float.MAX_VALUE;
            for (int i = 0; i < world.mission.nodes.length; i++) {
                if (!world.mission.lit(i)) {
                    ChapterMission.Node node = world.mission.nodes[i];
                    float d = Math.abs(node.x - world.hero.x) + Math.abs(node.y - world.hero.y);
                    if ((world.mission.kind == LevelDefinition.MissionKind.ORDERED_KEYS
                                    || world.mission.kind == LevelDefinition.MissionKind.MIRRORS)
                            && i != world.mission.count()) {
                        continue;
                    }
                    if (d < distance) {
                        distance = d;
                        target = node;
                    }
                }
            }
            if (target != null) {
                float targetX =
                        world.mission.kind == LevelDefinition.MissionKind.ESCORT
                                ? world.mission.cartX
                                : target.x;
                String arrow =
                        targetX > world.hero.x + 60 ? "→" : targetX < world.hero.x - 60 ? "←" : "●";
                String height =
                        target.y < world.hero.y - 80
                                ? " ↑"
                                : target.y > world.hero.y + 80 ? " ↓" : "";
                hudText(
                        c,
                        "ЦЕЛЬ "
                                + arrow
                                + height
                                + " · "
                                + Math.round(Math.abs(targetX - world.hero.x) / world.TILE_SIZE)
                                + " м"
                                + (world.mission.kind == LevelDefinition.MissionKind.SANDSTORM
                                        ? (world.mission.calm(world.clock - world.levelStartedAt)
                                                ? " · ЗАТИШЬЕ"
                                                : " · БУРЯ")
                                        : ""),
                        32,
                        91,
                        13,
                        Campaign.ACCENTS[world.levelIndex]);
            }
        }
        if (showDebugAnnotations) {
            hudText(
                    c,
                    "1/2: главы · 3: босс · 4: навыки",
                    world.viewWidth - 214,
                    30,
                    12,
                    Color.rgb(175, 199, 198));
        }
        if (world.boss.active && !world.boss.defeated) {
            float x = world.viewWidth - 350, w = 330;
            paint.setColor(Color.argb(230, 15, 28, 39));
            c.drawRoundRect(x, 43, x + w, 66, 5, 5, paint);
            paint.setColor(
                    world.boss.hp <= world.boss.maxHp / 2
                            ? Color.rgb(255, 133, 92)
                            : Color.rgb(232, 189, 93));
            c.drawRoundRect(
                    x + 3,
                    46,
                    x + 3 + (w - 6) * world.boss.hp / (float) world.boss.maxHp,
                    63,
                    4,
                    4,
                    paint);
            hudText(
                    c,
                    Campaign.BOSS_NAMES[world.boss.type]
                            + " · "
                            + world.boss.cue()
                            + " · ♥"
                            + world.bossLives,
                    x,
                    85,
                    10,
                    Color.rgb(244, 222, 172));
        }
        if (world.progress.heliUnlocked
                && !world.progress.flightTrained
                && !world.upgradeMenu
                && world.screen == UiScreen.PLAY
                && !world.won
                && !world.gameOver) {
            paint.setColor(Color.argb(240, 7, 32, 44));
            c.drawRoundRect(100, 470, world.viewWidth - 100, 535, 12, 12, paint);
            hudText(
                    c,
                    "ПОЛЁТ НА УШАХ ОТКРЫТ · ПРЫГНИ И УДЕРЖИВАЙ L2 / H",
                    122,
                    498,
                    18,
                    Color.rgb(151, 244, 196));
            hudText(
                    c,
                    "На земле запас восстанавливается. В потоке можно набрать высоту.",
                    122,
                    522,
                    14,
                    Color.WHITE);
        }
        if (world.progress.heliUnlocked) {
            paint.setColor(Color.argb(200, 7, 28, 43));
            c.drawRoundRect(18, 616, 202, 635, 6, 6, paint);
            paint.setColor(Campaign.ACCENTS[world.levelIndex]);
            c.drawRoundRect(
                    21, 619, 21 + 178 * world.fuel / world.progress.maxFuel(), 632, 4, 4, paint);
            hudText(c, "H / L2", 212, 631, 12, Color.rgb(209, 235, 236));
        }
        hudText(
                c,
                (world.showTouchControls
                                ? "Коснись главы · "
                                : BuildConfig.DEBUG ? "Tab · навыки " : "Select · навыки ")
                        + world.progress.points,
                world.viewWidth - 208,
                631,
                13,
                Campaign.ACCENTS[world.levelIndex]);
        if (world.messageTimer > 0 && !world.won && !world.gameOver) {
            paint.setColor(Color.argb(238, 5, 19, 31));
            c.drawRoundRect(90, 557, world.viewWidth - 90, 632, 12, 12, paint);
            hudText(c, world.messageTitle, 110, 584, 17, Color.rgb(145, 238, 203));
            float size = 15;
            while (font.measureText(world.messageBody) > world.viewWidth - 220 && size > 10) {
                font.setTextSize(--size);
            }
            hudText(c, world.messageBody, 110, 612, size, Color.rgb(220, 234, 231));
        }
    }

    Canvas lockFrame() {
        if (!holder.getSurface().isValid()) {
            return null;
        }
        try {
            try {
                return android.os.Build.VERSION.SDK_INT >= 26
                        ? holder.lockHardwareCanvas()
                        : holder.lockCanvas();
            } catch (IllegalStateException | UnsupportedOperationException error) {
                return holder.lockCanvas();
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            if (!running || !holder.getSurface().isValid()) {
                GameDiagnostics.event("SURFACE_LOST during frame lock");
                return null;
            }
            throw error;
        }
    }

    void render() {
        Canvas c = lockFrame();
        if (c == null) {
            return;
        }
        try {
            c.drawColor(Color.BLACK);
            float
                    s =
                            Math.min(
                                    c.getWidth() / world.viewWidth,
                                    c.getHeight() / world.LOGICAL_HEIGHT),
                    ox = (c.getWidth() - world.viewWidth * s) / 2,
                    oy = (c.getHeight() - world.LOGICAL_HEIGHT * s) / 2;
            c.save();
            c.translate(ox, oy);
            c.scale(s, s);
            background(c);
            c.save();
            c.translate(
                    -world.cam
                            + (world.shake > 0
                                    ? visualRandom.nextFloat() * world.shake - world.shake / 2
                                    : 0),
                    world.WORLD_OFFSET_Y
                            - world.camY
                            + (world.shake > 0
                                    ? visualRandom.nextFloat() * world.shake - world.shake / 2
                                    : 0));
            drawInterior(c);
            tiles(c);
            worldObjects(c);
            expeditionObjects(c);
            drawMission(c);
            for (Item i : world.items) {
                item(c, i);
            }
            for (Enemy e : world.enemies) {
                enemy(c, e);
            }
            for (Bullet b : world.bullets) {
                bullet(c, b);
            }
            for (Spark q : world.sparks) {
                paint.setColor(q.color);
                paint.setAlpha((int) (255 * world.clamp(q.life / .65f, 0, 1)));
                c.drawRect(q.x, q.y, q.x + q.size, q.y + q.size, paint);
            }
            paint.setAlpha(255);
            drawAlly(c);
            drawBoss(c);
            player(c);
            finishFlag(c);
            c.restore();
            hud(c);
            campaignHud(c);
            if (world.showTouchControls && world.screen == UiScreen.PLAY && !world.upgradeMenu) {
                touch(c);
            }
            if (!world.upgradeMenu && (world.paused || world.won || world.gameOver)) {
                overlay(
                        c,
                        world.gameOver
                                ? "ПОРАЖЕНИЕ"
                                : world.won
                                        ? (world.levelIndex < Campaign.COUNT - 1
                                                ? "ГЛАВА ПРОЙДЕНА"
                                                : "ЛЕС И ПОСЕЛЕНИЕ СПАСЕНЫ")
                                        : "ПАУЗА",
                        world.gameOver
                                ? "Start — меню · Enter — повторить"
                                : world.won
                                        ? (world.levelIndex < Campaign.COUNT - 1
                                                ? "Start — меню · Enter — дальше · "
                                                        + (BuildConfig.DEBUG
                                                                ? "Tab / Select"
                                                                : "Select")
                                                        + " — прокачка"
                                                : "Лев спас людей. Start — меню · Enter — выбор"
                                                        + " главы.")
                                        : "START / ENTER — продолжить");
            }
            if (world.won && world.screen == UiScreen.PLAY && !world.upgradeMenu) {
                hudText(
                        c,
                        "Очки главы: "
                                + world.levelScore
                                + " · Рекорд: "
                                + world.records.bestScore[world.levelIndex]
                                + " · Время: "
                                + LevelRecords.time(
                                        (int) ((world.clock - world.levelStartedAt) * 1000)),
                        90,
                        460,
                        20,
                        Color.WHITE);
            }
            if (world.screen != UiScreen.PLAY) {
                drawMenu(c);
            }
            if (world.upgradeMenu) {
                drawUpgrades(c);
            }
            c.restore();
        } finally {
            try {
                holder.unlockCanvasAndPost(c);
            } catch (IllegalArgumentException | IllegalStateException error) {
                if (running && holder.getSurface().isValid()) {
                    throw error;
                }
                GameDiagnostics.event("SURFACE_LOST during frame post");
            }
        }
    }

    void background(Canvas c) {
        Bitmap scene =
                Campaign.THEMES[world.levelIndex] == 0
                        ? assets.jungle
                        : assets.backdrops[Campaign.THEMES[world.levelIndex] - 1];
        float w = scene.getWidth(), x = -(world.cam * .12f) % w;
        paint.setColor(Color.WHITE);
        for (float drawX = x; drawX < world.viewWidth; drawX += w) {
            c.drawBitmap(scene, drawX, 0, backgroundPaint);
        }
        paint.setColor(Color.argb(16 + (int) (world.camY * .06f), 1, 10, 24));
        c.drawRect(0, 0, world.viewWidth, world.LOGICAL_HEIGHT, paint);
    }

    void tiles(Canvas c) {
        int a = Math.max(0, (int) (world.cam / world.TILE_SIZE) - 2),
                b = Math.min(world.widthInTiles, a + (int) (world.viewWidth / world.TILE_SIZE) + 5);
        for (int y = 0; y < world.heightInTiles; y++) {
            for (int x = a; x < b; x++) {
                if (world.wall[y][x]) {
                    float l = x * world.TILE_SIZE, t = y * world.TILE_SIZE;
                    boolean top = y == 0 || !world.wall[y - 1][x];
                    int variant = Math.floorMod(x / 3 + y * 7, 3),
                            sx = variant * 108 + (x % 3) * 36,
                            sy = top ? 0 : 108 + (y % 3) * 36;
                    tileSource.set(sx, sy, sx + 36, sy + 36);
                    tileTarget.set(l, t, l + world.TILE_SIZE + .2f, t + world.TILE_SIZE + .2f);
                    paint.setColor(Color.WHITE);
                    if (Campaign.THEMES[world.levelIndex] == 0) {
                        c.drawBitmap(assets.terrain, tileSource, tileTarget, terrainPaint);
                    } else {
                        int ox = (x % 3) * 36, oy = top ? 0 : 36 + (y % 2) * 36;
                        tileSource.set(ox, oy, ox + 36, oy + 36);
                        c.drawBitmap(
                                assets.surfaces[Campaign.THEMES[world.levelIndex] - 1],
                                tileSource,
                                tileTarget,
                                terrainPaint);
                    }
                    if (top) {
                        paint.setColor(
                                Campaign.THEMES[world.levelIndex] <= 2
                                        ? Color.rgb(154, 220, 91)
                                        : Campaign.ACCENTS[world.levelIndex]);
                        c.drawRect(l, t, l + world.TILE_SIZE, t + 1, paint);
                        // Small silhouettes add foliage above the textured walkable edge.
                        for (int i = 0; Campaign.THEMES[world.levelIndex] <= 2 && i < 3; i++) {
                            float px = l + 5 + i * 12, ht = 3 + Math.floorMod(x * 11 + i * 7, 6);
                            paint.setColor(
                                    i % 2 == 0 ? Color.rgb(79, 155, 70) : Color.rgb(121, 191, 76));
                            paint.setStrokeWidth(1);
                            c.drawLine(px, t, px - 2, t - ht, paint);
                            c.drawLine(px - 2, t - ht + 2, px - 5, t - ht + 1, paint);
                        }
                    }
                    if (x == 0 || !world.wall[y][x - 1]) {
                        paint.setColor(Color.argb(110, 1, 10, 17));
                        c.drawRect(l, t, l + 2, t + world.TILE_SIZE, paint);
                    }
                    if (x == world.widthInTiles - 1 || !world.wall[y][x + 1]) {
                        paint.setColor(Color.argb(145, 1, 10, 17));
                        c.drawRect(
                                l + world.TILE_SIZE - 3,
                                t,
                                l + world.TILE_SIZE,
                                t + world.TILE_SIZE,
                                paint);
                    }
                    if (y == world.heightInTiles - 1 || !world.wall[y + 1][x]) {
                        paint.setColor(Color.argb(135, 1, 9, 15));
                        c.drawRect(
                                l,
                                t + world.TILE_SIZE - 3,
                                l + world.TILE_SIZE,
                                t + world.TILE_SIZE,
                                paint);
                        if (Campaign.THEMES[world.levelIndex] <= 2 && x % 3 == 1) {
                            paint.setColor(Color.rgb(56, 110, 67));
                            paint.setStrokeWidth(2);
                            float len = 9 + (x * 7) % 13;
                            c.drawLine(
                                    l + 12,
                                    t + world.TILE_SIZE,
                                    l + 16,
                                    t + world.TILE_SIZE + len,
                                    paint);
                            c.drawOval(
                                    l + 11,
                                    t + world.TILE_SIZE + 5,
                                    l + 17,
                                    t + world.TILE_SIZE + 9,
                                    paint);
                            c.drawOval(
                                    l + 15,
                                    t + world.TILE_SIZE + 11,
                                    l + 21,
                                    t + world.TILE_SIZE + 15,
                                    paint);
                        }
                    }
                }
            }
        }
    }

    int runFrameIndex() {
        return PlayerPose.runFrame(world, assets);
    }

    void player(Canvas c) {
        Bitmap sprite = world.playerPose().sprite;
        c.save();
        c.translate(world.hero.x + 15, world.hero.y + 42);
        if (world.hero.face < 0) {
            c.scale(-1, 1);
        }
        paint.setColor(Color.argb(90, 1, 9, 16));
        if (world.hero.ground) {
            c.drawOval(-23, -5, 23, 3, paint);
        }
        drawReadable(c, sprite, -sprite.getWidth() / 2f, -sprite.getHeight());
        c.restore();
    }

    void enemy(Canvas c, Enemy e) {
        if (e.kind >= 7) {
            drawMinion(c, e);
            return;
        }
        if (e.kind >= 3 && e.kind < 7) {
            Bitmap art = assets.villains[(e.kind - 3) % 4];
            c.save();
            c.translate(e.x + 17, e.y + 40);
            if (world.hero.x > e.x) {
                c.scale(-1, 1);
            }
            c.scale(.44f, .44f);
            if (e.slow > 0) {
                paint.setAlpha(170);
            }
            c.drawBitmap(art, -art.getWidth() / 2f, -166, paint);
            paint.setAlpha(255);
            c.restore();
            return;
        }
        int kind = Math.max(0, Math.min(2, e.kind));
        float width = kind == 1 ? 68 : kind == 2 ? 53 : 55, height = kind == 2 ? 57 : 52;
        boolean faceRight = kind == 2 ? world.hero.x > e.x : e.dir > 0;
        c.save();
        c.translate(e.x + 17, e.y + 40);
        if (faceRight) {
            c.scale(-1, 1);
        }
        c.drawBitmap(assets.enemySprites[kind], -width / 2, -height, paint);
        c.restore();
    }

    void item(Canvas c, Item item) {
        float y = item.y + (float) Math.sin(item.phase) * 3;
        if (item.kind == 0) {
            paint.setColor(Color.WHITE);
            c.drawBitmap(assets.energySeed, item.x + 2, y - 4, paint);
        } else if (item.kind == 3) {
            drawArchive(c, item);
        } else if (item.kind == 4) {
            drawDevice(c, 5, item.x + 12, y + 28, .62f);
        } else if (item.kind == 5) {
            star(c, item.x + 12, y + 12, 12, Campaign.ACCENTS[world.levelIndex]);
        } else {
            drawDevice(c, 4, item.x + 14, y + 28, .65f);
            paint.setColor(
                    item.kind == 2 ? Color.rgb(255, 137, 152) : world.weaponColor(item.value));
            c.drawCircle(item.x + 14, y + 10, 3, paint);
        }
    }

    void bullet(Canvas c, Bullet b) {
        paint.setColor(world.weaponColor(b.weapon));
        c.drawCircle(b.x, b.y, b.size / 2, paint);
        paint.setColor(Color.WHITE);
        c.drawCircle(b.x, b.y, Math.max(2, b.size / 5), paint);
    }

    void finishFlag(Canvas c) {
        float x = (world.widthInTiles - 3) * world.TILE_SIZE;
        boolean open =
                world.gateOpen && (Campaign.boss(world.levelIndex) < 0 || world.boss.defeated);
        drawDevice(c, open ? 1 : 0, x, 13 * world.TILE_SIZE, 1.4f);
        hudText(
                c,
                open ? "ВЫХОД" : "УЗЕЛ ЗАКРЫТ",
                x - 40,
                13 * world.TILE_SIZE - 98,
                12,
                open ? Color.rgb(161, 242, 217) : Color.rgb(238, 193, 129));
    }

    // Presentation only: capture tests can omit developer annotations.
    boolean showDebugAnnotations = BuildConfig.DEBUG;

    void hud(Canvas c) {
        c.save();
        c.translate((world.viewWidth - 960) / 2, 0);
        float y = world.LOGICAL_HEIGHT - 72;
        paint.setColor(Color.argb(238, 7, 20, 30));
        c.drawRoundRect(18, y, 942, world.LOGICAL_HEIGHT - 12, 16, 16, paint);
        paint.setColor(Color.rgb(64, 121, 113));
        c.drawRoundRect(18, y, 942, y + 3, 2, 2, paint);
        // Heart, star, trophy, weapon and clock are drawn as sharp vector icons.
        paint.setColor(Color.rgb(255, 102, 128));
        Path heart = new Path();
        heart.moveTo(49, y + 43);
        heart.cubicTo(18, y + 23, 38, y + 12, 49, y + 25);
        heart.cubicTo(60, y + 12, 80, y + 23, 49, y + 43);
        c.drawPath(heart, paint);
        hudText(c, world.health + " / " + world.maximumHealth(), 76, y + 37, 22, Color.WHITE);
        hudText(c, "ПОПЫТКИ ×" + world.lives, 76, y + 53, 9, Color.rgb(156, 190, 199));
        star(c, 174, y + 30, 14, Color.rgb(255, 220, 98));
        hudText(
                c,
                String.format(Locale.US, "%07d", world.levelScore),
                198,
                y + 38,
                23,
                Color.WHITE);
        hudText(c, "ОЧКИ ГЛАВЫ", 198, y + 54, 9, Color.rgb(156, 190, 199));
        paint.setColor(Color.rgb(130, 190, 184));
        c.drawRoundRect(350, y + 18, 370, y + 34, 3, 3, paint);
        c.drawRect(358, y + 33, 362, y + 42, paint);
        c.drawRect(353, y + 41, 367, y + 44, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        c.drawArc(344, y + 18, 376, y + 36, 0, 180, false, paint);
        paint.setStyle(Paint.Style.FILL);
        hudText(
                c,
                String.format(Locale.US, "%07d", world.records.bestScore[world.levelIndex]),
                385,
                y + 37,
                18,
                Color.rgb(160, 202, 198));
        hudText(c, "РЕКОРД ГЛАВЫ", 385, y + 54, 9, Color.rgb(156, 190, 199));
        paint.setColor(world.gunColor());
        c.drawRoundRect(527, y + 22, 563, y + 32, 3, 3, paint);
        c.drawRect(535, y + 32, 543, y + 42, paint);
        c.drawRect(561, y + 24, 570, y + 29, paint);
        hudText(
                c,
                world.gun == 0 ? "∞" : String.valueOf(world.ammo[world.gun]),
                585,
                y + 39,
                27,
                Color.WHITE);
        hudText(c, world.gunName(), 526, y + 54, 9, Color.rgb(160, 202, 198));
        paint.setColor(Color.rgb(115, 208, 216));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        c.drawCircle(692, y + 31, 12, paint);
        c.drawLine(692, y + 31, 692, y + 23, paint);
        c.drawLine(692, y + 31, 699, y + 34, paint);
        paint.setStyle(Paint.Style.FILL);
        hudText(
                c,
                String.format(
                        Locale.US,
                        "%02d:%02d",
                        (int) (world.clock - world.levelStartedAt) / 60,
                        (int) (world.clock - world.levelStartedAt) % 60),
                714,
                y + 38,
                21,
                Color.WHITE);
        for (int i = 0; i < 2; i++) {
            paint.setColor(
                    i >= world.hero.jumps ? Color.rgb(145, 240, 110) : Color.rgb(42, 65, 61));
            c.drawCircle(824 + i * 21, y + 31, 6, paint);
        }
        if (showDebugAnnotations) {
            hudText(
                    c,
                    Math.round(world.fps) + " FPS",
                    866,
                    y + 37,
                    12,
                    world.fps >= 55 ? Color.rgb(90, 225, 150) : Color.rgb(255, 110, 90));
        }
        if (world.combo > 1 && world.comboTimer > 0) {
            hudText(c, "×" + world.combo, 462, 48, 28, Color.rgb(255, 128, 145));
        }
        c.restore();
    }

    void hudText(Canvas c, String text, float x, float y, float size, int color) {
        font.setTextSize(size);
        font.setColor(color);
        c.drawText(text, x, y, font);
    }

    void star(Canvas c, float x, float y, float radius, int color) {
        Path star = new Path();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float r = i % 2 == 0 ? radius : radius * .46f;
            float px = x + (float) Math.cos(a) * r, py = y + (float) Math.sin(a) * r;
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.close();
        paint.setColor(color);
        c.drawPath(star, paint);
    }

    void touch(Canvas c) {
        paint.setColor(Color.argb(50, 255, 255, 255));
        c.drawCircle(91, world.LOGICAL_HEIGHT - 158, 58, paint);
        c.drawCircle(world.viewWidth - 90, world.LOGICAL_HEIGHT - 164, 52, paint);
        c.drawCircle(world.viewWidth - 194, world.LOGICAL_HEIGHT - 138, 40, paint);
        c.drawCircle(world.viewWidth - 285, world.LOGICAL_HEIGHT - 174, 32, paint);
        font.setColor(Color.argb(130, 255, 255, 255));
        font.setTextSize(25);
        c.drawText("<  >", 57, world.LOGICAL_HEIGHT - 149, font);
        c.drawText("A", world.viewWidth - 99, world.LOGICAL_HEIGHT - 155, font);
        c.drawText("X", world.viewWidth - 203, world.LOGICAL_HEIGHT - 130, font);
        c.drawText("Y", world.viewWidth - 294, world.LOGICAL_HEIGHT - 166, font);
        if (world.progress.heliUnlocked
                && !world.progress.flightTrained
                && !world.upgradeMenu
                && world.screen == UiScreen.PLAY
                && !world.won
                && !world.gameOver) {
            paint.setColor(Color.argb(240, 7, 32, 44));
            c.drawRoundRect(100, 470, world.viewWidth - 100, 535, 12, 12, paint);
            hudText(
                    c,
                    "ПОЛЁТ НА УШАХ ОТКРЫТ · ПРЫГНИ И УДЕРЖИВАЙ L2 / H",
                    122,
                    498,
                    18,
                    Color.rgb(151, 244, 196));
            hudText(
                    c,
                    "На земле запас восстанавливается. В потоке можно набрать высоту.",
                    122,
                    522,
                    14,
                    Color.WHITE);
        }
        if (world.progress.heliUnlocked) {
            paint.setColor(Color.argb(60, 140, 255, 210));
            c.drawCircle(world.viewWidth - 370, world.LOGICAL_HEIGHT - 150, 32, paint);
            c.drawText("H", world.viewWidth - 379, world.LOGICAL_HEIGHT - 142, font);
        }
    }

    void overlay(Canvas c, String a, String b) {
        paint.setColor(Color.argb(225, 2, 10, 19));
        c.drawRect(0, 0, world.viewWidth, world.LOGICAL_HEIGHT, paint);
        font.setTextAlign(Paint.Align.CENTER);
        font.setColor(Color.rgb(75, 230, 191));
        font.setTextSize(50);
        c.drawText(a, world.viewWidth / 2, world.LOGICAL_HEIGHT / 2 - 15, font);
        font.setColor(Color.WHITE);
        font.setTextSize(19);
        c.drawText(b, world.viewWidth / 2, world.LOGICAL_HEIGHT / 2 + 30, font);
        font.setTextAlign(Paint.Align.LEFT);
    }

    void drawMenu(Canvas c) {
        paint.setColor(Color.argb(246, 5, 13, 25));
        c.drawRect(0, 0, world.viewWidth, world.LOGICAL_HEIGHT, paint);
        c.save();
        c.translate((world.viewWidth - 960) / 2, 0);
        int accent =
                Campaign.ACCENTS[
                        world.screen == UiScreen.CHAPTER_SELECT
                                ? world.chapterSelection
                                : world.levelIndex];
        hudText(c, "STU · NEON HARE", 48, 62, 32, accent);
        hudText(
                c,
                world.screen == UiScreen.CHAPTER_SELECT
                        ? "ВЫБОР ГЛАВЫ · личные рекорды"
                        : "Твой поход сохранён · контрольная точка " + (world.checkpoint + 1),
                48,
                96,
                17,
                Color.rgb(182, 205, 218));
        if (world.screen == UiScreen.MAIN_MENU) {
            String[] labels = {
                world.gameOver
                        ? "Повторить попытку"
                        : world.won && world.levelIndex < Campaign.COUNT - 1
                                ? "Следующая глава"
                                : "Продолжить",
                "Выбор уровня и рекорды",
                "Прокачка · " + world.progress.points + " очк.",
                "Сохранить и выйти"
            };
            for (int i = 0; i < labels.length; i++) {
                float y = 143 + i * 96;
                paint.setColor(
                        i == world.menuSelection ? Color.rgb(29, 65, 77) : Color.rgb(14, 29, 46));
                c.drawRoundRect(48, y, 520, y + 78, 13, 13, paint);
                if (i == world.menuSelection) {
                    paint.setColor(accent);
                    c.drawRoundRect(48, y, 53, y + 78, 2, 2, paint);
                }
                hudText(c, labels[i], 72, y + 47, 23, Color.WHITE);
            }
            drawReadable(c, assets.nova, 708, 150);
            hudText(
                    c,
                    String.format(
                            Locale.US,
                            "%02d · %s",
                            world.levelIndex + 1,
                            Campaign.TITLES[world.levelIndex]),
                    558,
                    272,
                    16,
                    accent);
            hudText(c, "Очки главы: " + world.levelScore, 558, 313, 22, Color.WHITE);
            hudText(
                    c,
                    "Время: "
                            + LevelRecords.time(
                                    (int) ((world.clock - world.levelStartedAt) * 1000)),
                    558,
                    350,
                    18,
                    Color.rgb(195, 214, 224));
            hudText(
                    c,
                    "Рекорд главы: " + world.records.bestScore[world.levelIndex],
                    558,
                    393,
                    19,
                    Color.rgb(247, 211, 139));
            hudText(
                    c,
                    "Лучшее время: "
                            + LevelRecords.time(world.records.bestMillis[world.levelIndex]),
                    558,
                    428,
                    17,
                    Color.rgb(195, 214, 224));
            hudText(c, "Рекорд похода: " + world.best, 558, 477, 18, Color.rgb(195, 214, 224));
            hudText(
                    c,
                    "Открыто глав: " + world.progress.unlockedCount() + " / " + Campaign.COUNT,
                    558,
                    515,
                    18,
                    Color.rgb(195, 214, 224));
            hudText(
                    c,
                    world.progress.heliUnlocked
                            ? "Уши открыты: прыжок + удерживай L2 / H"
                            : "Уши откроет Лев после победы в главе 02",
                    558,
                    563,
                    15,
                    accent);
            hudText(c, ChapterMission.NAMES[world.levelIndex], 48, 592, 17, accent);
            float helpSize = 15;
            font.setTextSize(helpSize);
            while (font.measureText(ChapterMission.HELP[world.levelIndex]) > 864 && helpSize > 10) {
                font.setTextSize(--helpSize);
            }
            hudText(
                    c,
                    ChapterMission.HELP[world.levelIndex],
                    48,
                    619,
                    helpSize,
                    Color.rgb(154, 184, 195));
        } else {
            int page = world.chapterSelection / 6, first = page * 6;
            for (int i = first; i < Math.min(first + 6, Campaign.COUNT); i++) {
                int slot = i - first;
                float x = 48 + (slot % 2) * 440, y = 128 + (slot / 2) * 139;
                boolean locked = !world.progress.isUnlocked(i);
                int color = locked ? Color.rgb(83, 102, 119) : Campaign.ACCENTS[i];
                paint.setColor(
                        i == world.chapterSelection
                                ? Color.rgb(29, 65, 77)
                                : Color.rgb(14, 29, 46));
                c.drawRoundRect(x, y, x + 424, y + 125, 12, 12, paint);
                hudText(
                        c,
                        String.format(Locale.US, "%02d · %s", i + 1, Campaign.TITLES[i]),
                        x + 16,
                        y + 28,
                        16,
                        color);
                if (locked) {
                    hudText(
                            c,
                            "Пройди предыдущую главу",
                            x + 16,
                            y + 68,
                            16,
                            Color.rgb(126, 147, 161));
                } else {
                    hudText(
                            c,
                            "Рекорд: "
                                    + world.records.bestScore[i]
                                    + "   ·   Время: "
                                    + LevelRecords.time(world.records.bestMillis[i]),
                            x + 16,
                            y + 61,
                            15,
                            Color.WHITE);
                    hudText(
                            c,
                            "Тайники: "
                                    + world.records.bestSecrets[i]
                                    + "   ·   Победы: "
                                    + world.records.clears[i],
                            x + 16,
                            y + 90,
                            15,
                            Color.rgb(180, 205, 214));
                    hudText(
                            c,
                            (world.progress.hasEgg(i) ? "★ " : "☆ ") + ChapterMission.NAMES[i],
                            x + 16,
                            y + 113,
                            12,
                            color);
                }
            }
            hudText(
                    c,
                    "Страница "
                            + (page + 1)
                            + " / "
                            + ((Campaign.COUNT + 5) / 6)
                            + " · ↓ дальше · повторный заход начинается с начала главы",
                    48,
                    594,
                    15,
                    Color.rgb(166, 194, 206));
        }
        hudText(
                c,
                world.showTouchControls
                        ? "Коснись пункта · Назад: верхняя полоса"
                        : "↑↓ / ←→ — выбор · A / Enter — открыть · B / Esc — назад · Start —"
                                + " продолжить",
                48,
                659,
                16,
                Color.rgb(205, 223, 230));
        hudText(
                c,
                "Select — прокачка · Рекорды обновляются после прохождения главы",
                48,
                692,
                14,
                Color.rgb(143, 173, 188));
        c.restore();
    }

    void drawMission(Canvas c) {
        if (world.mission == null || world.mission.nodesAreOptional()) {
            return;
        }
        int color = Campaign.ACCENTS[world.levelIndex];
        if (world.definition.weather == LevelDefinition.Weather.GUSTS
                || world.mission.kind == LevelDefinition.MissionKind.SANDSTORM
                        && !world.mission.calm(world.clock - world.levelStartedAt)) {
            paint.setColor(
                    world.definition.weather == LevelDefinition.Weather.GUSTS
                            ? Color.argb(65, 185, 225, 248)
                            : Color.argb(85, 250, 183, 93));
            paint.setStrokeWidth(2);
            for (int i = 0; i < 28; i++) {
                float
                        x =
                                world.cam
                                        + ((i * 137
                                                        + world.clock
                                                                * (world.definition.weather
                                                                                == LevelDefinition
                                                                                        .Weather
                                                                                        .GUSTS
                                                                        ? 190
                                                                        : 260))
                                                % world.viewWidth),
                        y = world.camY + (i * 73) % 540;
                c.drawLine(x, y, x + 22, y - 5, paint);
            }
        }
        for (int i = 0; i < world.mission.nodes.length; i++) {
            ChapterMission.Node node = world.mission.nodes[i];
            if (node.x < world.cam - 80 || node.x > world.cam + world.viewWidth + 80) {
                continue;
            }
            boolean lit = world.mission.lit(i);
            paint.setColor(lit ? Color.rgb(126, 232, 162) : color);
            paint.setStrokeWidth(lit ? 2 : 4);
            paint.setStyle(Paint.Style.STROKE);
            if (node.mode == ChapterMission.RING) {
                c.drawOval(node.x - 29, node.y - 39, node.x + 29, node.y + 39, paint);
                c.drawOval(node.x - 23, node.y - 33, node.x + 23, node.y + 33, paint);
            } else if (node.mode == ChapterMission.SPEED) {
                hudText(c, "ϟ", node.x - 8, node.y - 9, 28, lit ? Color.rgb(126, 232, 162) : color);
                c.drawLine(node.x - 22, node.y - 38, node.x - 22, node.y + 24, paint);
                c.drawLine(node.x + 22, node.y - 38, node.x + 22, node.y + 24, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            if (node.mode != ChapterMission.RING
                    && node.mode != ChapterMission.SPEED
                    && node.mode != ChapterMission.CARGO
                    && node.mode != ChapterMission.CAPTAIN) {
                if (node.mode == ChapterMission.CRYSTAL) {
                    Path crystal = new Path();
                    crystal.moveTo(node.x, node.y - 27);
                    crystal.lineTo(node.x + 17, node.y);
                    crystal.lineTo(node.x, node.y + 26);
                    crystal.lineTo(node.x - 17, node.y);
                    crystal.close();
                    c.drawPath(crystal, paint);
                    paint.setColor(Color.argb(180, 240, 255, 255));
                    c.drawCircle(node.x - 3, node.y - 5, 4, paint);
                } else {
                    drawDevice(
                            c,
                            lit ? 1 : node.mode == ChapterMission.PORTAL ? 3 : 0,
                            node.x,
                            node.y + 24,
                            .72f);
                }
            }
            hudText(
                    c,
                    lit ? "✓" : String.valueOf(i + 1),
                    node.x - 5,
                    node.y - 45,
                    16,
                    lit ? Color.rgb(126, 232, 162) : Color.WHITE);
            if (Math.abs(world.hero.x + 15 - node.x) < 120 && !lit) {
                String label =
                        node.mode == ChapterMission.RING
                                ? "УДЕРЖИВАЙ L2 / H"
                                : node.mode == ChapterMission.PERCH
                                        ? "ПРИЗЕМЛИСЬ И ПОСТОЙ"
                                        : node.mode == ChapterMission.KEY
                                                ? (i == world.mission.count()
                                                        ? "ЗАБЕРИ КЛЮЧ " + (i + 1)
                                                        : "СНАЧАЛА КЛЮЧ "
                                                                + (world.mission.count() + 1))
                                                : node.mode == ChapterMission.CRYSTAL
                                                        ? "СТРЕЛЯЙ X / Z"
                                                        : node.mode == ChapterMission.PORTAL
                                                                ? "↓ / E · ЗЕРКАЛО"
                                                                : node.mode == ChapterMission.SAND
                                                                        ? (world.mission.calm(
                                                                                        world.clock
                                                                                                - world.levelStartedAt)
                                                                                ? "ЗАТИШЬЕ ·"
                                                                                        + " КОСНИСЬ"
                                                                                : "БУРЯ · ПОДОЖДИ")
                                                                        : node.mode
                                                                                        == ChapterMission
                                                                                                .SPEED
                                                                                ? "РАЗГОНИСЬ →"
                                                                                : node.mode
                                                                                                == ChapterMission
                                                                                                        .RESCUE
                                                                                        ? "СТОЙ У"
                                                                                                + " ПУЛЬТА"
                                                                                                + " 1 СЕК."
                                                                                        : "КАПИТАН";
                hudText(c, label, node.x - 78, node.y - 65, 11, Color.WHITE);
                if ((node.mode == ChapterMission.PERCH || node.mode == ChapterMission.RESCUE)
                        && world.mission.hold > 0) {
                    paint.setColor(color);
                    c.drawRect(
                            node.x - 28,
                            node.y + 31,
                            node.x - 28 + 56 * world.mission.hold,
                            node.y + 35,
                            paint);
                }
            }
        }
        if (world.mission.kind == LevelDefinition.MissionKind.ESCORT) {
            float x = world.mission.cartX, y = world.mission.cartFloorY;
            drawDevice(c, 4, x, y - 6, 1.2f);
            paint.setColor(Color.rgb(56, 76, 89));
            c.drawRoundRect(x - 27, y - 25, x + 27, y - 9, 5, 5, paint);
            paint.setColor(color);
            c.drawCircle(x - 18, y - 7, 7, paint);
            c.drawCircle(x + 18, y - 7, 7, paint);
            hudText(
                    c,
                    "ГРУЗ · "
                            + Math.round(
                                    (x - world.mission.cartStartX)
                                            / (world.mission.nodes[0].x - world.mission.cartStartX)
                                            * 100)
                            + "%",
                    x - 46,
                    y - 88,
                    13,
                    Color.WHITE);
            if (Math.abs(world.hero.x - x) < 180) {
                hudText(c, "ИДИ РЯДОМ →", x - 48, y - 64, 12, color);
            }
        }
    }

    private final java.util.Random visualRandom = new java.util.Random(23);

    private final GameWorld world;

    private final GameAssets assets;

    private final SurfaceHolder holder;

    volatile boolean running;

    GameRenderer(GameWorld world, GameAssets assets, SurfaceHolder holder) {
        this.world = world;
        this.assets = assets;
        this.holder = holder;
        font.setTypeface(Typeface.create("sans", Typeface.BOLD));
        paint.setFilterBitmap(false);
        ColorMatrix atmosphere = new ColorMatrix();
        atmosphere.setSaturation(.56f);
        ColorMatrix distance =
                new ColorMatrix(
                        new float[] {
                            .82f, 0, 0, 0, 0, 0, .82f, 0, 0, 2, 0, 0, .86f, 0, 5, 0, 0, 0, 1, 0
                        });
        atmosphere.postConcat(distance);
        backgroundPaint.setColorFilter(new ColorMatrixColorFilter(atmosphere));
        ColorMatrix material = new ColorMatrix();
        material.setSaturation(.83f);
        terrainPaint.setColorFilter(new ColorMatrixColorFilter(material));
        outlinePaint.setColor(Color.rgb(4, 15, 27));
    }
}
