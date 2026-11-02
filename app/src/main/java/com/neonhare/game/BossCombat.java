package com.neonhare.game;

import android.graphics.Color;

import com.neonhare.game.GameWorld.Shot;
import com.neonhare.game.GameWorld.Wave;

final class BossCombat {

    private final GameWorld world;

    BossCombat(GameWorld world) {
        this.world = world;
    }

    void beginBossWindup() {
        world.boss.beginWindup(world.hero.x, world.hero.vx);
    }

    void updateBoss(float dt) {
        if (Campaign.boss(world.levelIndex) < 0 || world.gameOver || world.bossLives <= 0) {
            return;
        }
        if (!world.boss.active
                && !world.boss.defeated
                && world.gateOpen
                && world.hero.x > 138 * world.TILE_SIZE
                && world.hero.y < 520) {
            world.encounterStarted = true;
            world.boss.activate(world.hero.x);
            world.announce(
                    Campaign.BOSS_NAMES[world.boss.type],
                    "Z — стреляй. После атаки урон ×2. " + Campaign.TAUNTS[world.boss.type],
                    4);
        }
        int events = world.boss.step(dt, world.hero.x, world.hero.vx);
        if ((events & BossBrain.WAVE_EVENT) != 0) {
            world.waves.add(new Wave(world.boss.x + 48, 13 * world.TILE_SIZE - 18, -390));
            world.waves.add(new Wave(world.boss.x + 48, 13 * world.TILE_SIZE - 18, 390));
        }
        if ((events & BossBrain.FAN_EVENT) != 0) {
            bossVolley();
        }
        if ((events & BossBrain.IMPACT_EVENT) != 0) {
            world.shake = 7;
            world.burst(
                    world.boss.x + 48, 13 * world.TILE_SIZE - 5, Color.rgb(255, 155, 101), 14, 170);
        }
        if (world.boss.active
                && !world.boss.defeated
                && world.hit(
                        world.hero.x,
                        world.hero.y,
                        30,
                        42,
                        world.boss.x + 7,
                        world.boss.y + 10,
                        82,
                        100)) {
            world.hurt(false, "boss-contact");
        }
        for (int i = world.waves.size() - 1; i >= 0; i--) {
            Wave wave = world.waves.get(i);
            wave.x += wave.vx * dt;
            wave.life -= dt;
            if (world.hit(world.hero.x, world.hero.y, 30, 42, wave.x - 14, wave.y - 16, 28, 32)) {
                world.hurt(false, "boss-wave");
                break;
            }
            if (wave.life <= 0
                    || wave.x < 138 * world.TILE_SIZE
                    || wave.x > world.widthInTiles * world.TILE_SIZE) {
                world.waves.remove(i);
            }
        }
    }

    void bossVolley() {
        float originX = world.boss.x + 48,
                originY = world.boss.y + 35,
                dx = world.hero.x + 15 - originX,
                dy = world.hero.y + 20 - originY;
        float angle = (float) Math.atan2(dy, dx);
        int count = world.boss.type == 4 ? 5 : 3;
        for (int i = 0; i < count; i++) {
            float a = angle + (i - (count - 1) / 2f) * .24f;
            world.hostileShots.add(
                    new Shot(
                            originX,
                            originY,
                            (float) Math.cos(a) * 285,
                            (float) Math.sin(a) * 285));
        }
    }

    void damageBoss(int damage) {
        if (!world.boss.active || world.boss.defeated) {
            return;
        }
        world.boss.takeHit(damage);
        world.burst(world.boss.x + 48, world.boss.y + 35, Color.rgb(255, 209, 108), 6, 140);
        if (world.boss.hp == 0) {
            world.boss.defeated = true;
            world.boss.active = false;
            world.waves.clear();
            world.add(12000);
            world.hero.inv = 3;
            if (world.boss.type == 0) {
                world.progress.befriendLion();
                world.announce(
                        "ЛЕВ СТАНОВИТСЯ СОЮЗНИКОМ",
                        "«Я помогу исправить то, что сделал. Возьми аэропривод: твои уши поднимут"
                                + " тебя в небо».",
                        10);
            } else {
                world.announce(
                        Campaign.BOSS_NAMES[world.boss.type] + " ОТСТУПАЕТ",
                        world.boss.type == 4
                                ? "Лев вывел людей из цитадели. Лес и поселение снова получают"
                                        + " воду."
                                : "Открой следующий маршрут. Лев прикроет твой отход.",
                        7);
            }
            world.store();
        }
    }
}
