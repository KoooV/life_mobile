package com.example.life;

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
import com.example.life.model.Chat;
import com.example.life.model.Message;
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

public class ChatListFragment extends Fragment implements ChatAdapter.OnChatClickListener {

    private static final String TAG = "ChatListFragment";

    public interface OnChatSelectedListener {
        void onChatSelected(String chatId, String otherUserId);
    }

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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");
        if (context instanceof OnChatSelectedListener) {
            chatSelectedListener = (OnChatSelectedListener) context;
        } else {
             Log.e(TAG, context.toString() + " must implement OnChatSelectedListener");
            //throw new RuntimeException(context.toString() + " must implement OnChatSelectedListener");
        }
        if (context instanceof OnSettingsButtonClickListener) {
            settingsButtonClickListener = (OnSettingsButtonClickListener) context;
        } else {
             Log.e(TAG, context.toString() + " must implement OnSettingsButtonClickListener");
            //throw new RuntimeException(context.toString() + " must implement OnSettingsButtonClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "onViewCreated: currentUserId=" + currentUserId);
        } else {
            Toast.makeText(getContext(), "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onViewCreated: Пользователь не аутентифицирован");
            return;
        }

        chatList = new ArrayList<>();
        messageListeners = new HashMap<>();
        chatAdapter = new ChatAdapter(chatList, this);

        binding.chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.chatsRecyclerView.setAdapter(chatAdapter);

        loadChats();

        binding.settingsButton.setOnClickListener(v -> {
            Log.d(TAG, "settingsButton clicked");
            if (settingsButtonClickListener != null) {
                settingsButtonClickListener.onSettingsButtonClick();
            } else {
                 Log.w(TAG, "settingsButtonClickListener is null");
            }
        });

