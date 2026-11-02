package com.neonhare.game;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** Android surface and event adapter. Gameplay state belongs exclusively to GameWorld's owner. */
public final class GameView extends SurfaceView implements SurfaceHolder.Callback {

    final GameWorld world;

    final GameRenderer renderer;

    final GameAssets assets;

    private final GameLoop loop;

    public GameView(Context context) {
        this(context, false);
    }

    public GameView(Context context, boolean testProfile) {
        super(context);
        Campaign.initialize(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        assets = new GameAssets(context);
        world =
                new GameWorld(
                        context,
                        BuildConfig.DEBUG && testProfile,
                        assets,
                        () -> post(() -> ((Activity) context).finish()));
        renderer = new GameRenderer(world, assets, getHolder());
        loop = new GameLoop(world, renderer);
        getHolder().addCallback(this);
    }

    static boolean isControllerSource(int sources) {
        return GameWorld.isControllerSource(sources);
    }

    static float logicalWidthFor(int width, int height) {
        return GameWorld.logicalWidthFor(width, height);
    }

    boolean gameKey(int code) {
        return InputBindings.handles(code);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        if (!gameKey(code)) {
            return super.onKeyDown(code, event);
        }
        world.enqueue(GameInputEvent.key(code, event, true));
        return true;
    }

    @Override
    public boolean onKeyUp(int code, KeyEvent event) {
        if (!gameKey(code)) {
            return super.onKeyUp(code, event);
        }
        world.enqueue(GameInputEvent.key(code, event, false));
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        world.enqueue(GameInputEvent.motion(event, true));
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & android.view.InputDevice.SOURCE_JOYSTICK)
                == android.view.InputDevice.SOURCE_JOYSTICK) {
            world.enqueue(GameInputEvent.motion(event, false));
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    public void clearInput() {
        world.enqueue(GameInputEvent.clear());
    }

    public void resume() {
        loop.resume();
    }

    public void pause() {
        loop.pause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        loop.surfaceReady(true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        loop.surfaceReady(false);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        world.enqueue(GameInputEvent.resize(width, height));
    }
}
