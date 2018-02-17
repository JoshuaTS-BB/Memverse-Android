package com.memverse.www.memverse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.Memverse;
import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

/**
 * Created by Joshua Swaim on 12/14/17.
 * This class is the parent of all other activity classes in this app.
 */

public abstract class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Object for interacting with Memverse
    protected Memverse memverse = new Memverse();

    /**
     * Initialize the activity (this should be called by onCreate method in child classes
     * @param view_id The resource ID of the view to use for this activity
     */
    protected void setupNavigationActivity(int view_id) {
        setContentView(R.layout.main);

        getLayoutInflater().inflate(view_id, (LinearLayout) findViewById(R.id.content));

        // Set up an action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Don't display a text version of the title (it's already in a picture in the action bar)
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Add a button to the action bar for displaying the navigation drawer
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle navToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.action_navigation_drawer_open, R.string.action_navigation_drawer_close);
        drawer.addDrawerListener(navToggle);
        navToggle.syncState();

        // Listen for button presses in the navigation drawer
        ((NavigationView) findViewById(R.id.navigation)).setNavigationItemSelectedListener(this);

        // Hide menu buttons that are not needed
        switch (view_id) {
            case R.layout.activity_main:
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.home_nav_button).setVisible(false);
                break;
            case R.layout.activity_login:
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.login_nav_button).setVisible(false);
                break;
            case R.layout.activity_view_verses:
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.view_verses_nav_button).setVisible(false);
                break;
            case R.layout.activity_review:
                ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.review_nav_button).setVisible(false);
                break;
        }
        if (memverse.is_loggedIn()) {
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.login_nav_button).setVisible(false);
        }
        else {
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.logout_nav_button).setVisible(false);
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.view_verses_nav_button).setVisible(false);
            ((NavigationView) findViewById(R.id.navigation)).getMenu().findItem(R.id.review_nav_button).setVisible(false);
        }

        // Display user information in navigation drawer
        if (memverse.is_loggedIn()) {
            memverse.showGravatar((ImageView) ((NavigationView) findViewById(R.id.navigation))
                    .getHeaderView(0).findViewById(R.id.profile_picture));
            memverse.getUsername(new MemverseCallback<String>() {
                @Override
                public void call(String username) {
                    ((TextView) ((NavigationView) findViewById(R.id.navigation)).getHeaderView(0)
                            .findViewById(R.id.username)).setText(username);
                }
            }, new MemverseCallback<String>() {
                @Override
                public void call(String input) {
                    // The username could not be retrieved
                    ((TextView) ((NavigationView) findViewById(R.id.navigation)).getHeaderView(0)
                        .findViewById(R.id.username)).setText("");
                }
            });
            ((TextView) ((NavigationView) findViewById(R.id.navigation)).getHeaderView(0)
                    .findViewById(R.id.emailTextView)).setText(memverse.getEmail());
        }
    }

    /**
     * Handle menu button selections (this is called whenever a menu item in the navigation drawer
     * is selected).
     * @param item the menu item that was selected
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case  R.id.home_nav_button:
                launchActivity(MainActivity.class);
                break;
            case  R.id.login_nav_button:
                launchActivity(LoginActivity.class);
                break;
            case  R.id.logout_nav_button:
                memverse.logout();
                launchActivity(MainActivity.class);
                break;
            case  R.id.view_verses_nav_button:
                launchActivity(ViewVersesActivity.class);
                break;
            case  R.id.review_nav_button:
                launchActivity(ReviewActivity.class);
                break;
        }
        // Hide drawer after use
        ((DrawerLayout) findViewById(R.id.drawerLayout)).closeDrawer(GravityCompat.START);
        // Return false: don't highlight the menu item that was selected
        return false;
    }

    /**
     * If the navigation drawer is visible when the back button is pressed, hide it. Otherwise,
     * go with the back button's default behavior.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Prepare to switch to a new activity and then start it.
     * @param activity the new activity to start
     */
    protected void launchActivity(Class activity) {
        startActivity(new Intent(this, activity));
    }

    /**
     * Show the progress spinner and hide everything else if show is true, and vice versa if show is
     * false.
     * @param show true if the progress spinner should be shown and false if it should be hidden
     */
    protected void showProgress(final boolean show) {
        final View main_content = findViewById(R.id.mainContentLayout);
        final ProgressBar progressBar = findViewById(R.id.progressSpinner);

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        main_content.setVisibility(show ? View.GONE : View.VISIBLE);
        main_content.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        main_content.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    /**
     * Display the given error message in a snackbar at the bottom of the screen
     * @param msg the error message to display
     */
    protected void showErrorMsg(String msg) {
        Snackbar.make(findViewById(R.id.coordinatorLayout), msg, Snackbar.LENGTH_LONG).show();
    }
}
