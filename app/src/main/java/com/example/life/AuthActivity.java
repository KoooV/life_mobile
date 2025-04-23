package com.example.life;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;

public class AuthActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        EdgeToEdge.enable(this);
        hideSystemUI();
    }
}