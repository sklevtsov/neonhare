package com.neonhare.game;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/** Reproducible composed-device captures of the real renderer; test APK only, isolated save. */
public final class ScreenshotCapture extends Instrumentation {
    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            Activity activity =
                    startActivitySync(
                            new Intent()
                                    .setClassName(getTargetContext(), MainActivity.class.getName())
                                    .putExtra("test_profile", true)
                                    .addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            Field field = MainActivity.class.getDeclaredField("gameView");
            field.setAccessible(true);
            GameView view = (GameView) field.get(activity);
            SystemClock.sleep(500);
            for (int scene = 0; scene < 4; scene++) {
                final int index = scene;
                final Throwable[] failure = {null};
                runOnMainSync(
                        () -> {
                            try {
                                view.pause();
                                arrange(view, index);
                                view.renderer.render();
                            } catch (Throwable error) {
                                failure[0] = error;
                            }
                        });
                if (failure[0] != null) {
                    throw new IllegalStateException("Capture setup failed", failure[0]);
                }
                SystemClock.sleep(250);
                Bitmap screenshot = getUiAutomation().takeScreenshot();
                if (screenshot == null) {
                    throw new IllegalStateException("Device screenshot unavailable");
                }
                File destination =
                        new File(getTargetContext().getFilesDir(), "screenshot-" + scene + ".png");
                try (FileOutputStream output = new FileOutputStream(destination)) {
                    if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        throw new IllegalStateException("PNG encoding failed");
                    }
                }
                screenshot.recycle();
            }
            runOnMainSync(activity::finish);
            result.putString("stream", "\nPASS: four gameplay screenshots\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "\nFAIL: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void arrange(GameView view, int scene) {
        GameWorld world = view.world;
        world.loadLevel(new int[] {0, 1, 2, 6}[scene]);
        world.viewWidth = 960;
        view.renderer.showDebugAnnotations = false;
        java.util.Arrays.fill(world.records.bestScore, 0);
        world.showTouchControls = false;
        world.lives = 5;
        world.messageTimer = 0;
        world.hero.inv = 0;
        world.hero.face = 1;
        world.hero.vx = 160;
        world.hero.stride = 2;
        world.hero.ground = true;
        world.hero.x = scene == 3 ? 32 * 36 : 17 * 36;
        world.hero.y = 425.99f;
        if (scene == 1) {
            world.previewBoss();
            world.hero.x = 147 * 36;
            world.hero.inv = 0;
            world.messageTimer = 0;
            world.updateBoss(.016f);
        } else if (scene == 2) {
            world.hero.x = 26 * 36;
            world.hero.y = 185;
            world.hero.ground = false;
            world.gliding = true;
        }
        world.cam = Math.max(0, world.hero.x - 330);
        world.camY = 0;
        world.bullets.clear();
        world.gun = GameWorld.BLASTER;
        world.shoot();
    }
}
