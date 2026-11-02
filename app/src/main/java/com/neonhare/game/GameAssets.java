package com.neonhare.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.IdentityHashMap;
import java.util.Map;

final class GameAssets {

    final Bitmap jungle;

    final Bitmap nova;

    final Bitmap energySeed;

    final Bitmap terrain;

    final Bitmap lion;

    final Bitmap heliSprite;

    final PlayerPose standingPose;
    final PlayerPose flyingPose;
    final PlayerPose[] runningPoses = new PlayerPose[8];

    final Bitmap[] backdrops = new Bitmap[6];

    final Bitmap[] surfaces = new Bitmap[6];

    final Bitmap[] villains = new Bitmap[4];

    final Bitmap[] runFrames = new Bitmap[8];

    final Bitmap[] enemySprites = new Bitmap[3];

    Bitmap[] lionFrames;

    Bitmap[] villainFrames;

    Bitmap[] minionSprites;

    Bitmap[] devices;

    final Map<Bitmap, Rect> spriteBounds = new IdentityHashMap<>();

    Bitmap bossSprite(BossBrain boss) {
        if (boss.type == 0) {
            int pose =
                    boss.defeated || boss.state == BossBrain.RECOVER
                            ? 5
                            : boss.state == BossBrain.WINDUP
                                    ? 2
                                    : boss.state == BossBrain.LEAP
                                            ? 3
                                            : boss.state == BossBrain.CHARGE
                                                            || boss.state == BossBrain.SLAM
                                                    ? 4
                                                    : ((int) boss.stride & 1);
            return lionFrames[pose];
        }
        int column = boss.type == 1 ? 1 : boss.type == 2 ? 0 : boss.type == 3 ? 2 : 3,
                row =
                        boss.state == BossBrain.SEEK
                                ? ((int) boss.stride & 1)
                                : boss.state == BossBrain.RECOVER ? 0 : 2;
        return villainFrames[row * 4 + column];
    }

