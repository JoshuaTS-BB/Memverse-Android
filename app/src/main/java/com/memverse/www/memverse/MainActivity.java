package com.memverse.www.memverse;

import android.os.Bundle;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

public class MainActivity extends NavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_main);
    }
}
