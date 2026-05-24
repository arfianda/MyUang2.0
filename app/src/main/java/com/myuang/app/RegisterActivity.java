package com.myuang.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class RegisterActivity extends BaseActivity {
    private MyUangRepository repository;
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText balanceInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        repository = MyUangRepository.get(this);
        nameInput = findViewById(R.id.inputRegisterName);
        emailInput = findViewById(R.id.inputRegisterEmail);
        passwordInput = findViewById(R.id.inputRegisterPassword);
        balanceInput = findViewById(R.id.inputRegisterBalance);
        registerButton = findViewById(R.id.btnRegister);

        ImageButton toggle = findViewById(R.id.btnToggleRegisterPassword);
        bindPasswordToggle(passwordInput, toggle);

        registerButton.setOnClickListener(v -> register());
        findViewById(R.id.btnGoogleRegister).setOnClickListener(v -> toast("Aktifkan provider Google di Firebase Auth untuk memakai Google Sign-In"));
        findViewById(R.id.btnFacebookRegister).setOnClickListener(v -> toast("Aktifkan provider Facebook di Firebase Auth untuk memakai Facebook Sign-In"));

        TextView login = findViewById(R.id.linkLogin);
        login.setOnClickListener(v -> finish());
    }

    private void register() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        double initialBalance = parseAmount(balanceInput.getText().toString());

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Nama wajib diisi");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email wajib diisi");
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Minimal 6 karakter");
            return;
        }

        setLoading(true);
        repository.register(name, email, password, initialBalance, (success, message) -> {
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
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? "Memproses..." : getString(R.string.register_action));
    }
}
