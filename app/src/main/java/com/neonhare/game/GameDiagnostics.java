package com.neonhare.game;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded local diagnostics. Routine file IO never runs on the simulation or UI thread. */
final class GameDiagnostics {

    private static final Object FILE_LOCK = new Object();

    private static final ThreadPoolExecutor WRITER =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(256),
                    task -> {
                        Thread thread = new Thread(task, "NeonHareDiagnostics");
                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.DiscardPolicy());

    private static volatile File journal;

    private static boolean installed;

    static synchronized void install(Context context) {
        File directory = context.getExternalFilesDir("diagnostics");
        if (directory == null) {
            directory = new File(context.getNoBackupFilesDir(), "diagnostics");
        }
        if (directory.isDirectory() || directory.mkdirs()) {
            journal = new File(directory, "events.txt");
        }
        if (!installed) {
            installed = true;
            Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(
                    (thread, error) -> {
                        String message =
                                "FATAL thread="
                                        + thread.getName()
                                        + "\n"
                                        + Log.getStackTraceString(error);
                        Log.e("NeonHareEvents", message);
                        // Fatal path is synchronous so process termination cannot discard the crash
                        // itself.
                        append(System.currentTimeMillis(), message);
                        if (previous != null) {
                            previous.uncaughtException(thread, error);
                        } else {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(10);
                        }
                    });
        }
        event(
                "ACTIVITY_CREATE version="
                        + BuildConfig.VERSION_NAME
                        + " pid="
                        + android.os.Process.myPid());
    }

    static void event(String message) {
        String bounded =
                message.length() > 16384 ? message.substring(0, 16384) + " [truncated]" : message;
        Log.i("NeonHareEvents", bounded);
        long timestamp = System.currentTimeMillis();
        WRITER.execute(() -> append(timestamp, bounded));
    }

    private static void append(long timestamp, String message) {
        synchronized (FILE_LOCK) {
            File file = journal;
            if (file == null) {
                return;
            }
            try {
                if (file.length() > 512 * 1024) {
                    File previous = new File(file.getParentFile(), "events-previous.txt");
                    if (previous.exists() && !previous.delete()) {
                        return;
                    }
                    if (!file.renameTo(previous)) {
                        return;
                    }
                }
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(timestamp + " " + message + "\n");
                }
            } catch (IOException error) {
                Log.w("NeonHareEvents", "Cannot write diagnostic journal", error);
            }
        }
    }

    private GameDiagnostics() {}
}
