package com.myuang.app;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReceiptParser {
    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,})");

    public static class Result {
        public String merchant;
        public double totalAmount;
    }

    private ReceiptParser() {
    }

    public static Result parse(String recognizedText) {
        Result result = new Result();
        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            return result;
        }

        String[] lines = recognizedText.split("\\r?\\n");
        result.merchant = firstReadableLine(lines);
        result.totalAmount = findTotalAmount(lines);
        return result;
    }

    private static String firstReadableLine(String[] lines) {
        for (String line : lines) {
            String clean = line.trim();
            if (clean.length() >= 3 && !clean.matches(".*\\d{4,}.*")) {
                return clean;
            }
        }
        return "";
    }

    private static double findTotalAmount(String[] lines) {
        double strongestCandidate = 0;
        double largestNumber = 0;
        for (String line : lines) {
            double amount = largestMoneyValue(line);
            if (amount <= 0) {
                continue;
            }
            largestNumber = Math.max(largestNumber, amount);

            String lowerLine = line.toLowerCase(Locale.US);
            if (lowerLine.contains("total")
                    || lowerLine.contains("jumlah")
                    || lowerLine.contains("grand")
                    || lowerLine.contains("subtotal")
                    || lowerLine.contains("bayar")) {
                strongestCandidate = Math.max(strongestCandidate, amount);
            }
        }
        return strongestCandidate > 0 ? strongestCandidate : largestNumber;
    }

    private static double largestMoneyValue(String line) {
        Matcher matcher = MONEY_PATTERN.matcher(line);
        double largest = 0;
        while (matcher.find()) {
            String raw = matcher.group(1).replace(".", "").replace(",", "");
            try {
                largest = Math.max(largest, Double.parseDouble(raw));
            } catch (NumberFormatException ignored) {
                // Ignore OCR fragments that cannot be parsed as numbers.
            }
        }
        return largest;
    }
}
