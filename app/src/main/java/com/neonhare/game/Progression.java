package com.neonhare.game;

import android.content.SharedPreferences;

/** Permanent campaign rewards; completion and discovery masks prevent reward farming. */
final class Progression {

    static final String[] SKILLS = {"ЛЁТЧИК", "АРСЕНАЛ", "ЖИВУЧЕСТЬ", "СЛЕДОПЫТ", "РЕАКТОР"};

    static final String[] EFFECTS = {
        "Дольше полёт и точнее управление в воздухе",
        "Быстрее стрельба; со 2 ранга — больше урона",
        "Больше жизней и дольше защита после попадания",
        "Видны близкие тайники; больше награда за находки",
        "Быстрее заряд и сильнее подъём на ушах"
    };

    final int[] ranks = new int[5];

    int points, currentLevel, unlockedLevel, weaponMask = 7;
    boolean lionAlly, heliUnlocked, flightTrained;
    private final java.util.Set<String> completed = new java.util.HashSet<>(),
            eggs = new java.util.HashSet<>(),
            training = new java.util.HashSet<>(),
            unlocked = new java.util.HashSet<>();

    Progression() {
        if (Campaign.COUNT > 0) {
            unlocked.add(Campaign.level(0).id);
        }
    }

    boolean buy(int skill) {
        if (skill < 0 || skill >= ranks.length || ranks[skill] >= 3) {
            return false;
        }
        int cost = ranks[skill] + 1;
        if (points < cost) {
            return false;
        }
        points -= cost;
        ranks[skill]++;
        return true;
    }

    boolean complete(int level) {
        if (!completed.add(Campaign.level(level).id)) {
            return false;
        }
        points = Math.min(1000000, points + 2);
        int next = Math.min(Campaign.COUNT - 1, level + 1);
        unlocked.add(Campaign.level(next).id);
        unlockedLevel = Math.max(unlockedLevel, next);
        return true;
    }

    boolean discover(int chapter) {
        if (!eggs.add(Campaign.level(chapter).id)) {
            return false;
        }
        points = Math.min(1000000, points + 1);
        return true;
    }

    boolean train(int chapter) {
        if (!training.add(Campaign.level(chapter).id)) {
            return false;
        }
        points = Math.min(1000000, points + 1);
        return true;
    }

    boolean hasEgg(int chapter) {
        return eggs.contains(Campaign.level(chapter).id);
    }

    boolean hasTraining(int chapter) {
        return training.contains(Campaign.level(chapter).id);
    }

    boolean isUnlocked(int chapter) {
        return chapter >= 0
                && chapter < Campaign.COUNT
                && unlocked.contains(Campaign.level(chapter).id);
    }

    int unlockedCount() {
        return unlocked.size();
    }

    void befriendLion() {
        lionAlly = true;
        heliUnlocked = true;
    }

    float maxFuel() {
        return 1.6f + ranks[0] * .65f + ranks[4] * .35f;
    }

    void write(SharedPreferences.Editor editor) {
        String current = Campaign.level(currentLevel).id;
        editor.putString("campaign.levelId", current)
                .putStringSet("campaign.completedIds", new java.util.HashSet<>(completed))
                .putStringSet("campaign.eggIds", new java.util.HashSet<>(eggs))
                .putStringSet("campaign.trainingIds", new java.util.HashSet<>(training))
                .putStringSet("campaign.unlockedIds", new java.util.HashSet<>(unlocked));
        // Retain legacy projections for a rollback to the installed 0.3 local build.
        editor.putInt("campaign.level", Math.max(0, LegacySaveIds.index(current)))
                .putInt("campaign.unlocked", legacyUnlocked())
                .putInt("campaign.completed", legacyMask(completed))
                .putInt("campaign.training", legacyMask(training))
                .putInt("campaign.eggs", legacyMask(eggs));
        editor.putInt("campaign.points", points)
                .putInt("campaign.weapons", weaponMask)
                .putBoolean("campaign.lion", lionAlly)
                .putBoolean("campaign.heli", heliUnlocked)
                .putBoolean("campaign.flightTrained", flightTrained);
        for (int i = 0; i < ranks.length; i++) {
            editor.putInt("campaign.skill." + i, ranks[i]);
        }
    }

    private int legacyUnlocked() {
        int maximum = 0;
        for (String id : unlocked) {
            maximum = Math.max(maximum, LegacySaveIds.index(id));
        }
        return maximum;
    }

    private static int legacyMask(java.util.Set<String> ids) {
        int mask = 0;
        for (int i = 0; i < LegacySaveIds.LEVELS.length; i++) {
            if (ids.contains(LegacySaveIds.LEVELS[i])) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private static void readSet(
            SharedPreferences save, String name, String legacy, java.util.Set<String> target) {
        if (save.contains(name)) {
            target.addAll(save.getStringSet(name, java.util.Collections.emptySet()));
        } else {
            int mask = save.getInt(legacy, 0);
            for (int i = 0; i < LegacySaveIds.LEVELS.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    target.add(LegacySaveIds.LEVELS[i]);
                }
            }
        }
    }

    static Progression read(SharedPreferences save) {
        save = new SafePreferences(save);
        Progression p = new Progression();
        String current =
                save.getString(
                        "campaign.levelId", LegacySaveIds.at(save.getInt("campaign.level", 0)));
        p.currentLevel = Math.max(0, Campaign.indexOf(current));
        readSet(save, "campaign.completedIds", "campaign.completed", p.completed);
        readSet(save, "campaign.eggIds", "campaign.eggs", p.eggs);
        readSet(save, "campaign.trainingIds", "campaign.training", p.training);
        if (save.contains("campaign.unlockedIds")) {
            p.unlocked.addAll(
                    save.getStringSet("campaign.unlockedIds", java.util.Collections.emptySet()));
        } else {
            int last =
                    Math.min(
                            LegacySaveIds.LEVELS.length - 1,
                            Math.max(0, save.getInt("campaign.unlocked", 0)));
            for (int i = 0; i <= last; i++) {
                p.unlocked.add(LegacySaveIds.LEVELS[i]);
            }
        }
        p.unlocked.add(Campaign.level(p.currentLevel).id);
        for (int i = 0; i < Campaign.COUNT; i++) {
            if (p.isUnlocked(i)) {
                p.unlockedLevel = i;
            }
        }
        p.points = Math.max(0, Math.min(1000000, save.getInt("campaign.points", 0)));
        p.weaponMask = (save.getInt("campaign.weapons", 7) & 31) | 1;
        p.lionAlly = save.getBoolean("campaign.lion", false);
        p.heliUnlocked = save.getBoolean("campaign.heli", false);
        p.flightTrained = save.getBoolean("campaign.flightTrained", false);
        for (int i = 0; i < p.ranks.length; i++) {
            p.ranks[i] = Math.max(0, Math.min(3, save.getInt("campaign.skill." + i, 0)));
        }
        return p;
    }
}
