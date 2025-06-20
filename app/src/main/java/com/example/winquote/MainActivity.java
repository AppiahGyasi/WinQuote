package com.example.winquote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView quoteTextView;
    private TextView authorTextView;
    private Button refreshButton;
    private Button saveButton;
    private Button shareButton;
    private Button viewSavedButton;
    private ProgressBar progressBar;

    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private static final String SAVED_QUOTES_KEY = "saved_quotes";
    private static final String PREFS_NAME = "WinQuotePrefs";

    private String currentQuote = "";
    private String currentAuthor = "";

    private static final String API_URL = "https://zenquotes.io/api/random";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRequestQueue();
        setupSharedPreferences();
        setupClickListeners();

        // Load initial quote
        fetchNewQuote();
    }

    private void initializeViews() {
        quoteTextView = findViewById(R.id.quoteTextView);
        authorTextView = findViewById(R.id.authorTextView);
        refreshButton = findViewById(R.id.refreshButton);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        viewSavedButton = findViewById(R.id.viewSavedButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRequestQueue() {
        // Create Volley request queue with better configuration
        requestQueue = Volley.newRequestQueue(this, new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                return connection;
            }
        });
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void setupClickListeners() {
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchNewQuote();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentQuote();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShareOptions();
            }
        });

        viewSavedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSavedQuotesActivity();
            }
        });
    }

    private void fetchNewQuote() {
        Log.d(TAG, "Starting to fetch new quote...");

        // Add network state check
        if (!isConnected()) {
            Log.e(TAG, "No internet connection available");
            showError("No internet connection");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        refreshButton.setEnabled(false);

        // Create JsonArrayRequest (ZenQuotes returns an array, not a single object)
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                API_URL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Quote API Response: " + response.toString());

                        try {
                            if (response.length() > 0) {
                                JSONObject quoteObject = response.getJSONObject(0);

                                // ZenQuotes uses "q" for quote and "a" for author
                                currentQuote = quoteObject.optString("q", "");
                                currentAuthor = quoteObject.optString("a", "");

                                if (!currentQuote.isEmpty() && !currentAuthor.isEmpty()) {
                                    quoteTextView.setText("\"" + currentQuote + "\"");
                                    authorTextView.setText("- " + currentAuthor);
                                    Log.d(TAG, "Quote loaded successfully: " + currentQuote);
                                } else {
                                    Log.e(TAG, "Empty quote or author received");
                                    showError("Invalid quote data received");
                                }
                            } else {
                                Log.e(TAG, "Empty response array");
                                showError("No quotes received from server");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            showError("Error parsing quote data");
                        }

                        progressBar.setVisibility(View.GONE);
                        refreshButton.setEnabled(true);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        Log.e(TAG, "Error details: " + error.toString());

                        if (error.networkResponse != null) {
                            Log.e(TAG, "HTTP Status Code: " + error.networkResponse.statusCode);
                            Log.e(TAG, "Response data: " +
                                    new String(error.networkResponse.data));
                        } else {
                            Log.e(TAG, "Network response is null - likely connectivity issue");
                        }

                        String errorMessage = "Failed to fetch quote. ";
                        if (error.networkResponse != null) {
                            errorMessage += "HTTP " + error.networkResponse.statusCode;
                        } else {
                            errorMessage += "Check your internet connection.";
                        }

                        showError(errorMessage);
                        progressBar.setVisibility(View.GONE);
                        refreshButton.setEnabled(true);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "WinQuote-Android-App/1.0");
                headers.put("Accept", "application/json");
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
        };

        // Set retry policy with logging
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,  // 15 seconds timeout
                2,      // 2 retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Add request to queue with logging
        requestQueue.add(request);
        Log.d(TAG, "Request added to queue: " + API_URL);
    }

    private void saveCurrentQuote() {
        if (currentQuote.isEmpty()) {
            showError("No quote to save");
            return;
        }

        Set<String> savedQuotes = sharedPreferences.getStringSet(SAVED_QUOTES_KEY,
                new HashSet<>(Arrays.asList("")));

        if (savedQuotes.contains(currentQuote + "|" + currentAuthor)) {
            showError("Quote already saved!");
            return;
        }

        savedQuotes.add(currentQuote + "|" + currentAuthor);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(SAVED_QUOTES_KEY, savedQuotes);
        editor.apply();

        Toast.makeText(this, "Quote saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showShareOptions() {
        if (currentQuote.isEmpty()) {
            showError("No quote to share");
            return;
        }

        String shareText = "\"" + currentQuote + "\"\n\n- " + currentAuthor +
                "\n\nShared via WinQuote App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        Intent chooser = Intent.createChooser(shareIntent, "Share quote via");
        startActivity(chooser);
    }

    private void openSavedQuotesActivity() {
        Intent intent = new Intent(this, SavedQuotesActivity.class);
        startActivity(intent);
    }

    private void showError(String message) {
        Log.e(TAG, "Showing error to user: " + message);
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            refreshButton.setEnabled(true);
        });
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return cap != null &&
                cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}