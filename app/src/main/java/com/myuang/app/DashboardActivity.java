package com.myuang.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DashboardActivity extends BaseActivity {
    private MyUangRepository repository;
    private ListenerRegistration userListener;
    private ListenerRegistration transactionListener;
    private TextView balanceText;
    private TextView statusText;
    private TextView todayExpenseText;
    private TextView totalIncomeText;
    private TextView dashboardTipText;
    private LinearLayout recentTransactionsList;
    private View[] bars;
    private double currentSaldo;
    private List<FinanceTransaction> currentTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        if (!requireAuthenticated()) {
            return;
        }
        bindBottomNav("dashboard");

        repository = MyUangRepository.get(this);
        balanceText = findViewById(R.id.textDashboardBalance);
        statusText = findViewById(R.id.textDashboardStatus);
        todayExpenseText = findViewById(R.id.textDashboardTodayExpense);
        totalIncomeText = findViewById(R.id.textDashboardTotalIncome);
        dashboardTipText = findViewById(R.id.textDashboardTip);
        recentTransactionsList = findViewById(R.id.listRecentTransactions);
        bars = new View[]{
                findViewById(R.id.barDashboard0),
                findViewById(R.id.barDashboard1),
                findViewById(R.id.barDashboard2),
                findViewById(R.id.barDashboard3),
                findViewById(R.id.barDashboard4),
                findViewById(R.id.barDashboard5),
                findViewById(R.id.barDashboard6)
        };

        findViewById(R.id.btnAddTransaction).setOnClickListener(v -> openScreen(AddTransactionActivity.class));
        findViewById(R.id.btnDashboardNotification).setOnClickListener(v -> toast(getString(R.string.toast_dashboard_sync)));
        findViewById(R.id.linkDashboardSeeAll).setOnClickListener(v -> openScreen(AnalyticsActivity.class));

        listenForData();
    }

    @Override
    protected void onDestroy() {
        if (userListener != null) {
            userListener.remove();
        }
        if (transactionListener != null) {
            transactionListener.remove();
        }
        super.onDestroy();
    }

    private void listenForData() {
        userListener = repository.listenUser(new MyUangRepository.UserCallback() {
            @Override
            public void onUser(MyUangRepository.UserProfile user) {
                currentSaldo = user.saldo;
                renderDashboard();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });

        transactionListener = repository.listenTransactions(new MyUangRepository.TransactionsCallback() {
            @Override
            public void onTransactions(List<FinanceTransaction> transactions) {
                currentTransactions = transactions;
                renderDashboard();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    private void renderDashboard() {
        balanceText.setText(formatCurrency(currentSaldo));
        statusText.setText(currentTransactions.size() + " transaksi");
        todayExpenseText.setText(formatCurrency(todayExpense()));
        totalIncomeText.setText(formatCurrency(totalByType(FinanceTransaction.TYPE_INCOME)));
        dashboardTipText.setText(AiRecommendationEngine.analyze(currentTransactions, currentSaldo).dashboardTip);
        renderBars();
        renderRecentTransactions();
    }

    private double todayExpense() {
        Calendar today = Calendar.getInstance();
        double total = 0;
        for (FinanceTransaction transaction : currentTransactions) {
            if (transaction.isExpense() && isSameDay(today, transaction.createdAt)) {
                total += transaction.amount;
            }
        }
        return total;
    }

    private double totalByType(String type) {
        double total = 0;
        for (FinanceTransaction transaction : currentTransactions) {
            if (type.equals(transaction.type)) {
                total += transaction.amount;
            }
        }
        return total;
    }

    private void renderBars() {
        double[] totals = lastSevenDayExpenses();
        double max = 0;
        for (double total : totals) {
            max = Math.max(max, total);
        }
        for (int i = 0; i < bars.length; i++) {
            int height = max <= 0 ? 8 : 12 + (int) Math.round((totals[i] / max) * 116);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bars[i].getLayoutParams();
            params.height = dp(height);
            bars[i].setLayoutParams(params);
        }
    }

    private double[] lastSevenDayExpenses() {
        double[] totals = new double[7];
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, -6);
        resetTime(start);

        for (FinanceTransaction transaction : currentTransactions) {
            if (!transaction.isExpense()) {
                continue;
            }
            Date createdAt = transaction.createdAt == null ? new Date(0) : transaction.createdAt;
            Calendar txDay = Calendar.getInstance();
            txDay.setTime(createdAt);
            resetTime(txDay);
            long diff = (txDay.getTimeInMillis() - start.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
            if (diff >= 0 && diff < 7) {
                totals[(int) diff] += transaction.amount;
            }
        }
        return totals;
    }

    private void renderRecentTransactions() {
        recentTransactionsList.removeAllViews();
        if (currentTransactions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada transaksi. Tambahkan transaksi pertama untuk melihat riwayat.");
            empty.setTextColor(getColorCompat(R.color.on_surface_variant));
            empty.setTextSize(14);
            empty.setPadding(0, dp(12), 0, 0);
            recentTransactionsList.addView(empty);
            return;
        }

        int limit = Math.min(3, currentTransactions.size());
        for (int i = 0; i < limit; i++) {
            recentTransactionsList.addView(createTransactionRow(currentTransactions.get(i)));
        }
    }

    private View createTransactionRow(FinanceTransaction transaction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackgroundResource(R.drawable.bg_card);
        row.setElevation(dp(2));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconForCategory(transaction.category));
        icon.setColorFilter(getColorCompat(colorForTransaction(transaction)));
        icon.setBackgroundResource(backgroundForCategory(transaction.category));
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.leftMargin = dp(12);
        row.addView(textColumn, textParams);

        TextView title = new TextView(this);
        title.setText(transaction.note == null || transaction.note.isEmpty() ? transaction.category : transaction.note);
        title.setTextColor(getColorCompat(R.color.on_surface));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        textColumn.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(transaction.label == null || transaction.label.isEmpty()
                ? transaction.category
                : transaction.category + "  " + transaction.label);
        subtitle.setTextColor(getColorCompat(R.color.on_surface_variant));
        subtitle.setTextSize(14);
        textColumn.addView(subtitle);

        TextView amount = new TextView(this);
        amount.setText((transaction.isIncome() ? "+" : "-") + formatCurrency(transaction.amount));
        amount.setTextColor(getColorCompat(transaction.isIncome() ? R.color.success : R.color.danger));
        amount.setTextSize(18);
        amount.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(amount);
        return row;
    }

    private int iconForCategory(String category) {
        String lower = category == null ? "" : category.toLowerCase();
        if (lower.contains("transport")) {
            return R.drawable.ic_transport;
        }
        if (lower.contains("belanja") || lower.contains("shopping")) {
            return R.drawable.ic_shopping;
        }
        if (lower.contains("income") || lower.contains("gaji") || lower.contains("salary")) {
            return R.drawable.ic_income;
        }
        return R.drawable.ic_food;
    }

    private int colorForTransaction(FinanceTransaction transaction) {
        if (transaction.isIncome()) {
            return R.color.success;
        }
        String lower = transaction.category == null ? "" : transaction.category.toLowerCase();
        if (lower.contains("transport")) {
            return R.color.primary;
        }
        if (lower.contains("belanja") || lower.contains("shopping")) {
            return R.color.success;
        }
        return R.color.warning;
    }

    private int backgroundForCategory(String category) {
        String lower = category == null ? "" : category.toLowerCase();
        if (lower.contains("transport")) {
            return R.drawable.bg_chip_blue;
        }
        if (lower.contains("belanja") || lower.contains("shopping")) {
            return R.drawable.bg_chip_green;
        }
        return R.drawable.bg_chip_warning;
    }

    private boolean isSameDay(Calendar day, Date date) {
        if (date == null) {
            return false;
        }
        Calendar other = Calendar.getInstance();
        other.setTime(date);
        return day.get(Calendar.YEAR) == other.get(Calendar.YEAR)
                && day.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR);
    }

    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
