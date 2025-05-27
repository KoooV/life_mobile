package com.example.life;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.life.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Получаем текущего пользователя Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Отображаем UID пользователя
        if (user != null) {
            binding.uidTextView.setText("Ваш ID: " + user.getUid());
        } else {
            binding.uidTextView.setText("Ваш ID: Недоступен");
        }

        // Обработка нажатия кнопки Назад
        binding.backButton.setOnClickListener(v -> {
            // Возвращаемся к предыдущему фрагменту в Back Stack (список чатов)
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 