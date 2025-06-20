package com.example.winquote;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SavedQuotesAdapter extends RecyclerView.Adapter<SavedQuotesAdapter.ViewHolder> {

    private ArrayList<Quote> quotes;
    private SavedQuotesActivity activity;

    public SavedQuotesAdapter(ArrayList<Quote> quotes, SavedQuotesActivity activity) {
        this.quotes = quotes;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_quote, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Quote quote = quotes.get(position);

        holder.quoteText.setText("\"" + quote.getText() + "\"");
        holder.authorText.setText("- " + quote.getAuthor());

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareQuote(quote);
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.removeQuote(quote);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    private void shareQuote(Quote quote) {
        String shareText = "\"" + quote.getText() + "\"\n\n- " + quote.getAuthor() +
                "\n\nShared via WinQuote App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        Intent chooser = Intent.createChooser(shareIntent, "Share quote via");
        activity.startActivity(chooser);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView quoteText;
        TextView authorText;
        Button shareButton;
        Button deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            quoteText = itemView.findViewById(R.id.savedQuoteText);
            authorText = itemView.findViewById(R.id.savedAuthorText);
            shareButton = itemView.findViewById(R.id.shareQuoteButton);
            deleteButton = itemView.findViewById(R.id.deleteQuoteButton);
        }
    }
}