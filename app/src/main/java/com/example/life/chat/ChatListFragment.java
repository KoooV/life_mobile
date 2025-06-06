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

import com.example.life.databinding.FragmentChatListBinding;
import com.example.life.chat.model.Chat;
import com.example.life.chat.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фрагмент для отображения списка чатов пользователя.
 * Позволяет просматривать существующие чаты, создавать новые и переходить к ним.
 * Использует Firebase для хранения и синхронизации данных чатов.
 */
public class ChatListFragment extends Fragment implements ChatAdapter.OnChatClickListener {

    private static final String TAG = "ChatListFragment";

    /**
     * Интерфейс для обработки выбора чата.
     * Позволяет фрагменту сообщить активности о выборе конкретного чата.
     */
    public interface OnChatSelectedListener {
        void onChatSelected(String chatId, String otherUserId);
    }

    /**
     * Интерфейс для обработки нажатия кнопки настроек.
     * Позволяет фрагменту сообщить активности о необходимости
     * переключения на экран настроек.
     */
    public interface OnSettingsButtonClickListener {
        void onSettingsButtonClick();
    }

    private OnChatSelectedListener chatSelectedListener;
    private OnSettingsButtonClickListener settingsButtonClickListener;

    private FragmentChatListBinding binding;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String currentUserId;

    private ChatAdapter chatAdapter;
    private List<Chat> chatList;
    private Map<String, ChildEventListener> messageListeners;

    /**
     * Привязывает фрагмент к контексту активности.
     * Проверяет, реализует ли активность необходимые интерфейсы.
     * 
     * @param context контекст активности
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnChatSelectedListener) {
            chatSelectedListener = (OnChatSelectedListener) context;
        } else {
            Log.e(TAG, context.toString() + " must implement OnChatSelectedListener");
        }
        if (context instanceof OnSettingsButtonClickListener) {
            settingsButtonClickListener = (OnSettingsButtonClickListener) context;
        } else {
            Log.e(TAG, context.toString() + " must implement OnSettingsButtonClickListener");
        }
    }

    /**
     * Создает и возвращает представление фрагмента.
     * Использует ViewBinding для доступа к элементам интерфейса.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Инициализирует компоненты интерфейса и настраивает обработчики событий.
     * Подключается к Firebase и загружает список чатов пользователя.
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

        chatList = new ArrayList<>();
        messageListeners = new HashMap<>();
        chatAdapter = new ChatAdapter(chatList, this);

        binding.chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.chatsRecyclerView.setAdapter(chatAdapter);

        loadChats();

        binding.settingsButton.setOnClickListener(v -> {
            if (settingsButtonClickListener != null) {
                settingsButtonClickListener.onSettingsButtonClick();
            }
        });

        binding.addUserButton.setOnClickListener(v -> {
            String otherUserId = binding.userIdEditText.getText().toString().trim();

            if (otherUserId.isEmpty()) {
                Toast.makeText(getContext(), "Пожалуйста, введите ID пользователя", Toast.LENGTH_SHORT).show();
                return;
            }

            if (otherUserId.equals(currentUserId)) {
                Toast.makeText(getContext(), "Вы не можете создать чат с самим собой", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseReference.child("users").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        checkOrCreateChat(otherUserId);
                    } else {
                        Toast.makeText(getContext(), "Пользователь с таким ID не найден.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Ошибка при поиске пользователя: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Загружает список чатов пользователя из Firebase.
     * Для каждого чата создает слушатель последнего сообщения.
     */
    private void loadChats() {
        databaseReference.child("user_chats").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String otherUserId = chatSnapshot.getKey();
                    String chatId = chatSnapshot.getValue(String.class);
                    if (otherUserId != null && chatId != null) {
                        Chat chat = new Chat(chatId, otherUserId, "", 0);
                        chatList.add(chat);
                        listenForLastMessage(chatId, otherUserId);
                    }
                }
                chatAdapter.updateChats(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки чатов: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Создает слушатель для последнего сообщения в чате.
     * Обновляет информацию о чате при получении нового сообщения.
     * 
     * @param chatId ID чата
     * @param otherUserId ID собеседника
     */
    private void listenForLastMessage(String chatId, String otherUserId) {
        DatabaseReference messagesRef = databaseReference.child("messages").child(chatId);
        ChildEventListener listener = messagesRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    updateChatLastMessage(chatId, otherUserId, message.getText(), message.getTimestamp());
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
                Log.e(TAG, "listenForLastMessage: onCancelled for chatId=" + chatId, error.toException());
            }
        });
        messageListeners.put(chatId, listener);
    }

    /**
     * Обновляет информацию о последнем сообщении в чате.
     * 
     * @param chatId ID чата
     * @param otherUserId ID собеседника
     * @param lastMessage текст последнего сообщения
     * @param timestamp время последнего сообщения
     */
    private void updateChatLastMessage(String chatId, String otherUserId, String lastMessage, long timestamp) {
        for (int i = 0; i < chatList.size(); i++) {
            Chat chat = chatList.get(i);
            if (chat.getId().equals(chatId)) {
                chat.setLastMessage(lastMessage);
                chat.setLastMessageTimestamp(timestamp);
                chatAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Проверяет существование чата с пользователем и создает новый, если необходимо.
     * 
     * @param otherUserId ID пользователя для создания чата
     */
    private void checkOrCreateChat(String otherUserId) {
        databaseReference.child("user_chats").child(currentUserId).child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String chatId = snapshot.getValue(String.class);
                    if (chatId != null && chatSelectedListener != null) {
                        chatSelectedListener.onChatSelected(chatId, otherUserId);
                    }
                } else {
                    createNewChat(otherUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка при проверке чата: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Создает новый чат с пользователем.
     * Создает записи в базе данных для обоих участников чата.
     * 
     * @param otherUserId ID пользователя для создания чата
     */
    private void createNewChat(String otherUserId) {
        String chatId = databaseReference.child("chats").push().getKey();
        if (chatId == null) {
            Toast.makeText(getContext(), "Ошибка при создании чата", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("user_chats/" + currentUserId + "/" + otherUserId, chatId);
        updates.put("user_chats/" + otherUserId + "/" + currentUserId, chatId);

        databaseReference.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (chatSelectedListener != null) {
                        chatSelectedListener.onChatSelected(chatId, otherUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при создании чата: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Обработчик нажатия на чат в списке.
     * Переключает на экран чата с выбранным пользователем.
     * 
     * @param chat выбранный чат
     */
    @Override
    public void onChatClick(Chat chat) {
        if (chatSelectedListener != null) {
            chatSelectedListener.onChatSelected(chat.getId(), chat.getOtherUserId());
        }
    }

    /**
     * Отвязывает фрагмент от активности и очищает ссылки на слушатели.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        chatSelectedListener = null;
        settingsButtonClickListener = null;
    }

    /**
     * Очищает ресурсы при уничтожении представления фрагмента.
     * Удаляет все слушатели сообщений из Firebase.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ChildEventListener listener : messageListeners.values()) {
            if (listener != null) {
                databaseReference.removeEventListener(listener);
            }
        }
        messageListeners.clear();
        binding = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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