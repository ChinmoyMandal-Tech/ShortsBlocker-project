package com.shortsblocker;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Watches YouTube. When the bottom-bar "Shorts" button is on screen, draws a
 * small touch-absorbing (transparent) window exactly over it. Nothing else is covered,
 * so scrolling, other tabs, search, etc. keep working.
 */
public class ShortsBlockerService extends AccessibilityService {

    private static final String YT = "com.google.android.youtube";

    private WindowManager wm;
    private View wall;
    private WindowManager.LayoutParams lp;
    private final Rect current = new Rect();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean pending = false;
    private long lastBack = 0;

    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            pending = false;
            try {
                refresh();
            } catch (Throwable ignored) {
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Debounce: process at most once every ~120 ms.
        if (!pending) {
            pending = true;
            handler.postDelayed(updater, 120);
        }
    }

    @Override
    public void onInterrupt() {
        removeWall();
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        handler.removeCallbacks(updater);
        removeWall();
        return super.onUnbind(intent);
    }

    // ------------------------------------------------------------------

    private void refresh() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || !YT.contentEquals(String.valueOf(root.getPackageName()))) {
            removeWall();
            return;
        }

        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);

        // Experimental: if a Shorts player somehow opened, go back out of it.
        if (p.getBoolean("exitShorts", false) && inShortsPlayer(root)) {
            long now = System.currentTimeMillis();
            if (now - lastBack > 1500) {
                lastBack = now;
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }

        Rect target = findShortsButton(root, p.getString("label", "Shorts"));
        if (target == null) {
            removeWall();
        } else {
            showWall(target, p.getBoolean("debug", false));
        }
    }

    private boolean inShortsPlayer(AccessibilityNodeInfo root) {
        String[] ids = {YT + ":id/reel_player_page_container", YT + ":id/reel_recycler"};
        for (String id : ids) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByViewId(id);
            if (l != null && !l.isEmpty()) return true;
        }
        return false;
    }

    private Rect findShortsButton(AccessibilityNodeInfo root, String label) {
        if (label == null || label.trim().isEmpty()) label = "Shorts";
        label = label.trim();

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(label);
        if (nodes == null) return null;

        DisplayMetrics dm = getResources().getDisplayMetrics();

        for (AccessibilityNodeInfo n : nodes) {
            if (!matches(n.getText(), label) && !matches(n.getContentDescription(), label)) continue;

            // Climb to the clickable tab container (up to 4 levels).
            AccessibilityNodeInfo c = n;
            for (int i = 0; i < 4 && c != null && !c.isClickable(); i++) c = c.getParent();
            if (c == null || !c.isClickable()) c = n;

            Rect r = new Rect();
            c.getBoundsInScreen(r);
            if (r.isEmpty()) continue;

            // Must be the bottom navigation bar: lower part of screen and button-sized.
            if (r.centerY() < dm.heightPixels * 0.82f) continue;
            if (r.width() > dm.widthPixels * 0.5f) continue;

            return r;
        }
        return null;
    }

    private boolean matches(CharSequence s, String label) {
        if (s == null) return false;
        String t = s.toString();
        return t.length() <= label.length() + 25
                && t.toLowerCase().contains(label.toLowerCase());
    }

    // ------------------------------------------------------------------

    private void showWall(Rect r, boolean debug) {
        if (wm == null) return;
        int color = debug ? Color.argb(110, 255, 0, 0) : Color.TRANSPARENT;

        if (wall == null) {
            wall = new View(this);
            wall.setClickable(true);
            wall.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent e) {
                    return true; // swallow every touch
                }
            });
            lp = new WindowManager.LayoutParams(
                    r.width(), r.height(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = r.left;
            lp.y = r.top;
            wall.setBackgroundColor(color);
            wm.addView(wall, lp);
            current.set(r);
            return;
        }

        wall.setBackgroundColor(color);
        if (!current.equals(r)) {
            lp.x = r.left;
            lp.y = r.top;
            lp.width = r.width();
            lp.height = r.height();
            wm.updateViewLayout(wall, lp);
            current.set(r);
        }
    }

    private void removeWall() {
        if (wall != null && wm != null) {
            try {
                wm.removeView(wall);
            } catch (Throwable ignored) {
            }
        }
        wall = null;
        current.setEmpty();
    }
}
