package com.example.tiktokcloneproject.activity;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.fragment.InboxFragment;
import com.example.tiktokcloneproject.fragment.NavigationFragment;

public class InboxActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        // Hiển thị InboxFragment vào vùng chứa chính
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.flInboxContainer, InboxFragment.newInstance("inbox"));
        
        // Hiển thị thanh Navigation bên dưới
        ft.replace(R.id.flNavigation, NavigationFragment.newInstance("navigation"));
        ft.commit();
    }
}
