package com.neonhare.game;

import android.graphics.Color;

import com.neonhare.game.GameWorld.Enemy;
import com.neonhare.game.GameWorld.Shot;

import java.util.Iterator;

final class EnemySystem {

    private final GameWorld world;

    EnemySystem(GameWorld world) {
        this.world = world;
    }

    void stepEnemy(Enemy enemy, float dt) {
        int pieces =
                Math.max(
                        1,
                        (int)
                                Math.ceil(
                                        Math.max(Math.abs(enemy.vx * dt), Math.abs(enemy.vy * dt))
                                                / 6));
        float dx = enemy.vx * dt / pieces, dy = enemy.vy * dt / pieces;
        for (int i = 0; i < pieces; i++) {
            float next = enemy.x + dx;
            if (world.physicalAt(next + (dx > 0 ? 34 : 0), enemy.y + 19)) {
                enemy.dir *= -1;
                enemy.vx = 0;
                dx = 0;
            } else {
                enemy.x = world.clamp(next, 0, world.widthInTiles * world.TILE_SIZE - 35);
            }
            float feet = enemy.y + 40;
            enemy.y += dy;
            if (dy > 0) {
                int first = Math.max(0, (int) Math.ceil((feet - .02f) / world.TILE_SIZE)),
                        lastRow =
                                Math.min(
                                        world.heightInTiles - 1,
                                        (int) Math.floor((enemy.y + 40) / world.TILE_SIZE));
                int col =
                        Math.max(
                                0,
                                Math.min(
                                        world.widthInTiles - 1,
                                        (int) ((enemy.x + 17) / world.TILE_SIZE)));
                for (int row = first; row <= lastRow; row++) {
                    if (world.wall[row][col]) {
                        enemy.y = row * world.TILE_SIZE - 40 - .01f;
                        enemy.vy = 0;
                        dy = 0;
                        break;
                    }
                }
            } else if (dy < 0
                    && (world.physicalAt(enemy.x + 5, enemy.y)
                            || world.physicalAt(enemy.x + 29, enemy.y))) {
                enemy.y -= dy;
                enemy.vy = 0;
                dy = 0;
            }
        }
        enemy.ground =
                world.solid(enemy.x + 6, enemy.y + 40.6f)
                        || world.solid(enemy.x + 28, enemy.y + 40.6f);
        if (enemy.y > world.heightInTiles * world.TILE_SIZE + 180) {
            enemy.dead = true;
        }
    }

    void enemyFire(Enemy enemy, int count) {
        float x = enemy.x + 17,
                y = enemy.y + 10,
                angle = (float) Math.atan2(world.hero.y + 20 - y, world.hero.x + 15 - x);
        for (int i = 0; i < count; i++) {
            float a = angle + (i - (count - 1) / 2f) * .2f;
            world.hostileShots.add(
                    new Shot(x, y, (float) Math.cos(a) * 230, (float) Math.sin(a) * 230));
        }
    }

    boolean enemyHittable(Enemy enemy) {
        return enemy.kind != 12 || enemy.mode != 0;
    }

