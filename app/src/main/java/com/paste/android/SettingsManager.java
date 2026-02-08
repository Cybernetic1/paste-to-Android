package com.paste.android;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final String PREFS_NAME = "PasteToAndroidPrefs";
    private static final String KEY_DESTINATIONS = "destinations";
    private static final String KEY_SELECTED_INDEX = "selected_index";
    private static final String KEY_HAS_SSH_KEY = "has_ssh_key";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<LinuxDestination> getDestinations() {
        String json = prefs.getString(KEY_DESTINATIONS, "[]");
        List<LinuxDestination> destinations = new ArrayList<>();
        
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                destinations.add(new LinuxDestination(
                    obj.getString("name"),
                    obj.getString("user"),
                    obj.getString("host"),
                    obj.getInt("port"),
                    obj.getString("directory")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return destinations;
    }

    public void saveDestinations(List<LinuxDestination> destinations) {
        JSONArray array = new JSONArray();
        
        try {
            for (LinuxDestination dest : destinations) {
                JSONObject obj = new JSONObject();
                obj.put("name", dest.getName());
                obj.put("user", dest.getUser());
                obj.put("host", dest.getHost());
                obj.put("port", dest.getPort());
                obj.put("directory", dest.getDirectory());
                array.put(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        prefs.edit().putString(KEY_DESTINATIONS, array.toString()).apply();
    }

    public int getSelectedIndex() {
        return prefs.getInt(KEY_SELECTED_INDEX, 0);
    }

    public void setSelectedIndex(int index) {
        prefs.edit().putInt(KEY_SELECTED_INDEX, index).apply();
    }

    public boolean hasSSHKey() {
        return prefs.getBoolean(KEY_HAS_SSH_KEY, false);
    }

    public void setHasSSHKey(boolean hasKey) {
        prefs.edit().putBoolean(KEY_HAS_SSH_KEY, hasKey).apply();
    }
}
