package com.memverse.www.memverse;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.memverse.www.memverse.MemverseInterface.MemverseCallback;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AddVersesActivity extends NavigationActivity {

    String[] bibleBookNames={"genesis", "exodus", "leviticus", "numbers", "deuteronomy", "joshua",
            "judges", "ruth", "1samuel", "2samuel", "1kings", "2kings", "1chronicles",
            "2chronicles", "ezra", "nehemiah", "esther", "job", "psalms", "proverbs",
            "ecclesiastes", "song of solomon", "sos", "isaiah", "jeremiah", "lamentations", "ezekiel",
            "daniel", "hosea", "joel", "amos", "obediah", "jonah", "micah", "nahum", "habakkuk",
            "zephaniah", "haggai", "zechariah", "malachi", "matthew", "mark", "luke", "john",
            "acts", "romans", "1corinthians", "2corinthians", "galatians", "ephesians",
            "philippians", "colossians", "1thessalonians", "2thessalonians", "1timothy",
            "2timothy", "titus", "philemon", "hebrews", "james", "1peter", "2peter", "1john",
            "2john", "3john", "jude", "revelations"};
    String[] bibleBookDisplayNames={"genesis", "exodus", "leviticus", "numbers", "deuteronomy",
            "joshua", "judges", "ruth", "1 samuel", "2 samuel", "1 kings", "2 kings", "1 chronicles",
            "2 chronicles", "ezra", "nehemiah", "esther", "job", "psalms", "proverbs",
            "ecclesiastes", "song of solomon", "song of solomon", "isaiah", "jeremiah",
            "lamentations", "ezekiel", "daniel", "hosea", "joel", "amos", "obediah", "jonah",
            "micah", "nahum", "habakkuk", "zephaniah", "haggai", "zechariah", "malachi", "matthew",
            "mark", "luke", "john", "acts", "romans", "1 corinthians", "2 corinthians", "galatians",
            "ephesians", "philippians", "colossians", "1 thessalonians", "2 thessalonians",
            "1 timothy", "2 timothy", "titus", "philemon", "hebrews", "james", "1 peter", "2 peter",
            "1 john", "2 john", "3 john", "jude", "revelation"};
    //A list of the current user's memory verses (so that the activity knows which verses the user
    // can delete)
    List<JSONObject> memoryVerses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNavigationActivity(R.layout.activity_add_verses);

        findViewById(R.id.addVerses_searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showVerseLoadProgress(true);
                String ref = ((EditText) findViewById(R.id.addVerses_refInput)).getText().toString();
                String[] parse = parseReference(ref);
                if (parse!=null) {
                    String book;
                    int chapter, start_verse, end_verse;
                    try {
                        book=parse[0];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        showErrorMsg("Reference could not be parsed");
                        return;
                    }
                    try {
                        chapter=Integer.parseInt(parse[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        showErrorMsg("Reference could not be parsed");
                        return;
                    }
                    try {
                        start_verse=Integer.parseInt(parse[2]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        start_verse=0;
                    }
                    try {
                        end_verse=Integer.parseInt(parse[3]);
                    } catch(ArrayIndexOutOfBoundsException e) {
                        end_verse=start_verse;
                    }
                    memverse.getVerses(book, chapter, start_verse, end_verse, "NKJ",
                            new MemverseCallback<List<JSONObject>>() {
                                @Override
                                public void call(final List<JSONObject> verses) {
                                    memverse.getMemoryVerses(new MemverseCallback<List<JSONObject>>() {
                                        @Override
                                        public void call(List<JSONObject> mem_verses) {
                                            memoryVerses = mem_verses;
                                            showVerseLoadProgress(false);
                                            for(int i=0; i<verses.size(); i++) {
                                                JSONObject verse = verses.get(i);
                                                try {
                                                    displayVerse(verse, i==0);
                                                } catch (JSONException e) {
                                                    showErrorMsg(getResources().getString(R.string.error_generic));
                                                }
                                            }
                                        }
                                    }, new MemverseCallback<String>() {
                                        @Override
                                        public void call(String error_type) {
                                            switch(error_type) {
                                                case "network error":
                                                    showErrorMsg(getString(R.string.error_network));
                                                    break;
                                                case "authentication error":
                                                    showErrorMsg(getString(R.string.error_authorization));
                                                    break;
                                                default:
                                                    showErrorMsg(getString(R.string.error_generic));
                                            }
                                        }
                                    });
                                }
                            }, new MemverseCallback<String>() {
                                @Override
                                public void call(String error_type) {
                                    if (error_type.equals("network error")) {
                                        showErrorMsg(getString(R.string.error_network));
                                    }
                                    showVerseLoadError();
                                }
                            });
                } else {
                    showVerseLoadError();
                }
            }
        });
    }

    /**
     * Add the given verse to the list of displayed verses.
     * @param verse the verse to display
     * @param isFirstVerse true if the verse will be the first verse in the list and false otherwise
     * @throws JSONException if the verse JSONObject is not correctly formatted (see Memverse API)
     */
    private void displayVerse(final JSONObject verse, boolean isFirstVerse) throws JSONException {
        String ref=memverse.getReference(verse);
        String text=verse.getString("text");
        LinearLayout verseList=findViewById(R.id.addVerses_verses);
        //Create divider if this verse is not the first in the list
        if(!isFirstVerse) {
            LinearLayout divider=new LinearLayout(this);
            LinearLayout.LayoutParams divLayoutParams=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,3);
            divLayoutParams.setMargins(8,8,8,8);
            divider.setLayoutParams(divLayoutParams);
            divider.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
            verseList.addView(divider);
        }
        // Create a text view for the reference
        TextView referenceView=new TextView(this);
        referenceView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        referenceView.setText(ref);
        referenceView.setTextColor(getResources().getColor(R.color.colorPrimaryTextLight));
        referenceView.setTypeface(Typeface.DEFAULT_BOLD);
        verseList.addView(referenceView);
        //Create a text view for the verse's text
        TextView textView=new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setText(text);
        textView.setTextColor(getResources().getColor(R.color.colorSecondaryTextLight));
        verseList.addView(textView);
        //Create an "Add Verse" button if the verse is not yet one of the user's memory verses
        final int mv_idx=isMemoryVerse(verse);
        if(mv_idx==-1) {
            final Button addVerse = new Button(this);
            addVerse.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            addVerse.setText(R.string.action_add_verse);
            addVerse.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            addVerse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    memverse.addVerse(verse, new MemverseCallback<JSONObject>() {
                        @Override
                        public void call(JSONObject input) {
                            showErrorMsg("Verse added");
                            addVerse.setVisibility(View.GONE);
                        }
                    }, new MemverseCallback<String>() {
                        @Override
                        public void call(String error_type) {
                            if (error_type.equals("network error")) {
                                showErrorMsg(getString(R.string.error_network));
                            } else {
                                showErrorMsg(error_type);
                            }
                        }
                    });
                }
            });
            verseList.addView(addVerse);
        }
        //Create a "Delete Verse" button if the verse is already one of the user's memory verses
        else {
            final Button deleteVerse = new Button(this);
            deleteVerse.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            deleteVerse.setText(R.string.action_delete_verse);
            deleteVerse.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            deleteVerse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    memverse.deleteVerse(memoryVerses.get(mv_idx), new MemverseCallback<String>() {
                        @Override
                        public void call(String input) {
                            showErrorMsg("Verse deleted");
                            deleteVerse.setVisibility(View.GONE);
                        }
                    }, new MemverseCallback<String>() {
                        @Override
                        public void call(String error_type) {
                            switch (error_type) {
                                case "network error":
                                    showErrorMsg(getString(R.string.error_network));
                                    break;
                                case "authentication error":
                                    showErrorMsg(getString(R.string.error_authorization));
                                    break;
                                default:
                                    showErrorMsg(getString(R.string.error_generic));
                            }
                        }
                    });
                }
            });
            verseList.addView(deleteVerse);
        }
    }

    /**
     * Separate the components of the given reference into an Array. For example, "Gen. 1:3" is
     * changed to {"genesis", "1", "3"}. "1Chr 5:2-5" is changed to {"1 chronicles", "5", "2", "5"}.
     * "2 thess 3" is changed to {"2 thessalonians", "3"}.
     * @param ref the inputted reference
     * @return an Array containing the separated components of the given reference (or null if the
     * input cannot be parsed)
     */
    private String[] parseReference(String ref) {
        String[] rtn;
        // convert everything to lower case; remove beginning whitespaces, whitespaces following
        // anything other than a letter, and all punctuation except for ":" and "-"; and split at
        // the remaining whitespaces
        String[] parsed = ref.toLowerCase().replaceAll("[\\W&&[^\\s:-]]+", " ")
                .replaceAll("^\\s+|(?<=\\d)\\s","").split("\\s++");
        // if the length of parsed is not 2, the input is not a recognizable reference
        if(parsed.length!=2) {
            return null;
        }
        String[] parsedChapVerse = parsed[1].split(":");
        if(parsedChapVerse.length==1) {
            rtn = new String[]{parseBook(parsed[0]), parsed[1]};
        } else if(parsedChapVerse.length==2) {
            String[] parsedStartEnd = parsedChapVerse[1].split("-");
            if(parsedStartEnd.length==1) {
                rtn = new String[]{parseBook(parsed[0]), parsedChapVerse[0], parsedChapVerse[1]};
            } else if(parsedStartEnd.length==2) {
                rtn = new String[]{parseBook(parsed[0]), parsedChapVerse[0], parsedStartEnd[0], parsedStartEnd[1]};
            } else {
                return null;
            }
        } else {
            return null;
        }

        if(rtn[0].equals("")) return null;
        for(int i=1; i<rtn.length; i++) if(!rtn[i].matches("\\d+")) return null;

        return rtn;
    }

    /**
     * Convert Bible book abbreviations to the full book name (in lowercase).
     * @param book a lowercase abbreviation for a book of the Bible with spaces and punctuation
     *             removed
     * @return the full name of the book of the Bible in all lowercase letters (or an empty String
     * if the input cannot be parsed)
     */
    private String parseBook(String book) {
        int idx=-1;
        for(int i=0; i<bibleBookNames.length; i++) {
            int bLen=book.length();
            if(bLen<=bibleBookNames[i].length()) {
                if (bibleBookNames[i].substring(0, bLen).equals(book)) {
                    if (idx == -1) idx = i;
                    else return ""; // book could be multiple books of the bible
                }
            }
        }
        if(idx==-1) return "";
        else return bibleBookDisplayNames[idx];
    }

    /**
     * Show the error message for when verses could not be loaded
     */
    private void showVerseLoadError() {
        showVerseLoadProgress(false);
        findViewById(R.id.addVerses_verseListErrorText).setVisibility(View.VISIBLE);
    }

    /**
     * Show or hide the progress spinner in the verse list panel
     * @param show true if the progress spinner should be shown and false if it should be hidden
     */
    private void showVerseLoadProgress(final boolean show) {
        if(show) {
            ((ViewGroup) findViewById(R.id.addVerses_verses)).removeAllViews();
            findViewById(R.id.addVerses_verseListFillerText).setVisibility(View.GONE);
            findViewById(R.id.addVerses_verseListErrorText).setVisibility(View.GONE);
            findViewById(R.id.addVerses_verseListProgress).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.addVerses_verseListProgress).setVisibility(View.GONE);
        }
    }

    /**
     * If the given verse is not one of the current user's memory verses, return -1. Otherwise return
     * the index of the verse in the user's memory verse list. Return -1 if an error occurs.
     * @param verse a JSONObject representing the verse to check for in the user's memory verse list
     *              (see Memverse API for the exact form of this JSONObject)
     * @return the index of the verse in the user's memory verse list, or -1 if the given verse is
     * not in the list or if an error occurs.
     */
    private int isMemoryVerse(JSONObject verse) {
        if (memoryVerses.size() > 0) {
            try {
                for (int i = 0; i < memoryVerses.size(); i++) {
                    if (memoryVerses.get(i).getJSONObject("verse").getInt("id") ==
                            verse.getInt("id")) {
                        return i;
                    }
                }
            } catch (JSONException e) {
                showErrorMsg(getString(R.string.error_generic));
                return -1;
            }
        }
        return -1;
    }
}