    void updateEnemies(float dt) {
        for (Enemy enemy : world.enemies) {
            if (enemy.dead || Math.abs(enemy.x - world.hero.x) > world.viewWidth * 1.65f) {
                continue;
            }
            enemy.phase += dt;
            enemy.slow = Math.max(0, enemy.slow - dt);
            enemy.attack = Math.max(0, enemy.attack - dt);
            enemy.modeTime -= dt;
            float speed = enemy.slow > 0 ? .42f : 1, distance = world.hero.x - enemy.x;
            int toward = distance < 0 ? -1 : 1;
            if (enemy.kind == 2 || enemy.kind == 9) {
                if (enemy.kind == 2
                        && enemy.mode == 0
                        && enemy.attack <= 0
                        && Math.abs(distance) < 260) {
                    float dy = world.hero.y - enemy.y,
                            len = Math.max(1, (float) Math.sqrt(distance * distance + dy * dy));
                    enemy.vx = distance / len * 210;
                    enemy.vy = dy / len * 210;
                    enemy.mode = 1;
                    enemy.modeTime = .65f;
                }
                if (enemy.kind == 2 && enemy.mode == 1) {
                    enemy.x += enemy.vx * dt * speed;
                    enemy.y += enemy.vy * dt * speed;
                    if (enemy.modeTime <= 0) {
                        enemy.mode = 0;
                        enemy.attack = 2.2f;
                    }
                } else {
                    enemy.x += toward * (Math.abs(distance) < 350 ? 35 : 0) * dt * speed;
                    enemy.y +=
                            (enemy.homeY + (float) Math.sin(enemy.phase * 3) * 13 - enemy.y)
                                    * Math.min(1, dt * 5);
                }
                if (enemy.kind == 9) {
                    if (enemy.mode == 0 && enemy.attack <= 0 && Math.abs(distance) < 450) {
                        enemy.mode = 1;
                        enemy.modeTime = .45f;
                    } else if (enemy.mode == 1 && enemy.modeTime <= 0) {
                        enemyFire(enemy, 1);
                        enemy.mode = 0;
                        enemy.attack = 2.2f;
                    }
                }
            } else {
                enemy.vy = Math.min(enemy.vy + 1500 * dt, 800);
                if (enemy.kind == 11) {
                    enemy.vx = 0;
                    if (enemy.mode == 0 && enemy.attack <= 0 && Math.abs(distance) < 440) {
                        enemy.mode = 1;
                        enemy.modeTime = .55f;
                    } else if (enemy.mode == 1 && enemy.modeTime <= 0) {
                        enemyFire(enemy, 3);
                        enemy.mode = 0;
                        enemy.attack = 2.5f;
                    }
                } else if (enemy.kind == 10) {
                    if (enemy.ground && enemy.attack <= 0 && Math.abs(distance) < 400) {
                        enemy.dir = toward;
                        enemy.vy = -580;
                        enemy.vx = toward * 185 * speed;
                        enemy.attack = 1.5f;
                        enemy.ground = false;
                    } else if (enemy.ground) {
                        enemy.vx = 0;
                    }
                } else if (enemy.kind == 12) {
                    if (enemy.mode == 0) {
                        enemy.vx = enemy.vy = 0;
                        enemy.y = enemy.homeY;
                        if (Math.abs(distance) < 190) {
                            enemy.mode = 1;
                            enemy.modeTime = .4f;
                            enemy.dir = toward;
                        }
                    } else if (enemy.mode == 1 && enemy.modeTime <= 0) {
                        enemy.mode = 2;
                        enemy.modeTime = .85f;
                    } else if (enemy.mode == 2) {
                        enemy.vx = enemy.dir * 205 * speed;
                        if (enemy.modeTime <= 0) {
                            enemy.mode = 3;
                            enemy.modeTime = 1;
                            enemy.vx = 0;
                        }
                    } else if (enemy.mode == 3 && enemy.modeTime <= 0) {
                        enemy.mode = 0;
                        enemy.homeY = enemy.y;
                    }
                } else if (enemy.kind == 1 || enemy.kind == 8) {
                    if (enemy.mode == 0) {
                        enemy.vx = enemy.dir * 65 * speed;
                        if (enemy.attack <= 0 && Math.abs(distance) < 250) {
                            enemy.dir = toward;
                            enemy.mode = 1;
                            enemy.modeTime = .5f;
                            enemy.vx = 0;
                        }
                    } else if (enemy.mode == 1) {
                        enemy.vx = 0;
                        if (enemy.modeTime <= 0) {
                            enemy.mode = 2;
                            enemy.modeTime = .58f;
                        }
                    } else if (enemy.mode == 2) {
                        enemy.vx = enemy.dir * 275 * speed;
                        if (enemy.modeTime <= 0) {
                            enemy.mode = 3;
                            enemy.modeTime = .95f;
                            enemy.vx = 0;
                        }
                    } else {
                        enemy.vx = 0;
                        if (enemy.modeTime <= 0) {
                            enemy.mode = 0;
                            enemy.attack = 1.2f;
                        }
                    }
                } else {
                    enemy.vx = enemy.dir * (enemy.kind == 7 ? 72 : 82) * speed;
                }
                if (enemy.ground
                        && enemy.vx != 0
                        && !world.solid(enemy.x + (enemy.vx > 0 ? 42 : -8), enemy.y + 43)) {
                    enemy.dir *= -1;
                    enemy.vx = -enemy.vx;
                }
                if (enemy.kind != 12 || enemy.mode != 0) {
                    stepEnemy(enemy, dt);
                }
            }
            if (enemyHittable(enemy)
                    && world.hit(world.hero.x, world.hero.y, 30, 42, enemy.x, enemy.y, 34, 40)) {
                if (world.hero.vy > 120 && world.hero.y + 35 < enemy.y + 18) {
                    defeat(enemy);
                    world.hero.vy = -430;
                } else {
                    world.hurt(false, "enemy");
                }
            }
        }
        for (Iterator<Enemy> it = world.enemies.iterator(); it.hasNext(); ) {
            if (it.next().dead) {
                it.remove();
            }
        }
    }

