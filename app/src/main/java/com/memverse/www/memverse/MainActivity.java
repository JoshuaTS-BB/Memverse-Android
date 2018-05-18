package com.memverse.www.memverse;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

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

        // Setup the link to the Privacy Policy
        TextView ppLink = findViewById(R.id.privacyPolicyLink);
        ppLink.setText(Html.fromHtml("<a href='"+getResources().getString(R.string.privacy_policy_url)+
                "'>"+getResources().getString(R.string.privacy_policy_name)+"</a>"));
        ppLink.setMovementMethod(LinkMovementMethod.getInstance());

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
        findViewById(R.id.main_addVersesButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity(AddVersesActivity.class);
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
