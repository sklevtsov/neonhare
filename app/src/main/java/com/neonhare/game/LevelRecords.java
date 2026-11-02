package com.neonhare.game;

import android.content.SharedPreferences;

/** Independent personal bests; replaying a chapter never erases an earlier record. */
final class LevelRecords {

    private final java.util.BitSet dirty = new java.util.BitSet();

    final int[] bestScore = new int[Campaign.COUNT],
            bestMillis = new int[Campaign.COUNT],
            bestSecrets = new int[Campaign.COUNT],
            clears = new int[Campaign.COUNT];

    LevelRecords(SharedPreferences save) {
        save = new SafePreferences(save);
        for (int i = 0; i < Campaign.COUNT; i++) {
            bestScore[i] = readRecord(save, i, "score");
            bestMillis[i] = readRecord(save, i, "time");
            bestSecrets[i] = readRecord(save, i, "secrets");
            clears[i] = readRecord(save, i, "clears");
            if (!save.contains(recordKey(i, "score")) && (bestScore[i] != 0 || clears[i] != 0)) {
                dirty.set(i);
            }
        }
    }

    void complete(int chapter, int score, float seconds, int secrets) {
        bestScore[chapter] = Math.max(bestScore[chapter], score);
        int millis = Math.max(1, Math.round(seconds * 1000));
        if (bestMillis[chapter] == 0 || millis < bestMillis[chapter]) {
            bestMillis[chapter] = millis;
        }
        bestSecrets[chapter] = Math.max(bestSecrets[chapter], secrets);
        clears[chapter]++;
        dirty.set(chapter);
    }

    void write(SharedPreferences.Editor editor) {
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) {
            editor.putInt(recordKey(i, "score"), bestScore[i])
                    .putInt(recordKey(i, "time"), bestMillis[i])
                    .putInt(recordKey(i, "secrets"), bestSecrets[i])
                    .putInt(recordKey(i, "clears"), clears[i]);
        }
        dirty.clear();
    }

    private static String recordKey(int chapter, String field) {
        return "record." + Campaign.level(chapter).id + "." + field;
    }

    private static int readRecord(SharedPreferences save, int chapter, String field) {
        String key = recordKey(chapter, field);
        int legacy = LegacySaveIds.index(Campaign.level(chapter).id);
        return Math.max(
                0,
                save.getInt(
                        key, legacy >= 0 ? save.getInt("record." + field + "." + legacy, 0) : 0));
    }

    static String time(int millis) {
        if (millis <= 0) {
            return "—";
        }
        int seconds = millis / 1000;
        return String.format(
                java.util.Locale.US,
                "%02d:%02d.%01d",
                seconds / 60,
                seconds % 60,
                (millis % 1000) / 100);
    }
}
