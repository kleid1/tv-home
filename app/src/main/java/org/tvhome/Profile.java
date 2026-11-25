package org.tvhome;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * This TV's setup, written by tools/make-profile.py and pushed to the app's files folder as tv-profile.json:
 * the Home apps, the remote's buttons, the Live TV source, the clock and the headphones guard.
 * Read fresh where needed, so a new push applies without reinstalling.
 */
final class Profile {
    static final String FILE = "tv-profile.json";

    final JSONObject json;

    private Profile(JSONObject json) {
        this.json = json;
    }

    static Profile load(Context context) {
        File f = new File(context.getExternalFilesDir(null), FILE);
        try {
            return new Profile(new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)));
        } catch (Exception missingOrBroken) {
            return new Profile(new JSONObject());
        }
    }

    JSONArray apps() {
        JSONArray a = json.optJSONArray("apps");
        return a != null ? a : new JSONArray();
    }

    /** The package of a Home app id, or null. */
    String packageOf(String id) {
        JSONArray a = apps();
        for (int i = 0; i < a.length(); i++) {
            JSONObject app = a.optJSONObject(i);
            if (app != null && id.equals(app.optString("id"))) return app.optString("package", null);
        }
        return null;
    }

    /** What a remote key code opens: an app id, "settings", or null for keys left to the TV. */
    String keyTarget(int keyCode) {
        JSONObject k = json.optJSONObject("keys");
        return k == null ? null : k.optString(String.valueOf(keyCode), null);
    }

    String liveTvPackage() {
        JSONObject t = json.optJSONObject("liveTv");
        return t == null ? null : t.optString("package", null);
    }

    /** "antenna" opens the TV tuner directly; anything else opens the Live TV app as it is. */
    boolean liveTvAntenna() {
        JSONObject t = json.optJSONObject("liveTv");
        return t != null && "antenna".equals(t.optString("source"));
    }

    boolean waitForMeDefault() {
        JSONObject h = json.optJSONObject("headphones");
        return h == null || h.optBoolean("waitForMe", true);
    }
}
