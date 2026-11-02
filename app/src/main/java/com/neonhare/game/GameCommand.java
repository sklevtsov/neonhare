package com.neonhare.game;

/** Semantic commands; chapter indices are payloads, never encoded into command numbers. */
final class GameCommand {

    enum Action {
        NEW_CAMPAIGN,
        NEXT_CHAPTER,
        DEBUG_NEXT_CHAPTER,
        PREVIEW_BOSS,
        RETRY,
        DEBUG_PREVIOUS_CHAPTER,
        OPEN_MENU,
        CONTINUE,
        CHAPTER_SELECT,
        UPGRADES,
        SAVE_AND_EXIT,
        LOAD_CHAPTER
    }

    final Action action;

    final int chapter;

    GameCommand(Action action) {
        this(action, -1);
    }

    GameCommand(Action action, int chapter) {
        this.action = action;
        this.chapter = chapter;
    }
}
