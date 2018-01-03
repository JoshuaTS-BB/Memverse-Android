package com.memverse.www.memverse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ViewVersesActivity extends NavigationActivity {
    private static final String TAG = ViewVersesActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_view_verses);

        memverse.getMemoryVerses(new MemverseCallback<JSONArray>() {
            @Override
            public void call(JSONArray input) {
                showVerses(input);
                showProgress(false);
            }
        });
    }

    private void showVerses(JSONArray verses) {
        if (verses!=null) {
            try {
                if(verses.length()>0) {
                    TableLayout table = (TableLayout) findViewById(R.id.view_verses_table);
                    for (int i = 0; i < verses.length(); i++) {
                        JSONObject verse_data = verses.getJSONObject(i);
                        JSONObject verse = verse_data.getJSONObject("verse");

                        TableRow row = new TableRow(this);

                        TextView reference = new TextView(this);
                        reference.setText(verse.getString("book")+" "+
                                ((Integer) verse.getInt("chapter")).toString()+":"+
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
                Log.d(TAG, "FAILED JSON");
                e.printStackTrace(); // The JSONObject verses wasn't in the correct format
            }
        }
    }
}
