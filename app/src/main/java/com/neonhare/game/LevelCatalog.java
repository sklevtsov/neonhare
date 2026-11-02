package com.neonhare.game;

import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads packaged content once. Campaign order is separate from stable save identities. */
final class LevelCatalog {

    final List<LevelDefinition> levels;

    LevelCatalog(List<LevelDefinition> definitions) {
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Empty campaign");
        }
        Set<String> ids = new HashSet<>();
        for (LevelDefinition definition : definitions) {
            if (!ids.add(definition.id)) {
                throw new IllegalArgumentException("Duplicate campaign id");
            }
        }
        levels = Collections.unmodifiableList(new ArrayList<>(definitions));
    }

    LevelCatalog(AssetManager assets) throws IOException, JSONException {
        JSONObject index = new JSONObject(read(assets, "levels/campaign.json"));
        LevelDefinition.require(index.getInt("schemaVersion") == 1, "Unsupported campaign schema");
        JSONArray ids = index.getJSONArray("levels");
        LevelDefinition.require(ids.length() > 0 && ids.length() <= 512, "Invalid campaign length");
        Set<String> unique = new HashSet<>();
        List<LevelDefinition> loaded = new ArrayList<>();
        for (int i = 0; i < ids.length(); i++) {
            String id = ids.getString(i);
            LevelDefinition.require(
                    id.matches("[a-z][a-z0-9-]{0,63}") && unique.add(id),
                    "Invalid/duplicate campaign id");
            LevelDefinition level =
                    new LevelDefinition(
                            new JSONObject(read(assets, "levels/" + id + "/level.json")));
            LevelDefinition.require(level.id.equals(id), "Level id differs from folder: " + id);
            loaded.add(level);
        }
        levels = Collections.unmodifiableList(loaded);
    }

    static String read(AssetManager assets, String path) throws IOException {
        try (InputStream input = assets.open(path);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > 1024 * 1024) {
                    throw new IOException("Level asset exceeds 1 MiB: " + path);
                }
                bytes.write(buffer, 0, count);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    int indexOf(String id) {
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
