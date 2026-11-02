package com.neonhare.game;

/** Frozen 0.3 save mapping. Never reorder this list when editing the current campaign. */
final class LegacySaveIds {

    static final String[] LEVELS = {
        "green-rift",
        "waterworks",
        "floating-gardens",
        "storm-nursery",
        "neon-convoy",
        "debt-towers",
        "crystal-memory",
        "mirror-archive",
        "scarlet-dunes",
        "sand-convoy",
        "northern-lights",
        "registry-citadel"
    };

    static String at(int index) {
        return index >= 0 && index < LEVELS.length ? LEVELS[index] : LEVELS[0];
    }

    static int index(String id) {
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private LegacySaveIds() {}
}
