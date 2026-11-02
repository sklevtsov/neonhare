package com.neonhare.game;

/** Presentation styles only. Geometry is authored in each level.json. */
final class LevelArchitecture {

    static final int OPEN = 0, CISTERN = 1, TOWER = 2, CAVERN = 3, PYRAMID = 4;

    static int style(int chapter) {
        return Campaign.level(chapter).style.ordinal();
    }

    private LevelArchitecture() {}
}
