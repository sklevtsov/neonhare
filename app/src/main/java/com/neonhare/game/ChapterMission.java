package com.neonhare.game;

/**
 * Chapter-specific objectives, separate from scenery and boss combat. Coordinates are world pixels.
 */
final class ChapterMission {

    static final int RING = 0,
            PERCH = 1,
            CARGO = 2,
            KEY = 3,
            CRYSTAL = 4,
            PORTAL = 5,
            SAND = 6,
            CAPTAIN = 7,
            SPEED = 8,
            RESCUE = 9;

    static final String[] NAMES = missionNames();

    static final String[] HELP = Campaign.BRIEFS;

    private static String[] missionNames() {
        String[] result = new String[Campaign.COUNT];
        for (int i = 0; i < result.length; i++) {
            result[i] = Campaign.level(i).missionName;
        }
        return result;
    }

    static final class Node {

        final float x, y;

        final int mode;

        Node(float x, float y, int mode, boolean worldCoordinates) {
            this.x = x;
            this.y = y;
            this.mode = mode;
        }

        Node(int column, int floor, int mode) {
            x = column * 36;
            y = floor * 36 - (mode == RING ? 90 : 24);
            this.mode = mode;
        }
    }

    final int chapter;

    final Node[] nodes;

    int mask;

    float hold, cartX;

    final LevelDefinition.MissionKind kind;

    final float cartFloorY,
            cartSpeed,
            holdSeconds,
            minSpeed,
            calmSeconds,
            stormSeconds,
            escortRadius,
            cartStartX;

    ChapterMission(int chapter) {
        this.chapter = chapter;
        LevelDefinition definition = Campaign.level(chapter);
        kind = definition.missionKind;
        nodes = new Node[definition.goals.size()];
        holdProgress = new float[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            LevelDefinition.Goal goal = definition.goals.get(i);
            nodes[i] = new Node(goal.x, goal.y, goal.type, true);
        }
        cartFloorY = definition.cartFloorY;
        cartSpeed = definition.cartSpeed;
        holdSeconds = definition.holdSeconds;
        minSpeed = definition.minSpeed;
        calmSeconds = definition.calmSeconds;
        stormSeconds = definition.stormSeconds;
        escortRadius = definition.escortRadius;
        cartStartX = definition.cartStartX;
        cartX = cartStartX;
    }

    boolean nodesAreOptional() {
        return kind == LevelDefinition.MissionKind.EXPLORE
                || kind == LevelDefinition.MissionKind.ORDERED_RELAYS;
    }

    boolean done() {
        return mask == ((1 << nodes.length) - 1);
    }

    boolean lit(int index) {
        return (mask & (1 << index)) != 0;
    }

    int count() {
        return Integer.bitCount(mask);
    }

    boolean calm(float elapsed) {
        return elapsed % (calmSeconds + stormSeconds) < calmSeconds;
    }

    boolean near(Node node, float x, float y) {
        return Math.abs(x + 15 - node.x) < 37 && Math.abs(y + 21 - node.y) < 45;
    }

    private final ObjectiveRules.Frame frame = new ObjectiveRules.Frame();

    final float[] holdProgress;

    void step(
            float dt, float x, float y, float vx, boolean flying, boolean grounded, float elapsed) {
        frame.dt = dt;
        frame.x = x;
        frame.y = y;
        frame.vx = vx;
        frame.flying = flying;
        frame.grounded = grounded;
        frame.elapsed = elapsed;
        hold = 0;
        for (int i = 0; i < nodes.length; i++) {
            if (!lit(i) && ObjectiveRules.rule(nodes[i].mode).frame(this, i, frame)) {
                mask |= 1 << i;
            }
        }
    }

    boolean shoot(float x, float y, float size) {
        for (int i = 0; i < nodes.length; i++) {
            if (!lit(i) && ObjectiveRules.rule(nodes[i].mode).shot(this, i, x, y, size)) {
                mask |= 1 << i;
                return true;
            }
        }
        return false;
    }

    void arrive(float x, float y) {
        for (int i = 0; i < nodes.length; i++) {
            if (!lit(i) && ObjectiveRules.rule(nodes[i].mode).arrival(this, i, x, y)) {
                mask |= 1 << i;
                return;
            }
        }
    }

    void captain(int index) {
        for (int i = 0; i < nodes.length; i++) {
            if (!lit(i) && ObjectiveRules.rule(nodes[i].mode).defeat(this, i, index)) {
                mask |= 1 << i;
            }
        }
    }
}
