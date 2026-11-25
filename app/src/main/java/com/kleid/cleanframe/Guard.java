package com.kleid.cleanframe;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/**
 * Keeps the TV from taking headphones on its own. Android TV reconnects its paired headphones whenever it starts
 * or wakes, which pulls AirPods away from the phone they are playing on. When headphones connect without having
 * been connected from the Headphones panel, the guard disconnects them a moment later. Headphones connected from the panel
 * are left alone until they disconnect. Nothing is unpaired.
 *
 * ACL_CONNECTED and ACL_DISCONNECTED are among the broadcasts Android still delivers to manifest receivers.
 */
public final class Guard extends BroadcastReceiver {
    private static final String PREFS = "guard";
    private static final long SETTLE_MS = 2500;

    /** The owner connected this device from the app: let it stay. */
    static void allow(Context ctx, String address, boolean allowed) {
        SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (allowed) e.putBoolean(address, true); else e.remove(address);
        e.apply();
    }

    /** "Wait for me to connect": on, the TV never keeps headphones it connected by itself. */
    static boolean enabled(Context ctx) {
        // Until the owner flips it on the Headphones panel, the profile's choice applies.
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, Profile.load(ctx).waitForMeDefault());
    }

    static void setEnabled(Context ctx, boolean on) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, on).apply();
    }

    private static final String KEY_ON = "guard_on";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        BluetoothDevice device = Build.VERSION.SDK_INT >= 33
                ? intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class)
                : legacyDevice(intent);
        if (device == null) return;
        String address = device.getAddress();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
            prefs.edit().remove(address).apply();   // next time it connects on its own, it is the TV's doing
            return;
        }
        if (!BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) return;
        try {
            if (!Link.isAudio(device.getBluetoothClass())) return;   // remotes and game pads are never touched
        } catch (SecurityException noPermission) {
            return;
        }
        if (!enabled(ctx) || prefs.getBoolean(address, false)) return;
        final PendingResult pending = goAsync();
        final Context app = ctx.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(() -> disconnect(app, device, pending), SETTLE_MS);
    }

    private static void disconnect(Context ctx, BluetoothDevice device, PendingResult pending) {
        BluetoothManager bm = ctx.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bm == null ? null : bm.getAdapter();
        if (adapter == null) {
            pending.finish();
            return;
        }
        try {
            adapter.getProfileProxy(ctx, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    try {
                        Link.set((BluetoothA2dp) proxy, device, false);
                    } finally {
                        adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy);
                        pending.finish();
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) { }
            }, BluetoothProfile.A2DP);
        } catch (SecurityException noPermission) {
            pending.finish();
        }
    }

    @SuppressWarnings("deprecation")
    private static BluetoothDevice legacyDevice(Intent intent) {
        return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    }
}
