package com.memverse.www.memverse.MemverseInterface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Joshua on 12/20/17.
 * This is a class for interacting with the Memverse API. It is used by MemverseInterface.Memverse.
 *
 * Error types:
 *  input error -- a parameter passed to a function was invalid
 *  authorization error -- user must log in to perform the attempted action
 *  response error -- the response from a server is malformed/unexpected
 *  network error --  something went wrong while attempting to connect to a server
 */

class MemverseWebInterface {

    // The client ID used for the Memverse API
    private static final String APPLICATION_ID = "68e537b8bbcb0e314fea4ed90387d6753c08b9cee42a61c0fa7d92bb92a31131";

    // Authentication token (received after log in and used to authenticate API requests)
    private static String auth_token="";

    /**
     * Attempt to get an authentication token from the Memverse API. Call success_callback if the
     * authentication was successful. Otherwise, pass the error type to error_callback (see class
     * description for a list of possible error types).
     * @param input_email the email address of the user who is trying to log in
     * @param input_password the password of the user who is trying to log in
     * @param success_callback a callback to which will be passed true if the log in was successful
     *                         or false if the connection was successful but the password and/or
     *                         username were incorrect
     * @param error_callback a callback to which will be passed the error type if an error (other
     *                       than a username/password error) occurred (see class description for
     *                       a list of possible error types)
     */
    void authenticate(String input_email, String input_password,
                      final MemverseCallback<Boolean> success_callback,
                      final MemverseCallback<String> error_callback) {
        // These are the parameters that will be passed to the authentication API
        Map<String, String> post_data = new HashMap<>();
        // This log in will use the user's username and password
        post_data.put("grant_type", "password");
        // This gives the app complete access to the user's account
        post_data.put("scope", "admin");
        // The user's username
        post_data.put("username", input_email);
        // The user's password
        post_data.put("password", input_password);
        // The app's client id number
        post_data.put("client_id", APPLICATION_ID);

        // Attempt to log in
        api_call("oauth/token", post_data, "POST", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    auth_token=response.getString("access_token");
                    success_callback.call(true); // authentication was successful
                } catch (JSONException e1) {
                    // The response doesn't contain an access token (possibly because of an error)
                    try {
                        // Attempt to get the error message
                        if (response.getString("error").equals("Invalid email or " +
                                "password.")) {
                            // The username and/or password given by the user is incorrect
                            success_callback.call(false);
                        } else {
                            // Some other error occurred.
                            error_callback.call("response error");
                        }
                    } catch (JSONException e2) {
                        // The response from the server is malformed/unexpected
                        error_callback.call("response error");
                    }
                }
            }
        }, error_callback);
    }

    /**
     * Set the auth_token to "" (this should be called on log out).
     */
    void resetAuthToken() {
        auth_token="";
    }

    /**
     * Attempt to pass a list of the all the current user's memory verses to success_callback sorted
     * in the order specified by sort_order. If an error occurs, pass the error type to
     * error_callback instead.
     * @param sort_order the order in which to sort the verses (see Memverse API documentation
     *                   for possible values of this parameter).
     * @param success_callback the callback to which to pass the user's memory verses
     * @param error_callback the callback to which to pass the error type if there is an error (see
     *                       class description for possible error types).
     */
    void getMemoryVerses(String sort_order, final MemverseCallback<JSONArray> success_callback,
                         final MemverseCallback<String> error_callback) {
        // Since the Memverse API only returns a maximum of 100 verses at a time, call the recursive
        // version of getMemoryVerses starting with the first 100 verses (page_num=1)
        getMemoryVerses(sort_order, success_callback, error_callback, new JSONArray(), 1);
    }

    /**
     * Recursively retrieve each page of memory verses and pass them all to success_callback on
     * completion. If an error occurs, pass the error type to error_callback instead. (This function
     * is used by getMemoryVerses(String, MemverseCallback, MemverseCallback) above.)
     * @param sort_order the order in which to sort the verses
     * @param success_callback the callback to which to pass the user's memory verses
     * @param error_callback the callback to which to pass the error type if there is an error
     * @param prev_verses a JSONArray of all the memory verses that have been retrieved in previous
     *                    iterations
     * @param page_num the page number for the current iteration (each page contains 100 verses max)
     */
    private void getMemoryVerses(final String sort_order,
                                 final MemverseCallback<JSONArray> success_callback,
                                 final MemverseCallback<String> error_callback,
                                 final JSONArray prev_verses, final Integer page_num) {
        // These are the parameters that will be passed to the API
        Map<String, String> data = new HashMap<>();
        // Memverse sorts verses canonically (by verse reference) by default, so only specify the
        // sort order if some other order is requested (an error will occur if you set the "sort"
        // field to "ref")
        if(!sort_order.equals("ref")) data.put("sort", sort_order);
        data.put("page", page_num.toString());

        // Attempt to retrieve verses
        api_call("1/memverses", data, "GET", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    JSONArray new_verses = response.getJSONArray("response");
                    // Add newly retrieved verses to the list of previously retrieved verses
                    for (int i = 0; i < new_verses.length(); i++) {
                        prev_verses.put(new_verses.get(i));
                    }
                    // If count==100, there may still be more verses to retrieve
                    if (response.getInt("count")==100) {
                        getMemoryVerses(sort_order, success_callback, error_callback, prev_verses,
                                page_num+1);
                    } else {
                        // All the verses have been retrieved and added to the prev_verses array.
                        // Send them to the callback
                        success_callback.call(prev_verses);
                    }
                } catch (JSONException e) {
                    // The response was not correctly formatted, possibly due to an
                    // authentication error
                    error_callback.call("response error");
                }
            }
        }, error_callback);
    }

    /**
     * Attempt to retrieve a verse from the Bible.
     * @param book the full name of the book of the Bible containing the verse (lowercase or
     *             capitalized)
     * @param chap the chapter containing the verse
     * @param verse the verse number of the verse
     * @param success_callback the callback to which to pass the verses if it successfully retrieved
     * @param error_callback the callback to which to pass the error type if there is an error
     */
    public void getVerse(String book, String chap, String verse, String translation,
                         final MemverseCallback<JSONObject> success_callback,
                         final MemverseCallback<String> error_callback) {
        // These are the parameters that will be passed to the API
        Map<String, String> params = new HashMap<>();
        params.put("tl", translation);
        params.put("bk", book);
        params.put("ch", chap);
        params.put("vs", verse);

        // Attempt to retrieve the verse
        api_call("1/verses/lookup", params, "GET", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    success_callback.call(response.getJSONObject("response"));
                } catch (JSONException e) {
                    // The response was not correctly formatted
                    error_callback.call("response error");
                }
            }
        }, error_callback);
    }

    /**
     * Attempt to retrieve all the verses from a chapter of the Bible.
     * @param book the full name of the book of the Bible containing the chapter (lowercase or
     *             capitalized)
     * @param chap the number of the chapter to retrieve
     * @param translation the three-letter abbreviation of the name of the translation to use
     * @param success_callback the callback to which to pass the verses upon success
     * @param error_callback the callback to which to pass the error type if there is an error
     */
    void getChapter(final String book, final String chap, final String translation,
                    final MemverseCallback<JSONArray> success_callback,
                    final MemverseCallback<String> error_callback) {
        // Since the Memverse API only returns a maximum of 15 verses from a chapter at a time, call
        // the recursive version of getChapter starting with the first 15 verses (page_num=1)
        getChapter(book, chap, translation, success_callback, error_callback, new JSONArray(), 1);
    }

    /**
     * Recursively retrieve all the verses from a chapter of the Bible.
     * @param book the full name of the book of the Bible containing the chapter (lowercase or
     *             capitalized)
     * @param chap the number of the chapter to retrieve
     * @param translation the three-letter abbreviation of the name of the translation to use
     * @param success_callback the callback to which to pass the verses upon success
     * @param error_callback the callback to which to pass the error type if there is an error
     * @param prev_verses verses previously retrieved by earlier recursive calls of this function
     * @param page_num the page number (there are fifteen verses per page; start with page 1 and the
     *                 function will recursively retrieve all subsequent pages)
     */
    private void getChapter(final String book, final String chap, final String translation,
                            final MemverseCallback<JSONArray> success_callback,
                            final MemverseCallback<String> error_callback,
                            final JSONArray prev_verses, final Integer page_num) {
        // These are the parameters that will be passed to the API
        Map<String, String> params = new HashMap<>();
        params.put("tl", translation);
        params.put("bk", book);
        params.put("ch", chap);
        params.put("page", page_num.toString());

        // Attempt to retrieve verses
        api_call("1/verses/chapter", params, "GET", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    JSONArray new_verses = response.getJSONArray("response");
                    // Add newly retrieved verses to the list of previously retrieved verses
                    for (int i = 0; i < new_verses.length(); i++) {
                        prev_verses.put(new_verses.get(i));
                    }
                    // There may still be more pages of verses to retrieve
                    JSONObject pagination=response.getJSONObject("pagination");
                    if (pagination.getInt("current")<pagination.getInt("pages")) {
                        getChapter(book, chap, translation, success_callback, error_callback,
                                prev_verses, page_num+1);
                    } else {
                        // All the verses have been retrieved and added to the prev_verses array.
                        // Send them to the callback
                        success_callback.call(prev_verses);
                    }
                } catch (JSONException e) {
                    // The response was not correctly formatted, possibly due to an
                    // authentication error
                    error_callback.call("response error");
                }
            }
        }, error_callback);
    }

    /**
     * Attempt to add a new memory verse to the current user's list of verses to review.
     * @param verse_id the id number of the verse (see Memverse API)
     * @param success_callback the callback to call if the addition is successful.
     * @param error_callback the callback to which to pass the error type if there is an error
     */
    public void addVerse(String verse_id, final MemverseCallback<JSONObject> success_callback,
                         final MemverseCallback<String> error_callback) {
        // These are the parameters that will be passed to the API
        Map<String, String> params = new HashMap<>();
        params.put("id", verse_id);
        // Attempt to add the verse
        api_call("1/memverses", params, "POST", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    success_callback.call(response.getJSONObject("response"));
                } catch (JSONException e) {
                    // The response was not correctly formatted (maybe the verse was already added,
                    // or maybe the verse id was incorrect)
                    error_callback.call("response error");
                }
            }
        }, error_callback);
    }

    /**
     * Attempt to delete a memory verse from the current user's list of verses to review.
     * @param verse_id the id number of the verse (see Memverse API, must be the id associated with
     *                 the verse in the user's list of memory verses)
     * @param success_callback the callback to call if the deletion is successful.
     * @param error_callback the callback to which to pass the error type if there is an error
     */
    public void deleteVerse(String verse_id, final MemverseCallback<String> success_callback,
                         final MemverseCallback<String> error_callback) {
        // Attempt to add the verse
        api_call("1/memverses/"+verse_id, new HashMap<String, String>(), "DELETE",
                new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    response.getJSONObject("error");
                    //Some kind of error occurred
                    error_callback.call("response error");
                } catch (JSONException e) {
                    success_callback.call("Success");
                }
            }
        }, error_callback);
    }

    /**
     * Attempt to record a rating for a verse.
     * @param verse_id the Memverse id of the verse to be rated
     * @param rating a number between 1 and 5 (inclusive) representing the rating to be recorded
     * @param success_callback the callback to be called upon success and to which to pass true if
     *                         the verse has been memorized and false if it hasn't
     * @param error_callback the callback to which to pass the error type if an error occurs (see
     *                       the class description for a list of possible error types)
     */
    void rateVerse(String verse_id, String rating,
                   final MemverseCallback<Boolean> success_callback,
                   final MemverseCallback<String> error_callback) {
        // These are the parameters that will be passed to the API
        Map<String, String> data = new HashMap<>();
        data.put("id", verse_id);
        data.put("q", rating);

        api_call("1/memverses/" + verse_id, data, "PUT", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    success_callback.call(response.getJSONObject("response")
                            .getString("status").equals("Memorized"));
                } catch (JSONException e) {
                    // The response was malformed/unexpected (maybe the verse id was incorrect)
                    error_callback.call("response error");
                }
            }
        }, error_callback);
    }

    /**
     * Return information about the current user in a JSONObject (see the Memverse API for the form
     * of this JSONObject).
     * @param success_callback the callback to which to pass a JSONObject with information about the
     *                         current user on success
     * @param error_callback the callback to which to pass the error type if an error occurs (see
     *                       the class description for a list of possible error types)
     */
    void getCurentUser(final MemverseCallback<JSONObject> success_callback,
                       final MemverseCallback<String> error_callback) {
        api_call("1/me/", new HashMap<String, String>(), "GET", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject response) {
                try {
                    success_callback.call(response.getJSONObject("response"));
                } catch (JSONException e) {
                    // The response was malformed/unexpected (maybe no user is logged in)
                    error_callback.call("response error");
                }
            }
        }, error_callback);
    }

    /**
     * Attempt to load the user's profile picture from Gravatar and pass it to the callback.
     * Otherwise, pass null to the callback.
     * @param email the user's email address
     * @param callback the callback to which a Bitmap containing the user's profile picture will be
     *                 passed (null will be passed to this callback if there is an error)
     */
    void getGravatar(String email, final MemverseCallback<Bitmap> callback) {
        // This is the address at which the user's profile picture should be found. The d=wavatar
        // parameter makes Gravatar return a random image for users who do not have Gravatar
        // accounts.
        String url = "https://www.gravatar.com/avatar/"+md5Hash(email)+"?d=wavatar";

        // Attempt to load the image and pass it to the callback
        (new LoadImageTask (url, callback)).execute((Void) null);
    }

    /**
     * Make a call to the Memverse API and pass the response to the callback function in the form
     * of a JSON object. If an error occurs, pass the error type to error_callback instead (see the
     * class description for a list of possible error types).
     * @param api specifies which part of the API to call (i.e. "oauth/token", "user", "verses/lookup")
     * @param params a list a parameters to pass to the API
     * @param method the method to use when calling the API ("GET", "POST", "DELETE", or "PUT")
     * @param success_callback the callback to which a JSONObject representing the API's response
     *                         will be passed if the API call is successful
     * @param error_callback the callback to which the error type will be passed if an error occurs
     */
    private void api_call(String api, Map<String, String> params, String method,
                          final MemverseCallback<JSONObject> success_callback,
                          final MemverseCallback<String> error_callback) {
        (new LoadJSONTask("https://www.memverse.com/"+api, params, method, success_callback,
                error_callback)).execute((Void) null);
    }

    /**
     * @return a String representing the MD5 hash of the input (used by getGravatar)
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            // Convert hash to a string
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * A thread for loading JSON data from the internet
     */
    private static class LoadJSONTask extends AsyncTask<Void, Void, JSONObject> {

        // The URL of the server to connect to
        private String url_string;
        // The parameters to pass to the server
        private Map<String, String> params;
        // The method to use when connecting ("GET", "POST", "PUT", etc.)
        private String method;
        // The callback to notify of the server's response if connection is successful
        private MemverseCallback<JSONObject> success_callback;
        // The callback to which to pass the error type if an error occurs (see the class
        // description for a list of possible error types)
        private MemverseCallback<String> error_callback;
        // Stores an error type when needed
        private String error_type;

        /**
         * Initialize the parameters that will be used to make the request
         * @param input_url the URL of the server to connect to
         * @param input_params the parameters to pass to the server
         * @param input_method the method to use when connecting ("GET", "POST", "PUT", etc.)
         * @param input_success_callback the callback to notify of the server's response
         */
        LoadJSONTask(String input_url, Map<String, String> input_params, String input_method,
                     MemverseCallback<JSONObject> input_success_callback,
                     MemverseCallback<String> input_error_callback) {
            url_string = input_url;
            params = input_params;
            method = input_method;
            success_callback = input_success_callback;
            error_callback = input_error_callback;
        }

        /**
         * Send the request to the server and retrieve the response. If an error occurs, return null.
         * @param ignored No parameters are used
         * @return the server's response or null if an error occurs
         */
        @Override
        protected JSONObject doInBackground(Void... ignored) {
            try {
                URL url;
                // When making a POST or PUT request, the parameters will be sent later
                if (params.isEmpty() || method.equals("POST") || method.equals("PUT"))
                    url = new URL(url_string);
                // When making a GET request, attach the parameters to the URL
                else
                    url = new URL(url_string+"?"+formatAPIParams(params));
                // Open a connection to the server
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                // Attempt to send any parameters and retrieve a response
                try {
                    // Set the request method (GET, POST, PUT, or DELETE)
                    urlConnection.setRequestMethod(method);

                    // Tell the server to respond with JSON (www.memverse.com will sometimes return
                    // HTML for certain API request if this is not specified)
                    urlConnection.setRequestProperty("Accept", "application/json");

                    // If we have an authorization token, send it in the header
                    if (!auth_token.equals("") && url.getHost().equals("www.memverse.com")) {
                        urlConnection.setRequestProperty("Authorization", "Bearer "+auth_token);
                    }

                    // Send parameters for POST and PUT requests
                    if (!params.isEmpty() && (method.equals("POST") || method.equals("PUT"))) {
                        // Allow output so that parameters can be sent
                        urlConnection.setDoOutput(true);
                        urlConnection.setChunkedStreamingMode(0);

                        // Create an output stream and send the parameters
                        BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                        out.write(formatAPIParams(params).getBytes());
                        out.flush();
                        out.close();
                    }

                    // Retrieves response string
                    BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
                    String response_string = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();
                    in.close();

                    // Convert the response string to a JSONObject and return it
                    return new JSONObject(response_string);

                } catch (JSONException e) {
                    // The response string could not be converted to a JSONObject
                    if(method.toUpperCase().equals("DELETE")) {
                        //If the DELETE method was used, there should be no response. Return an
                        // empty JSONObject
                        return new JSONObject();
                    }
                    error_type="response error";
                } catch (ProtocolException e) {
                    // Something went wrong with setRequestMethod
                    error_type="network error";
                } catch (IOException e1) {
                    // Something went wrong with the output or input stream. Attempt to retrieve the
                    // error stream
                    try {
                        BufferedInputStream err_in =
                                new BufferedInputStream(urlConnection.getErrorStream());
                        java.util.Scanner err_scanner =
                                new java.util.Scanner(err_in, "UTF-8").useDelimiter("\\A");
                        String error_string = err_scanner.hasNext() ? err_scanner.next() : "";
                        err_scanner.close();
                        err_in.close();

                        // Convert the response string to a JSONObject and return it
                        return new JSONObject(error_string);
                    } catch (IOException e2) {
                        // Could not read error stream
                        error_type="network error";
                    } catch (JSONException e2) {
                        // Could not convert the error stream to JSON
                        error_type="network error";
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                // The url was malformed
                error_type="input error";
            } catch (IOException e) {
                // Something went wrong with url.openConnection
                error_type="network error";
            }
            // null will only be returned if an error occurred
            return null;
        }

        /**
         * Pass the server's response to the success callback unless an error occurred. If an error
         * occurs, pass the error type to the error callback (see class description for a list of
         * error types).
         * @param response the server's response (or null if an error occurred)
         */
        @Override
        protected void onPostExecute(final JSONObject response) {
            if(response!=null) success_callback.call(response);
            else error_callback.call(error_type);
        }

        /**
         * Convert a map of key-value pairs to a string for use in GET, POST, PUT, and DELETE
         * requests (e.g {"a": "1", "b": "2"} -> "a=1&b=2"). If there is an error, return an empty
         * String.
         * @param params Map with paramater names as keys pointing to each paramater's corresponding
         *               value
         * @return a string properly formatted for GET, POST, PUT, and DELETE requests
         */
        private String formatAPIParams(Map<String, String> params) {
            StringBuilder paramsString = new StringBuilder();
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    paramsString.append("&"+URLEncoder.encode(entry.getKey(), "UTF-8")+"="+
                            URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
                // Remove initial "&"
                paramsString.deleteCharAt(0);
                return paramsString.toString();
            } catch (UnsupportedEncodingException e) {
                // Something went wrong with URLEncoder
                return "";
            }
        }
    }

    /**
     * A thread for loading image data from the internet
     */
    public static class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {

        private String url="";
        private Bitmap image;
        MemverseCallback<Bitmap> callback;

        /**
         * Initialize the parameters that will be used to make the request
         * @param input_url the url at which the image is located
         * @param input_callback the callback to which to pass the image when it is loaded (or null
         *                       if an error occurs)
         */
        LoadImageTask(String input_url, MemverseCallback<Bitmap> input_callback) {
            url = input_url;
            callback = input_callback;
        }

        /**
         * Send the request to the server and return the image. If an error occurs, return null
         * @param ignored No parameters are used
         * @return the requested image or null if an error occurs
         */
        @Override
        protected Bitmap doInBackground(Void... ignored) {
            try {
                InputStream image_stream = (InputStream) new URL(url).getContent();
                image = BitmapFactory.decodeStream(image_stream);
                image_stream.close();
                return image;
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Pass the image (or null if an error occurred) to the callback.
         * @param image the image (or null if an error occurred)
         */
        @Override
        protected void onPostExecute(final Bitmap image) {
            callback.call(image);
        }
    }
}
