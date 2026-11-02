package com.neonhare.game;

import android.view.KeyEvent;
import android.view.MotionEvent;

/** UI-owned platform events are copied before crossing into the simulation thread. */
abstract class GameInputEvent {

    abstract void apply(GameWorld world);

    void release() {}

    static GameInputEvent key(int code, KeyEvent event, boolean down) {
        KeyEvent copy = new KeyEvent(event);
        return new GameInputEvent() {

            @Override
            void apply(GameWorld world) {
                if (down) {
                    world.controls.onKeyDown(code, copy);
                } else {
                    world.controls.onKeyUp(code, copy);
                }
            }
        };
    }

    static GameInputEvent motion(MotionEvent event, boolean touch) {
        MotionEvent copy = MotionEvent.obtain(event);
        return new GameInputEvent() {

            @Override
            void apply(GameWorld world) {
                if (touch) {
                    world.controls.onTouchEvent(copy);
                } else {
                    world.controls.onGenericMotionEvent(copy);
                }
            }

            @Override
            void release() {
                copy.recycle();
            }
        };
    }

    static GameInputEvent clear() {
        return new GameInputEvent() {

            @Override
            void apply(GameWorld world) {
                world.clearInput();
            }
        };
    }

    static GameInputEvent resize(int width, int height) {
        return new GameInputEvent() {

            @Override
            void apply(GameWorld world) {
                world.physicalWidth = Math.max(1, width);
                world.physicalHeight = Math.max(1, height);
                world.viewWidth = GameWorld.logicalWidthFor(width, height);
            }
        };
    }
}
