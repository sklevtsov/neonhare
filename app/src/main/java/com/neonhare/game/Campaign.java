package com.neonhare.game;

import android.content.Context;

/**
 * Campaign facade; all chapter content is loaded from assets/levels, never selected by index in
 * code.
 */
final class Campaign {

    static int COUNT;

    static String[] TITLES, BRIEFS, EGGS, EGG_NOTES;
    static int[] THEMES, BOSSES, RELAYS, ACCENTS;

    private static LevelCatalog catalog;

    static final String[] BOSS_NAMES = {
        "ЛЕВ АРДЕН", "ВЕСПЕРА", "РУК · СБОРЩИК ДОЛГОВ", "МОРРОУ · КАРТОГРАФ", "АРХИВАРИУС РЕЕСТРА"
    };

    static final String[] TAUNTS = {
        "Узел кормит убежище. Не заставляй меня выбирать, кто умрёт.",
        "Я сохраню свой сад. Остальное — сорняки.",
        "Долг твоего друга принадлежит мне. А значит, и твой.",
        "Если стереть дорогу с карты, никто не вспомнит тех, кто по ней шёл.",
        "Вне Реестра вас не существует. И воду вам никто не обещал."
    };

    static synchronized void initialize(Context context) {
        if (catalog != null) {
            return;
        }
        try {
            LevelCatalog loaded = new LevelCatalog(context.getAssets());
            COUNT = loaded.levels.size();
            TITLES = new String[COUNT];
            BRIEFS = new String[COUNT];
            EGGS = new String[COUNT];
            EGG_NOTES = new String[COUNT];
            THEMES = new int[COUNT];
            BOSSES = new int[COUNT];
            RELAYS = new int[COUNT];
            ACCENTS = new int[COUNT];
            for (int i = 0; i < COUNT; i++) {
                LevelDefinition level = loaded.levels.get(i);
                TITLES[i] = level.title;
                BRIEFS[i] = level.help;
                EGGS[i] = level.eggTitle;
                EGG_NOTES[i] = level.eggNote;
                THEMES[i] = level.theme;
                BOSSES[i] = level.boss;
                ACCENTS[i] = level.accent;
                for (LevelDefinition.ObjectSpec object : level.objects) {
                    if (object.group.equals("relays")) {
                        RELAYS[i]++;
                    }
                }
            }
            catalog = loaded;
        } catch (Exception error) {
            throw new IllegalStateException("Invalid packaged campaign", error);
        }
    }

    static LevelDefinition level(int chapter) {
        if (catalog == null) {
            throw new IllegalStateException("Campaign not initialized");
        }
        return catalog.levels.get(chapter);
    }

    static int indexOf(String id) {
        return catalog.indexOf(id);
    }

    static int boss(int chapter) {
        return level(chapter).boss;
    }

    private Campaign() {}
}
