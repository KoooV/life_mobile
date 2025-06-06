package com.example.life.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.life.core.MainActivity;
import com.example.life.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class AuthActivity extends BaseActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private FirebaseAuth auth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        EdgeToEdge.enable(this);
        hideSystemUI();

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.auth_email);
        passwordEditText = findViewById(R.id.auth_password);

        findViewById(R.id.auth_entrBtn).setOnClickListener(v -> loginUser());

        findViewById(R.id.registerTextView).setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        // Попытка входа через Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Вход успешен, переходим на главную страницу (где будет фрагмент мессенджера)
                        Toast.makeText(AuthActivity.this, "Вход успешен!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AuthActivity.this, MainActivity.class); // Замените MainActivity.class на вашу основную Activity
                        startActivity(intent);
                        finish(); // Закрываем текущую активность, чтобы пользователь не мог вернуться по кнопке "Назад"
                    } else {
                        // Вход неуспешен, обрабатываем ошибки
                        String errorMessage = "Ошибка входа.";
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "Пользователь с таким email не существует.";
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Неверный пароль.";
                        }
                        Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}