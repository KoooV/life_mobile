package com.example.life;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements MessengerFragment.OnMenuButtonClickListener, ChatListFragment.OnSettingsButtonClickListener, ChatListFragment.OnChatSelectedListener {

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

    @Override
    public void onMenuButtonClick() {
        // Переходим на ChatListFragment при нажатии кнопки меню в MessengerFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatListFragment())
                .addToBackStack("chat_list_fragment") // Добавляем в Back Stack с тегом
                .commit();
    }

    @Override
    public void onSettingsButtonClick() {
        // Переходим на SettingsFragment при нажатии кнопки настроек в ChatListFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .addToBackStack("settings_fragment") // Добавляем в Back Stack с тегом
                .commit();
    }

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