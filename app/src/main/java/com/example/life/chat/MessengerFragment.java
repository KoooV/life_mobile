package com.example.life.chat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.life.databinding.FragmentMessengerBinding;
import com.example.life.chat.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент для отображения и управления чатом между пользователями.
 * Обеспечивает отправку и получение сообщений в реальном времени через Firebase.
 */
public class MessengerFragment extends Fragment {

    private static final String TAG = "MessengerFragment";

    /**
     * Интерфейс для обработки нажатия кнопки меню.
     * Позволяет фрагменту сообщить активности о необходимости
     * переключения на список чатов.
     */
    public interface OnMenuButtonClickListener {
        void onMenuButtonClick();
    }

    private OnMenuButtonClickListener menuButtonClickListener;

    private FragmentMessengerBinding binding;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String currentUserId;
    private String chatId;
    private String otherUserId;

    private ChildEventListener messagesListener;
    private DatabaseReference messagesRef;

    private RecyclerView recyclerView;

    /**
     * Создает новый экземпляр фрагмента с указанными параметрами чата.
     * 
     * @param chatId ID чата
     * @param otherUserId ID собеседника
     * @return новый экземпляр MessengerFragment
     */
    public static MessengerFragment newInstance(String chatId, String otherUserId) {
        MessengerFragment fragment = new MessengerFragment();
        Bundle args = new Bundle();
        args.putString("chatId", chatId);
        args.putString("otherUserId", otherUserId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Привязывает фрагмент к контексту активности.
     * Проверяет, реализует ли активность интерфейс OnMenuButtonClickListener.
     * 
     * @param context контекст активности
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnMenuButtonClickListener) {
            menuButtonClickListener = (OnMenuButtonClickListener) context;
        } else {
            Log.e(TAG, context.toString() + " must implement OnMenuButtonClickListener");
        }
    }

    /**
     * Инициализирует фрагмент и получает параметры чата из аргументов.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            otherUserId = getArguments().getString("otherUserId");
        }
    }

    /**
     * Создает и возвращает представление фрагмента.
     * Использует ViewBinding для доступа к элементам интерфейса.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessengerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Инициализирует компоненты интерфейса и настраивает обработчики событий.
     * Подключается к Firebase и настраивает слушатель сообщений.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(getContext(), "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId == null || otherUserId == null) {
            Toast.makeText(getContext(), "Ошибка: ID чата или ID другого пользователя отсутствует", Toast.LENGTH_SHORT).show();
        } else {
            binding.userStatusTextView.setText(otherUserId);
        }

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(currentUserId);

        binding.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.messagesRecyclerView.setAdapter(messageAdapter);

        if (chatId != null) {
            messagesRef = databaseReference.child("messages").child(chatId);
            loadMessages();
        }

        binding.sendButton.setOnClickListener(v -> {
            String messageText = binding.messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            } else {
                Toast.makeText(getContext(), "Введите сообщение", Toast.LENGTH_SHORT).show();
            }
        });

        binding.pathTextView.setOnClickListener(v -> {
            if (menuButtonClickListener != null) {
                menuButtonClickListener.onMenuButtonClick();
            }
        });
    }

    /**
     * Загружает сообщения из Firebase и настраивает слушатель для обновлений в реальном времени.
     * При получении новых сообщений обновляет список и прокручивает к последнему сообщению.
     */
    private void loadMessages() {
        if (messagesRef == null) {
            Log.e(TAG, "loadMessages: messagesRef is null!");
            return;
        }
        messagesListener = messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    messageList.add(message);
                    messageAdapter.setMessages(messageList);
                    binding.messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки сообщений: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Ошибка загрузки сообщений", error.toException());
            }
        });
    }

    /**
     * Отправляет новое сообщение в Firebase.
     * Создает уникальный ID для сообщения и сохраняет его в базе данных.
     * 
     * @param text текст сообщения для отправки
     */
    private void sendMessage(String text) {
        if (currentUserId == null || chatId == null) {
            Toast.makeText(getContext(), "Ошибка: Не удалось отправить сообщение (пользователь или чат не определен)", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(getContext(), "Ошибка: Не удалось создать ID сообщения", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        Message message = new Message(messageId, text, currentUserId, timestamp);

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    binding.messageEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка отправки сообщения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Ошибка отправки сообщения", e);
                });
    }

    /**
     * Отвязывает фрагмент от активности и очищает ссылку на слушатель кнопки меню.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        menuButtonClickListener = null;
    }

    /**
     * Очищает ресурсы при уничтожении представления фрагмента.
     * Удаляет слушатель сообщений из Firebase.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }
} 