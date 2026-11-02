package com.neonhare.game;

/**
 * Owns simulation and rendering. A timed-out stop cannot start a second concurrent world thread.
 */
final class GameLoop {

    private final GameWorld world;

    private final GameRenderer renderer;

    private boolean resumed, surfaceReady;

    private Worker worker;

    GameLoop(GameWorld world, GameRenderer renderer) {
        this.world = world;
        this.renderer = renderer;
    }

    synchronized void resume() {
        resumed = true;
        startIfReady();
    }

    void pause() {
        Worker stopping;
        synchronized (this) {
            resumed = false;
            stopping = worker;
            if (stopping != null) {
                stopping.stopping = true;
                stopping.interrupt();
            }
            renderer.running = false;
        }
        join(stopping);
    }

    void surfaceReady(boolean ready) {
        Worker stopping = null;
        synchronized (this) {
            surfaceReady = ready;
            if (ready) {
                startIfReady();
            } else {
                stopping = worker;
                if (stopping != null) {
                    stopping.stopping = true;
                    stopping.interrupt();
                }
                renderer.running = false;
            }
        }
        join(stopping);
    }

    private void startIfReady() {
        if (!resumed || !surfaceReady || worker != null) {
            return;
        }
        worker = new Worker();
        renderer.running = true;
        worker.start();
    }

    private void join(Worker stopping) {
        if (stopping == null || stopping == Thread.currentThread()) {
            return;
        }
        try {
            stopping.join(1000);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    synchronized boolean isRunning() {
        return worker != null && !worker.stopping;
    }

    private final class Worker extends Thread {

        volatile boolean stopping;

        Worker() {
            super("NeonHareLoop");
        }

        @Override
        public void run() {
            world.claimOwnership();
            long previous = System.nanoTime(), fpsSince = previous;
            float lag = 0;
            int frames = 0;
            try {
                while (!stopping) {
                    long frame = System.nanoTime();
                    lag += Math.min((frame - previous) / 1_000_000_000f, .1f);
                    previous = frame;
                    world.processInput(true);
                    while (lag >= GameWorld.FIXED_STEP_SECONDS) {
                        if (world.screen == UiScreen.PLAY
                                && !world.paused
                                && !world.upgradeMenu
                                && !world.won
                                && !world.gameOver) {
                            world.tick(GameWorld.FIXED_STEP_SECONDS);
                        }
                        lag -= GameWorld.FIXED_STEP_SECONDS;
                    }
                    renderer.render();
                    world.flushSave(false);
                    frames++;
                    long elapsed = System.nanoTime() - fpsSince;
                    if (elapsed >= 1_000_000_000L) {
                        world.fps = frames * 1_000_000_000f / elapsed;
                        frames = 0;
                        fpsSince = System.nanoTime();
                    }
                    long wait = 16_666_667L - (System.nanoTime() - frame);
                    if (wait > 0) {
                        try {
                            Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
                        } catch (InterruptedException error) {
                            if (stopping) {
                                break;
                            }
                        }
                    }
                }
            } finally {
                Thread.interrupted();
                try {
                    world.clearInput();
                    world.store();
                    world.flushSave(true);
                } finally {
                    world.releaseOwnership();
                }
                synchronized (GameLoop.this) {
                    if (worker == this) {
                        worker = null;
                        renderer.running = false;
                        startIfReady();
                    }
                }
            }
        }
    }
}
