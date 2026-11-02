package com.neonhare.game;

import java.util.Arrays;

/** Real shots in both directions and every pose, including a barrel obstructed by masonry. */
final class WeaponOriginChecks {
    int verify(GameView view) {
        GameWorld world = view.world;
        world.restart();
        world.enemies.clear();
        for (boolean[] row : world.wall) {
            Arrays.fill(row, false);
        }
        world.hero.x = 200;
        world.hero.y = 150;
        int checks = 0;
        for (int pose = 0; pose < 10; pose++) {
            world.gliding = pose == 9;
            world.hero.ground = pose < 9;
            world.hero.vx = pose < 8 ? 100 : 0;
            world.hero.stride = pose < 8 ? (pose + .1f) / 1.3f : 0;
            PlayerPose attachment = world.playerPose();
            require(
                    attachment.sprite
                            == (pose < 8
                                    ? view.assets.runFrames[pose]
                                    : pose == 9 ? view.assets.heliSprite : view.assets.nova),
                    "Rendered pose");
            world.hero.face = 1;
            float right = attachment.muzzleX(world.hero);
            for (int face : new int[] {-1, 1}) {
                world.hero.face = face;
                float x = attachment.muzzleX(world.hero), y = attachment.muzzleY(world.hero);
                require(
                        Math.abs(x - 215) > 25 && y < 153,
                        "Muzzle is at the barrel, above body center");
                require(Math.abs((x - 215) - face * (right - 215)) < .001f, "Mirror attachment");
                for (int weapon = 0; weapon < 5; weapon++) {
                    world.bullets.clear();
                    world.gun = weapon;
                    world.ammo[weapon] = 20;
                    world.shoot();
                    require(
                            world.bullets.size() == (weapon == GameWorld.SCATTER ? 5 : 1),
                            "Shot count");
                    for (GameWorld.Bullet bullet : world.bullets) {
                        require(
                                Math.abs(bullet.x - x) < .001f && Math.abs(bullet.y - y) < .001f,
                                "Every projectile starts at the displayed muzzle");
                        require(Math.signum(bullet.vx) == face, "Shot follows facing");
                        checks += 2;
                    }
                    world.hero.vx = pose < 8 ? 100 : 0;
                    checks++;
                }
                checks += 2;
            }
            checks++;
        }
        world.gliding = false;
        world.hero.vx = 0;
        world.hero.face = 1;
        require(
                Math.abs(world.playerPose().muzzleX(world.hero) - 252.474f) < .01f
                        && Math.abs(world.playerPose().muzzleY(world.hero) - 150.678f) < .01f,
                "Idle attachment matches the approved sprite barrel");
        world.hero.y = 180;
        world.wall[5][7] = true;
        world.bullets.clear();
        world.gun = GameWorld.BLASTER;
        world.shoot();
        require(world.bullets.get(0).x <= 252, "Adjacent wall clips the muzzle path");
        world.updateBullets(.05f);
        require(world.bullets.isEmpty(), "A shot cannot emerge behind an adjacent wall");
        world.wall[5][7] = false;
        world.bullets.clear();
        world.shoot();
        GameWorld.Bullet shot = world.bullets.get(0);
        GameWorld.Enemy target = new GameWorld.Enemy(shot.x - 40, shot.y - 20, 0);
        world.enemies.add(target);
        world.updateBullets(0);
        require(target.dead, "The visible left edge of a round also registers a hit");
        world.restart();
        return checks + 4;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
