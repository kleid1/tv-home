package org.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * The remote's app buttons. With Google TV's home switched off nothing answers them, so this service catches the
 * keys the profile names and opens their apps. It runs in its own small process (":keys"), so a busy Home screen
 * can never delay a key press. Only real remote presses reach it: keys injected with adb skip accessibility.
 * Holding a filtering service costs held-volume ramping on TCL firmware (one step per press).
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
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }
}
