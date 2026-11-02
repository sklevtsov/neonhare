package com.neonhare.game;

final class PhysicsSystem {

    private final GameWorld world;

    PhysicsSystem(GameWorld world) {
        this.world = world;
    }

    boolean physicalAt(float x, float y) {
        int col = (int) Math.floor(x / world.TILE_SIZE),
                row = (int) Math.floor(y / world.TILE_SIZE);
        return col < 0
                || col >= world.widthInTiles
                || (row >= 0
                        && row < world.heightInTiles
                        && world.wall[row][col]
                        && !world.ledge[row][col]);
    }

    void move(float dx, float dy) {
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 6));
        float sx = dx / n, sy = dy / n;
        world.hero.ground = false;
        for (int i = 0; i < n; i++) {
            world.hero.x += sx;
            if (blocked(world.hero.x, world.hero.y)) {
                int edge = (int) Math.floor((world.hero.x + (sx > 0 ? 27 : 3)) / world.TILE_SIZE);
                world.hero.x =
                        sx > 0
                                ? edge * world.TILE_SIZE - 27 - .01f
                                : (edge + 1) * world.TILE_SIZE - 3 + .01f;
                world.hero.vx = 0;
                sx = 0;
            }
            float oldFeet = world.hero.y + 42;
            world.hero.y += sy;
            if (sy > 0) {
                int first = Math.max(0, (int) Math.ceil((oldFeet - .02f) / world.TILE_SIZE)),
                        lastRow =
                                Math.min(
                                        world.heightInTiles - 1,
                                        (int) Math.floor((world.hero.y + 42) / world.TILE_SIZE));
                int leftTile = Math.max(0, (int) Math.floor((world.hero.x + 3) / world.TILE_SIZE)),
                        rightTile =
                                Math.min(
                                        world.widthInTiles - 1,
                                        (int) Math.floor((world.hero.x + 27) / world.TILE_SIZE));
                boolean landed = false;
                for (int row = first; row <= lastRow && !landed; row++) {
                    for (int col = leftTile; col <= rightTile; col++) {
                        if (world.ledge[row][col]) {
                            world.hero.y = row * world.TILE_SIZE - 42 - .01f;
                            world.hero.vy = 0;
                            world.hero.jumps = 0;
                            world.hero.ground = true;
                            sy = 0;
                            landed = true;
                            break;
                        }
                    }
                }
            }
            if (blocked(world.hero.x, world.hero.y)) {
                int edge = (int) Math.floor((world.hero.y + (sy > 0 ? 42 : 2)) / world.TILE_SIZE);
                world.hero.y =
                        sy > 0
                                ? edge * world.TILE_SIZE - 42 - .01f
                                : (edge + 1) * world.TILE_SIZE - 2 + .01f;
                if (sy > 0) {
                    world.hero.ground = true;
                    world.hero.jumps = 0;
                }
                world.hero.vy = 0;
                sy = 0;
            }
        }
        world.hero.ground = blocked(world.hero.x, world.hero.y + .6f) || onLedge();
        if (world.hero.ground) {
            world.hero.jumps = 0;
        }
        world.hero.x = world.clamp(world.hero.x, 0, world.widthInTiles * world.TILE_SIZE - 30);
        if (!world.gateOpen) {
            world.hero.x =
                    Math.min(
                            world.hero.x,
                            world.definition.gateColumn * world.TILE_SIZE - 27 - .01f);
        }
        if (world.boss.active && !world.boss.defeated) {
            world.hero.x = Math.max(world.hero.x, 138 * world.TILE_SIZE);
        }
        if (Campaign.boss(world.levelIndex) >= 0 && !world.boss.defeated) {
            world.hero.x =
                    Math.min(world.hero.x, world.definition.exitColumn * world.TILE_SIZE - 1);
        }
    }

    float spawnFloor(float x) {
        int col = Math.max(0, Math.min(world.widthInTiles - 1, (int) (x / world.TILE_SIZE)));
        for (int row = 0; row < world.heightInTiles; row++) {
            if (world.wall[row][col] && !world.ledge[row][col]) {
                return row * world.TILE_SIZE;
            }
        }
        return 13 * world.TILE_SIZE;
    }

    boolean onLedge() {
        float feet = world.hero.y + 42;
        int row = Math.round(feet / world.TILE_SIZE);
        if (row < 0
                || row >= world.heightInTiles
                || Math.abs(feet - row * world.TILE_SIZE) > .65f
                || world.hero.vy < 0) {
            return false;
        }
        for (int col = Math.max(0, (int) Math.floor((world.hero.x + 3) / world.TILE_SIZE));
                col
                        <= Math.min(
                                world.widthInTiles - 1,
                                (int) Math.floor((world.hero.x + 27) / world.TILE_SIZE));
                col++) {
            if (world.ledge[row][col]) {
                return true;
            }
        }
        return false;
    }

    boolean blocked(float x, float y) {
        int x0 = (int) Math.floor((x + 3) / world.TILE_SIZE),
                x1 = (int) Math.floor((x + 27) / world.TILE_SIZE);
        int y0 = (int) Math.floor((y + 2) / world.TILE_SIZE),
                y1 = (int) Math.floor((y + 42) / world.TILE_SIZE);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (tx < 0
                        || tx >= world.widthInTiles
                        || (ty >= 0
                                && ty < world.heightInTiles
                                && world.wall[ty][tx]
                                && !world.ledge[ty][tx])) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean solid(float x, float y) {
        int tx = (int) Math.floor(x / world.TILE_SIZE), ty = (int) Math.floor(y / world.TILE_SIZE);
        if (tx < 0 || tx >= world.widthInTiles) {
            return true;
        }
        if (ty < 0 || ty >= world.heightInTiles) {
            return false;
        }
        return world.wall[ty][tx];
    }
}