    RectF bossHurtBox(BossBrain boss) {
        Bitmap image = bossSprite(boss);
        Rect bounds = spriteBounds.get(image);
        if (bounds == null) {
            int w = image.getWidth(),
                    h = image.getHeight(),
                    left = w,
                    top = h,
                    right = 0,
                    bottom = 0;
            int[] pixels = new int[w * h];
            image.getPixels(pixels, 0, w, 0, 0, w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if ((pixels[y * w + x] >>> 24) > 32) {
                        left = Math.min(left, x);
                        right = Math.max(right, x + 1);
                        top = Math.min(top, y);
                        bottom = Math.max(bottom, y + 1);
                    }
                }
            }
            bounds = new Rect(left, top, right, bottom);
            spriteBounds.put(image, bounds);
        }
        float center = boss.x + 48,
                anchor = boss.y + 110 - 166,
                left =
                        boss.dir > 0
                                ? center + image.getWidth() / 2f - bounds.right
                                : center - image.getWidth() / 2f + bounds.left;
        return new RectF(
                left - 3,
                anchor + bounds.top - 3,
                left + bounds.width() + 3,
                anchor + bounds.bottom + 3);
    }

    Bitmap[] gridFrames(int resource, int columns, int rows, int height) {
        Bitmap raw = BitmapFactory.decodeResource(resources, resource);
        int w = raw.getWidth() / columns, h = raw.getHeight() / rows;
        int top = h, bottom = 0;
        Bitmap[] frames = new Bitmap[columns * rows];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = Bitmap.createBitmap(raw, i % columns * w, i / columns * h, w, h);
            int[] pixels = new int[w * h];
            frames[i].getPixels(pixels, 0, w, 0, 0, w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if ((pixels[y * w + x] >>> 24) > 64) {
                        top = Math.min(top, y);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
        }
        if (bottom < top) {
            top = 0;
            bottom = h - 1;
        }
        int region = bottom - top + 1, width = Math.round(w * height / (float) region);
        for (int i = 0; i < frames.length; i++) {
            Bitmap cropped = Bitmap.createBitmap(frames[i], 0, top, w, region);
            Bitmap frame = frames[i];
            frames[i] = scaleAndRecycle(cropped, width, height);
            if (frame != cropped) {
                frame.recycle();
            }
        }
        raw.recycle();
        return frames;
    }

    static Bitmap trimSprite(Bitmap frame, int height) {
        int w = frame.getWidth(), h = frame.getHeight(), minX = w, minY = h, maxX = 0, maxY = 0;
        int[] pixels = new int[w * h];
        frame.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((pixels[y * w + x] >>> 24) > 64) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (minX > maxX) {
            return scaleAndRecycle(frame, height, height);
        }
        Bitmap crop = Bitmap.createBitmap(frame, minX, minY, maxX - minX + 1, maxY - minY + 1);
        int width = Math.round(crop.getWidth() * height / (float) crop.getHeight());
        Bitmap out = scaleAndRecycle(crop, width, height);
        if (crop != frame) {
            frame.recycle();
        }
        return out;
    }

    static Bitmap scaleAndRecycle(Bitmap source, int width, int height) {
        Bitmap scaled = Bitmap.createScaledBitmap(source, width, height, false);
        if (scaled != source) {
            source.recycle();
        }
        return scaled;
    }

    private final android.content.res.Resources resources;

    GameAssets(Context context) {
        resources = context.getResources();
        Bitmap raw = BitmapFactory.decodeResource(resources, R.drawable.alien_jungle);
        jungle = scaleAndRecycle(raw, 1980, 720);
        raw = BitmapFactory.decodeResource(resources, R.drawable.nova_green);
        nova = scaleAndRecycle(raw, 82, 71);
        raw = BitmapFactory.decodeResource(resources, R.drawable.nova_run);
        int fw = raw.getWidth() / 4, fh = raw.getHeight() / 2;
        for (int i = 0; i < 8; i++) {
            Bitmap frame = Bitmap.createBitmap(raw, (i % 4) * fw, (i / 4) * fh, fw, fh);
            int minX = fw, minY = fh, maxX = 0, maxY = 0;
            int[] pixels = new int[fw * fh];
            frame.getPixels(pixels, 0, fw, 0, 0, fw, fh);
            for (int y = 0; y < fh; y++) {
                for (int x = 0; x < fw; x++) {
                    if ((pixels[y * fw + x] >>> 24) > 64) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            Bitmap trimmed =
                    Bitmap.createBitmap(frame, minX, minY, maxX - minX + 1, maxY - minY + 1);
            int width = Math.round(trimmed.getWidth() * 71f / trimmed.getHeight());
            runFrames[i] = scaleAndRecycle(trimmed, width, 71);
            if (frame != trimmed) {
                frame.recycle();
            }
        }
        raw.recycle();
        raw = BitmapFactory.decodeResource(resources, R.drawable.jungle_terrain);
        terrain = scaleAndRecycle(raw, 324, 216);
        raw = BitmapFactory.decodeResource(resources, R.drawable.lev_arden);
        lion = scaleAndRecycle(raw, 190, 166);
        raw = BitmapFactory.decodeResource(resources, R.drawable.nova_heli);
        heliSprite = scaleAndRecycle(raw, 100, 80);
        // Attachments authored against the visible barrel tips, in each trimmed sprite's space.
        standingPose = new PlayerPose(nova, .957f, .418f);
        flyingPose = new PlayerPose(heliSprite, .935f, .423f);
        float[] runningMuzzleY = {.402f, .383f, .405f, .390f, .422f, .411f, .432f, .402f};
        for (int i = 0; i < runningPoses.length; i++) {
            runningPoses[i] = new PlayerPose(runFrames[i], .985f, runningMuzzleY[i]);
        }
        raw = BitmapFactory.decodeResource(resources, R.drawable.campaign_worlds);
        int bw = raw.getWidth() / 2, bh = raw.getHeight() / 3;
        for (int i = 0; i < 6; i++) {
            backdrops[i] =
                    scaleAndRecycle(
                            Bitmap.createBitmap(raw, i % 2 * bw, i / 2 * bh, bw, bh), 1620, 720);
        }
        raw.recycle();
        raw = BitmapFactory.decodeResource(resources, R.drawable.campaign_terrain);
        int tw = raw.getWidth() / 2, th = raw.getHeight() / 3;
        for (int i = 0; i < 6; i++) {
            surfaces[i] =
                    scaleAndRecycle(
                            Bitmap.createBitmap(raw, i % 2 * tw, i / 2 * th, tw, th), 108, 108);
        }
        raw.recycle();
        raw = BitmapFactory.decodeResource(resources, R.drawable.campaign_villains);
        int ew = raw.getWidth() / 2, eh = raw.getHeight() / 2;
        for (int i = 0; i < 4; i++) {
            villains[i] = trimSprite(Bitmap.createBitmap(raw, i % 2 * ew, i / 2 * eh, ew, eh), 166);
        }
        raw.recycle();
        raw = BitmapFactory.decodeResource(resources, R.drawable.energy_seed);
        energySeed = scaleAndRecycle(raw, 21, 33);
        raw = BitmapFactory.decodeResource(resources, R.drawable.enemies_v2);
        int cell = raw.getWidth() / 3;
        int[] widths = {55, 68, 53}, heights = {52, 52, 57};
        for (int i = 0; i < 3; i++) {
            Bitmap crop = Bitmap.createBitmap(raw, i * cell, 0, cell, raw.getHeight());
            enemySprites[i] = scaleAndRecycle(crop, widths[i], heights[i]);
        }
        raw.recycle();
        lionFrames = gridFrames(R.drawable.lev_combat, 3, 2, 166);
        villainFrames = gridFrames(R.drawable.villain_combat, 4, 3, 166);
        devices = gridFrames(R.drawable.guardian_devices, 3, 2, 66);
        raw = BitmapFactory.decodeResource(resources, R.drawable.minions);
        int mw = raw.getWidth() / 3, mh = raw.getHeight() / 2;
        minionSprites = new Bitmap[6];
        for (int i = 0; i < 6; i++) {
            minionSprites[i] =
                    trimSprite(
                            Bitmap.createBitmap(raw, i % 3 * mw, i / 3 * mh, mw, mh),
                            i == 4 ? 55 : 48);
        }
        raw.recycle();
    }
}
