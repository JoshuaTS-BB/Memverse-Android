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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    // dateFormat specifies the format that Memverse uses to store dates
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Interface for handling internet interactions (static so that it's shared by all instances of Memverse)
    private final static MemverseWebInterface webInterface = new MemverseWebInterface();

    /**
     * Attempt to log into Memverse account. Pass true to the callback if successful and false
     * otherwise.
     * @param input_email the email address of the user attempting to log in
     * @param input_password the password of the user attempting to log in
     * @param success_callback a callback to which will be passed true if the log in was successful
     *                         or false if the connection was successful but the password and/or
     *                         username were incorrect
     * @param error_callback a callback to which will be passed the error type if an error (other
     *                       than a username/password error) occurs (see MemverseWebInterface
     *                       class description for a list of possible error types)
     */
    public void login(final String input_email, String input_password,
                      final MemverseCallback<Boolean> success_callback,
                      final MemverseCallback<String> error_callback) {
        webInterface.authenticate(input_email, input_password, new MemverseCallback<Boolean>() {
            @Override
            public void call(Boolean success) {
                if (success) {
                    // Login succeeded: save information and inform callback
                    email = input_email.toLowerCase();
                    logged_in = true;
                    success_callback.call(true);
                } else {
                    // The password and/or username were incorrect: inform callback
                    success_callback.call(false);
                }
            }
        }, error_callback);
    }

    /**
     * Return the reference of the given verse.
     * @param verse a JSONObject containing information about the verse formatted according to the
     *              Memverse API
     * @throws JSONException the verse object was not correctly formatted (see Memverse API for
     *                       correct format)
     */
    public String getReference(JSONObject verse) throws JSONException {
        return verse.getString("book") + " " +
                ((Integer) verse.getInt("chapter")).toString() + ":" +
                ((Integer) verse.getInt("versenum")).toString();
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
     * Attempt to retrieve all the verses in the given passage and pass them as an array to the
     * success_callback. If an error occurs (like a network error), pass the error type to the
     * error_callback.
     * @param book the full name of the book of the Bible containing the desired passage (case
     *             insensitive)
     * @param chapter the number of the chapter of the Bible containing the desired passage
     * @param start_verse the number of the first verse in the passage (can be set to 0 if the
     *                    whole chapter is desired)
     * @param end_verse the number of the last verse in the passage (can be the same as start_verse
     *                  and can be set to 0 if the whole chapter is desired)
     * @param success_callback the callback to which an ArrayList of the verses in the passage will
     *                         be passed if retrieval is successful
     * @param error_callback a callback to which will be passed the error type if an error occurs
     *                       (see MemverseWebInterface class description for a list of possible
     *                       error types)
     */
    public void getVerses(String book, int chapter, int start_verse, int end_verse, String translation,
                            final MemverseCallback<List<JSONObject>> success_callback,
                            final MemverseCallback<String> error_callback) {
        if(start_verse!=0 && end_verse>=start_verse)
            // If the start and end verses are specified, recursively retreive all the verses
            getVerses(book, chapter, start_verse, end_verse, translation,
                    new ArrayList<JSONObject>(end_verse-start_verse+1),
                    success_callback, error_callback);
        else if(start_verse!=0)
            // If just the start verse is specified, retrieve it
            getVerses(book, chapter, start_verse, start_verse, translation,
                    new ArrayList<JSONObject>(1),
                    success_callback, error_callback);
        else {
            webInterface.getChapter(book, Integer.toString(chapter), translation,
                    new MemverseCallback<JSONArray>() {
                @Override
                public void call(JSONArray verses) {
                    List<JSONObject> versesList = new ArrayList<>(verses.length());
                    try {
                        for (int i = 0; i < verses.length(); i++) {
                            versesList.add(verses.getJSONObject(i));
                        }
                        success_callback.call(versesList);
                    } catch (JSONException e) {
                        // Something went wrong while parsing the verses JSONArray
                        error_callback.call("response error");
                    }
                }
            }, error_callback);
        }
    }

    /**
     * Recursively retrieve all the verses in the given passage and pass them as an array to the
     * success_callback. If an error occurs (like a network error), pass the error type to the
     * error_callback.
     * @param book the full name of the book of the Bible containing the desired passage (case
     *             insensitive)
     * @param chapter the number of the chapter of the Bible containing the desired passage
     * @param start_verse the number of the first verse in the passage (can be left blank if the
     *                    whole chapter is desired)
     * @param end_verse the number of the last verse in the passage (can be the same as start_verse
     *                  and can be left blank if the whole chapter is desired)
     * @param translation the abbreviation of the desired Bible translation (note: for New King
     *                    James Version, use NKJ, not NKJV)
     * @param previous_verses a list of the previously retrieved verses
     * @param success_callback the callback to which an ArrayList of the verses in the passage will
     *                         be passed if retrieval is successful
     * @param error_callback a callback to which will be passed the error type if an error occurs
     *                       (see MemverseWebInterface class description for a list of possible
     *                       error types)
     */
    private void getVerses(final String book, final int chapter, final int start_verse,
                           final int end_verse, final String translation,
                           final List<JSONObject> previous_verses,
                           final MemverseCallback<List<JSONObject>> success_callback,
                           final MemverseCallback<String> error_callback) {
        webInterface.getVerse(book, Integer.toString(chapter), Integer.toString(start_verse), translation,
                new MemverseCallback<JSONObject>() {
                    @Override
                    public void call(JSONObject verse) {
                        // Add the verse to the list of previous verses
                        previous_verses.add(verse);
                        if(end_verse==start_verse)
                            success_callback.call(previous_verses);
                        else
                            getVerses(book, chapter, start_verse+1, end_verse,
                                    translation, previous_verses, success_callback, error_callback);
                    }
                }, error_callback);
    }

    /**
     * Attempt to retrieve the current user's memory verses and pass them in an ArrayList to the
     * given callback (see Memverse API documentation for the format of the ArrayList). If an error
     * occurs (like a network error) or if no user is logged in, pass the error type to the
     * error_callback.
     * @param success_callback the callback to which an ArrayList of the user's memory verses will be
     *                         passed if retrieval is successful
     * @param error_callback a callback to which will be passed the error type if an error occurs
     *                       (see MemverseWebInterface class description for a list of possible
     *                       error types)
     */
    public void getMemoryVerses(final MemverseCallback<List<JSONObject>> success_callback,
                                final MemverseCallback<String> error_callback) {
        if (logged_in) {
            webInterface.getMemoryVerses("ref", new MemverseCallback<JSONArray>() {
                @Override
                public void call(JSONArray verses) {
                    // Convert JSON Array to a regular Array and pass it to the success callback
                    List<JSONObject> versesList = new ArrayList<>(verses.length());
                    try {
                        for (int i = 0; i < verses.length(); i++) {
                            versesList.add(verses.getJSONObject(i));
                        }
                        success_callback.call(versesList);
                    } catch (JSONException e) {
                        // Something went wrong while parsing the verses JSONArray
                        error_callback.call("response error");
                    }
                }
            }, error_callback);
        } else {
            error_callback.call("authorization error");
        }
    }

    /**
     * Check whether the given verse is due for review.
     * @param verse a JSONObject representing a user's verse (see Memverse API for proper format)
     * @return true if the verse's next test date is today or before today
     * @throws JSONException the verse JSONObject was not correctly formatted
     * @throws ParseException something went wrong with the date parser
     */
    public boolean isVerseDue(JSONObject verse) throws JSONException, ParseException {
        // Check whether the verse's next test date is today or before today
        return dateFormat.parse(verse.getString("next_test")).compareTo(Calendar.getInstance()
                .getTime()) <= 0;
    }

    /**
     * Check whether the given verse is pending or not.
     * @param verse a JSONObject representing a user's verse (see Memverse API for proper format)
     * @return true if the verse's status is pending, and false otherwise
     * @throws JSONException the verse JSONObject was not correctly formatted
     */
    public boolean isVersePending(JSONObject verse) throws JSONException {
        return verse.getString("status").equals("Pending");
    }

    /**
     * Return out all verses that are due for review from the given verses list.
     * @param verses an ArrayList of verses from which all the verses due for review will be
     *               extracted (see Memverse API for the format of this array)
     * @return all the verses form the given verses array that are due for review
     */
    public List<JSONObject> getDueMemoryVerses(List<JSONObject> verses) throws JSONException,
            ParseException {
        // This array will hold a list of verses that are due
        List<JSONObject> due_verses = new ArrayList<>();
        if (verses.size() > 0) {
            for (int i = 0; i < verses.size(); i++) {
                JSONObject verse_data = verses.get(i);
                // Don't count this verse as due for review if it's status is pending
                if (verse_data.getString("status").equals("Pending")) continue;
                // Check whether the verse is due for review
                if (isVerseDue(verse_data)) {
                    due_verses.add(verse_data);
                }
            }
        }
        return due_verses;
    }

    /**
     * Sort a list of verses by their next test date.
     * @param verses A list of verses (see Memverse API for the format of each verse JSONObject)
     */
    public void sortVersesByDate(List<JSONObject> verses) {
        Collections.sort(verses, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject obj1, JSONObject obj2) {
                try {
                    return dateFormat.parse(obj1.getString("next_test"))
                            .compareTo(dateFormat.parse(obj2.getString("next_test")));
                } catch (JSONException e) {
                    return 0;
                } catch (ParseException e) {
                    return 0;
                }
            }
        });
    }

    /**
     * Attempt to add the given verse to the current user's list of memory verses and pass the verse
     * to the success_callback upon successful. If an error occurs, pass the error type to the
     * error_callback.
     * @param verse a JSONObject containing the verse to be added
     * @param success_callback the callback to call if the addition is successful.
     * @param error_callback the callback to which to pass the error type if there is an error
     */
    public void addVerse(JSONObject verse, MemverseCallback<JSONObject> success_callback,
                         MemverseCallback<String> error_callback) {
        if(logged_in) {
            try {
                webInterface.addVerse(Integer.toString(verse.getInt("id")), success_callback,
                        error_callback);
            } catch (JSONException e) {
                error_callback.call("input error");
            }
        } else {
            error_callback.call("authentication error");
        }
    }

    /**
     * Attempt to delete the given verse to the current user's list of memory verses and pass
     * "Success" to the success_callback upon successful. If an error occurs, pass the error type
     * to the error_callback.
     * @param verse a JSONObject containing the verse to be added
     * @param success_callback the callback to call if the addition is successful.
     * @param error_callback the callback to which to pass the error type if there is an error
     */
    public void deleteVerse(JSONObject verse, MemverseCallback<String> success_callback,
                         MemverseCallback<String> error_callback) {
        try {
            webInterface.deleteVerse(Integer.toString(verse.getInt("id")), success_callback,
                    error_callback);
        } catch (JSONException e) {
            error_callback.call("input error");
        }
    }

    /**
     * Attempt to record a rating for a verse.
     * @param verse a JSONObject containing a field called "id" pointing to an integer representing
     *              the id of the verse to be rated (the JSONObjects returned by the Memverse API
     *              satisfy this requirement).
     * @param rating a number between 1 and 5 (inclusive) representing the rating to be recorded
     * @param success_callback the callback to be called if the rating task was successful and to
     *                         which to pass true if the verse has been memorized and false if it
     *                         hasn't
     * @param error_callback the callback to which will be passed the error type if an error occurs
     *                       (see MemverseWebInterface class description for a list of possible
     *                       error types)
     */
    public void rateVerse(JSONObject verse, int rating, MemverseCallback<Boolean> success_callback,
                          MemverseCallback<String> error_callback) {
        try {
            webInterface.rateVerse(((Integer) verse.getInt("id")).toString(),
                    ((Integer) rating).toString(), success_callback, error_callback);
        } catch (JSONException e) {
            // verse JSONObject does not have the required id field
            error_callback.call("input error");
        }
    }

    /**
     * Get the current user's username and pass it to success_callback. If an error occurs, pass the
     * error type to error_callback instead.
     * @param success_callback the callback to which to pass the current user's username
     * @param error_callback the callback to which to pass the error type if an error occurs
     *                       (see MemverseWebInterface class description for a list of possible
     *                       error types)
     */
    public void getUsername(final MemverseCallback<String> success_callback,
                            final MemverseCallback<String> error_callback) {
        if(logged_in) {
            webInterface.getCurentUser(new MemverseCallback<JSONObject>() {
                @Override
                public void call(JSONObject response) {
                    try {
                        success_callback.call(response.getString("name"));
                    } catch (JSONException e) {
                        error_callback.call("response error");
                    }
                }
            }, error_callback);
        } else {
            error_callback.call("authorization error");
        }
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
