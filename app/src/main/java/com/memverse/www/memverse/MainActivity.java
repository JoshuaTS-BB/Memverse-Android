package com.memverse.www.memverse;

import android.os.Bundle;
import android.view.View;
// TODO: Keep from going to forbidden screens after a logout
public class MainActivity extends NavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                memverse.logout();
                launchActivity(MainActivity.class);
            }
        });
    }
}
