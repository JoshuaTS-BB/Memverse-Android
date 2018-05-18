package com.memverse.www.memverse;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

import java.util.List;

/**
 * Allow users to log into their Memverse accounts
 */
public class LoginActivity extends NavigationActivity {
    // Constants used when interfacing with Google SmartLock to store and retrieve passwords
    final int RC_READ=0;
    final int RC_SAVE=1;

    // A key that can be used in an extra in an Intent object by other activities when they start a
    // LoginActivity. The value associated with this key tells the LoginActivity whether to try to
    // automatically log the user in or not. It's value  should be the String "true" or the String
    // "false".
    public final static String AUTOMATIC_LOGIN="AUTOMATIC_LOGIN";

    // The name of the Google SmartLock account type where passwords for this app will be saved
    private final String SMARTLOCK_ACCOUNT_TYPE_MEMVERSE = "MEMVERSE_ANDROID_APP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Since this is the launch activity, its label in the manifest has to be set to the App's
        // title. Therefore, this activity's label has to be manually set back to the login activity
        // title.
        setTitle(R.string.title_activity_login);

        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_login);

        String do_auto_login = getIntent().getStringExtra(AUTOMATIC_LOGIN);
        if(do_auto_login!=null && do_auto_login.equals("true")) {
            // Attempt to log in automatically
            attemptSmartLockLogin(true);
        } else {
            // Attempt to automatically fill out the log in form
            attemptSmartLockLogin(false);
        }
    }

    /**
     * Attempt to log into the user's Memverse account and display an error message if unsuccessful.
     * @param email the user's email
     * @param password the user's Memverse password
     */
    private void attemptLogin(final String email, final String password) {
        // Attempt log in and display a progress spinner while waiting
        showProgress(true);
        memverse.login(email, password, new MemverseCallback<Boolean>() {
            @Override
            public void call(Boolean success) {
                showProgress(false);
                if (success) {
                    // The log in was successful
                    // Attempt to save the password
                    savePassword(email, password);
                    // Go to the home page
                    launchActivity(MainActivity.class);
                } else {
                    // Wrong username/password
                    showErrorMsg(getString(R.string.error_incorrect_password));
                    findViewById(R.id.login_passwordInput).requestFocus();
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

    /**
     * Attempt to log into the user's Meverse account using the credentials entered into the sign in
     * form.
     */
    private void formLogin() {
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
            attemptLogin(email, password);
        }
    }

    /**
     * Tells the Activity to wait for the user to enter a username and password into the sign in form
     * (probably because the automatic log in failed).
     * @param email an email to automatically fill into the email part of the form (can be "")
     * @param password a password to automatically fill into the password part of the form (can be "")
     */
    private void listenForFormLogin(String email, String password) {
        // Fill in email and password (they might be empty Strings)
        EditText password_input = findViewById(R.id.login_passwordInput);
        password_input.setText(password);
        ((EditText) findViewById(R.id.login_emailInput)).setText(email);
        // Hide the progress spinner and display the log in form
        showProgress(false);
        // Listen for input events that signal that the user is ready to submit the form (e.g.
        // pressing the enter button)
        password_input.setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                        if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                            formLogin();
                            return true;
                        }
                        return false;
                    }
                });

        // Listen for clicks on the sign in button
        findViewById(R.id.login_submitButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                formLogin();
            }
        });
    }

    /**
     * Attempt to either automatically log the user in or to automatically fill out the login form
     * using a password saved on Google SmartLock.
     * @param do_automatic_login if true, automatically log in; if false, automatically fill out login
     *                           form
     */
    private void attemptSmartLockLogin(final boolean do_automatic_login) {
        // Create a request object to request a Memverse account password from Google SmartLock
        CredentialRequest req = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(SMARTLOCK_ACCOUNT_TYPE_MEMVERSE)
                .build();
        // Make the request for account information
        Credentials.getClient(this).request(req).addOnCompleteListener(
                new OnCompleteListener<CredentialRequestResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<CredentialRequestResponse> task) {
                        if (task.isSuccessful()) {
                            // A Memverse email and password were successfully retrieved
                            Credential credential = task.getResult().getCredential();
                            if(do_automatic_login) {
                                // Automatically log the user in without waiting for the user to
                                // submit the login form
                                attemptLogin(credential.getId(), credential.getPassword());
                            } else {
                                // Automatically fill out the login form but do not submit it
                                listenForFormLogin(credential.getId(), credential.getPassword());
                            }
                        } else {
                            // It's possible that the password retrieval was unsuccessful because
                            // the user has multiple Memverse accounts saved on SmartLock
                            Exception e = task.getException();
                            if (e instanceof ResolvableApiException) {
                                // If the exception is resolvable, attempt to resolve it
                                try {
                                    // Open up a dialog that asks the user to choose which of the
                                    // saved Memverse accounts to use. Once an account is chosen,
                                    // the onActivityResult method will be called.
                                    ((ResolvableApiException) e).startResolutionForResult(LoginActivity.this, RC_READ);
                                } catch (IntentSender.SendIntentException send_err) {
                                    // Something went wrong. Just display the blank login form so
                                    // that the user can manually fill it out
                                    listenForFormLogin("", "");
                                }
                            } else {
                                // No password could be retrieved. Just display the blank login form
                                // so that the user can manually fill it out
                                listenForFormLogin("", "");
                            }
                        }
                    }
                });
    }

    /**
     * Save the given email and password to Google SmartLock so that it can be automatically
     * retrieved in the future (note: the user's permission is required for the password to be saved).
     * @param email the email address used for the Memverse account
     * @param password the password used for the Memverse account
     */
    private void savePassword(String email, String password) {
        // Create a new Credential object from the email and password
        Credential credential = new Credential.Builder(email)
                .setPassword(password)
                .build();
        // Save this LoginActivity so that it can be accessed from the addOnCompleteListener
        final Activity loginActivity = this;
        // Attempt to save the credentials
        Credentials.getClient(this).save(credential).addOnCompleteListener(
                new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // The email and password were successfully saved
                            return;
                        }

                        // A resolvable exception may be thrown because the user needs to give
                        // permission for the password to be saved on SmartLock
                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) e;
                            try {
                                // Attempt to get permission to save the password
                                rae.startResolutionForResult(loginActivity, RC_SAVE);
                            } catch (IntentSender.SendIntentException e2) {
                                // Something went wrong. The password could not be saved. Oh well...
                            }
                        } else {
                            // The password could not be saved.
                        }
                    }
                });
    }

    /**
     * This function is called when the user has to choose from multiple Memverse accounts on Google
     * SmartLock after the user has chosen an account to use
     * @param requestCode used to make sure that the function call was triggered by an attempt to
     *                    retrieve account data from Google SmartLock
     * @param resultCode indicates whether the system was able to successfully determine which
     *                   Memverse account on SmartLock was chosen
     * @param data contains extra information about something...
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If this function call was triggered by an attempt to retrieve Memverse account data from
        // Google SmartLock
        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                // Retrieve the user's Memverse account information from SmartLock
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                // Automatically fill out the login form with the retrieved. Since the user had to
                // choose an account from a dialog, do not automatically log in but instead wait for
                // the user to submit the login form
                listenForFormLogin(credential.getId(), credential.getPassword());
            } else {
                // The user's account information could not be retireved. Just display an empty login
                // form and wait for the user to fill it out
                listenForFormLogin("", "");
            }
        }

    }
}

