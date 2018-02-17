package com.memverse.www.memverse;

import android.os.Bundle;
import android.view.View;

/**
 * Notify users that they have no more verses to review
 */
public class ReviewCompletedActivity extends NavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_review_completed);
        // TODO: Display more information about the user's progress
    }
}
