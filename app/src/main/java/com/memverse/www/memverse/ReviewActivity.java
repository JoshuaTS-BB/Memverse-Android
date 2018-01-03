package com.memverse.www.memverse;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReviewActivity extends NavigationActivity {
    JSONArray verses;
    int current_verse_index;
    JSONObject current_verse;
    PopupWindow ratePopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_review);

        current_verse_index=0;

        ratePopup = new PopupWindow(ReviewActivity.this);
        ratePopup.setContentView(getLayoutInflater().inflate(R.layout.rate_verse_popup, null));
        ((Button) ratePopup.getContentView().findViewById(R.id.rate1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memverse.rateVerse(current_verse, 1);
                ratePopup.dismiss();
                try {
                    current_verse_index++;
                    askVerse(verses.getJSONObject(current_verse_index));
                } catch (JSONException e) {
                    e.printStackTrace(); //TODO: Handle errors
                }
            }
        });
        ((Button) ratePopup.getContentView().findViewById(R.id.rate2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memverse.rateVerse(current_verse, 2);
                ratePopup.dismiss();
                try {
                    current_verse_index++;
                    askVerse(verses.getJSONObject(current_verse_index));
                } catch (JSONException e) {
                    e.printStackTrace(); //TODO: Handle errors
                }
            }
        });
        ((Button) ratePopup.getContentView().findViewById(R.id.rate3)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memverse.rateVerse(current_verse, 3);
                ratePopup.dismiss();
                try {
                    current_verse_index++;
                    askVerse(verses.getJSONObject(current_verse_index));
                } catch (JSONException e) {
                    e.printStackTrace(); //TODO: Handle errors
                }
            }
        });
        ((Button) ratePopup.getContentView().findViewById(R.id.rate4)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memverse.rateVerse(current_verse, 4);
                ratePopup.dismiss();
                try {
                    current_verse_index++;
                    askVerse(verses.getJSONObject(current_verse_index));
                } catch (JSONException e) {
                    e.printStackTrace(); //TODO: Handle errors
                }
            }
        });
        ((Button) ratePopup.getContentView().findViewById(R.id.rate5)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                memverse.rateVerse(current_verse, 5);
                ratePopup.dismiss();
                try {
                    current_verse_index++;
                    askVerse(verses.getJSONObject(current_verse_index));
                } catch (JSONException e) {
                    e.printStackTrace(); //TODO: Handle errors
                }
            }
        });

        ((Button) findViewById(R.id.rateButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ratePopup.showAtLocation(findViewById(R.id.main_content), Gravity.TOP, 0, 300);
            }
        });

        memverse.getDueMemoryVerses(new MemverseCallback<JSONArray>() {
            @Override
            public void call(JSONArray input) {
                verses=input;
                try {
                    askVerse(verses.getJSONObject(0));
                } catch (JSONException e) {
                    e.printStackTrace(); //TODO: Show network error message
                }
                ((EditText) findViewById(R.id.verseInput)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        provideFeedback(editable.toString());
                    }
                });
                showProgress(false);
            }
        });
    }

    private void askVerse(JSONObject verse) {
        try {
            current_verse = verse;
            JSONObject v = verse.getJSONObject("verse");
            ((TextView) findViewById(R.id.verseFeedback)).setText("");
            ((TextView) findViewById(R.id.referenceView)).setText(v.getString("book")+" "+
                    ((Integer) v.getInt("chapter")).toString()+":"+
                    ((Integer) v.getInt("versenum")).toString());
            ((TextView) findViewById(R.id.verseFeedback)).setText("");
            ((EditText) findViewById(R.id.verseInput)).setText("");
        } catch (JSONException e) {
            e.printStackTrace(); //TODO: Show network error message
        }
    }

    public void provideFeedback(String input_text) {
        try {
            String actual_text=current_verse.getJSONObject("verse").getString("text");
            String[] input=essentialize(input_text);
            String[] actual=essentialize(actual_text);
            String[] actual_with_punc=actual_text.split(" ");
            boolean correct=true;

            StringBuilder strBuilder = new StringBuilder();
            for (int i=0; i<input.length; i++) {
                try {
                    if (input[i].equals(actual[i])) {
                        strBuilder.append(" ").append(actual_with_punc[i]);
                    }
                    else {
                        strBuilder.append(" ...");
                        correct=false;
                        break;
                    }
                } catch (IndexOutOfBoundsException e) {
                    strBuilder.append(" ...");
                    correct=false;
                    break;
                }
            }
            if (input.length!=actual.length) correct=false;
            strBuilder.deleteCharAt(0); // Remove initial space
            ((TextView) findViewById(R.id.verseFeedback)).setText(strBuilder.toString());
            ((TextView) findViewById(R.id.correctFeedback)).setVisibility(correct ? View.VISIBLE : View.GONE);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(ReviewActivity.class.getSimpleName()+" provideFeedback", "Feedback don't work.");
        }
    }

    private String[] essentialize(String str) {
        StringBuilder strBuilder = new StringBuilder();
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if(Character.isLetter(c) || c==' ')
                strBuilder.append(str.charAt(i));
        }
        return strBuilder.toString().toLowerCase().split(" ");
    }
}
