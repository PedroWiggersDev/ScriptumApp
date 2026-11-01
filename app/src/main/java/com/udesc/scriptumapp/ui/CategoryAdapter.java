package com.udesc.scriptumapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.udesc.scriptumapp.R;
import com.udesc.scriptumapp.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryHolder> {
    private final List<Category> items;
    private final Consumer<Category> onCategoryClick;

    CategoryAdapter(List<Category> categories, Consumer<Category> onCategoryClick) {
        this.items = new ArrayList<>(categories);
        this.onCategoryClick = onCategoryClick;
    }

    @NonNull
    @Override
    public CategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        item.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new CategoryHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHolder holder, int position) {
        Category category = items.get(position);
        holder.title.setText(category.name);
        holder.count.setText(category.documentCount + " docs");
        holder.itemView.setOnClickListener(view -> onCategoryClick.accept(category));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class CategoryHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView count;

        CategoryHolder(View item) {
            super(item);
            title = item.findViewById(R.id.category_item_title);
            count = item.findViewById(R.id.category_item_count);
        }
    }
}
