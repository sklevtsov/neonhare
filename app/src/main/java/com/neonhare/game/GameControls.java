package com.neonhare.game;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

final class GameControls {

    public boolean onKeyDown(int c, KeyEvent e) {
        world.showTouchControls = false;
        c = numberKey(c);
        if (world.screen != UiScreen.PLAY) {
            if (e.getRepeatCount() == 0) {
                menuKey(c);
            }
            return true;
        }
        if (c == KeyEvent.KEYCODE_ESCAPE || c == KeyEvent.KEYCODE_M) {
            if (e.getRepeatCount() == 0) {
                world.queueAction(GameCommand.Action.OPEN_MENU);
            }
            return true;
        }
        if (c == KeyEvent.KEYCODE_TAB || c == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (e.getRepeatCount() == 0) {
                world.toggleUpgrades();
            }
            return true;
        }
        if (world.upgradeMenu) {
            if (e.getRepeatCount() == 0) {
                if (c >= KeyEvent.KEYCODE_1 && c <= KeyEvent.KEYCODE_5) {
                    world.selectedSkill = c - KeyEvent.KEYCODE_1;
                } else if (c == KeyEvent.KEYCODE_DPAD_UP) {
                    world.selectedSkill = (world.selectedSkill + 4) % 5;
                } else if (c == KeyEvent.KEYCODE_DPAD_DOWN) {
                    world.selectedSkill = (world.selectedSkill + 1) % 5;
                } else if (c == KeyEvent.KEYCODE_ENTER
                        || c == KeyEvent.KEYCODE_BUTTON_A
                        || c == KeyEvent.KEYCODE_Z) {
                    world.buyUpgrade(world.selectedSkill);
                } else if (c == KeyEvent.KEYCODE_BACK) {
                    world.toggleUpgrades();
                }
            }
            return true;
        }
        if (gameKey(c)) {
            if (e.getRepeatCount() == 0) {
                key(c, true);
            }
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int c, KeyEvent e) {
        if (world.screen != UiScreen.PLAY || world.upgradeMenu) {
            return true;
        }
        c = numberKey(c);
        if (gameKey(c)) {
            key(c, false);
            return true;
        }
        return false;
    }

    static int numberKey(int c) {
        return c >= KeyEvent.KEYCODE_NUMPAD_1 && c <= KeyEvent.KEYCODE_NUMPAD_9
                ? KeyEvent.KEYCODE_1 + c - KeyEvent.KEYCODE_NUMPAD_1
                : c;
    }

    boolean jumpKey(int c) {
        return c == KeyEvent.KEYCODE_DPAD_UP
                || c == KeyEvent.KEYCODE_SPACE
                || c == KeyEvent.KEYCODE_BUTTON_A
                || c == KeyEvent.KEYCODE_BUTTON_B;
    }

    boolean fireKey(int c) {
        return c == KeyEvent.KEYCODE_Z
                || c == KeyEvent.KEYCODE_BUTTON_X
                || c == KeyEvent.KEYCODE_BUTTON_R1
                || c == KeyEvent.KEYCODE_BUTTON_R2;
    }

    boolean gameKey(int c) {
        c = numberKey(c);
        return world.screen != UiScreen.PLAY
                || world.upgradeMenu
                || c == KeyEvent.KEYCODE_ESCAPE
                || c == KeyEvent.KEYCODE_M
                || c == KeyEvent.KEYCODE_TAB
                || c == KeyEvent.KEYCODE_BUTTON_SELECT
                || c == KeyEvent.KEYCODE_H
                || c == KeyEvent.KEYCODE_E
                || c == KeyEvent.KEYCODE_BUTTON_L2
                || (BuildConfig.DEBUG
                        && (c == KeyEvent.KEYCODE_1
                                || c == KeyEvent.KEYCODE_2
                                || c == KeyEvent.KEYCODE_3
                                || c == KeyEvent.KEYCODE_4))
                || jumpKey(c)
                || fireKey(c)
                || c == KeyEvent.KEYCODE_BACK
                || c == KeyEvent.KEYCODE_DPAD_DOWN
                || c == KeyEvent.KEYCODE_DPAD_LEFT
                || c == KeyEvent.KEYCODE_DPAD_RIGHT
                || c == KeyEvent.KEYCODE_X
                || c == KeyEvent.KEYCODE_P
                || c == KeyEvent.KEYCODE_K
                || c == KeyEvent.KEYCODE_ENTER
                || c == KeyEvent.KEYCODE_BUTTON_Y
                || c == KeyEvent.KEYCODE_BUTTON_L1
                || c == KeyEvent.KEYCODE_BUTTON_START;
    }

    void refreshKeys() {
        world.left = world.heldKeys.contains(KeyEvent.KEYCODE_DPAD_LEFT);
        world.right = world.heldKeys.contains(KeyEvent.KEYCODE_DPAD_RIGHT);
        world.jump = world.hatJump;
        world.fire = world.triggerFire;
        world.heliHeld =
                world.triggerHeli
                        || world.heldKeys.contains(KeyEvent.KEYCODE_H)
                        || world.heldKeys.contains(KeyEvent.KEYCODE_BUTTON_L2);
        for (int c : world.heldKeys) {
            if (jumpKey(c)) {
                world.jump = true;
            }
            if (fireKey(c)) {
                world.fire = true;
            }
        }
    }

    void key(int c, boolean down) {
        world.showTouchControls = false;
        boolean wasJump = world.jump, wasFire = world.fire;
        if (down) {
            world.heldKeys.add(c);
        } else {
            world.heldKeys.remove(c);
        }
        refreshKeys();
        if (world.jump && !wasJump) {
            world.jumpQueued = true;
        }
        if (world.fire && !wasFire) {
            world.fireQueued = true;
        }
        if (down && c == KeyEvent.KEYCODE_K && BuildConfig.DEBUG) {
            world.hero.y = world.heightInTiles * world.TILE_SIZE + 100;
        }
        if (down
                && (c == KeyEvent.KEYCODE_X
                        || c == KeyEvent.KEYCODE_BUTTON_Y
                        || c == KeyEvent.KEYCODE_BUTTON_L1)) {
            world.nextGun();
        }
        if (down
                && (c == KeyEvent.KEYCODE_P
                        || c == KeyEvent.KEYCODE_ENTER
                        || c == KeyEvent.KEYCODE_BUTTON_START)) {
            if (c == KeyEvent.KEYCODE_BUTTON_START) {
                world.queueAction(GameCommand.Action.OPEN_MENU);
            } else if (world.won && world.levelIndex < Campaign.COUNT - 1) {
                world.queueAction(GameCommand.Action.NEXT_CHAPTER);
            } else if (world.gameOver) {
                world.queueAction(GameCommand.Action.RETRY);
            } else if (world.won) {
                world.queueAction(GameCommand.Action.CHAPTER_SELECT);
            } else {
                world.queueAction(GameCommand.Action.OPEN_MENU);
            }
            world.store();
        }
        if (down && BuildConfig.DEBUG && c == KeyEvent.KEYCODE_2) {
            world.queueAction(GameCommand.Action.DEBUG_NEXT_CHAPTER);
        }
        if (down && BuildConfig.DEBUG && c == KeyEvent.KEYCODE_3) {
            world.queueAction(GameCommand.Action.PREVIEW_BOSS);
        }
        if (down && BuildConfig.DEBUG && c == KeyEvent.KEYCODE_4) {
            world.progress.points += 3;
            world.toggleUpgrades();
        }
        if (down && BuildConfig.DEBUG && c == KeyEvent.KEYCODE_1) {
            world.queueAction(GameCommand.Action.DEBUG_PREVIOUS_CHAPTER);
        }
        if (down && (c == KeyEvent.KEYCODE_E || c == KeyEvent.KEYCODE_DPAD_DOWN)) {
            world.interactQueued = true;
        }
        // BACK may be emitted by the handheld's B mapping. Consume it in-game.
    }

    public boolean onGenericMotionEvent(MotionEvent e) {
        if ((e.getSource() & InputDevice.SOURCE_JOYSTICK) != 0
                && e.getAction() == MotionEvent.ACTION_MOVE) {
            world.showTouchControls = false;
            if (world.screen != UiScreen.PLAY) {
                int x =
                        (int)
                                Math.signum(
                                        Math.abs(e.getAxisValue(MotionEvent.AXIS_HAT_X)) > .5f
                                                ? e.getAxisValue(MotionEvent.AXIS_HAT_X)
                                                : Math.abs(e.getAxisValue(MotionEvent.AXIS_X)) > .5f
                                                        ? e.getAxisValue(MotionEvent.AXIS_X)
                                                        : 0);
                int y =
                        (int)
                                Math.signum(
                                        Math.abs(e.getAxisValue(MotionEvent.AXIS_HAT_Y)) > .5f
                                                ? e.getAxisValue(MotionEvent.AXIS_HAT_Y)
                                                : Math.abs(e.getAxisValue(MotionEvent.AXIS_Y)) > .5f
                                                        ? e.getAxisValue(MotionEvent.AXIS_Y)
                                                        : 0);
                if (y != 0 && y != world.menuAxisY) {
                    navigateMenu(y * (world.screen == UiScreen.CHAPTER_SELECT ? 2 : 1));
                } else if (world.screen == UiScreen.CHAPTER_SELECT
                        && x != 0
                        && x != world.menuAxisX) {
                    navigateMenu(x);
                }
                world.menuAxisX = x;
                world.menuAxisY = y;
                return true;
            }
            boolean wasJump = world.jump, wasFire = world.fire;
            world.hatX = e.getAxisValue(MotionEvent.AXIS_HAT_X);
            float axis = e.getAxisValue(MotionEvent.AXIS_X);
            world.stick = Math.abs(world.hatX) > .5f ? world.hatX : axis;
            world.hatJump = e.getAxisValue(MotionEvent.AXIS_HAT_Y) < -.5f;
            world.triggerHeli = e.getAxisValue(MotionEvent.AXIS_LTRIGGER) > .35f;
            world.triggerFire =
                    e.getAxisValue(MotionEvent.AXIS_RTRIGGER) > .35f
                            || e.getAxisValue(MotionEvent.AXIS_BRAKE) > .35f;
            refreshKeys();
            if (world.jump && !wasJump) {
                world.jumpQueued = true;
            }
            if (world.fire && !wasFire) {
                world.fireQueued = true;
            }
            return true;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent e) {
        world.showTouchControls = true;
        float
                scale =
                        Math.min(
                                world.physicalWidth / world.viewWidth,
                                world.physicalHeight / world.LOGICAL_HEIGHT),
                tx = (e.getX() - (world.physicalWidth - world.viewWidth * scale) / 2) / scale,
                ty = (e.getY() - (world.physicalHeight - world.LOGICAL_HEIGHT * scale) / 2) / scale;
        if (world.screen != UiScreen.PLAY) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                float mx = tx - (world.viewWidth - 960) / 2;
                if (ty < 112) {
                    if (world.screen == UiScreen.CHAPTER_SELECT) {
                        world.screen = UiScreen.MAIN_MENU;
                    } else {
                        world.queueAction(GameCommand.Action.CONTINUE);
                    }
                } else if (world.screen == UiScreen.MAIN_MENU
                        && mx >= 48
                        && mx <= 520
                        && ty >= 143
                        && ty < 509) {
                    world.menuSelection = Math.min(3, (int) ((ty - 143) / 96));
                    confirmMenu();
                } else if (world.screen == UiScreen.CHAPTER_SELECT
                        && mx >= 48
                        && mx < 928
                        && ty >= 128
                        && ty < 545) {
                    int slot = (int) ((ty - 128) / 139) * 2 + (mx >= 488 ? 1 : 0);
                    world.chapterSelection = (world.chapterSelection / 6) * 6 + slot;
                    confirmMenu();
                } else if (world.screen == UiScreen.CHAPTER_SELECT && ty >= 558 && ty < 625) {
                    world.chapterSelection =
                            ((world.chapterSelection / 6 + 1) % ((Campaign.COUNT + 5) / 6)) * 6;
                }
            }
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                && ty >= 78
                && ty < 124
                && tx >= world.viewWidth - 180) {
            world.queueAction(GameCommand.Action.OPEN_MENU);
            return true;
        }
        if (world.upgradeMenu) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (ty < 112) {
                    world.toggleUpgrades();
                } else if (ty >= 132 && ty < 612) {
                    world.selectedSkill = Math.min(4, (int) ((ty - 132) / 96));
                    world.buyUpgrade(world.selectedSkill);
                }
            }
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN && ty < 72 && tx < 590) {
            world.toggleUpgrades();
            return true;
        }
        if ((world.won || world.gameOver) && e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (world.won && world.levelIndex < Campaign.COUNT - 1) {
                world.queueAction(GameCommand.Action.NEXT_CHAPTER);
            } else if (world.gameOver) {
                world.queueAction(GameCommand.Action.RETRY);
            } else {
                world.queueAction(GameCommand.Action.CHAPTER_SELECT);
            }
            return true;
        }
        boolean wasJump = world.jump, wasFire = world.fire;
        world.left = world.right = world.jump = world.fire = world.heliHeld = false;
        float
                s =
                        Math.min(
                                world.physicalWidth / world.viewWidth,
                                world.physicalHeight / world.LOGICAL_HEIGHT),
                ox = (world.physicalWidth - world.viewWidth * s) / 2,
                oy = (world.physicalHeight - world.LOGICAL_HEIGHT * s) / 2;
        for (int i = 0; i < e.getPointerCount(); i++) {
            if (e.getActionMasked() == MotionEvent.ACTION_POINTER_UP && i == e.getActionIndex()) {
                continue;
            }
            float x = (e.getX(i) - ox) / s, y = (e.getY(i) - oy) / s;
            if (y < world.LOGICAL_HEIGHT - 240 || y > world.LOGICAL_HEIGHT - 78) {
                continue;
            }
            if (x < 155) {
                world.left |= x < 91;
                world.right |= x >= 91;
            }
            if (x > world.viewWidth - 145) {
                world.jump = true;
            } else if (x > world.viewWidth - 245) {
                world.fire = true;
            } else if (x > world.viewWidth - 325
                    && e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                world.nextGun();
            } else if (x > world.viewWidth - 415 && world.progress.heliUnlocked) {
                world.heliHeld = true;
            }
        }
        if (world.jump && !wasJump) {
            world.jumpQueued = true;
        }
        if (world.fire && !wasFire) {
            world.fireQueued = true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP
                || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            world.left = world.right = world.jump = world.fire = world.heliHeld = false;
        }
        return true;
    }

    void confirmMenu() {
        if (world.screen == UiScreen.CHAPTER_SELECT) {
            if (world.progress.isUnlocked(world.chapterSelection)) {
                world.queueChapter(world.chapterSelection);
            }
            return;
        }
        world.queueAction(
                world.menuSelection == 0
                        ? GameCommand.Action.CONTINUE
                        : world.menuSelection == 1
                                ? GameCommand.Action.CHAPTER_SELECT
                                : world.menuSelection == 2
                                        ? GameCommand.Action.UPGRADES
                                        : GameCommand.Action.SAVE_AND_EXIT);
    }

    void navigateMenu(int direction) {
        if (world.screen == UiScreen.MAIN_MENU) {
            world.menuSelection = Math.floorMod(world.menuSelection + direction, 4);
        } else {
            world.chapterSelection =
                    Math.max(0, Math.min(Campaign.COUNT - 1, world.chapterSelection + direction));
        }
    }

    void menuKey(int code) {
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            navigateMenu(world.screen == UiScreen.CHAPTER_SELECT ? -2 : -1);
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            navigateMenu(world.screen == UiScreen.CHAPTER_SELECT ? 2 : 1);
        } else if (code == KeyEvent.KEYCODE_DPAD_LEFT && world.screen == UiScreen.CHAPTER_SELECT) {
            navigateMenu(-1);
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT && world.screen == UiScreen.CHAPTER_SELECT) {
            navigateMenu(1);
        } else if (code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_BUTTON_A
                || code == KeyEvent.KEYCODE_Z
                || code == KeyEvent.KEYCODE_SPACE) {
            confirmMenu();
        } else if (code == KeyEvent.KEYCODE_BACK
                || code == KeyEvent.KEYCODE_ESCAPE
                || code == KeyEvent.KEYCODE_BUTTON_B) {
            if (world.screen == UiScreen.CHAPTER_SELECT) {
                world.screen = UiScreen.MAIN_MENU;
            } else {
                world.queueAction(GameCommand.Action.CONTINUE);
            }
        } else if (code == KeyEvent.KEYCODE_BUTTON_START || code == KeyEvent.KEYCODE_P) {
            world.queueAction(GameCommand.Action.CONTINUE);
        } else if (code == KeyEvent.KEYCODE_BUTTON_SELECT || code == KeyEvent.KEYCODE_TAB) {
            world.queueAction(GameCommand.Action.UPGRADES);
        }
    }

    private final GameWorld world;

    GameControls(GameWorld world) {
        this.world = world;
    }
}