    void updateEncounters(float dt) {
        for (Enemy enemy : world.enemies) {
            if (enemy.kind >= 3 && enemy.kind < 7) {
                int identity = (enemy.kind - 3) % 4;
                if (Math.abs(world.hero.x - enemy.x) < 310 && world.seenVillains.add(identity)) {
                    world.announce(
                            Campaign.BOSS_NAMES[
                                    identity == 0 ? 2 : identity == 1 ? 1 : identity == 2 ? 3 : 4],
                            Campaign.TAUNTS[
                                    identity == 0 ? 2 : identity == 1 ? 1 : identity == 2 ? 3 : 4],
                            5);
                }
                if (Math.abs(world.hero.x - enemy.x) < 520
                        && Math.abs(world.hero.y - enemy.y) < 300
                        && enemy.attack <= 0) {
                    float dx = world.hero.x - enemy.x,
                            dy = world.hero.y - enemy.y,
                            len = Math.max(1, (float) Math.sqrt(dx * dx + dy * dy));
                    world.hostileShots.add(
                            new Shot(enemy.x + 17, enemy.y + 18, dx / len * 250, dy / len * 250));
                    enemy.attack = identity == 0 ? 1.6f : identity == 2 ? 2.4f : 2;
                }
            }
        }
        for (int i = world.hostileShots.size() - 1; i >= 0; i--) {
            Shot shot = world.hostileShots.get(i);
            shot.x += shot.vx * dt;
            shot.y += shot.vy * dt;
            shot.life -= dt;
            if (world.hit(world.hero.x, world.hero.y, 30, 42, shot.x - 6, shot.y - 6, 12, 12)) {
                world.hostileShots.remove(i);
                world.hurt(false, "projectile");
                break;
            }
            if (shot.life <= 0 || world.physicalAt(shot.x, shot.y)) {
                world.hostileShots.remove(i);
            }
        }
    }

    void defeat(Enemy e) {
        if (e.dead) {
            return;
        }
        e.dead = true;
        if (world.mission != null && e.questIndex >= 0) {
            world.mission.captain(e.questIndex);
            world.store();
        }
        world.add((e.kind == 2 ? 400 : 250) * world.combo);
        world.combo = Math.min(8, world.combo + 1);
        world.comboTimer = 2.4f;
        world.burst(e.x + 17, e.y + 20, Color.rgb(255, 106, 84), 18, 280);
        world.shake = 7;
    }
}
