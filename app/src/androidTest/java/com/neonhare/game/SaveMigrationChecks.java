package com.neonhare.game;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Collections;

/** Old installed saves, stable identities, malformed data, and progress beyond a 32-bit mask. */
final class SaveMigrationChecks {

    private int checks;

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    int verify(Context context, GameWorld world) throws Exception {
        SharedPreferences save = context.getSharedPreferences("migration_contract_checks", 0);
        save.edit().clear().commit();
        save.edit()
                .putInt("campaign.level", 6)
                .putInt("campaign.unlocked", 8)
                .putInt("campaign.eggs", 1 << 6)
                .putInt("campaign.training", 1 << 1)
                .putInt("campaign.completed", (1 << 1) | (1 << 6))
                .putInt("campaign.points", 17)
                .putInt("campaign.skill.0", 2)
                .putInt("record.score.6", 12000)
                .commit();
        Progression old = Progression.read(save);
        check(
                old.currentLevel == 6
                        && old.hasEgg(6)
                        && old.hasTraining(1)
                        && old.points == 17
                        && old.ranks[0] == 2,
                "0.3 preferences migrate without losing rewards");
        check(!old.complete(6) && old.points == 17, "Old completion does not grant points twice");
        check(
                new LevelRecords(save).bestScore[6] == 12000,
                "Numeric legacy records migrate by historical level ID");
        SharedPreferences.Editor editor = save.edit();
        old.write(editor);
        editor.commit();
        save.edit().putInt("campaign.level", 0).putInt("campaign.eggs", 1).commit();
        Progression ids = Progression.read(save);
        check(
                ids.currentLevel == 6 && ids.hasEgg(6) && !ids.hasEgg(0),
                "Stable IDs take precedence over stale numeric projections");
        save.edit()
                .putString("campaign.points", "malformed")
                .putFloat("session.elapsed", Float.NaN)
                .commit();
        check(
                Progression.read(save).points == 0
                        && new SafePreferences(save).getFloat("session.elapsed", 10) == 10,
                "Wrong types and non-finite values fall back safely");
        java.lang.reflect.Field catalogField = Campaign.class.getDeclaredField("catalog");
        catalogField.setAccessible(true);
        Object original = catalogField.get(null);
        int originalCount = Campaign.COUNT;
        try {
            java.util.List<LevelDefinition> definitions = new java.util.ArrayList<>();
            String template =
                    LevelCatalog.read(context.getAssets(), "levels/green-rift/level.json");
            for (int i = 0; i < 40; i++) {
                definitions.add(
                        new LevelDefinition(
                                new JSONObject(template).put("id", "contract-level-" + i)));
            }
            LevelCatalog catalog = new LevelCatalog(definitions);
            catalogField.set(null, catalog);
            Campaign.COUNT = 40;
            Progression expanded = new Progression();
            check(
                    expanded.discover(0) && !expanded.hasEgg(32),
                    "Chapter 0 and 32 no longer alias the same bit");
            check(
                    expanded.discover(32) && expanded.train(39) && expanded.complete(33),
                    "Campaign progression supports more than 32 levels");
            save.edit().clear().commit();
            editor = save.edit();
            expanded.write(editor);
            editor.commit();
            Progression loaded = Progression.read(save);
            check(
                    loaded.hasEgg(0)
                            && loaded.hasEgg(32)
                            && loaded.hasTraining(39)
                            && !loaded.complete(33),
                    "Expanded progression survives save/reload");
            Collections.reverse(definitions);
            catalogField.set(null, new LevelCatalog(definitions));
            Progression reordered = Progression.read(save);
            check(
                    reordered.hasEgg(39) && reordered.hasEgg(7) && reordered.hasTraining(0),
                    "Reordering the campaign preserves level identities");
        } finally {
            catalogField.set(null, original);
            Campaign.COUNT = originalCount;
            save.edit().clear().commit();
        }
        save.edit()
                .clear()
                .putInt("session.chapter", 1)
                .putFloat("session.elapsed", Float.NaN)
                .putInt("campaign.points", 17)
                .commit();
        java.io.File backup =
                new java.io.File(
                        context.getNoBackupFilesDir(), "migration_contract_checks-before-v2.json");
        if (backup.exists() && !backup.delete()) {
            throw new AssertionError("Cannot clean isolated test backup");
        }
        SessionStore repository = new SessionStore(context, "migration_contract_checks");
        world.loadLevel(1);
        world.health = 1;
        world.encounterStarted = true;
        world.boss.hp = 23;
        world.bossLives = 2;
        repository.persist(world, true);
        check(
                backup.isFile() && backup.length() > 0,
                "Migration preserves a complete original snapshot before writing v2");
        world.buildLevel();
        world.health = world.maximumHealth();
        repository.restore(world);
        check(
                world.health == 1 && world.boss.hp == 23 && world.bossLives == 2,
                "Session restore does not heal Stu or replenish boss attempts");
        check(
                save.getInt("save.schemaVersion", 0) == 2
                        && save.getString("session.levelId", "").equals("waterworks"),
                "Versioned session stores stable chapter identity");
        save.edit().putInt("save.schemaVersion", 99).commit();
        boolean refused = false;
        try {
            new SessionStore(context, "migration_contract_checks");
        } catch (IllegalStateException expected) {
            refused = true;
        }
        check(
                refused && save.getInt("save.schemaVersion", 0) == 99,
                "A newer save version is refused without overwriting it");
        save.edit().clear().commit();
        world.restart();
        return checks;
    }
}
