package com.neonhare.game;

import android.graphics.Bitmap;

/** Sprite-local attachments shared by rendering and shooting; coordinates are mirrored together. */
final class PlayerPose {
    final Bitmap sprite;
    private final float muzzleX;
    private final float muzzleY;

    PlayerPose(Bitmap sprite, float normalizedMuzzleX, float normalizedMuzzleY) {
        this.sprite = sprite;
        muzzleX = sprite.getWidth() * (normalizedMuzzleX - .5f);
        muzzleY = sprite.getHeight() * (normalizedMuzzleY - 1);
    }

    float muzzleX(GameWorld.Player hero) {
        return hero.x + 15 + hero.face * muzzleX;
    }

    float muzzleY(GameWorld.Player hero) {
        return hero.y + 42 + muzzleY;
    }

    static int runFrame(GameWorld world, GameAssets assets) {
        int frame = (int) (world.hero.stride * 1.3f) % assets.runningPoses.length;
        return frame < 0 ? frame + assets.runningPoses.length : frame;
    }

    static PlayerPose current(GameWorld world, GameAssets assets) {
        if (world.gliding) {
            return assets.flyingPose;
        }
        return world.hero.ground && Math.abs(world.hero.vx) > 30
                ? assets.runningPoses[runFrame(world, assets)]
                : assets.standingPose;
    }
}