        binding.addUserButton.setOnClickListener(v -> {
            Log.d(TAG, "addUserButton clicked");
            String otherUserId = binding.userIdEditText.getText().toString().trim();
            Log.d(TAG, "addUserButton: otherUserId from input = '" + otherUserId + "'");

            if (otherUserId.isEmpty()) {
                Toast.makeText(getContext(), "Пожалуйста, введите ID пользователя", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "addUserButton: otherUserId is empty");
                return;
            }

            if (otherUserId.equals(currentUserId)) {
                Toast.makeText(getContext(), "Вы не можете создать чат с самим собой", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "addUserButton: Attempted to create chat with self");
                return;
            }

            Log.d(TAG, "Searching for user with ID: " + otherUserId);
            databaseReference.child("users").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "addUserButton: onDataChange triggered. snapshot.exists() = " + snapshot.exists());
                    if (snapshot.exists()) {
                         Log.d(TAG, "addUserButton: User found with ID: " + otherUserId);
                        checkOrCreateChat(otherUserId);
                    } else {
                        Toast.makeText(getContext(), "Пользователь с таким ID не найден.", Toast.LENGTH_SHORT).show();
                         Log.w(TAG, "addUserButton: User not found with ID: " + otherUserId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "addUserButton: onCancelled triggered! Error searching for user", error.toException());
                    Toast.makeText(getContext(), "Ошибка при поиске пользователя: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                     Log.e(TAG, "addUserButton: Error searching for user", error.toException());
                }
            });
        });
    }

    private void loadChats() {
        Log.d(TAG, "loadChats: Loading chats for currentUserId=" + currentUserId);
        databaseReference.child("user_chats").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                 Log.d(TAG, "loadChats: Received chat data. Number of chats: " + snapshot.getChildrenCount());
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String otherUserId = chatSnapshot.getKey();
                    String chatId = chatSnapshot.getValue(String.class);
                    if (otherUserId != null && chatId != null) {
                        Chat chat = new Chat(chatId, otherUserId, "", 0);
                         Log.d(TAG, "loadChats: Added chat: chatId=" + chatId + ", otherUserId=" + otherUserId);
                        chatList.add(chat);
                        listenForLastMessage(chatId, otherUserId);
                    }
                }
                chatAdapter.updateChats(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки чатов: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                 Log.e(TAG, "loadChats: Error loading chats", error.toException());
            }
        });
    }

    private void listenForLastMessage(String chatId, String otherUserId) {
        Log.d(TAG, "listenForLastMessage: Listening for last message for chatId=" + chatId);
        DatabaseReference messagesRef = databaseReference.child("messages").child(chatId);
        ChildEventListener listener = messagesRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    Log.d(TAG, "listenForLastMessage: Last message added for chatId=" + chatId + ": " + message.getText());
                    updateChatLastMessage(chatId, otherUserId, message.getText(), message.getTimestamp());
                } else {
                    Log.w(TAG, "listenForLastMessage: Received null message snapshot for chatId=" + chatId);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "listenForLastMessage: onChildChanged for chatId=" + chatId);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                 Log.d(TAG, "listenForLastMessage: onChildRemoved for chatId=" + chatId);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                 Log.d(TAG, "listenForLastMessage: onChildMoved for chatId=" + chatId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "listenForLastMessage: onCancelled for chatId=" + chatId, error.toException());
            }
        });
        messageListeners.put(chatId, listener);
    }

    private void updateChatLastMessage(String chatId, String otherUserId, String lastMessage, long timestamp) {
        Log.d(TAG, "updateChatLastMessage: Updating chat last message for chatId=" + chatId);
        for (int i = 0; i < chatList.size(); i++) {
            Chat chat = chatList.get(i);
            if (chat.getId().equals(chatId)) {
                chat.setLastMessage(lastMessage);
                chat.setLastMessageTimestamp(timestamp);
                chatAdapter.notifyItemChanged(i);
                 Log.d(TAG, "updateChatLastMessage: Updated chat " + chatId + " at position " + i);
                break;
            }
        }
    }

    private void checkOrCreateChat(String otherUserId) {
        Log.d(TAG, "checkOrCreateChat: Checking for existing chat with user: " + otherUserId);
        databaseReference.child("user_chats").child(currentUserId).child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "checkOrCreateChat: onDataChange triggered. snapshot.exists() = " + snapshot.exists());
                if (snapshot.exists()) {
                    String chatId = snapshot.getValue(String.class);
                     Log.d(TAG, "checkOrCreateChat: Chat exists, chatId=" + chatId);
                    if (chatId != null && chatSelectedListener != null) {
                        chatSelectedListener.onChatSelected(chatId, otherUserId);
                    }
                } else {
                    Log.d(TAG, "checkOrCreateChat: Chat does not exist, creating new.");
                    createNewChat(otherUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "checkOrCreateChat: onCancelled triggered! Error: " + error.getMessage(), error.toException());
                Toast.makeText(getContext(), "Ошибка при проверке чата: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                 Log.e(TAG, "checkOrCreateChat: Error checking chat", error.toException());
            }
        });
    }

    private void createNewChat(String otherUserId) {
        Log.d(TAG, "createNewChat: Creating new chat with user: " + otherUserId);
        String chatId = databaseReference.child("chats").push().getKey();
        if (chatId == null) {
            Toast.makeText(getContext(), "Ошибка создания чата.", Toast.LENGTH_SHORT).show();
             Log.e(TAG, "createNewChat: Failed to get new chatId");
            return;
        }

        Map<String, Object> chatParticipants = new HashMap<>();
        chatParticipants.put(currentUserId, true);
        chatParticipants.put(otherUserId, true);

        databaseReference.child("chats").child(chatId).child("participants").setValue(chatParticipants)
                .addOnSuccessListener(aVoid -> {
                     Log.d(TAG, "createNewChat: Chat participants set for chatId=" + chatId);
                    Map<String, Object> userChats = new HashMap<>();
                    userChats.put("user_chats/" + currentUserId + "/" + otherUserId, chatId);
                    userChats.put("user_chats/" + otherUserId + "/" + currentUserId, chatId);

                    databaseReference.updateChildren(userChats)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), "Чат создан!", Toast.LENGTH_SHORT).show();
                                binding.userIdEditText.setText("");
                                 Log.d(TAG, "createNewChat: User chats updated. Transitioning to chat.");
                                if (chatSelectedListener != null) {
                                    chatSelectedListener.onChatSelected(chatId, otherUserId);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Ошибка обновления чатов пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                 Log.e(TAG, "createNewChat: Error updating user chats", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка создания чата: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                     Log.e(TAG, "createNewChat: Error creating chat", e);
                });
    }

    @Override
    public void onChatClick(Chat chat) {
        Log.d(TAG, "onChatClick: Chat clicked. chatId=" + chat.getId() + ", otherUserId=" + chat.getOtherUserId());
        if (chatSelectedListener != null) {
            chatSelectedListener.onChatSelected(chat.getId(), chat.getOtherUserId());
        } else {
             Log.w(TAG, "onChatClick: chatSelectedListener is null");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        chatSelectedListener = null;
        settingsButtonClickListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        // Удаляем все слушатели сообщений
        for (ChildEventListener listener : messageListeners.values()) {
            if (listener != null) {
                databaseReference.child("messages").removeEventListener(listener);
                 Log.d(TAG, "onDestroyView: Removed message listener.");
            }
        }
        messageListeners.clear();
        binding = null;
    }

     @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.d(TAG, "onViewStateRestored");
    }
} 