package dev.local.samsungalarmwidget;

import android.content.Context;
import android.content.SharedPreferences;

final class AlarmStateCache {
    private static final String PREFS = "alarm_state_cache";
    private static final String KEY_CACHED = "cached";
    private static final String KEY_TRIGGER = "trigger";

    private final SharedPreferences preferences;

    AlarmStateCache(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean hasValue() {
        return preferences.getBoolean(KEY_CACHED, false);
    }

    Long get() {
        if (!hasValue() || !preferences.contains(KEY_TRIGGER)) return null;
        return preferences.getLong(KEY_TRIGGER, 0L);
    }

    void put(Long triggerMillis) {
        if (hasValue()) {
            Long current = get();
            if (current == null ? triggerMillis == null : current.equals(triggerMillis)) return;
        }
        SharedPreferences.Editor editor = preferences.edit().putBoolean(KEY_CACHED, true);
        if (triggerMillis == null) editor.remove(KEY_TRIGGER);
        else editor.putLong(KEY_TRIGGER, triggerMillis);
        editor.apply();
    }
}
