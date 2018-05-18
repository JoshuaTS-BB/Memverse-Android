package com.memverse.www.memverse;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Allow users to review their memory verses
 */
public class ReviewActivity extends NavigationActivity {
    // An array of all the current user's memory verses (including ones that are not due for review)
    // in canonical order stored in JSONObjects as specified by the Memverse API
    List<JSONObject> all_verses = new ArrayList<>();
    // An array of all the verses the current user has to review for the day (not including verses
    // that have already been reviewed or verses from the passage that the user is currently
    // reviewing)
    List<JSONObject> due_verses = new ArrayList<>();
    // A list of all the verses due for review in the passage that the user is currently reviewing
    List<JSONObject> current_passage = new ArrayList<>();
    // The verse that comes right before the one the user is reviewing (only set if the previous
    // verse if one of the user's memory verses)
    JSONObject previous_verse;
    // The verse that the user is currently reviewing
    JSONObject current_verse;
    // Set to true if live feedback should be displayed and false otherwise
    boolean show_feedback=true;
    // Set to false once the instructions have been hidden
    boolean instructions_visible=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_review);

        /*// Create a popup window with buttons for users to rate their performance on each verse
        ratePopup = new PopupWindow(ReviewActivity.this);
        // The next two lines allow the popup to be dismissed when user touches outside the popup
        // window
        ratePopup.setOutsideTouchable(true);
        ratePopup.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPopupBackground)));
        // Use the rate_verse_popup layout
        ratePopup.setContentView(getLayoutInflater().inflate(R.layout.rate_verse_popup, null));*/
        // Set onclick listeners for the rate buttons
        findViewById(R.id.review_rate1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateCurrentVerse(1);
                findViewById(R.id.review_rateBar).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.review_rate2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateCurrentVerse(2);
                findViewById(R.id.review_rateBar).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.review_rate3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateCurrentVerse(3);
                findViewById(R.id.review_rateBar).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.review_rate4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateCurrentVerse(4);
                findViewById(R.id.review_rateBar).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.review_rate5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateCurrentVerse(5);
                findViewById(R.id.review_rateBar).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.review_rateButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ratePopup.showAtLocation(findViewById(R.id.mainContentLayout), Gravity.TOP, 0, 300);
                LinearLayout rateBar=findViewById(R.id.review_rateBar);
                if(rateBar.getVisibility()==View.VISIBLE) rateBar.setVisibility(View.GONE);
                else rateBar.setVisibility(View.VISIBLE);
            }
        });

        // Show full verse that is currently being reviewed when switch is thrown and hide it
        // otherwise
        ((SwitchCompat) findViewById(R.id.review_showVerseSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            // If the switch was just set to true, show the verse's text in the
                            // feedback view
                            try {
                                JSONObject v = current_verse.getJSONObject("verse");
                                TextView feedbackView = findViewById(R.id.review_verseFeedbackView);
                                feedbackView.setText(((Integer) v.getInt("versenum"))
                                        .toString() + " " + v.getString("text"));
                                feedbackView.setVisibility(View.VISIBLE);
                                // Don't show regular feedback while verse is displayed
                                show_feedback=false;
                            } catch (JSONException e) {
                                // The current verse's text could not be retrieved for some reason
                                showErrorMsg(getString(R.string.error_generic));
                            }
                        } else {
                            // If the switch was just set to false, make sure that regular feedback
                            // is displayed
                            show_feedback=true;
                            provideFeedback(((EditText) findViewById(R.id.review_verseInput)).getText().toString());
                        }
                    }
        });

        // Retrieve a list of the user's memory verses
        memverse.getMemoryVerses(new MemverseCallback<List<JSONObject>>() {
            @Override
            public void call(List<JSONObject> input) {
                // input is a list of the user's memory verses stored in JSONObjects as specified
                // by the Memverse API
                all_verses = input;
                try {
                    // Retrieve the memory verses that are due for review
                    due_verses = memverse.getDueMemoryVerses(input);
                    // Sort verses by the date they are due for review
                    memverse.sortVersesByDate(due_verses);
                    // Ask user to review the next verse in the list
                    askNextVerse();
                    // Listen for input events (like key presses) and provide live feedback
                    ((EditText) findViewById(R.id.review_verseInput)).addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if(show_feedback) provideFeedback(editable.toString());
                            //Hide the instructions if they are currently visible
                            if(instructions_visible) {
                                findViewById(R.id.review_instructions).setVisibility(View.GONE);
                                instructions_visible = false;
                            }
                        }
                    });
                } catch (JSONException e) {
                    // The input JSONArray was not correctly formatted
                    showErrorMsg(getString(R.string.error_generic));
                } catch (ParseException e) {
                    // An error occurred while parsing the dates in the next_test fields
                    showErrorMsg(getString(R.string.error_generic));
                }
                // Stop displaying the progress spinner (displayed by default when activity is loaded)
                showProgress(false);
            }
        }, new MemverseCallback<String>() {
            // This callback is called if an error occurred
            @Override
            public void call(String error_type) {
                // Hide progress spinner
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
     * Ask the user to review the next due verse and set relevant class variables
     */
    private void askNextVerse() {
        previous_verse=current_verse;
        current_verse=null;
        // If there are no more verses left in the current passage, get the next passage (verses are
        // removed from current_passage as they are reviewed)
        if (current_passage.isEmpty()) {
            // If there are no more due verses, the review session is complete (verses are removed
            // from due_verses as they are reviewed)
            if (due_verses.isEmpty()) {
                showReviewCompletedScreen();
                return;
            }
            else getNextPassage();
        }
        // Find the next due verse in the current passage
        for (int i=0; i<current_passage.size(); i++) {
            JSONObject verse = current_passage.get(i);
            try {
                if (memverse.isVerseDue(verse) && !memverse.isVersePending(verse)) {
                    current_verse = verse;
                    // If the first verse in current_passage is due, previous_verse will already be
                    // set either to the last verse that was reviewed if that verse was in the same
                    // passage or to null if a new passage was retrieved by getNextPassage. Note:
                    // verses are removed from current_passage when they are reviewed, so it's
                    // possible that current_passage.get(0) is not actually the first verse in the
                    // passage.
                    if (i==0) askVerse(current_verse, previous_verse);
                    // Otherwise, the previous verse can be found right before the next due verse in
                    // current_passage
                    else {
                        previous_verse = current_passage.get(i-1);
                        askVerse(current_verse, previous_verse);
                    }
                    // Remove all verses in current_passage up to and including the one that will
                    // now be reviewed
                    for (int j=0; j<=i; j++) {
                        current_passage.remove(0);
                    }
                    break;
                }
            } catch (JSONException e) {
                showErrorMsg(getString(R.string.error_generic));
            } catch (ParseException e) {
                // Thrown by memverse.isVerseDue
                showErrorMsg(getString(R.string.error_generic));
            }
        }
        // If current_verse is still null, there must be no more due verses in current_passage.
        // Empty it and try again with the next passage
        if (current_verse==null) {
            current_passage.clear();
            askNextVerse();
        }
    }

    /**
     * Populate current_passage with all the verses from the passage containing the next verse that
     * is due to be reviewed
     */
    private void getNextPassage() {
        current_passage.clear();
        // Reset previous_verse so that askNextVerse doesn't think that a verse from a completely
        // different passage comes before the first verse of the new passage that is going to be
        // retrieved
        previous_verse=null;
        // Get the next due verse (note: verses are removed from due_verses as they are reviewed, so
        // due_verses.get(0) doesn't give the same verse over and over again)
        JSONObject verse = due_verses.get(0);
        try {
            // Verses from the same passage will have the same passage_id
            int passage_id = verse.getInt("passage_id");
            // Loop through the user's verses to find all the verses from the current passage,
            // including ones that are not due for review
            for (int i=0; i<all_verses.size(); i++) {
                JSONObject v = all_verses.get(i);
                if (v.getInt("passage_id")==passage_id) {
                    current_passage.add(v);
                    // If a verse is added to the current passage, make sure it's removed from
                    // due_verses so that the same passage doesn't get reviewed twice
                    for (int j=0; j<due_verses.size(); j++) {
                        if (due_verses.get(j).getInt("id")==v.getInt("id")) {
                            due_verses.remove(j);
                            break;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            showErrorMsg(getString(R.string.error_generic));
        }
    }

    /**
     * Prompt the user to review the given verse and prepare to give feedback
     * @param verse the verse to prompt the user to review (see Memverse API for the format of this
     *              JSONObject)
     * @param prev_verse the verse that comes right before the one that is to be reviewed or null if
     *                   the previous verse is not one of the user's memory verses
     */
    private void askVerse(JSONObject verse, JSONObject prev_verse) {
        try {
            // v is an object containing, among other things, the verse's text and reference (see
            // Memverse API for more information)
            JSONObject v = verse.getJSONObject("verse");
            // Display the verse's reference
            ((TextView) findViewById(R.id.review_referenceView)).setText(memverse.getReference(v));
            // Display the previous verse if given
            TextView prevVerseFeedback = findViewById(R.id.review_prevVerseFeedbackView);
            if (prev_verse!=null) {
                JSONObject pv = prev_verse.getJSONObject("verse");
                prevVerseFeedback.setText(((Integer)pv.getInt("versenum")).toString()+" "+
                        pv.getString("text"));
                prevVerseFeedback.setVisibility(View.VISIBLE);
            } else {
                // Hide the previous verse feedback view if it's not needed so that it doesn't take
                // up extra space
                prevVerseFeedback.setVisibility(View.GONE);
            }
            // Hide the verse feedback view until it's needed so that it doesn't take up extra space
            findViewById(R.id.review_verseFeedbackView).setVisibility(View.GONE);
            // Make sure the "Correct" notification is hidden
            findViewById(R.id.review_correctFeedbackView).setVisibility(View.GONE);
            // Reset the user input
            ((EditText) findViewById(R.id.review_verseInput)).setText("");
        } catch (JSONException e) {
            // The JSONObject verse was not correctly formatted
            showErrorMsg(getString(R.string.error_generic));
        }
    }

    /**
     * Compare the input_text to the actual text of the verse being reviewed (ignoring punctuation
     * and capitalization) and display feedback. Allow users to type only the first letter of each
     * word if they so desire.
     * @param input_text the text inputted by the user (to be compared to the actual text of the
     *                   current verse)
     */
    public void provideFeedback(String input_text) {
        TextView verseFeedbackView = findViewById(R.id.review_verseFeedbackView);
        // Hide the feeback view if it isn't needed at the moment so that it doesn't take up space
        if (input_text.isEmpty()) {
            verseFeedbackView.setVisibility(View.GONE);
            return;
        }

        try {
            // The text of the verse that is currently being reviewed
            String actual_text=current_verse.getJSONObject("verse").getString("text");
            // A list of the words of the input text with punctuation and capitalization removed
            String[] input=essentialize(input_text);
            // A list of the words of the actual text with punctuation and capitalization removed
            String[] actual=essentialize(actual_text);
            // A list of the words of the actual text (including punctuation and capitalization)
            // with an extra space at the beginning to separate the text from the verse number. The
            // regex breaks the string at word boundaries that occur after punctuation/spacing so
            // that punctuation will always be included with the word that comes next and never with
            // the word that comes before.
            String[] actual_with_punc=(" "+actual_text).split("\\b(?=[\\W])");
            // Set to false if the input doesn't match the actual text
            boolean correct=true;

            // Used to build the String that will be displayed as feedback to the user
            StringBuilder feedback = new StringBuilder();
            // Display the verse number at the beginning
            feedback.append(current_verse.getJSONObject("verse").getInt("versenum"));

            // Loop through all the words in the input
            // iact will be used to keep track of the current index in the actual array, whereas
            // i will be used to keep track of the current index in the input array.
            int iact=0;
            if(input.length!=0) {
                for(int i = 0; i < input.length; i++) {
                    try {
                        if(input[i].equals(actual[iact])) {
                            // The input matches the actual text
                            feedback.append(actual_with_punc[iact]);
                        } else {
                            // If two "words" in a row from the input are only single characters,
                            // enter "single character mode" and only require the user to type the
                            // first letter of each word.
                            boolean prev_single=false;
                            boolean next_single=false;
                            if (i>0) prev_single=input[i-1].length()==1;
                            if (i<input.length-1) next_single=input[i+1].length()==1;
                            if(input[i].length()==1 && (prev_single || next_single) &&
                                    input[i].charAt(0)==actual[iact].charAt(0)) {
                                feedback.append(actual_with_punc[iact]);
                            } else {
                                // If the first two letters of the current input word match the
                                // first letters of the next two words in the actual text, enter
                                // "single character mode with no spaces" and only require the user
                                // to type the first letter of each word without putting spaces in
                                // between.
                                if(iact<actual.length-1 && input[i].charAt(0)==actual[iact].charAt(0)
                                        && input[i].charAt(1)==actual[iact+1].charAt(0)) {
                                    for(int j = 0; j < input[i].length(); j++) {
                                        if(input[i].charAt(j)==actual[iact].charAt(0)) {
                                            feedback.append(actual_with_punc[iact]);
                                        } else {
                                            feedback.append(" ...");
                                            correct = false;
                                        }
                                        // Increase iact so that it points to the correct word in
                                        // the actual text
                                        iact++;
                                    }
                                    // Because iact has already been adjusted, this subtraction is
                                    // required so that the adjustment of iact at the end of the
                                    // loop won't throw it off.
                                    iact--;
                                } else {
                                    // The input entered at this point does not match the actual text
                                    feedback.append(" ...");
                                    correct = false;
                                }
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        // There are more words in the input than are in the actual text
                        feedback.append(" ...");
                        correct = false;
                        break;
                    }
                    //Increase iact so that it points to the next word (i is automatically increased
                    // by the loop)
                    iact++;
                }
            }
            // Check whether the input includes all the words in the actual text
            if (iact!=actual.length) correct=false;

            // Show the word "Correct" and display ending punctuation if the input matches the
            // actual verse text
            if(correct) {
                feedback.append(actual_with_punc[actual_with_punc.length-1]);
                findViewById(R.id.review_correctFeedbackView).setVisibility(View.VISIBLE);
            } else {
                // Otherwise, make sure "Correct" is hidden
                findViewById(R.id.review_correctFeedbackView).setVisibility(View.GONE);
            }

            // Show feedback and make sure it's visible
            verseFeedbackView.setText(feedback.toString());
            verseFeedbackView.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // The current_verse JSONObject is not formatted correctly
            showErrorMsg(getString(R.string.error_generic));
        }
    }

    /**
     * Send the user's rating of the current verse to the Memverse server
     * @param rating a rating between 1 and 5 (inclusive)
     */
    private void rateCurrentVerse(int rating) {
        memverse.rateVerse(current_verse, rating, new MemverseCallback<Boolean>() {
            @Override
            public void call(Boolean is_memorized) {
                askNextVerse();
                // Stop displaying progress spinner
                showProgress(false);
                // TODO: Display "You memorized this verse" message if verse is memorized
            }
        }, new MemverseCallback<String>() {
            @Override
            public void call(String error_type) {
                if (error_type.equals("network error")) {
                    showErrorMsg(getString(R.string.error_network));
                } if (error_type.equals("authorization error")) {
                    showErrorMsg(getString(R.string.error_authorization));
                } else {
                    showErrorMsg(getString(R.string.error_generic));
                }
            }
        });
        // Display progress spinner until rating process finishes
        showProgress(true);
    }

    /**
     * Turn the given String into a list of its words stripped of their capitalization and punctuation
     * @param str the String to turn into a list of words
     * @return a String of words stripped of their capitalization and punctuation
     */
    @NonNull
    private String[] essentialize(String str) {
        // convert everything to lower case; remove beginning whitespaces, ending whitespaces, and
        // all punctuation except for "-"; and split at the remaining whitespaces
        return str.toLowerCase().replaceAll("^\\s+|\\s$+|[\\W&&[^\\s-]]+","")
                .split("\\s++");
    }

    /**
     * Show a screen notifying users that all their verses have been reviewed for the day
     */
    private void showReviewCompletedScreen() {
        launchActivity(ReviewCompletedActivity.class);
    }
}
