package org.tvhome;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What CleanFrame's device.js asks of the TV, answered on Android. Exposed to the page as window.AndroidTV and used
 * by RemoteKeys for the remote's buttons. Methods return at once: "" for success or a short message, or JSON.
 */
final class Bridge {
    /** Icons of installed apps, served to the page at /icons/ (HomeActivity's asset loader). */
    static final String ICON_DIR = "icons";

    private final Context context;
    private BluetoothA2dp a2dp;

    Bridge(Context context) {
        this.context = context;
    }

    /** Headphones need the Bluetooth audio profile; only the Home screen asks for it. */
    void connectBluetooth() {
        BluetoothAdapter adapter = bluetooth();
        if (adapter == null) return;
        try {
            adapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
                @Override public void onServiceConnected(int profile, BluetoothProfile proxy) { a2dp = (BluetoothA2dp) proxy; }
                @Override public void onServiceDisconnected(int profile) { a2dp = null; }
            }, BluetoothProfile.A2DP);
        } catch (SecurityException noPermission) {
            // Headphones stays empty until the Nearby devices permission is granted (tools/install.sh grants it).
        }
    }

    // ---- Profile and apps -------------------------------------------------------------------------------------

    @JavascriptInterface
    public String profile() {
        return Profile.load(context).json.toString();
    }

    @JavascriptInterface
    public String launch(String pkg) {
        Profile p = Profile.load(context);
        if (pkg.equals(p.liveTvPackage()) && p.liveTvAntenna()) {
            String tuner = tunerInput();
            if (tuner != null) return openInput(tuner);
        }
        PackageManager pm = context.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i == null ? "This app is not installed." : start(i);
    }

    @JavascriptInterface
    public String settings() {
        return start(new Intent(Settings.ACTION_SETTINGS));
    }

    @JavascriptInterface
    public String network() {
        return start(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    /** Installed TV apps for All apps: [{id, name, icon}], icon served at /icons/<package>.png. */
    @JavascriptInterface
    public String apps() {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> found = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        File dir = new File(context.getCacheDir(), ICON_DIR);
        dir.mkdirs();
        JSONArray out = new JSONArray();
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(context.getPackageName())) continue;
            try {
                saveIcon(ri.loadIcon(pm), new File(dir, pkg + ".png"));
                out.put(new JSONObject().put("id", pkg).put("name", String.valueOf(ri.loadLabel(pm)))
                        .put("icon", "/" + ICON_DIR + "/" + pkg + ".png"));
            } catch (Exception skip) {
                // an app whose label or icon cannot be read is left out
            }
        }
        return out.toString();
    }

    // ---- Inputs -----------------------------------------------------------------------------------------------

    /** HDMI inputs: [{number, name, connected, id}]. */
    @JavascriptInterface
    public String sources() {
        JSONArray out = new JSONArray();
        TvInputManager tim = tvInputs();
        if (tim == null) return out.toString();
        int n = 0;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.getType() != TvInputInfo.TYPE_HDMI || info.getParentId() != null) continue;
            try {
                out.put(new JSONObject().put("number", ++n).put("name", String.valueOf(info.loadLabel(context)))
                        .put("connected", tim.getInputState(info.getId()) == TvInputManager.INPUT_STATE_CONNECTED)
                        .put("id", info.getId()));
            } catch (Exception skip) {
                // unreadable input: not listed
            }
        }
        return out.toString();
    }

    @JavascriptInterface
    public String openInput(String inputId) {
        Intent view = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(TvContract.buildChannelUriForPassthroughInput(inputId), "vnd.android.cursor.item/channel");
        String pkg = Profile.load(context).liveTvPackage();
        String err = pkg == null ? "no Live TV app" : start(new Intent(view).setPackage(pkg));
        return err.isEmpty() ? err : start(view);
    }

    /** The TV's own antenna tuner: a tuner input from the TV maker's input service, not an internet channel app. */
    private String tunerInput() {
        TvInputManager tim = tvInputs();
        if (tim == null) return null;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.getType() == TvInputInfo.TYPE_TUNER && info.getId().contains("TunerInputService")) return info.getId();
        }
        return null;
    }

    private TvInputManager tvInputs() {
        return (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
    }

    // ---- Headphones -------------------------------------------------------------------------------------------

    /** Paired headphones and speakers: [{address, name, connected}]. */
    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public String headphones() {
        JSONArray out = new JSONArray();
        BluetoothAdapter adapter = bluetooth();
        if (adapter == null || a2dp == null) return out.toString();
        try {
            Set<String> on = new HashSet<>();
            for (BluetoothDevice d : a2dp.getConnectedDevices()) on.add(d.getAddress());
            for (BluetoothDevice d : adapter.getBondedDevices()) {
                if (!Link.isAudio(d.getBluetoothClass())) continue;
                out.put(new JSONObject().put("address", d.getAddress())
                        .put("name", d.getName() == null ? d.getAddress() : d.getName())
                        .put("connected", on.contains(d.getAddress())));
            }
        } catch (Exception noPermission) {
            // listed as empty
        }
        return out.toString();
    }

    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public String connectHeadphones(String address, boolean connect) {
        BluetoothDevice d = device(address);
        if (d == null || a2dp == null) return "These headphones are not paired with the TV.";
        Guard.allow(context, address, connect);   // connected here: the guard lets them stay
        return Link.set(a2dp, d, connect) ? "" : "The TV refused. Try again in the TV's Bluetooth settings.";
    }

    /** Unpairs; where Android refuses (newer versions keep this for system apps), opens Bluetooth settings. */
    @JavascriptInterface
    public String forgetHeadphones(String address) {
        BluetoothDevice d = device(address);
        if (d == null) return "";
        try {
            if ((Boolean) BluetoothDevice.class.getMethod("removeBond").invoke(d)) return "";
        } catch (ReflectiveOperationException | SecurityException refused) {
            // fall through to Settings
        }
        return start(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    @JavascriptInterface
    public String pairHeadphones() {
        return start(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    @JavascriptInterface
    public boolean waitForMe() {
        return Guard.enabled(context);
    }

    @JavascriptInterface
    public void setWaitForMe(boolean on) {
        Guard.setEnabled(context, on);
    }

    private BluetoothAdapter bluetooth() {
        BluetoothManager bm = context.getSystemService(BluetoothManager.class);
        return bm == null ? null : bm.getAdapter();
    }

    private BluetoothDevice device(String address) {
        BluetoothAdapter adapter = bluetooth();
        try {
            return adapter == null ? null : adapter.getRemoteDevice(address);
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    // ---- Helpers ----------------------------------------------------------------------------------------------

    private String start(Intent i) {
        try {
            context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return "";
        } catch (ActivityNotFoundException | SecurityException e) {
            return "The TV could not open this.";
        }
    }

    private static void saveIcon(Drawable d, File f) throws Exception {
        if (f.exists()) return;
        int w = Math.max(1, Math.min(256, d.getIntrinsicWidth())), h = Math.max(1, Math.min(256, d.getIntrinsicHeight()));
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, w, h);
        d.draw(new Canvas(b));
        try (FileOutputStream o = new FileOutputStream(f)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
        }
        b.recycle();
    }
}
