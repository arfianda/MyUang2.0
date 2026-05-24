package com.myuang.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public final class ThemeHelper {
    private static final String PREFS_NAME = "myuang_theme";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ThemeHelper() {
    }

    public static Context wrap(Context context) {
        Configuration config = new Configuration(context.getResources().getConfiguration());
        int nightMode = isDarkMode(context)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        return context.createConfigurationContext(config);
    }

    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }
}
