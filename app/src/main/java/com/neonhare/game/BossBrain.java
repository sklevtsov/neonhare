package com.neonhare.game;

/** Arena movement and attack timing, independent of rendering and Android input. */
final class BossBrain {

    static final int SEEK = 0,
            WINDUP = 1,
            CHARGE = 2,
            RECOVER = 3,
            SLAM = 4,
            LEAP = 5,
            VOLLEY = 6,
            MIRROR = 7;
    static final int DASH = 0, POUNCE = 1, GROUND_SLAM = 2, FAN = 3, TELEPORT = 4;
    static final int WAVE_EVENT = 1, FAN_EVENT = 2, IMPACT_EVENT = 4;
    private static final float FLOOR = 468, LEFT = 139 * 36, RIGHT = 164 * 36 - 100;
    float x, y, vx, vy, timer, flash, stride, targetX, teleportX, time;
    int hp, maxHp, type, state, cycle, dir, plan, phase;
    boolean active, defeated;

    void reset(int skin, int chapter) {
        x = 153 * 36;
        y = FLOOR - 110;
        vx = vy = timer = flash = stride = time = 0;
        type = Math.max(0, skin);
        maxHp = type == 0 ? 48 : 48 + chapter * 4;
        hp = maxHp;
        state = SEEK;
        cycle = phase = 0;
        dir = -1;
        plan = DASH;
        active = defeated = false;
    }

    void retryPosition() {
        x = 153 * 36;
        y = FLOOR - 110;
        vx = vy = 0;
        state = SEEK;
        timer = .8f;
        active = false;
    }

    void activate(float playerX) {
        active = true;
        state = SEEK;
        timer = .8f;
        dir = playerX < x ? -1 : 1;
    }

    void beginWindup(float playerX, float playerVx) {
        int[][] patterns = {
            {DASH, POUNCE, GROUND_SLAM},
            {POUNCE, FAN, GROUND_SLAM},
            {FAN, DASH, POUNCE},
            {TELEPORT, FAN, DASH},
            {GROUND_SLAM, FAN, POUNCE, FAN}
        };
        int[] sequence = patterns[type];
        plan = sequence[cycle % sequence.length];
        targetX = clamp(playerX + 15 + playerVx * .22f, LEFT + 15, RIGHT + 70);
        dir = targetX < x + 48 ? -1 : 1;
        teleportX = clamp(playerX - dir * 235, LEFT, RIGHT);
        state = WINDUP;
        timer = (phase == 1 ? .53f : .75f) + (plan == GROUND_SLAM ? .12f : 0);
        vx = 0;
    }

    int step(float dt, float playerX, float playerVx) {
        if (!active || defeated) {
            return 0;
        }
        int event = 0;
        time += dt;
        flash = Math.max(0, flash - dt);
        phase = hp <= maxHp / 2 ? 1 : 0;
        timer -= dt;
        if (state == SEEK) {
            float distance = playerX + 15 - (x + 48), range = type == 2 || type == 4 ? 290 : 165;
            dir = distance < 0 ? -1 : 1;
            vx =
                    Math.abs(distance) > range
                            ? dir * (phase == 1 ? 210 : 170)
                            : Math.abs(distance) < range - 65 ? -dir * 105 : 0;
            x = clamp(x + vx * dt, LEFT, RIGHT);
            y = FLOOR - 110 - (type == 1 ? 8 + 6 * (float) Math.sin(time * 4) : 0);
            if (timer <= 0) {
                beginWindup(playerX, playerVx);
            }
        } else if (state == WINDUP && timer <= 0) {
            if (plan == DASH) {
                state = CHARGE;
                vx = dir * (phase == 1 ? 850 : 710);
                timer = clamp(Math.abs(targetX - x - 48) / Math.abs(vx) + .18f, .38f, 1.0f);
            } else if (plan == POUNCE) {
                state = LEAP;
                vx = clamp((targetX - x - 48) / .88f, -550, 550);
                vy = -720;
                timer = 1.6f;
            } else if (plan == GROUND_SLAM) {
                state = SLAM;
                timer = .25f;
                event |= WAVE_EVENT | IMPACT_EVENT;
            } else if (plan == FAN) {
                state = VOLLEY;
                timer = .62f;
                vx = type == 2 ? -dir * 135 : 0;
                event |= FAN_EVENT;
            } else {
                state = MIRROR;
                timer = .32f;
                vx = 0;
            }
        } else if (state == CHARGE) {
            float before = x;
            x = clamp(x + vx * dt, LEFT, RIGHT);
            y = FLOOR - 110;
            if (timer <= 0 || Math.abs(x - before) < .01f) {
                event |= IMPACT_EVENT;
                recover();
            }
        } else if (state == LEAP) {
            vy += 1650 * dt;
            x = clamp(x + vx * dt, LEFT, RIGHT);
            y += vy * dt;
            if ((vy > 0 && y + 110 >= FLOOR) || timer <= 0) {
                y = FLOOR - 110;
                event |= WAVE_EVENT | IMPACT_EVENT;
                recover();
            }
        } else if (state == VOLLEY) {
            x = clamp(x + vx * dt, LEFT, RIGHT);
            if (timer <= 0) {
                recover();
            }
        } else if (state == MIRROR) {
            if (timer <= 0) {
                x = teleportX;
                dir = playerX < x ? -1 : 1;
                event |= FAN_EVENT;
                recover();
            }
        } else if (state == SLAM) {
            if (timer <= 0) {
                recover();
            }
        } else if (state == RECOVER) {
            y = FLOOR - 110;
            if (timer <= 0) {
                state = SEEK;
                timer = .38f;
            }
        }
        stride += Math.abs(vx) * dt * .035f;
        return event;
    }

    private void recover() {
        state = RECOVER;
        timer = phase == 1 ? .86f : 1.18f;
        vx = vy = 0;
        cycle++;
    }

    int takeHit(int damage) {
        if (!active || defeated) {
            return 0;
        }
        int actual = Math.max(1, damage) * (state == RECOVER ? 2 : 1);
        hp = Math.max(0, hp - actual);
        flash = .12f;
        if (hp == 0) {
            defeated = true;
            active = false;
            vx = vy = 0;
        }
        return actual;
    }

    String cue() {
        if (state == RECOVER) {
            return "ПРОМАХ · УРОН ×2";
        }
        if (state == SEEK) {
            return "СБЛИЖАЕТСЯ";
        }
        if (state == WINDUP) {
            return plan == DASH
                    ? "РЫВОК · ПЕРЕПРЫГНИ"
                    : plan == POUNCE
                            ? "ПРЫЖОК · УЙДИ С МЕТКИ"
                            : plan == GROUND_SLAM
                                    ? "ВОЛНА · ПРЫГАЙ"
                                    : plan == FAN
                                            ? "ЗАЛП · СМЕНИ ВЫСОТУ"
                                            : "ПЕРЕНОС · СЛЕДИ ЗА ТЕНЬЮ";
        }
        return state == LEAP
                ? "ПРИЗЕМЛЕНИЕ"
                : state == CHARGE ? "РЫВОК" : state == VOLLEY ? "ЗАЛП" : "АТАКА";
    }

    private static float clamp(float x, float a, float b) {
        return Math.max(a, Math.min(b, x));
    }
}
