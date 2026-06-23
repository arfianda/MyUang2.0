package com.myuang.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.firestore.ListenerRegistration;

public class ProfileActivity extends BaseActivity {
    private MyUangRepository repository;
    private ListenerRegistration userListener;
    private TextView nameText;
    private TextView emailText;
    private TextView balanceText;
    private double currentSaldo;
    private String currentName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (!requireAuthenticated()) {
            return;
        }
        bindBottomNav("profile");

        repository = MyUangRepository.get(this);
        nameText = findViewById(R.id.textProfileName);
        emailText = findViewById(R.id.textProfileEmail);
        balanceText = findViewById(R.id.textProfileBalance);

        findViewById(R.id.btnProfileNotification).setOnClickListener(v -> toast(getString(R.string.toast_profile_no_notification)));
        findViewById(R.id.btnEditAccount).setOnClickListener(v -> showNameDialog());
        findViewById(R.id.btnPaymentSettings).setOnClickListener(v -> showBalanceDialog());
        findViewById(R.id.btnExportReport).setOnClickListener(v -> toast(getString(R.string.toast_profile_export)));
        findViewById(R.id.btnReminderSettings).setOnClickListener(v -> toast(getString(R.string.toast_profile_reminder)));
        findViewById(R.id.btnHelp).setOnClickListener(v -> toast(getString(R.string.toast_profile_help)));

        Switch darkMode = findViewById(R.id.switchDarkMode);
        darkMode.setChecked(ThemeHelper.isDarkMode(this));
        darkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setDarkMode(this, isChecked);
            recreate();
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            repository.logout();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        userListener = repository.listenUser(new MyUangRepository.UserCallback() {
            @Override
            public void onUser(MyUangRepository.UserProfile user) {
                currentSaldo = user.saldo;
                currentName = user.name;
                nameText.setText(user.name);
                emailText.setText(user.email);
                balanceText.setText(getString(R.string.total_balance) + " " + formatCurrency(user.saldo));
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (userListener != null) {
            userListener.remove();
        }
        super.onDestroy();
    }

    private void showBalanceDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.hint_current_balance);
        input.setText(String.valueOf(Math.round(currentSaldo)));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.balance_settings)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    double saldo = parseAmount(input.getText().toString());
                    repository.updateSaldo(saldo, (success, message) -> toast(message));
                })
                .show();
    }

    private void showNameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        input.setHint(R.string.hint_account_name);
        input.setText(currentName);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.manage_account)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast(getString(R.string.validation_name_required));
                        return;
                    }
                    repository.updateName(name, (success, message) -> toast(message));
                })
                .show();
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
}
