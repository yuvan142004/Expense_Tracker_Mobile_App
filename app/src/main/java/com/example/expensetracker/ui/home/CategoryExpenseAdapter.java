package com.example.expensetracker.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;

import java.util.List;
import java.util.Locale;

public class CategoryExpenseAdapter extends RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder> {

    public static class CategoryExpenseItem {
        public int categoryId;
        public String categoryName;
        public double amount;
        public String colorHex;
        
        public CategoryExpenseItem(int id, String name, double amt, String color) {
            this.categoryId = id;
            this.categoryName = name;
            this.amount = amt;
            this.colorHex = color;
        }
    }

    private final Context context;
    private List<CategoryExpenseItem> items;

    public CategoryExpenseAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<CategoryExpenseItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryExpenseItem item = items.get(position);
        
        holder.tvCategoryName.setText(item.categoryName);
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", item.amount));
        
        // Set color dot
        try {
            holder.viewColorDot.setBackgroundColor(Color.parseColor(item.colorHex));
        } catch (Exception ignored) {
            holder.viewColorDot.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewColorDot;
        TextView tvCategoryName;
        TextView tvAmount;

        ViewHolder(View itemView) {
            super(itemView);
            viewColorDot = itemView.findViewById(R.id.viewColorDot);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
