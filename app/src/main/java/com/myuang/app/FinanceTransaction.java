package com.myuang.app;

import java.util.Date;

public class FinanceTransaction {
    public static final String TYPE_EXPENSE = "expense";
    public static final String TYPE_INCOME = "income";

    public String transactionId;
    public String userId;
    public String type;
    public String category;
    public String note;
    public String label;
    public double amount;
    public Date createdAt;

    public boolean isIncome() {
        return TYPE_INCOME.equals(type);
    }

    public boolean isExpense() {
        return TYPE_EXPENSE.equals(type);
    }
}
