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
import androidx.recyclerview.widget.RecyclerView;

import com.example.life.databinding.FragmentMessengerBinding;
import com.example.life.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MessengerFragment extends Fragment {

    private static final String TAG = "MessengerFragment";

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

    public MessengerFragment() {
        // Required empty public constructor
    }

    public static MessengerFragment newInstance(String chatId, String otherUserId) {
        MessengerFragment fragment = new MessengerFragment();
        Bundle args = new Bundle();
        args.putString("chatId", chatId);
        args.putString("otherUserId", otherUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");
        if (context instanceof OnMenuButtonClickListener) {
            menuButtonClickListener = (OnMenuButtonClickListener) context;
        } else {
            Log.e(TAG, context.toString() + " must implement OnMenuButtonClickListener");
            //throw new RuntimeException(context.toString() + " must implement OnMenuButtonClickListener");
            // Закомментировал выброс исключения, чтобы приложение не падало сразу, но логировало ошибку
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            otherUserId = getArguments().getString("otherUserId");
            Log.d(TAG, "onCreate: chatId=" + chatId + ", otherUserId=" + otherUserId);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        binding = FragmentMessengerBinding.inflate(inflater, container, false);
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

        if (chatId == null || otherUserId == null) {
            Toast.makeText(getContext(), "Ошибка: ID чата или ID другого пользователя отсутствует", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onViewCreated: chatId или otherUserId отсутствует");
            // Продолжаем работу, так как переход в меню должен быть возможен
        } else {
             binding.userStatusTextView.setText(otherUserId);
        }

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(currentUserId);

        recyclerView = view.findViewById(R.id.messagesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(messageAdapter);

        if (chatId != null) {
            messagesRef = databaseReference.child("messages").child(chatId);
            loadMessages();
        } else {
            Log.d(TAG, "onViewCreated: chatId отсутствует, не загружаем сообщения.");
        }


        binding.sendButton.setOnClickListener(v -> {
            Log.d(TAG, "sendButton clicked");
            String messageText = binding.messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            } else {
                Toast.makeText(getContext(), "Введите сообщение", Toast.LENGTH_SHORT).show();
            }
        });

        binding.pathTextView.setOnClickListener(v -> {
            Log.d(TAG, "pathTextView (menu) clicked");
            if (menuButtonClickListener != null) {
                menuButtonClickListener.onMenuButtonClick();
            } else {
                 Log.w(TAG, "menuButtonClickListener is null");
            }
        });
    }

    private void loadMessages() {
        Log.d(TAG, "loadMessages: Loading messages for chatId=" + chatId);
        if (messagesRef == null) {
             Log.e(TAG, "loadMessages: messagesRef is null!");
             return;
        }
        messagesListener = messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    Log.d(TAG, "loadMessages: Message added: " + message.getText());
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    messageAdapter.setMessages(messageList);
                } else {
                    Log.w(TAG, "loadMessages: Received null message snapshot.");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildChanged");
                // Обработка изменения сообщения (если нужно)
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                 Log.d(TAG, "onChildRemoved");
                // Обработка удаления сообщения (если нужно)
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                 Log.d(TAG, "onChildMoved");
                // Обработка перемещения сообщения (если нужно)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки сообщений: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Ошибка загрузки сообщений", error.toException());
            }
        });
    }

    private void sendMessage(String text) {
        Log.d(TAG, "sendMessage: " + text);
        if (currentUserId == null || chatId == null) {
            Toast.makeText(getContext(), "Ошибка: Не удалось отправить сообщение (пользователь или чат не определен)", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "sendMessage: currentUserId or chatId is null");
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Toast.makeText(getContext(), "Ошибка: Не удалось создать ID сообщения", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "sendMessage: Failed to get messageId");
            return;
        }

        long timestamp = System.currentTimeMillis();
        Message message = new Message(messageId, text, currentUserId, timestamp);

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    binding.messageEditText.setText("");
                    Log.d(TAG, "Сообщение успешно отправлено: " + messageId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка отправки сообщения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Ошибка отправки сообщения", e);
                });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        menuButtonClickListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
            Log.d(TAG, "onDestroyView: Messages listener removed.");
        }
        binding = null;
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