package com.memverse.www.memverse.MemverseInterface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
 */

class MemverseWebInterface {

    // The client ID used for the Memverse API
    private static final String APPLICATION_ID = "68e537b8bbcb0e314fea4ed90387d6753c08b9cee42a61c0fa7d92bb92a31131";

    // Authentication token (received after log in and used to authenticate API requests)
    private static String auth_token="";

    /**
     * Attempt to get an authentication token from the Memverse API. Pass true to the callback if
     * the log in is successful and false otherwise.
     * @param input_email the email address of the user who is trying to log in
     * @param input_password the password of the user who is trying to log in
     * @param callback a callback to which will be passed true if the log in is successful and
     *                 false otherwise
     */
    void authenticate(String input_email, String input_password,
                             final MemverseCallback<Boolean> callback) {
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
                if (response.has("access_token")) {
                    try {
                        auth_token=response.getString("access_token");
                        callback.call(true);
                    } catch (JSONException e) {
                        // The response is incorrectly formatted
                        callback.call(false);
                    }
                }
                // Log in was unsuccessful
                else callback.call(false);
            }
        });
    }

    void resetAuthToken() {
        auth_token="";
    }

    void getMemoryVerses(final MemverseCallback<JSONArray> callback, String sort_order) {
        getMemoryVerses(callback, new JSONArray(), sort_order, 1);
    }

    void getMemoryVerses(final MemverseCallback<JSONArray> callback,
                                 final JSONArray prev_verses, final String sort_order,
                                 final Integer page_num) {
        // These are the parameters that will be passed to the authentication API
        Map<String, String> data = new HashMap<>();
        if(!sort_order.equals("verse")) data.put("sort", sort_order);
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
                    // If count==100, there are still more verses to retrieve
                    if (response.getInt("count")==100) {
                        getMemoryVerses(callback, prev_verses, sort_order, page_num+1);
                    } else {
                        callback.call(prev_verses);
                    }
                } catch (JSONException e) {
                    // The response was not correctly formatted, possibly due to an
                    // authentication error
                    callback.call(null);
                }
            }
        });
    }

    void rateVerse(String verse_id, String rating) {
        Map<String, String> data = new HashMap<>();
        data.put("id", verse_id);
        data.put("q", rating);

        api_call("1/memverses/" + verse_id, data, "PUT", new MemverseCallback<JSONObject>() {
            @Override
            public void call(JSONObject input) {
                //TODO: Handle errors
            }
        });
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
     * of a JSON object
     * @param api specifies which part of the API to call (i.e. "oauth/token", "user", "verses/lookup")
     * @param params a list a parameters to pass to the API
     * @param method the method to use when calling the API ("GET", "POST", "DELETE", or "PUT")
     * @param callback the callback to which a JSONObject representing the API's response will be
     *                 passed (an empty JSON object will be passed to this callback if there is an
     *                 error)
     */
    private void api_call(String api, Map<String, String> params, String method,
                          final MemverseCallback<JSONObject> callback) {
        (new LoadJSONTask("https://www.memverse.com/"+api, params, method,
                callback)).execute((Void) null);
    }

    /**
     * Return a String representing the MD5 hash of the input (used by getGravatar)
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
        // The callback to notify of the server's response
        private MemverseCallback<JSONObject> callback;

        /**
         * Initialize the parameters that will be used to make the request
         * @param input_url the URL of the server to connect to
         * @param input_params the parameters to pass to the server
         * @param input_method the method to use when connecting ("GET", "POST", "PUT", etc.)
         * @param input_callback the callback to notify of the server's response
         */
        LoadJSONTask(String input_url, Map<String, String> input_params, String input_method,
                        MemverseCallback<JSONObject> input_callback) {
            url_string = input_url;
            params = input_params;
            method = input_method;
            callback = input_callback;
        }

        /**
         * Send the request to the server and retrieve the response
         * @param ignored No parameters are used
         * @return
         */
        @Override
        protected JSONObject doInBackground(Void... ignored) {
            try {
                URL url;
                if (method.equals("POST") || method.equals("PUT") || params.isEmpty())
                    url = new URL(url_string);
                else
                    url = new URL(url_string+"?"+formatAPIParams(params));
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                try {
                    urlConnection.setRequestMethod(method);

                    // Send authorization token if given
                    if (!auth_token.equals("")) {
                        urlConnection.setRequestProperty("Authorization", "Bearer "+auth_token);
                    }

                    // Send any parameters
                    if (!params.isEmpty() && (method.equals("POST") || method.equals("PUT"))) {
                        // Allow output so that parameters can be sent
                        urlConnection.setDoOutput(true);
                        urlConnection.setChunkedStreamingMode(0);

                        BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                        out.write(formatAPIParams(params).getBytes());
                        out.flush();
                        out.close();
                    }

                    // Retrieves response
                    BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
                    String response_string = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();
                    in.close();

                    return new JSONObject(response_string);

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    // Something went wrong with setRequestMethod
                    e.printStackTrace();
                } catch (IOException e) {
                    // Something went wrong with the BufferedOutputStream
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                // Something went wrong with the openConnection or URL creation
                e.printStackTrace();
            }
            return new JSONObject();
        }

        @Override
        protected void onPostExecute(final JSONObject response) {
            callback.call(response);
        }

        @Override
        protected void onCancelled() {
            callback.call(new JSONObject());
        }

        /**
         * Convert a map of key-value pairs to a string for use in POST and GET requests (e.g
         * {"a": "1", "b": "2"} -> "a=1&b=2"). If there is an error, return an empty String.
         * @param params Map with variable names as keys pointing to each variable's corresponding value
         * @return a string properly formatted for POST and GET requests
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
                e.printStackTrace();
                return "";
            }
        }
    }

    /**
     * A thread for loading image data from the internet
     */
    public class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {

        private String url="";
        private Bitmap image;
        MemverseCallback<Bitmap> callback;

        LoadImageTask(String input_url, MemverseCallback<Bitmap> input_callback) {
            url = input_url;
            callback = input_callback;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                InputStream image_stream = (InputStream) new URL(url).getContent();
                image = BitmapFactory.decodeStream(image_stream);
                image_stream.close();
                return image;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Bitmap image) {
            callback.call(image);
        }

        @Override
        protected void onCancelled() {
            callback.call(null);
        }
    }
}
