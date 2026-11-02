package com.neonhare.game;

import android.content.SharedPreferences;

import com.neonhare.game.GameWorld.CacheWall;
import com.neonhare.game.GameWorld.Enemy;
import com.neonhare.game.GameWorld.Item;
import com.neonhare.game.GameWorld.Relay;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

final class SessionStore {

    static final int SCHEMA_VERSION = 2;

    final SharedPreferences save;

    private final java.io.File migrationBackup;

    private java.util.Map<String, ?> beforeMigration;

    SessionStore(android.content.Context context, boolean testProfile) {
        this(context, testProfile ? "neon_hare_checks" : "neon_hare_progress");
    }

    SessionStore(android.content.Context context, String name) {
        if (!name.matches("[a-z_]{1,64}")) {
            throw new IllegalArgumentException("Invalid profile name");
        }
        save =
                new SafePreferences(
                        context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE));
        migrationBackup = new java.io.File(context.getNoBackupFilesDir(), name + "-before-v2.json");
        if (save.getInt("save.schemaVersion", 0) > SCHEMA_VERSION) {
            throw new IllegalStateException("Save format is newer than this game");
        }
        if (!save.getAll().isEmpty() && save.getInt("save.schemaVersion", 0) < SCHEMA_VERSION) {
            beforeMigration = save.getAll();
        }
    }

    private void backupBeforeMigration() {
        if (beforeMigration == null) {
            return;
        }
        if (!migrationBackup.exists()) {
            try {
                org.json.JSONObject data = new org.json.JSONObject();
                for (java.util.Map.Entry<String, ?> entry : beforeMigration.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Float
                            && (Float.isNaN((Float) value) || Float.isInfinite((Float) value))) {
                        value = String.valueOf(value);
                    }
                    data.put(
                            entry.getKey(),
                            value instanceof java.util.Set<?>
                                    ? new org.json.JSONArray((java.util.Collection<?>) value)
                                    : value);
                }
                java.io.File temporary =
                        new java.io.File(
                                migrationBackup.getParentFile(),
                                migrationBackup.getName() + ".tmp");
                try (java.io.FileWriter writer = new java.io.FileWriter(temporary)) {
                    writer.write(data.toString());
                }
                if (!temporary.renameTo(migrationBackup)) {
                    throw new java.io.IOException("Cannot commit migration backup");
                }
            } catch (Exception error) {
                throw new IllegalStateException(
                        "Cannot preserve original save before migration", error);
            }
        }
        beforeMigration = null;
    }

    void persist(GameWorld world, boolean force) {
        world.assertOwner();
        backupBeforeMigration();
        if (world.progress == null) {
            return;
        }
        SharedPreferences.Editor editor =
                save.edit()
                        .putInt("high_score", world.best)
                        .putInt("last_score", world.score)
                        .putInt("campaign.lore", world.loreMask);
        editor.putInt("save.schemaVersion", SCHEMA_VERSION)
                .putString("session.levelId", world.definition.id)
                .putInt("session.revision", world.definition.revision)
                .putString(
                        "session.checkpointId",
                        world.definition.checkpoints.get(world.checkpoint).id)
                .putInt("session.health", world.health);
        java.util.Set<String> goals = new java.util.HashSet<>();
        for (int i = 0; i < world.mission.nodes.length; i++) {
            if (world.mission.lit(i)) {
                goals.add(world.definition.goals.get(i).id);
            }
        }
        editor.putStringSet("session.goalIds", goals);
        java.util.Set<String> relayIds = new java.util.HashSet<>(),
                cacheIds = new java.util.HashSet<>();
        for (Relay relay : world.relays) {
            if (relay.active) {
                relayIds.add(relay.id);
            }
        }
        for (CacheWall cache : world.cacheWalls) {
            if (cache.broken) {
                cacheIds.add(cache.id);
            }
        }
        editor.putStringSet("session.relayIds", relayIds)
                .putStringSet("session.cacheIds", cacheIds);
        world.progress.write(editor);
        world.records.write(editor);
        editor.putInt("session.chapter", world.levelIndex)
                .putInt("session.checkpoint", world.checkpoint)
                .putInt("session.score", world.levelScore)
                .putInt("session.secrets", world.secrets)
                .putFloat("session.elapsed", Math.max(0, world.clock - world.levelStartedAt))
                .putStringSet("session.items", new HashSet<>(world.collectedItems))
                .putInt("session.gun", world.gun)
                .putInt("session.lives", world.lives)
                .putBoolean("session.won", world.won)
                .putBoolean("session.gameOver", world.gameOver)
                .putBoolean("session.bossFailed", world.bossEncounterFailed);
        int relayMask = 0, cacheMask = 0;
        for (int i = 0; i < world.relays.size(); i++) {
            if (world.relays.get(i).active) {
                relayMask |= 1 << i;
            }
        }
        for (int i = 0; i < world.cacheWalls.size(); i++) {
            if (world.cacheWalls.get(i).broken) {
                cacheMask |= 1 << i;
            }
        }
        if (world.mission != null) {
            editor.putInt("session.mission", world.mission.mask)
                    .putFloat("session.cart", world.mission.cartX);
        }
        editor.putInt("session.relays", relayMask)
                .putInt("session.caches", cacheMask)
                .putBoolean("session.encounter", world.encounterStarted)
                .putInt("session.bossLives", world.bossLives)
                .putInt("session.bossHp", world.boss.hp);
        for (int i = 0; i < world.ammo.length; i++) {
            editor.putInt("session.ammo." + i, world.ammo[i]);
        }
        if (force) {
            if (!editor.commit()) {
                throw new IllegalStateException("Unable to commit game save");
            }
        } else {
            editor.apply();
        }
    }

    void restore(GameWorld world) {
        String savedId =
                save.getString(
                        "session.levelId", LegacySaveIds.at(save.getInt("session.chapter", -1)));
        if (!save.contains("session.levelId") && save.getInt("session.chapter", -1) < 0) {
            return;
        }
        if (!savedId.equals(world.definition.id)) {
            return;
        }
        if (save.contains("session.revision")
                && save.getInt("session.revision", 0) != world.definition.revision) {
            GameDiagnostics.event("SESSION_CONTENT_CHANGED reset current run only");
            return;
        }
        world.checkpoint =
                Math.max(
                        0,
                        Math.min(
                                world.checkpointX.length - 1,
                                save.getInt("session.checkpoint", 0)));
        if (save.contains("session.checkpointId")) {
            world.checkpoint = 0;
            String pointId = save.getString("session.checkpointId", "");
            for (int i = 0; i < world.definition.checkpoints.size(); i++) {
                if (world.definition.checkpoints.get(i).id.equals(pointId)) {
                    world.checkpoint = i;
                }
            }
        }
        world.health =
                Math.max(
                        0,
                        Math.min(
                                world.maximumHealth(),
                                save.getInt("session.health", world.maximumHealth())));
        world.levelScore = Math.max(0, save.getInt("session.score", 0));
        world.secrets = Math.max(0, save.getInt("session.secrets", 0));
        world.levelStartedAt = world.clock - Math.max(0, save.getFloat("session.elapsed", 0));
        java.util.Set<String> collected =
                save.getStringSet("session.items", Collections.emptySet());
        world.collectedItems.clear();
        for (Iterator<Item> iterator = world.items.iterator(); iterator.hasNext(); ) {
            Item item = iterator.next();
            String legacy =
                    item.kind
                            + ":"
                            + item.value
                            + ":"
                            + Math.round(item.x)
                            + ":"
                            + Math.round(item.y);
            if (collected.contains(item.id) || collected.contains(legacy)) {
                world.collectedItems.add(item.id);
                iterator.remove();
            }
        }
        if (world.mission != null) {
            world.mission.mask =
                    save.getInt("session.mission", 0) & ((1 << world.mission.nodes.length) - 1);
            if (save.contains("session.goalIds")) {
                world.mission.mask = 0;
                java.util.Set<String> completed =
                        save.getStringSet("session.goalIds", Collections.emptySet());
                for (int i = 0; i < world.definition.goals.size(); i++) {
                    if (completed.contains(world.definition.goals.get(i).id)) {
                        world.mission.mask |= 1 << i;
                    }
                }
            }
            world.mission.cartX =
                    Math.max(
                            world.mission.cartStartX,
                            Math.min(
                                    world.mission.nodes.length == 0
                                            ? world.mission.cartStartX
                                            : world.mission.nodes[0].x,
                                    save.getFloat("session.cart", world.mission.cartStartX)));
            if (!world.mission.nodesAreOptional() && world.mission.done()) {
                world.setGate(true);
            }
            for (Iterator<Enemy> it = world.enemies.iterator(); it.hasNext(); ) {
                Enemy enemy = it.next();
                if (enemy.questIndex >= 0 && world.mission.lit(enemy.questIndex)) {
                    it.remove();
                }
            }
        }
        int relayMask = save.getInt("session.relays", 0),
                cacheMask = save.getInt("session.caches", 0);
        for (int i = 0; i < world.relays.size(); i++) {
            world.relays.get(i).active =
                    save.contains("session.relayIds")
                            ? save.getStringSet("session.relayIds", Collections.emptySet())
                                    .contains(world.relays.get(i).id)
                            : (relayMask & (1 << i)) != 0;
        }
        for (int i = 0; i < world.cacheWalls.size(); i++) {
            if (save.contains("session.cacheIds")
                    ? save.getStringSet("session.cacheIds", Collections.emptySet())
                            .contains(world.cacheWalls.get(i).id)
                    : (cacheMask & (1 << i)) != 0) {
                CacheWall cache = world.cacheWalls.get(i);
                cache.broken = true;
                world.wall[cache.y][cache.x] = false;
            }
        }
        boolean all = !world.relays.isEmpty();
        for (Relay relay : world.relays) {
            all &= relay.active;
        }
        if (all) {
            world.setGate(true);
        }
        for (int i = 0; i < world.ammo.length; i++) {
            world.ammo[i] = Math.max(0, save.getInt("session.ammo." + i, world.ammo[i]));
        }
        world.gun = Math.max(0, Math.min(world.ammo.length - 1, save.getInt("session.gun", 0)));
        world.lives =
                Math.max(
                        1,
                        Math.min(
                                world.maximumLives(),
                                save.getInt("session.lives", world.maximumLives())));
        world.hero.x = world.checkpointX[world.checkpoint];
        world.hero.y = world.checkpointY[world.checkpoint] - 42 - .01f;
        world.hero.inv = 2.5f;
        world.hero.vx = world.hero.vy = 0;
        world.hero.ground = true;
        world.cam =
                world.clamp(
                        world.hero.x - world.viewWidth * .35f,
                        0,
                        world.widthInTiles * world.TILE_SIZE - world.viewWidth);
        world.camY =
                world.clamp(world.hero.y - 290, 0, world.heightInTiles * world.TILE_SIZE - 560);
        if (Campaign.boss(world.levelIndex) >= 0 && save.getBoolean("session.encounter", false)) {
            world.encounterStarted = true;
            world.setGate(true);
            world.bossLives = Math.max(0, Math.min(3, save.getInt("session.bossLives", 3)));
            world.boss.hp =
                    Math.max(
                            0,
                            Math.min(
                                    world.boss.maxHp,
                                    save.getInt("session.bossHp", world.boss.maxHp)));
            world.boss.defeated = world.boss.hp == 0;
            world.boss.active = false;
            world.hero.x = 140 * world.TILE_SIZE;
            world.hero.y = 13 * world.TILE_SIZE - 42 - .01f;
            world.camY = 0;
            world.cam =
                    world.clamp(
                            world.hero.x - world.viewWidth * .35f,
                            0,
                            world.widthInTiles * world.TILE_SIZE - world.viewWidth);
        }
        world.won = save.getBoolean("session.won", false);
        world.gameOver = save.getBoolean("session.gameOver", false);
        world.bossEncounterFailed = save.getBoolean("session.bossFailed", false);
        world.diagnostic("SESSION_RESTORED");
    }
}
