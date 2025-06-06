package com.example.life.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
            binding.uidTextView.setText(user.getUid());
            binding.uidTextView.setTextColor(0xFF00E676); // Зеленый цвет в формате ARGB
            
            // Настраиваем обработчик нажатия на кнопку копирования
            binding.copyButton.setOnClickListener(v -> {
                // Получаем ID пользователя
                String userId = user.getUid();
                
                // Копируем ID в буфер обмена
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User ID", userId);
                clipboard.setPrimaryClip(clip);
                
                // Показываем уведомление об успешном копировании
                Toast.makeText(requireContext(), "ID скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
            });
        } else {
            binding.uidTextView.setText("ID недоступен");
            binding.uidTextView.setTextColor(0xFF00E676); // Зеленый цвет в формате ARGB
            binding.copyButton.setVisibility(View.GONE);
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