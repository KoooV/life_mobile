package com.example.life.settings;

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

/**
 * Фрагмент настроек приложения.
 * Отображает информацию о текущем пользователе и предоставляет
 * возможность возврата к предыдущему экрану.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    /**
     * Создает и возвращает представление фрагмента.
     * Использует ViewBinding для доступа к элементам интерфейса.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Инициализирует компоненты интерфейса и настраивает обработчики событий.
     * Отображает ID текущего пользователя и настраивает кнопку возврата.
     */
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

    /**
     * Очищает ресурсы при уничтожении представления фрагмента.
     * Освобождает ссылку на binding для предотвращения утечек памяти.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 