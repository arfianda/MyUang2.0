package com.myuang.app;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends BaseActivity {
    public static final String EXTRA_SCAN_NOTE = "scan_note";
    public static final String EXTRA_SCAN_MERCHANT = "scan_merchant";
    public static final String EXTRA_SCAN_AMOUNT = "scan_amount";

    private MyUangRepository repository;
    private Button expenseButton;
    private Button incomeButton;
    private Button saveButton;
    private Spinner categorySpinner;
    private EditText amountInput;
    private EditText dateInput;
    private EditText noteInput;
    private TextView lunchChip;
    private TextView gojekChip;
    private TextView coffeeChip;
    private boolean isIncome;
    private String selectedLabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);
        if (!requireAuthenticated()) {
            return;
        }
        bindBottomNav("add");

        repository = MyUangRepository.get(this);
        expenseButton = findViewById(R.id.btnExpenseType);
        incomeButton = findViewById(R.id.btnIncomeType);
        saveButton = findViewById(R.id.btnSaveTransaction);
        categorySpinner = findViewById(R.id.spinnerCategory);
        amountInput = findViewById(R.id.inputAmount);
        dateInput = findViewById(R.id.inputDate);
        noteInput = findViewById(R.id.inputNote);
        lunchChip = findViewById(R.id.chipLunch);
        gojekChip = findViewById(R.id.chipGojek);
        coffeeChip = findViewById(R.id.chipCoffee);
        setCategoryData(getResources().getStringArray(R.array.expense_categories_array));
        dateInput.setText(new SimpleDateFormat("dd MMM yyyy", new Locale("in", "ID")).format(new Date()));

        findViewById(R.id.btnAddBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnManualMode).setOnClickListener(v -> toast("Mode manual aktif"));
        findViewById(R.id.btnScanMode).setOnClickListener(v -> openScreen(ScannerActivity.class));
        expenseButton.setOnClickListener(v -> setTransactionType(false));
        incomeButton.setOnClickListener(v -> setTransactionType(true));

        lunchChip.setOnClickListener(v -> selectLabel("#MakanSiang"));
        gojekChip.setOnClickListener(v -> selectLabel("#Gojek"));
        coffeeChip.setOnClickListener(v -> selectLabel("#Kopi"));
        saveButton.setOnClickListener(v -> saveTransaction());

        prefillFromScan();
    }

    private void setTransactionType(boolean income) {
        isIncome = income;
        if (income) {
            incomeButton.setBackgroundResource(R.drawable.bg_button_success);
            incomeButton.setTextColor(getColorCompat(android.R.color.white));
            expenseButton.setBackgroundResource(android.R.color.transparent);
            expenseButton.setTextColor(getColorCompat(R.color.on_surface_variant));
            setCategoryData(getResources().getStringArray(R.array.income_categories_array));
        } else {
            expenseButton.setBackgroundResource(R.drawable.bg_button_danger);
            expenseButton.setTextColor(getColorCompat(android.R.color.white));
            incomeButton.setBackgroundResource(android.R.color.transparent);
            incomeButton.setTextColor(getColorCompat(R.color.on_surface_variant));
            setCategoryData(getResources().getStringArray(R.array.expense_categories_array));
        }
    }

    private void setCategoryData(String[] categories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void selectLabel(String label) {
        selectedLabel = label;
        updateChipState(lunchChip, "#MakanSiang".equals(label), R.drawable.bg_chip_green);
        updateChipState(gojekChip, "#Gojek".equals(label), R.drawable.bg_chip_blue);
        updateChipState(coffeeChip, "#Kopi".equals(label), R.drawable.bg_chip_warning);

        String currentNote = noteInput.getText().toString();
        if (!currentNote.contains(label)) {
            String prefix = currentNote.trim().isEmpty() ? "" : currentNote.trim() + " ";
            noteInput.setText(prefix + label);
            noteInput.setSelection(noteInput.getText().length());
        }
    }

    private void updateChipState(TextView chip, boolean selected, int selectedBackground) {
        chip.setBackgroundResource(selected ? selectedBackground : R.drawable.bg_chip_gray);
        chip.setTextColor(getColorCompat(selected ? R.color.on_surface : R.color.on_surface_variant));
    }

    private void prefillFromScan() {
        double scanAmount = getIntent().getDoubleExtra(EXTRA_SCAN_AMOUNT, 0);
        String merchant = getIntent().getStringExtra(EXTRA_SCAN_MERCHANT);
        String scanNote = getIntent().getStringExtra(EXTRA_SCAN_NOTE);

        if (scanAmount > 0) {
            amountInput.setText(String.valueOf(Math.round(scanAmount)));
        }
        if (merchant != null && !merchant.trim().isEmpty()) {
            noteInput.setText(merchant.trim());
        } else if (scanNote != null && !scanNote.trim().isEmpty()) {
            noteInput.setText(scanNote.trim());
        }
        if (scanNote != null && !scanNote.trim().isEmpty()) {
            selectedLabel = "#ScanStruk";
        }
    }

    private void saveTransaction() {
        double amount = parseAmount(amountInput.getText().toString());
        if (amount <= 0) {
            amountInput.setError("Jumlah wajib lebih dari 0");
            return;
        }

        String category = categorySpinner.getSelectedItem() == null
                ? ""
                : categorySpinner.getSelectedItem().toString();
        String type = isIncome ? FinanceTransaction.TYPE_INCOME : FinanceTransaction.TYPE_EXPENSE;
        String note = noteInput.getText().toString().trim();
        setLoading(true);
        repository.addTransaction(type, category, amount, note, selectedLabel, new Date(), (success, message) -> {
            setLoading(false);
            toast(message);
            if (success) {
                openScreen(DashboardActivity.class);
                finish();
            }
        });
    }

    private double parseAmount(String value) {
        String cleaned = value.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void setLoading(boolean loading) {
        saveButton.setEnabled(!loading);
        saveButton.setText(loading ? "Menyimpan..." : getString(R.string.save_transaction));
    }
}
