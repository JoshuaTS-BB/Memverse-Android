package com.memverse.www.memverse;

import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Allow users to see a list of all their memory verses
 */
public class ViewVersesActivity extends NavigationActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_view_verses);

        // Retrieve a list of all the user's memory verses
        memverse.getMemoryVerses(new MemverseCallback<List<JSONObject>>() {
            @Override
            public void call(List<JSONObject> input) {
                showVerses(input);
                // Make sure the progress spinner is no longer displayed now that the verses are
                // loaded
                showProgress(false);
            }
        }, new MemverseCallback<String>() {
            @Override
            public void call(String error_type) {
                // Hide the progress spinner
                showProgress(false);
                // Display an error message
                switch (error_type) {
                    case "network error":
                        showErrorMsg(getString(R.string.error_network));
                        break;
                    case "authorization error":
                        showErrorMsg(getString(R.string.error_authorization));
                        break;
                    default:
                        showErrorMsg(getString(R.string.error_generic));
                        break;
                }
            }
        });
    }

    /**
     * Display the given list of verses (see the Memverse API for the format of the JSONObject for
     * an individual verse)
     * @param verses the list of verses to be displayed
     */
    private void showVerses(List<JSONObject> verses) {
        try {
            if (verses.size() > 0) {
                TableLayout table = findViewById(R.id.view_verses_verseTableLayout);
                for (int i = 0; i < verses.size(); i++) {
                    // verse_data contains information about the verse specific to the user (e.g.
                    // whether it's memorized or not)
                    JSONObject verse_data = verses.get(i);
                    // verse contains, among other things, the text and reference of the verse
                    JSONObject verse = verse_data.getJSONObject("verse");

                    TableRow row = new TableRow(this);

                    TextView reference = new TextView(this);
                    reference.setText(verse.getString("book") + " " +
                            ((Integer) verse.getInt("chapter")).toString() + ":" +
                            ((Integer) verse.getInt("versenum")).toString());
                    row.addView(reference);

                    TextView status = new TextView(this);
                    status.setText(verse_data.getString("status"));
                    row.addView(status);

                    TextView next_test = new TextView(this);
                    next_test.setText(verse_data.getString("next_test"));
                    row.addView(next_test);

                    TextView interval = new TextView(this);
                    interval.setText(verse_data.getString("test_interval"));
                    row.addView(interval);

                    table.addView(row);
                }
            }
        } catch (JSONException e) {
            // The JSONObject verses wasn't in the correct format
            showErrorMsg(getString(R.string.error_generic));
        }
    }
}
