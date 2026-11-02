package com.neonhare.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Bounded, FIFO multi-producer mailbox. Overflow is explicit, never a silent command overwrite. */
final class CommandMailbox<T> {

    private final ArrayDeque<T> commands = new ArrayDeque<>();

    private final int capacity;

    CommandMailbox(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
    }

    synchronized boolean offer(T command) {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (commands.size() == capacity) {
            return false;
        }
        commands.addLast(command);
        return true;
    }

    synchronized T poll() {
        return commands.pollFirst();
    }

    synchronized List<T> clear() {
        List<T> discarded = new ArrayList<>(commands);
        commands.clear();
        return discarded;
    }

    synchronized int size() {
        return commands.size();
    }
}
