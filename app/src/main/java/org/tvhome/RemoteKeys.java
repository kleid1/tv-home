package org.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;

/**
 * The remote's app buttons. With Google TV's home switched off nothing answers them, so this service catches the
 * keys the profile names and opens their apps. It runs in its own small process (":keys"), so a busy Home screen
 * can never delay a key press. Only real remote presses reach it: keys injected with adb skip accessibility.
 * Holding a filtering service costs held-volume ramping on TCL firmware (one step per press).
 * It also presses Start now on Google TV's screensaver page when Home asks for the screensaver (Bridge.screensaver).
 */
public class RemoteKeys extends AccessibilityService {
    private Bridge bridge;

    @Override
    protected void onServiceConnected() {
        bridge = new Bridge(this);
        // TCL firmware ignores the XML flag on its own; ask for key filtering again at runtime.
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (bridge == null) return false;
        Profile profile = Profile.load(this);
        String target = profile.keyTarget(event.getKeyCode());
        if (target == null) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if ("settings".equals(target)) {
                bridge.settings();
            } else {
                String pkg = profile.packageOf(target);
                if (pkg != null) bridge.launch(pkg);
            }
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        File request = new File(getFilesDir(), Bridge.SAVER_REQUEST);
        if (!request.exists()) return;
        boolean fresh = System.currentTimeMillis() - request.lastModified() < 5000;
        if (!fresh) { request.delete(); return; }
        if (!"com.android.tv.settings".contentEquals(String.valueOf(event.getPackageName()))) return;
        request.delete();
        startScreensaver(0);
    }

    /** Clicks the focused row of the screensaver page (Start now), trying again while the page settles, then
     *  starts Home beneath the screensaver. */
    private void startScreensaver(int attempt) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo row = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (row != null && row.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            // Put Home back under the screensaver, so waking it lands on Home, not on the screensaver page.
            new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)), 1500);
            return;
        }
        if (attempt < 10) new Handler(Looper.getMainLooper()).postDelayed(() -> startScreensaver(attempt + 1), 150);
    }

    @Override
    public void onInterrupt() { }
}
