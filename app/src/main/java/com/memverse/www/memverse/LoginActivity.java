package com.memverse.www.memverse;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

import java.util.List;

/**
 * Allow users to log into their Memverse accounts
 */
public class LoginActivity extends NavigationActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_login);

        // Listen for input events that signal that the user is ready to submit the form (e.g.
        // pressing the enter button)
        ((EditText) findViewById(R.id.login_passwordInput)).setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                        if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                            attemptLogin();
                            return true;
                        }
                        return false;
                    }
                });

        // Listen for clicks on the sign in button
        findViewById(R.id.login_submitButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    /**
     * Attempt to log into the user's Memverse account and display an error message if unsuccessful.
     */
    private void attemptLogin() {
        AutoCompleteTextView emailView = findViewById(R.id.login_emailInput);
        final EditText passwordView = findViewById(R.id.login_passwordInput);

        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        if (TextUtils.isEmpty(email)) {
            // The user hasn't entered an email address
            emailView.setError(getString(R.string.error_field_required));
            emailView.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            // The user hasn't entered an email address
            passwordView.setError(getString(R.string.error_field_required));
            passwordView.requestFocus();
        } else {
            // Attempt log in and display a progress spinner while waiting
            showProgress(true);
            memverse.login(email, password, new MemverseCallback<Boolean>() {
                @Override
                public void call(Boolean success) {
                    showProgress(false);
                    if (success) {
                        // The log in was successful
                        launchActivity(MainActivity.class);
                    } else {
                        // Wrong username/password
                        showErrorMsg(getString(R.string.error_incorrect_password));
                        passwordView.requestFocus();
                    }
                }
            }, new MemverseCallback<String>() {
                @Override
                public void call(String error_type) {
                    // An error (other than wrong username/password) occurred (e.g. a network error)
                    showProgress(false);
                    if (error_type.equals("network error")) {
                        showErrorMsg(getString(R.string.error_network));
                    } else {
                        showErrorMsg(getString(R.string.error_generic));
                    }
                }
            });
        }
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        // TODO: Add autocomplete to log in form
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        ((AutoCompleteTextView) findViewById(R.id.login_emailInput)).setAdapter(adapter);
    }
}

