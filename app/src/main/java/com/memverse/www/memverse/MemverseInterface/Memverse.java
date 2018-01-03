package com.memverse.www.memverse.MemverseInterface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.memverse.www.memverse.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Joshua Swaim on 12/15/17.
 * This is a class for interacting with the Memverse API when an internet connection is available
 * and mimicking the Memverse API when the internet is not available.
 */

public class Memverse {

    // True if user is logged in, false by default (static so that it's shared by all instances of Memverse)
    private static boolean logged_in;
    // Email (received after log in) (static so that it's shared by all instances of Memverse)
    private static String email;

    // Interface for handling internet interactions (static so that it's shared by all instances of Memverse)
    private static MemverseWebInterface webInterface = new MemverseWebInterface();

    /**
     * Attempt to log into Memverse account. Pass true to the callback if successful and false
     * otherwise.
     * @param input_email the email address of the user attempting to log in
     * @param input_password the password of the user attempting to log in
     * @param callback a callback to be called on the completion of the log in task to which will be
     *                 passed true if log in was successful and false otherwise
     */
    public void login(final String input_email, String input_password, final MemverseCallback<Boolean> callback) {
        //TODO: Handle network errors
        webInterface.authenticate(input_email, input_password, new MemverseCallback<Boolean>() {
            @Override
            public void call(Boolean success) {
                if (success) {
                    // Login succeeded: save information and inform callback
                    email = input_email.toLowerCase();
                    logged_in = true;
                    callback.call(true);
                } else {
                    // Login failed: inform callback
                    callback.call(false);
                }
            }
        });
    }

    /**
     * Log out by resetting information specific to the current user and setting logged_in=false
     */
    public void logout() {
        logged_in=false;
        email="";
        webInterface.resetAuthToken();
    }

    /**
     * Display the current user's Gravatar profile picture in the given ImageView. If no user is
     * currently logged in, or if the picture cannot be downloaded, set the ImageView to
     * a stock image ("drawable/anonymous.png").
     * @param imageView the ImageView in which to display the picture
     */
    public void showGravatar(final ImageView imageView) {
        if (logged_in) {
            webInterface.getGravatar(email, new MemverseCallback<Bitmap>() {
                @Override
                public void call(Bitmap bitmap) {
                    if (bitmap!=null) imageView.setImageBitmap(bitmap);
                    // If bitmap is null, the image could not be downloaded
                    else imageView.setImageBitmap(BitmapFactory.decodeResource(
                            imageView.getContext().getResources(), R.drawable.anonymous));
                }
            });
        } else {
            imageView.setImageBitmap(BitmapFactory.decodeResource(
                    imageView.getContext().getResources(), R.drawable.anonymous));
        }
    }

    /**
     * Attempt to retrieve the current user's memory verses and pass them in a JSONArray to the
     * given callback (see Memverse API documentation for the format of the JSONArray). Pass null
     * to the callback if an error occurs (like a network error) or if no user is logged in.
     * @param callback the callback to which a JSONArray of the user's memory verses will be passed
     *                 (null will be passed to this callback if an error occurs or if no user is
     *                 logged in)
     */
    public void getMemoryVerses(final MemverseCallback<JSONArray> callback) {
        if (logged_in) {
            webInterface.getMemoryVerses(callback, "verse");
            // TODO: Handle network errors
        } else {
            callback.call(null);
        }
    }

    /**
     * Attempt to retrieve the current user's memory verses that are due for review and pass them
     * in a JSONArray to the given callback (see Memverse API documentation for the format of the
     * JSONArray). Pass null to the callback if an error occurs (like a network error) or if no user
     * is logged in.
     * @param callback the callback to which a JSONArray of the user's due memory verses will be
     *                 passed (null will be passed to this callback if an error occurs or if no user
     *                 is logged in)
     */
    public void getDueMemoryVerses(final MemverseCallback<JSONArray> callback) {
        if (logged_in) {
            webInterface.getMemoryVerses(new MemverseCallback<JSONArray>() {
                @Override
                public void call(JSONArray verses) {
                    if (verses!=null) {
                        try {
                            // This array will hold a list of verses that are due
                            JSONArray due_verses = new JSONArray();
                            if (verses.length() > 0) {
                                // dateFormat specifies the format that Memverse uses to store dates
                                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                for (int i = 0; i < verses.length(); i++) {
                                    JSONObject verse_data = verses.getJSONObject(i);
                                    // Don't count this verse as due for review if it's status is
                                    // pending
                                    if (verse_data.getString("status").equals("Pending")) continue;
                                    // Check whether the verse's next test date is today or before today
                                    if (dateFormat.parse(verse_data.getString("next_test"))
                                            .compareTo(Calendar.getInstance().getTime())<=0) {
                                        due_verses.put(verse_data);
                                    } else {
                                        // Verses are ordered by next test date, so once we reach the
                                        // first verse that isn't due, none of the rest will be due.
                                        break;
                                    }
                                }
                            }
                            callback.call(due_verses);
                        } catch (JSONException e) {
                            // The verses JSONObject was not in the correct format
                            callback.call(null);
                        } catch (ParseException e) {
                            // Something went wrong with the Date parser
                            callback.call(null);
                        } // TODO: Handle network errors
                    } else {
                        callback.call(null);
                    }
                }
            }, "next_test");
        } else {
            callback.call(null);
        }
    }

    /**
     * Attempt to record a rating for a verse.
     * @param verse a JSONObject containing a field called "id" pointing to an integer representing
     *              the id of the verse to be rated (the JSONObjects returned by the Memverse API
     *              satisfy this requirement).
     * @param rating a number between 1 and 5 (inclusive) representing the rating to be recorded
     * @throws JSONException the verse parameter does not contain the key "id"
     */
    public void rateVerse(JSONObject verse, int rating) throws JSONException {
        // TODO: Check for success
        // TODO: Handle network errors
        webInterface.rateVerse(((Integer) verse.getInt("id")).toString(), ((Integer) rating).toString());
    }

    /**
     * Determine whether anyone is logged in.
     * @return true if user is logged in and false otherwise
     */
    public boolean is_loggedIn() {
        return logged_in;
    }

    /**
     * Get the email address of the current user (return "" if no one is logged in).
     * @return the current user's email address (or "" if no one is logged in)
     */
    public String getEmail() {
        return email;
    }
}
