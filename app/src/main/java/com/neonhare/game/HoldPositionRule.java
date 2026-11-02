package com.neonhare.game;

/** Shared landing/rescue behavior with independent timers for each objective. */
final class HoldPositionRule implements ObjectiveRules.Rule {

    @Override
    public boolean frame(ChapterMission mission, int index, ObjectiveRules.Frame frame) {
        if (!frame.near(mission, index) || !frame.grounded || Math.abs(frame.vx) >= 35) {
            mission.holdProgress[index] = 0;
            return false;
        }
        mission.holdProgress[index] += frame.dt;
        mission.hold = Math.max(mission.hold, mission.holdProgress[index] / mission.holdSeconds);
        return mission.holdProgress[index] >= mission.holdSeconds;
    }
}
