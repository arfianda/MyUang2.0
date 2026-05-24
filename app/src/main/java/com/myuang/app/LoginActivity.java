package com.myuang.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class LoginActivity extends BaseActivity {
    private MyUangRepository repository;
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repository = MyUangRepository.get(this);
        emailInput = findViewById(R.id.inputLoginEmail);
        passwordInput = findViewById(R.id.inputLoginPassword);
        loginButton = findViewById(R.id.btnLogin);

        if (repository.isLoggedIn()) {
            openScreen(DashboardActivity.class);
            finish();
            return;
        }

        ImageButton toggle = findViewById(R.id.btnToggleLoginPassword);
        bindPasswordToggle(passwordInput, toggle);

        loginButton.setOnClickListener(v -> signIn());
        findViewById(R.id.btnLanguage).setOnClickListener(v -> toast("Bahasa otomatis: Indonesia"));
        findViewById(R.id.btnPhoneLogin).setOnClickListener(v -> toast("Aktifkan provider Phone di Firebase Auth untuk memakai nomor HP"));
        findViewById(R.id.linkForgotPassword).setOnClickListener(v -> resetPassword());

        TextView register = findViewById(R.id.linkRegister);
        register.setOnClickListener(v -> openScreen(RegisterActivity.class));
    }

    private void signIn() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email wajib diisi");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Kata sandi wajib diisi");
            return;
        }

        setLoading(true);
        repository.signIn(email, password, (success, message) -> {
            setLoading(false);
            toast(message);
            if (success) {
                openScreen(DashboardActivity.class);
                finish();
            }
        });
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Isi email dulu");
            return;
        }
        repository.sendPasswordReset(email, (success, message) -> toast(message));
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Memproses..." : getString(R.string.login_action));
    }
}
