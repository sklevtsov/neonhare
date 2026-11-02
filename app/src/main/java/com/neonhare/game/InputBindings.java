package com.neonhare.game;

import android.view.KeyEvent;

/**
 * Platform dispatch does not inspect mutable menu or gameplay state. Volume/system keys stay
 * usable.
 */
final class InputBindings {

    static boolean handles(int code) {
        if (code >= KeyEvent.KEYCODE_1 && code <= KeyEvent.KEYCODE_5
                || code >= KeyEvent.KEYCODE_NUMPAD_1 && code <= KeyEvent.KEYCODE_NUMPAD_9) {
            return true;
        }
        switch (code) {
            case KeyEvent.KEYCODE_TAB:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_H:
            case KeyEvent.KEYCODE_E:
            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_M:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_Z:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_X:
            case KeyEvent.KEYCODE_P:
            case KeyEvent.KEYCODE_K:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_START:
                return true;
            default:
                return false;
        }
    }

    private InputBindings() {}
}
