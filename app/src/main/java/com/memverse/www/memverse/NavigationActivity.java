package com.memverse.www.memverse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.Memverse;

/**
 * Created by josh on 12/14/17.
 */

public abstract class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = NavigationActivity.class.getSimpleName();

    // Object for interacting with Memverse
    protected Memverse memverse;

    protected void setupNavigationActivity(int view_id) {
        setContentView(view_id);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        // Load the Memverse object
        // memverse = (Memverse) intent.getParcelableExtra(Memverse.EXTRA_MEMVERSE_OBJECT);
        memverse = new Memverse();

        /**
         * Set up action bar and navigation drawer
         */
        // Set up an action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Don't display the title (it's already in a picture in the action bar)
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Add a button to the action bar for displaying the navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle navToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(navToggle);
        navToggle.syncState();

        // Listen for button presses in the navigation drawer (listener is defined in NavigationActivity)
        ((NavigationView) findViewById(R.id.navigation)).setNavigationItemSelectedListener(this);

        /**
         * Set up menu buttons
         */
        switch (getComponentName().getClassName()) {
            case "com.memverse.www.memverse.MainActivity":
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.home_nav_button).setVisible(false);
                break;
            case "com.memverse.www.memverse.LoginActivity":
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.login_nav_button).setVisible(false);
                break;
            case "com.memverse.www.memverse.ViewVersesActivity":
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.view_verses_nav_button).setVisible(false);
                break;
            case "com.memverse.www.memverse.ReviewActivity":
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.review_nav_button).setVisible(false);
                break;
        }
        if (memverse.is_loggedIn()) {
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.login_nav_button).setVisible(false);
        }
        else {
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.logout_nav_button).setVisible(false);
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.view_verses_nav_button).setVisible(false);
        }

        /**
         * Display user information in navigation drawer
         */
        if (memverse.is_loggedIn()) {
            memverse.showGravatar((ImageView) ((NavigationView) findViewById(R.id.navigation))
                    .getHeaderView(0).findViewById(R.id.profile_picture));
            //TODO: Display username
            ((TextView) ((NavigationView) findViewById(R.id.navigation)).getHeaderView(0)
                    .findViewById(R.id.username)).setText("");
            ((TextView) ((NavigationView) findViewById(R.id.navigation)).getHeaderView(0)
                    .findViewById(R.id.emailTextView)).setText(memverse.getEmail());
        }
    }

    // Called when a navigation menu button is pressed
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        boolean navigated=false;
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case  R.id.home_nav_button:
                launchActivity(MainActivity.class);
                navigated=true;
                break;
            case  R.id.login_nav_button:
                launchActivity(LoginActivity.class);
                navigated=true;
                break;
            case  R.id.logout_nav_button:
                memverse.logout();
                launchActivity(MainActivity.class);
                navigated=true;
                break;
            case  R.id.view_verses_nav_button:
                launchActivity(ViewVersesActivity.class);
                navigated=true;
                break;
            case  R.id.review_nav_button:
                launchActivity(ReviewActivity.class);
                navigated=true;
                break;
        }
        if (navigated) {
            // Hide drawer after use
            ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        }
        // Return false: don't display item as selected item
        return false;
    }

    @Override
    public void onBackPressed() {
        // Hide drawer when back button is pressed if it is visible. Otherwise, default behavior.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    protected void launchActivity(Class activity) {
        // Start new activity and pass it the memverse object in the intent
        //Intent intent = new Intent(this, activity);
        //intent.putExtra(Memverse.EXTRA_MEMVERSE_OBJECT, memverse);
        Log.d(TAG, "LAUNCH, oh, I'm osrry, I meant launch 2");
        startActivity(new Intent(this, activity));
    }

    /**
     * Show the progress spinner and hide everything else.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected void showProgress(final boolean show) {
        final View main_content = findViewById(R.id.main_content);
        final ProgressBar progressBar = findViewById(R.id.progress_spinner);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            main_content.setVisibility(show ? View.GONE : View.VISIBLE);
            main_content.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    main_content.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            main_content.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
