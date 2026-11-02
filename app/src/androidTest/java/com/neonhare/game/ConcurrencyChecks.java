package com.neonhare.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Threading contracts: no overwrite, bounded capacity, FIFO per producer, and one world owner. */
final class ConcurrencyChecks {

    private int checks;

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    int verify(GameView view) throws Exception {
        CommandMailbox<Integer> small = new CommandMailbox<>(2);
        check(small.offer(10) && small.offer(20) && !small.offer(30), "Overflow is explicit");
        check(
                small.poll() == 10 && small.poll() == 20 && small.poll() == null,
                "Queued commands are never overwritten");
        CommandMailbox<Integer> mailbox = new CommandMailbox<>(4096);
        List<Thread> producers = new ArrayList<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int producer = 0; producer < 8; producer++) {
            final int id = producer;
            Thread thread =
                    new Thread(
                            () -> {
                                try {
                                    for (int i = 0; i < 200; i++) {
                                        if (!mailbox.offer(id * 1000 + i)) {
                                            throw new AssertionError("unexpected overflow");
                                        }
                                    }
                                } catch (Throwable error) {
                                    failure.set(error);
                                }
                            });
            producers.add(thread);
            thread.start();
        }
        for (Thread producer : producers) {
            producer.join(2000);
        }
        check(
                failure.get() == null && mailbox.size() == 1600,
                "Concurrent producers retain every command");
        int[] previous = new int[8];
        java.util.Arrays.fill(previous, -1);
        Integer value;
        int consumed = 0;
        while ((value = mailbox.poll()) != null) {
            int producer = value / 1000, sequence = value % 1000;
            check(sequence == previous[producer] + 1, "Per-producer FIFO order");
            previous[producer] = sequence;
            consumed++;
        }
        check(consumed == 1600, "All producer commands consumed exactly once");
        GameWorld world = view.world;
        world.restart();
        world.queueAction(GameCommand.Action.OPEN_MENU);
        world.queueAction(GameCommand.Action.CHAPTER_SELECT);
        world.queueAction(GameCommand.Action.CONTINUE);
        check(
                world.actions.size() == 3,
                "Semantic actions queue instead of overwriting one integer");
        world.applyPendingCommand();
        check(
                world.actions.isEmpty() && world.screen == UiScreen.PLAY,
                "Rapid queued menu actions finish in order");
        world.claimOwnership();
        AtomicReference<Throwable> ownerViolation = new AtomicReference<>();
        Thread foreign =
                new Thread(
                        () -> {
                            try {
                                world.tick(GameWorld.FIXED_STEP_SECONDS);
                            } catch (Throwable expected) {
                                ownerViolation.set(expected);
                            }
                        });
        foreign.start();
        foreign.join(2000);
        world.releaseOwnership();
        check(
                ownerViolation.get() instanceof IllegalStateException,
                "World rejects mutation from a foreign thread");
        for (int cycle = 0; cycle < 12; cycle++) {
            view.resume();
            Thread.sleep(12);
            view.pause();
            world.claimOwnership();
            world.releaseOwnership();
            check(true, "Lifecycle cycle " + cycle + " releases ownership before another worker");
        }
        world.processInput(false);
        return checks;
    }
}
