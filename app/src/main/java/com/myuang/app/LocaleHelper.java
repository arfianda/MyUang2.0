package com.myuang.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public final class LocaleHelper {
    private static final String PREFS_NAME = "myuang_locale";
    private static final String KEY_LANGUAGE = "language";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_INDONESIAN = "in";

    private LocaleHelper() {
    }

    public static Context wrap(Context context) {
        return wrap(context, getLanguage(context));
    }

    public static Context wrap(Context context, String languageCode) {
        Locale locale = new Locale(normalizeLanguage(languageCode));
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        return context.createConfigurationContext(config);
    }

    public static void setLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, normalizeLanguage(languageCode)).apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_LANGUAGE, null);
        if (saved != null) {
            return normalizeLanguage(saved);
        }

        String systemLanguage = Locale.getDefault().getLanguage();
        if ("in".equalsIgnoreCase(systemLanguage) || "id".equalsIgnoreCase(systemLanguage)) {
            return LANGUAGE_INDONESIAN;
        }
        return LANGUAGE_ENGLISH;
    }

    public static String languageFromCountry(String countryCode) {
        if (countryCode != null && "ID".equalsIgnoreCase(countryCode)) {
            return LANGUAGE_INDONESIAN;
        }
        return LANGUAGE_ENGLISH;
    }

    private static String normalizeLanguage(String languageCode) {
        if ("id".equalsIgnoreCase(languageCode) || "in".equalsIgnoreCase(languageCode)) {
            return LANGUAGE_INDONESIAN;
        }
        return LANGUAGE_ENGLISH;
    }
}
