package com.example.winquote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SavedQuotesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SavedQuotesAdapter adapter;
    private ArrayList<Quote> savedQuotesList;
    private SharedPreferences sharedPreferences;
    private static final String SAVED_QUOTES_KEY = "saved_quotes";
    private static final String PREFS_NAME = "WinQuotePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_quotes);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saved Quotes");
        }

        setupRecyclerView();
        loadSavedQuotes();
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.savedQuotesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        savedQuotesList = new ArrayList<>();
        adapter = new SavedQuotesAdapter(savedQuotesList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadSavedQuotes() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> savedQuotes = sharedPreferences.getStringSet(SAVED_QUOTES_KEY, new HashSet<String>());

        savedQuotesList.clear();

        for (String quoteData : savedQuotes) {
            String[] parts = quoteData.split("\\|");
            if (parts.length == 2) {
                savedQuotesList.add(new Quote(parts[0], parts[1]));
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void removeQuote(Quote quote) {
        Set<String> savedQuotes = sharedPreferences.getStringSet(SAVED_QUOTES_KEY, new HashSet<String>());
        Set<String> updatedQuotes = new HashSet<>(savedQuotes);

        String quoteData = quote.getText() + "|" + quote.getAuthor();
        updatedQuotes.remove(quoteData);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SAVED_QUOTES_KEY, updatedQuotes);
        editor.apply();

        loadSavedQuotes(); // Refresh the list
    }
}