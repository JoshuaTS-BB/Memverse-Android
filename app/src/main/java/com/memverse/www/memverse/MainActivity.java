package com.memverse.www.memverse;

import android.os.Bundle;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

// TODO: Keep from going to forbidden screens after a logout
public class MainActivity extends NavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If there is no launching activity (the app was just started), go straight to the log in
        // page
        if(this.getIntent().getStringExtra("LAUNCHING_ACTIVITY")==null) {
            Map<String, String> data = new HashMap<>();
            data.put(LoginActivity.AUTOMATIC_LOGIN, "true");
            launchActivity(LoginActivity.class, data);
        }

        // Otherwise, setup the home page
        setupNavigationActivity(R.layout.activity_main);

        if(memverse.is_loggedIn()) {
            findViewById(R.id.main_loggedOutButtonsLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.main_loggedInButtonsLayout).setVisibility(View.GONE);
        }

        findViewById(R.id.main_loginButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity(LoginActivity.class);
            }
        });
        findViewById(R.id.main_reviewButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity(ReviewActivity.class);
            }
        });
        findViewById(R.id.main_viewVersesButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity(ViewVersesActivity.class);
            }
        });
        findViewById(R.id.main_logoutButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
    }
}
