package com.neonhare.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reusable objective behaviors. New behavior is registered here; existing chapter files stay
 * intact.
 */
final class ObjectiveRules {

    interface Rule {

        default boolean frame(ChapterMission mission, int index, Frame frame) {
            return false;
        }

        default boolean shot(ChapterMission mission, int index, float x, float y, float size) {
            return false;
        }

        default boolean arrival(ChapterMission mission, int index, float x, float y) {
            return false;
        }

        default boolean defeat(ChapterMission mission, int index, int defeatedIndex) {
            return false;
        }
    }

    static final class Frame {

        float dt, x, y, vx, elapsed;
        boolean flying, grounded;

        boolean near(ChapterMission mission, int index) {
            return mission.near(mission.nodes[index], x, y);
        }
    }

    private static final Map<Integer, Rule> RULES = createRules();

    private static Map<Integer, Rule> createRules() {
        Map<Integer, Rule> rules = new HashMap<>();
        rules.put(
                ChapterMission.RING,
                new Rule() {

                    @Override
                    public boolean frame(ChapterMission m, int i, Frame f) {
                        return f.flying && f.near(m, i);
                    }
                });
        Rule holding = new HoldPositionRule();
        rules.put(ChapterMission.PERCH, holding);
        rules.put(ChapterMission.RESCUE, holding);
        rules.put(
                ChapterMission.CARGO,
                new Rule() {

                    @Override
                    public boolean frame(ChapterMission m, int i, Frame f) {
                        if (Math.abs(f.x + 15 - m.cartX) < m.escortRadius
                                && Math.abs(f.y + 42 - m.cartFloorY) < 100) {
                            m.cartX = Math.min(m.nodes[i].x, m.cartX + m.cartSpeed * f.dt);
                        }
                        return m.cartX >= m.nodes[i].x;
                    }
                });
        rules.put(
                ChapterMission.KEY,
                new Rule() {

                    @Override
                    public boolean frame(ChapterMission m, int i, Frame f) {
                        return i == m.count() && f.near(m, i);
                    }
                });
        rules.put(
                ChapterMission.CRYSTAL,
                new Rule() {

                    @Override
                    public boolean shot(ChapterMission m, int i, float x, float y, float size) {
                        ChapterMission.Node n = m.nodes[i];
                        return Math.abs(x + size / 2 - n.x) < 25 + size / 2
                                && Math.abs(y + size / 2 - n.y) < 31 + size / 2;
                    }
                });
        rules.put(
                ChapterMission.PORTAL,
                new Rule() {

                    @Override
                    public boolean arrival(ChapterMission m, int i, float x, float y) {
                        return i == m.count() && m.near(m.nodes[i], x, y);
                    }
                });
        rules.put(
                ChapterMission.SAND,
                new Rule() {

                    @Override
                    public boolean frame(ChapterMission m, int i, Frame f) {
                        return m.calm(f.elapsed) && f.near(m, i);
                    }
                });
        rules.put(
                ChapterMission.CAPTAIN,
                new Rule() {

                    @Override
                    public boolean defeat(ChapterMission m, int i, int defeatedIndex) {
                        return i == defeatedIndex;
                    }
                });
        rules.put(
                ChapterMission.SPEED,
                new Rule() {

                    @Override
                    public boolean frame(ChapterMission m, int i, Frame f) {
                        return Math.abs(f.vx) >= m.minSpeed && f.near(m, i);
                    }
                });
        return Collections.unmodifiableMap(rules);
    }

    static Rule rule(int mode) {
        Rule rule = RULES.get(mode);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown objective behavior " + mode);
        }
        return rule;
    }

    private ObjectiveRules() {}
}
