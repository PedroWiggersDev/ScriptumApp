package com.udesc.scriptumapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.udesc.scriptumapp.R;
import com.udesc.scriptumapp.model.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentHolder> {
    private final List<Document> allItems = new ArrayList<>();
    private final List<Document> filteredItems = new ArrayList<>();
    private final Consumer<Document> onDocumentClick;

    DocumentAdapter(List<Document> documents, Consumer<Document> onDocumentClick) {
        this.onDocumentClick = onDocumentClick;
        setItems(documents);
    }

    void setItems(List<Document> documents) {
        allItems.clear();
        allItems.addAll(documents);
        filter("");
    }

    void filter(String query) {
        filteredItems.clear();
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        for (Document document : allItems) {
            if (document.title.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                filteredItems.add(document);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new DocumentHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentHolder holder, int position) {
        Document document = filteredItems.get(position);
        holder.title.setText(document.title);
        holder.summary.setText(document.getSummary());
        holder.status.setText(document.status + "\n" + document.getStatusDetail());
        holder.itemView.setOnClickListener(view -> onDocumentClick.accept(document));
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    static final class DocumentHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView summary;
        final TextView status;

        DocumentHolder(View item) {
            super(item);
            title = item.findViewById(R.id.document_item_title);
            summary = item.findViewById(R.id.document_item_summary);
            status = item.findViewById(R.id.document_item_status);
        }
    }
}
