package com.example.life;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.life.databinding.ActivityRegistrationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    private ActivityRegistrationBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();
                        // Создаем данные пользователя в Realtime Database
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("uid", uid);

                        database.getReference("users").child(uid)
                                .setValue(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegistrationActivity.this, 
                                            "Регистрация успешна", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(RegistrationActivity.this,
                                        "Ошибка сохранения данных: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(RegistrationActivity.this,
                                "Ошибка регистрации: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 