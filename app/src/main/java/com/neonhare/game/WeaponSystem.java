package com.neonhare.game;

import android.graphics.Color;
import android.graphics.RectF;

import com.neonhare.game.GameWorld.Bullet;
import com.neonhare.game.GameWorld.CacheWall;
import com.neonhare.game.GameWorld.Enemy;
import com.neonhare.game.GameWorld.Relay;

import java.util.Iterator;

final class WeaponSystem {

    private final GameWorld world;

    WeaponSystem(GameWorld world) {
        this.world = world;
    }

    void shoot() {
        if (world.ammo[world.gun] <= 0) {
            world.gun = world.BLASTER;
            return;
        }
        if (world.gun != world.BLASTER) {
            world.ammo[world.gun]--;
        }
        PlayerPose pose = world.playerPose();
        float x = pose.muzzleX(world.hero), y = pose.muzzleY(world.hero);
        boolean obstructed = false;
        // A long visible barrel must not place a new projectile beyond an adjacent wall.
        for (float probe = world.hero.x + 15;
                world.hero.face * (x - probe) >= 0;
                probe += world.hero.face) {
            if (world.physicalAt(probe, y)) {
                x = probe;
                obstructed = true;
                break;
            }
        }
        int first = world.bullets.size(), power = 1 + world.progress.ranks[1] / 2;
        if (world.gun == world.BLASTER) {
            world.bullets.add(new Bullet(x, y, world.hero.face * 760, 0, 16, power));
            world.hero.cool = .22f;
        } else if (world.gun == world.PULSE) {
            world.bullets.add(
                    new Bullet(
                            x,
                            y,
                            world.hero.face * 900,
                            world.rng.nextFloat() * 28 - 14,
                            11,
                            power));
            world.hero.cool = .09f;
        } else if (world.gun == world.SCATTER) {
            for (int i = -2; i <= 2; i++) {
                world.bullets.add(
                        new Bullet(
                                x,
                                y,
                                world.hero.face * (650 - Math.abs(i) * 25),
                                i * 95,
                                9,
                                2 + power - 1));
            }
            world.hero.cool = .55f;
            world.hero.vx -= world.hero.face * 35;
        } else if (world.gun == world.FROST) {
            world.bullets.add(new Bullet(x, y, world.hero.face * 620, 0, 19, power + 1));
            world.hero.cool = .34f;
        } else {
            world.bullets.add(new Bullet(x, y, world.hero.face * 850, -25, 12, power));
            world.hero.cool = .19f;
        }
        for (int i = first; i < world.bullets.size(); i++) {
            Bullet shot = world.bullets.get(i);
            shot.obstructedAtSpawn = obstructed;
            shot.weapon = world.gun;
            shot.bounces = world.gun == world.RICOCHET ? 2 : 0;
        }
        world.hero.cool *= 1 - world.progress.ranks[1] * .08f;
        world.burst(x, y, gunColor(), 4, 90);
    }

    void updateBullets(float dt) {
        for (Bullet b : world.bullets) {
            if (!b.obstructedAtSpawn) {
                b.x += b.vx * dt;
                b.y += b.vy * dt;
            }
            b.obstructedAtSpawn = false;
            b.life -= dt;
            for (Enemy e : world.enemies) {
                if (!e.dead
                        && world.enemyHittable(e)
                        && b.life > 0
                        && world.hit(b.left(), b.top(), b.size, b.size, e.x, e.y, 34, 40)) {
                    if (e.kind == 8 && e.mode != 3 && Math.signum(b.vx) == -e.dir) {
                        b.life = 0;
                        world.burst(b.x, b.y, Color.rgb(255, 194, 110), 4, 90);
                        continue;
                    }
                    e.hp -= b.damage;
                    if (b.weapon == world.FROST) {
                        e.slow = 2.5f;
                    }
                    b.life = 0;
                    world.burst(b.x, b.y, gunColor(), 7, 180);
                    if (e.hp <= 0) {
                        world.defeat(e);
                    }
                }
            }
        }
        for (Bullet b : world.bullets) {
            if (b.life > 0) {
                if (world.mission != null && world.mission.shoot(b.left(), b.top(), b.size)) {
                    b.life = 0;
                    world.add(1000);
                    world.store();
                    continue;
                }
                for (Relay relay : world.relays) {
                    if (!relay.active
                            && world.hit(
                                    b.left(),
                                    b.top(),
                                    b.size,
                                    b.size,
                                    relay.x - 18,
                                    relay.y - 18,
                                    36,
                                    36)) {
                        world.activateRelay(relay);
                        b.life = 0;
                        break;
                    }
                }
                for (CacheWall cache : world.cacheWalls) {
                    if (!cache.broken
                            && world.hit(
                                    b.left(),
                                    b.top(),
                                    b.size,
                                    b.size,
                                    cache.x * world.TILE_SIZE,
                                    cache.y * world.TILE_SIZE,
                                    world.TILE_SIZE,
                                    world.TILE_SIZE)) {
                        cache.broken = true;
                        world.wall[cache.y][cache.x] = false;
                        b.life = 0;
                        world.add(150);
                        world.secrets++;
                        world.burst(
                                cache.x * world.TILE_SIZE + 18,
                                cache.y * world.TILE_SIZE + 18,
                                Color.rgb(125, 205, 151),
                                14,
                                180);
                        world.announce(
                                "СКРЫТЫЙ ПРОХОД", "Трещины в кладке можно разбить выстрелами.", 3);
                        break;
                    }
                }
                if (world.boss.active
                        && !world.boss.defeated
                        && b.life > 0
                        && RectF.intersects(
                                world.bossHurtBox(),
                                new RectF(
                                        b.left(), b.top(), b.left() + b.size, b.top() + b.size))) {
                    world.damageBoss(b.damage);
                    b.life = 0;
                }
            }
        }
        for (Iterator<Bullet> it = world.bullets.iterator(); it.hasNext(); ) {
            Bullet b = it.next();
            if (b.life <= 0) {
                it.remove();
                continue;
            }
            if (world.physicalAt(b.x, b.y)) {
                if (b.bounces > 0) {
                    b.x -= b.vx * dt;
                    b.y -= b.vy * dt;
                    b.vx = -b.vx;
                    b.vy = -b.vy;
                    b.bounces--;
                    world.burst(b.x, b.y, Color.rgb(255, 187, 90), 4, 100);
                } else {
                    it.remove();
                }
            }
        }
    }

    int gunColor() {
        return weaponColor(world.gun);
    }

    int weaponColor(int weapon) {
        return weapon == world.PULSE
                ? 0xff32ebff
                : weapon == world.SCATTER
                        ? 0xffff695a
                        : weapon == world.FROST
                                ? 0xffaffff1
                                : weapon == world.RICOCHET ? 0xffffaf68 : 0xffffe14b;
    }

    String gunName() {
        return new String[] {"BLASTER", "PULSE", "SCATTER", "FROST", "RICOCHET"}[world.gun];
    }

    void nextGun() {
        for (int i = 0; i < 5; i++) {
            world.gun = (world.gun + 1) % 5;
            if (world.ammo[world.gun] > 0 && (world.progress.weaponMask & (1 << world.gun)) != 0) {
                break;
            }
        }
    }
}
