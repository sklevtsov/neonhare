package com.neonhare.game;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public final class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        GameDiagnostics.install(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        gameView =
                new GameView(
                        this,
                        BuildConfig.DEBUG && getIntent().getBooleanExtra("test_profile", false));
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(android.graphics.Color.BLACK);
        root.addView(gameView, new android.widget.FrameLayout.LayoutParams(-1, -1));
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            root.setOnApplyWindowInsetsListener(
                    (v, insets) -> {
                        android.view.DisplayCutout cutout = insets.getDisplayCutout();
                        v.setPadding(
                                cutout == null ? 0 : cutout.getSafeInsetLeft(),
                                cutout == null ? 0 : cutout.getSafeInsetTop(),
                                cutout == null ? 0 : cutout.getSafeInsetRight(),
                                cutout == null ? 0 : cutout.getSafeInsetBottom());
                        return insets;
                    });
        }
        setContentView(root);
        gameView.requestFocus();
        immersive();
    }

    private void immersive() {
        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        immersive();
        gameView.resume();
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (gameView != null && gameView.gameKey(event.getKeyCode())) {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                return gameView.onKeyDown(event.getKeyCode(), event);
            }
            if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                return gameView.onKeyUp(event.getKeyCode(), event);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        /* Some handheld profiles map B to BACK. */
    }

    @Override
    protected void onPause() {
        gameView.pause();
        gameView.clearInput();
        super.onPause();
    }
}
