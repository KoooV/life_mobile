package com.example.life.core;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.life.R;
import com.example.life.chat.ChatListFragment;
import com.example.life.chat.MessengerFragment;
import com.example.life.settings.SettingsFragment;

/**
 * Главная активность приложения, которая управляет навигацией между фрагментами.
 * Реализует интерфейсы для обработки взаимодействия с фрагментами чата и настроек.
 */
public class MainActivity extends AppCompatActivity implements MessengerFragment.OnMenuButtonClickListener, ChatListFragment.OnSettingsButtonClickListener, ChatListFragment.OnChatSelectedListener {

    /**
     * Инициализация активности и установка начального фрагмента.
     * Если savedInstanceState == null, значит это первый запуск активности,
     * и мы устанавливаем ChatListFragment как начальный фрагмент.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Загружаем ChatListFragment в контейнер при первом запуске, если нет сохраненного состояния
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChatListFragment())
                    .commit();
        }
    }

    /**
     * Обработчик нажатия кнопки меню в MessengerFragment.
     * Переключает на фрагмент списка чатов (ChatListFragment).
     * Использует addToBackStack для возможности возврата назад.
     */
    @Override
    public void onMenuButtonClick() {
        // Переходим на ChatListFragment при нажатии кнопки меню в MessengerFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatListFragment())
                .addToBackStack("chat_list_fragment") // Добавляем в Back Stack с тегом
                .commit();
    }

    /**
     * Обработчик нажатия кнопки настроек в ChatListFragment.
     * Переключает на фрагмент настроек (SettingsFragment).
     * Использует addToBackStack для возможности возврата назад.
     */
    @Override
    public void onSettingsButtonClick() {
        // Переходим на SettingsFragment при нажатии кнопки настроек в ChatListFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .addToBackStack("settings_fragment") // Добавляем в Back Stack с тегом
                .commit();
    }

    /**
     * Обработчик выбора чата в списке чатов.
     * Создает новый экземпляр MessengerFragment с выбранным чатом
     * и переключает на него.
     * 
     * @param chatId ID выбранного чата
     * @param otherUserId ID собеседника
     */
    @Override
    public void onChatSelected(String chatId, String otherUserId) {
        // Переходим в MessengerFragment с выбранным чатом
        MessengerFragment messengerFragment = MessengerFragment.newInstance(chatId, otherUserId); // Используем newInstance

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, messengerFragment)
                .addToBackStack("messenger_fragment") // Добавляем в Back Stack с тегом
                .commit();
    }
} 