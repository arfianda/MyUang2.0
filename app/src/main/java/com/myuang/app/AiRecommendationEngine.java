package com.myuang.app;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AiRecommendationEngine {
    private static final double WEEKLY_SAVING_TARGET = 500000;

    public static class Tip {
        public String title;
        public String body;
        public String category;

        public Tip(String title, String body, String category) {
            this.title = title;
            this.body = body;
            this.category = category;
        }
    }

    public static class Result {
        public String summary;
        public String dashboardTip;
        public String savingTarget;
        public String achievedText;
        public int savingProgress;
        public List<Tip> tips;
    }

    private AiRecommendationEngine() {
    }

    public static Result analyze(List<FinanceTransaction> transactions, double saldo) {
        Date now = new Date();
        Date currentWeekStart = daysAgo(now, 7);
        Date previousWeekStart = daysAgo(now, 14);

        List<FinanceTransaction> currentWeek = filterBetween(transactions, currentWeekStart, now);
        List<FinanceTransaction> previousWeek = filterBetween(transactions, previousWeekStart, currentWeekStart);

        double weeklyExpense = sumByType(currentWeek, FinanceTransaction.TYPE_EXPENSE);
        double previousExpense = sumByType(previousWeek, FinanceTransaction.TYPE_EXPENSE);
        double weeklyIncome = sumByType(currentWeek, FinanceTransaction.TYPE_INCOME);
        double savedThisWeek = Math.max(0, weeklyIncome - weeklyExpense);
        int progress = (int) Math.min(100, Math.round((savedThisWeek / WEEKLY_SAVING_TARGET) * 100));

        Map<String, Double> categoryTotals = categoryExpenseTotals(currentWeek);
        String topCategory = topKey(categoryTotals);
        double topAmount = topCategory == null ? 0 : categoryTotals.get(topCategory);
        double coffeeAmount = keywordExpense(currentWeek, "kopi", "coffee");

        Result result = new Result();
        result.savingTarget = formatCurrency(WEEKLY_SAVING_TARGET) + " Target";
        result.achievedText = progress + "% tercapai";
        result.savingProgress = progress;
        result.summary = buildSummary(weeklyExpense, previousExpense, topCategory, topAmount);
        result.dashboardTip = buildDashboardTip(transactions, saldo, topCategory, topAmount);
        result.tips = buildTips(transactions, weeklyExpense, previousExpense, topCategory, topAmount, coffeeAmount, saldo);
        return result;
    }

    public static Map<String, Double> categoryExpenseTotals(List<FinanceTransaction> transactions) {
        Map<String, Double> totals = new HashMap<>();
        for (FinanceTransaction transaction : transactions) {
            if (!transaction.isExpense()) {
                continue;
            }
            String category = transaction.category == null || transaction.category.trim().isEmpty()
                    ? "Lainnya"
                    : transaction.category;
            double current = totals.containsKey(category) ? totals.get(category) : 0;
            totals.put(category, current + transaction.amount);
        }
        return totals;
    }

    private static List<Tip> buildTips(List<FinanceTransaction> allTransactions, double weeklyExpense,
                                       double previousExpense, String topCategory, double topAmount,
                                       double coffeeAmount, double saldo) {
        List<Tip> tips = new ArrayList<>();

        if (allTransactions.isEmpty()) {
            tips.add(new Tip("Mulai dari saldo awal", "Tambahkan saldo dan transaksi pertama agar AI bisa membaca pola cashflow kamu.", "Onboarding"));
            tips.add(new Tip("Catat transaksi kecil", "Pengeluaran kecil seperti parkir, kopi, dan camilan sering menjadi sumber bocor halus.", "Tracking"));
            tips.add(new Tip("Cek dashboard harian", "Saldo, pemasukan, dan pengeluaran akan otomatis berubah setelah transaksi tersimpan.", "Dashboard"));
            return tips;
        }

        if (topCategory != null) {
            tips.add(new Tip("Pantau " + topCategory, topCategory + " menjadi pengeluaran terbesar minggu ini dengan total "
                    + formatCurrency(topAmount) + ". Tetapkan batas harian sebelum transaksi berikutnya.", topCategory));
        }

        if (coffeeAmount > 0) {
            tips.add(new Tip("Kurangi kopi siap beli", "Transaksi berlabel kopi minggu ini mencapai "
                    + formatCurrency(coffeeAmount) + ". Kurangi 2 kali pembelian untuk menambah ruang tabungan.", "Kopi"));
        }

        if (previousExpense > 0 && weeklyExpense > previousExpense) {
            int growth = (int) Math.round(((weeklyExpense - previousExpense) / previousExpense) * 100);
            tips.add(new Tip("Pengeluaran naik " + growth + "%", "Minggu ini lebih tinggi dari minggu sebelumnya. Review transaksi terbesar sebelum akhir pekan.", "Trend"));
        } else {
            tips.add(new Tip("Jaga ritme belanja", "Pengeluaran minggu ini masih terkendali dibanding minggu lalu. Pertahankan catatan transaksi harian.", "Trend"));
        }

        if (saldo < weeklyExpense && weeklyExpense > 0) {
            tips.add(new Tip("Saldo perlu dijaga", "Saldo saat ini lebih rendah dari pengeluaran 7 hari terakhir. Prioritaskan kebutuhan pokok dulu.", "Saldo"));
        } else {
            tips.add(new Tip("Alokasikan sisa saldo", "Pisahkan sebagian saldo untuk target tabungan agar uang tidak habis tanpa rencana.", "Saldo"));
        }

        while (tips.size() < 3) {
            tips.add(new Tip("Lengkapi catatan", "Semakin lengkap kategori dan label transaksi, semakin presisi rekomendasi AI yang muncul.", "Data"));
        }
        return tips.subList(0, 3);
    }

    private static String buildSummary(double weeklyExpense, double previousExpense, String topCategory, double topAmount) {
        if (weeklyExpense == 0) {
            return "Belum ada pengeluaran minggu ini. Tambahkan transaksi agar AI bisa membuat ringkasan personal.";
        }
        String categoryText = topCategory == null ? "belum ada kategori dominan" : "terbesar dari " + topCategory + " sebesar " + formatCurrency(topAmount);
        if (previousExpense > 0) {
            int growth = (int) Math.round(((weeklyExpense - previousExpense) / previousExpense) * 100);
            String direction = growth >= 0 ? "naik" : "turun";
            return "Pengeluaran minggu ini " + direction + " " + Math.abs(growth) + "% dari minggu lalu, " + categoryText + ".";
        }
        return "Pengeluaran minggu ini mencapai " + formatCurrency(weeklyExpense) + ", " + categoryText + ".";
    }

    private static String buildDashboardTip(List<FinanceTransaction> transactions, double saldo, String topCategory, double topAmount) {
        if (transactions.isEmpty()) {
            return "Tambahkan saldo dan transaksi pertama untuk mulai melihat insight cashflow real-time.";
        }
        if (topCategory != null && topAmount > 0) {
            return topCategory + " sedang menjadi pengeluaran terbesar. Total 7 hari terakhir: " + formatCurrency(topAmount) + ".";
        }
        if (saldo <= 0) {
            return "Saldo belum positif. Tambahkan pemasukan atau perbarui saldo awal dari profil.";
        }
        return "Transaksi sudah tersimpan real-time. Cek label cepat untuk melihat kebiasaan belanja lebih akurat.";
    }

    private static List<FinanceTransaction> filterBetween(List<FinanceTransaction> transactions, Date start, Date end) {
        List<FinanceTransaction> filtered = new ArrayList<>();
        for (FinanceTransaction transaction : transactions) {
            Date createdAt = transaction.createdAt == null ? new Date(0) : transaction.createdAt;
            if (!createdAt.before(start) && createdAt.before(end)) {
                filtered.add(transaction);
            }
        }
        return filtered;
    }

    private static double sumByType(List<FinanceTransaction> transactions, String type) {
        double total = 0;
        for (FinanceTransaction transaction : transactions) {
            if (type.equals(transaction.type)) {
                total += transaction.amount;
            }
        }
        return total;
    }

    private static double keywordExpense(List<FinanceTransaction> transactions, String firstKeyword, String secondKeyword) {
        double total = 0;
        for (FinanceTransaction transaction : transactions) {
            if (!transaction.isExpense()) {
                continue;
            }
            String text = ((transaction.note == null ? "" : transaction.note) + " "
                    + (transaction.label == null ? "" : transaction.label)).toLowerCase(Locale.US);
            if (text.contains(firstKeyword) || text.contains(secondKeyword)) {
                total += transaction.amount;
            }
        }
        return total;
    }

    private static String topKey(Map<String, Double> totals) {
        String top = null;
        double highest = 0;
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            if (top == null || entry.getValue() > highest) {
                top = entry.getKey();
                highest = entry.getValue();
            }
        }
        return top;
    }

    private static Date daysAgo(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        return calendar.getTime();
    }

    private static String formatCurrency(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("in", "ID"));
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return "Rp " + format.format(Math.round(Math.abs(amount)));
    }
}
