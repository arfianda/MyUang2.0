package com.myuang.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public abstract class BaseActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeHelper.wrap(LocaleHelper.wrap(newBase)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(getColorCompat(R.color.surface));
        window.setNavigationBarColor(getColorCompat(R.color.surface));
        window.getDecorView().setSystemUiVisibility(ThemeHelper.isDarkMode(this)
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    protected void bindBottomNav(String active) {
        setupNavItem(R.id.navDashboard, R.id.navDashboardIcon, R.id.navDashboardLabel, DashboardActivity.class, "dashboard".equals(active));
        setupNavItem(R.id.navAnalytics, R.id.navAnalyticsIcon, R.id.navAnalyticsLabel, AnalyticsActivity.class, "analytics".equals(active));
        setupNavItem(R.id.navAdd, R.id.navAddIcon, R.id.navAddLabel, AddTransactionActivity.class, "add".equals(active));
        setupNavItem(R.id.navTips, R.id.navTipsIcon, R.id.navTipsLabel, TipsActivity.class, "tips".equals(active));
        setupNavItem(R.id.navProfile, R.id.navProfileIcon, R.id.navProfileLabel, ProfileActivity.class, "profile".equals(active));
    }

    private void setupNavItem(int rootId, int iconId, int labelId, final Class<?> target, boolean active) {
        View root = findViewById(rootId);
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        if (root == null || icon == null || label == null) {
            return;
        }

        int activeColor = Color.WHITE;
        int inactiveColor = getColorCompat(R.color.on_surface_variant);
        root.setBackgroundResource(android.R.color.transparent);
        icon.setBackgroundResource(active ? R.drawable.bg_nav_active : android.R.color.transparent);
        icon.setColorFilter(active ? activeColor : inactiveColor);
        label.setTextColor(inactiveColor);
        root.setOnClickListener(v -> {
            if (!active) {
                Intent intent = new Intent(BaseActivity.this, target);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
    }

    protected void openScreen(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    protected void openLoginAndClearTask() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    protected boolean requireAuthenticated() {
        MyUangRepository repository = MyUangRepository.get(this);
        if (!repository.isConfigured()) {
            toast(repository.getConfigurationMessage());
            openLoginAndClearTask();
            return false;
        }
        if (!repository.isLoggedIn()) {
            openLoginAndClearTask();
            return false;
        }
        return true;
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void bindPasswordToggle(EditText input, ImageButton button) {
        button.setOnClickListener(v -> {
            boolean showPassword = !button.isSelected();
            button.setSelected(showPassword);
            input.setInputType(InputType.TYPE_CLASS_TEXT | (showPassword
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            input.setSelection(input.getText().length());
        });
    }

    protected int getColorCompat(int colorId) {
        return getResources().getColor(colorId);
    }

    protected String formatCurrency(double amount) {
        Locale locale = new Locale("in", "ID");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        String prefix = amount < 0 ? "-Rp " : "Rp ";
        return prefix + format.format(Math.round(Math.abs(amount)));
    }

    protected int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
