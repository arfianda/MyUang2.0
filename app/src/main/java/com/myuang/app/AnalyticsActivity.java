package com.myuang.app;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AnalyticsActivity extends BaseActivity {
    private MyUangRepository repository;
    private ListenerRegistration transactionListener;
    private Button weeklyButton;
    private Button monthlyButton;
    private TextView periodText;
    private TextView incomeTotal;
    private TextView expenseTotal;
    private TextView categoryOne;
    private TextView categoryTwo;
    private TextView categoryThree;
    private View[] bars;
    private boolean weeklyPeriod = true;
    private List<FinanceTransaction> currentTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        if (!requireAuthenticated()) {
            return;
        }
        bindBottomNav("analytics");

        repository = MyUangRepository.get(this);
        weeklyButton = findViewById(R.id.btnWeekly);
        monthlyButton = findViewById(R.id.btnMonthly);
        periodText = findViewById(R.id.textAnalyticsPeriod);
        incomeTotal = findViewById(R.id.textIncomeTotal);
        expenseTotal = findViewById(R.id.textExpenseTotal);
        categoryOne = findViewById(R.id.textCategorySummaryOne);
        categoryTwo = findViewById(R.id.textCategorySummaryTwo);
        categoryThree = findViewById(R.id.textCategorySummaryThree);
        bars = new View[]{
                findViewById(R.id.barAnalytics0),
                findViewById(R.id.barAnalytics1),
                findViewById(R.id.barAnalytics2),
                findViewById(R.id.barAnalytics3),
                findViewById(R.id.barAnalytics4),
                findViewById(R.id.barAnalytics5),
                findViewById(R.id.barAnalytics6),
        };

        weeklyButton.setOnClickListener(v -> setPeriod(true));
        monthlyButton.setOnClickListener(v -> setPeriod(false));
        findViewById(R.id.btnAnalyticsNotification).setOnClickListener(v -> toast(getString(R.string.toast_analytics_sync)));
        findViewById(R.id.linkAnalyticsAll).setOnClickListener(v -> toast(getString(R.string.toast_analytics_real)));

        transactionListener = repository.listenTransactions(new MyUangRepository.TransactionsCallback() {
            @Override
            public void onTransactions(List<FinanceTransaction> transactions) {
                currentTransactions = transactions;
                renderAnalytics();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (transactionListener != null) {
            transactionListener.remove();
        }
        super.onDestroy();
    }

    private void setPeriod(boolean weekly) {
        weeklyPeriod = weekly;
        weeklyButton.setBackgroundResource(weekly ? R.drawable.bg_segment_active : android.R.color.transparent);
        monthlyButton.setBackgroundResource(weekly ? android.R.color.transparent : R.drawable.bg_segment_active);
        weeklyButton.setTextColor(getColorCompat(weekly ? R.color.on_surface : R.color.on_surface_variant));
        monthlyButton.setTextColor(getColorCompat(weekly ? R.color.on_surface_variant : R.color.on_surface));
        periodText.setText(weekly ? getString(R.string.last_7_days) : getString(R.string.last_30_days));
        renderAnalytics();
    }

    private void renderAnalytics() {
        List<FinanceTransaction> periodTransactions = filterByPeriod(currentTransactions, weeklyPeriod ? 7 : 30);
        incomeTotal.setText(formatCurrency(totalByType(periodTransactions, FinanceTransaction.TYPE_INCOME)));
        expenseTotal.setText(formatCurrency(totalByType(periodTransactions, FinanceTransaction.TYPE_EXPENSE)));
        renderBars(periodTransactions);
        renderCategories(periodTransactions);
    }

    private List<FinanceTransaction> filterByPeriod(List<FinanceTransaction> transactions, int days) {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, -days + 1);
        resetTime(start);
        List<FinanceTransaction> filtered = new ArrayList<>();
        for (FinanceTransaction transaction : transactions) {
            Date createdAt = transaction.createdAt == null ? new Date(0) : transaction.createdAt;
            if (!createdAt.before(start.getTime())) {
                filtered.add(transaction);
            }
        }
        return filtered;
    }

    private double totalByType(List<FinanceTransaction> transactions, String type) {
        double total = 0;
        for (FinanceTransaction transaction : transactions) {
            if (type.equals(transaction.type)) {
                total += transaction.amount;
            }
        }
        return total;
    }

    private void renderBars(List<FinanceTransaction> transactions) {
        double[] totals = weeklyPeriod ? dailyBuckets(transactions) : monthlyBuckets(transactions);
        double max = 0;
        for (double total : totals) {
            max = Math.max(max, total);
        }
        for (int i = 0; i < bars.length; i++) {
            int height = max <= 0 ? 8 : 12 + (int) Math.round((totals[i] / max) * 128);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bars[i].getLayoutParams();
            params.height = dp(height);
            bars[i].setLayoutParams(params);
        }
    }

    private double[] dailyBuckets(List<FinanceTransaction> transactions) {
        double[] totals = new double[7];
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, -6);
        resetTime(start);
        for (FinanceTransaction transaction : transactions) {
            addToBucket(transaction, start, totals, 1);
        }
        return totals;
    }

    private double[] monthlyBuckets(List<FinanceTransaction> transactions) {
        double[] totals = new double[7];
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, -29);
        resetTime(start);
        for (FinanceTransaction transaction : transactions) {
            addToBucket(transaction, start, totals, 5);
        }
        return totals;
    }

    private void addToBucket(FinanceTransaction transaction, Calendar start, double[] totals, int bucketSizeDays) {
        if (!transaction.isExpense()) {
            return;
        }
        Date createdAt = transaction.createdAt == null ? new Date(0) : transaction.createdAt;
        Calendar txDay = Calendar.getInstance();
        txDay.setTime(createdAt);
        resetTime(txDay);
        long dayDiff = (txDay.getTimeInMillis() - start.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
        int bucket = (int) (dayDiff / bucketSizeDays);
        if (bucket >= 0 && bucket < totals.length) {
            totals[bucket] += transaction.amount;
        }
    }

    private void renderCategories(List<FinanceTransaction> transactions) {
        TextView[] views = new TextView[]{categoryOne, categoryTwo, categoryThree};
        Map<String, Double> totals = AiRecommendationEngine.categoryExpenseTotals(transactions);
        double totalExpense = 0;
        for (double amount : totals.values()) {
            totalExpense += amount;
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(totals.entrySet());
        Collections.sort(entries, (left, right) -> Double.compare(right.getValue(), left.getValue()));

        for (int i = 0; i < views.length; i++) {
            if (i >= entries.size() || totalExpense <= 0) {
                views[i].setText(i == 0 ? "Belum ada pengeluaran pada periode ini" : "-");
                continue;
            }
            Map.Entry<String, Double> entry = entries.get(i);
            int percent = (int) Math.round((entry.getValue() / totalExpense) * 100);
            views[i].setText(entry.getKey() + "   " + formatCurrency(entry.getValue()) + "   " + percent + "%");
        }
    }

    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
