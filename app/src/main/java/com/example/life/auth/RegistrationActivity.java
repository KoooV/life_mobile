package com.example.life.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;

import com.example.life.databinding.ActivityRegistrationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends BaseActivity {
    private static final String TAG = "RegistrationDebug";
    private ActivityRegistrationBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        hideSystemUI();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

        Log.d(TAG, "Attempting to register user with email: " + email);

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Log.d(TAG, "Email or password field is empty.");
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Log.d(TAG, "Passwords do not match.");
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);

                    Log.d(TAG, "Firebase Auth createUserWithEmailAndPassword complete.");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth registration successful.");
                        String uid = auth.getCurrentUser().getUid();
                        Log.d(TAG, "User UID: " + uid);

                        // Создаем данные пользователя в Realtime Database
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("uid", uid);

                        Log.d(TAG, "Saving user data to Realtime Database for UID: " + uid);

                        database.getReference("users").child(uid)
                                .setValue(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User data successfully saved to Realtime Database.");
                                    Toast.makeText(RegistrationActivity.this, 
                                            "Регистрация успешна", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error saving user data to Realtime Database: " + e.getMessage(), e);
                                    Toast.makeText(RegistrationActivity.this,
                                        "Ошибка сохранения данных: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "Firebase Auth registration failed: " + task.getException().getMessage(), task.getException());
                        Toast.makeText(RegistrationActivity.this,
                                "Ошибка регистрации: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 