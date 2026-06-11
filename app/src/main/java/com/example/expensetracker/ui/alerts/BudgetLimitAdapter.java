package com.example.expensetracker.ui.alerts;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.BudgetLimit;
import com.example.expensetracker.data.entity.Category;

public class BudgetLimitAdapter
        extends ListAdapter<BudgetLimit, BudgetLimitAdapter.ViewHolder> {

    public interface OnDeleteListener { void onDelete(BudgetLimit limit); }
    public interface OnEditListener   { void onEdit(BudgetLimit limit);   }

    private final Context          context;
    private final OnDeleteListener deleteListener;
    private final OnEditListener   editListener;

    public BudgetLimitAdapter(Context context,
                               OnDeleteListener deleteListener,
                               OnEditListener editListener) {
        super(DIFF_CALLBACK);
        this.context        = context;
        this.deleteListener = deleteListener;
        this.editListener   = editListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_limit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetLimit limit = getItem(position);

        AppDatabase.dbExecutor.execute(() -> {
            Category cat = AppDatabase.getInstance(context)
                    .categoryDao().getCategoryByIdSync(limit.categoryId);
            holder.itemView.post(() ->
                holder.tvCategory.setText(cat != null ? cat.name : "Unknown"));
        });

        holder.tvLimit.setText(String.format("\u20B9%.0f / %s", limit.limitAmount, limit.period));
        holder.tvActive.setText(limit.isActive ? "Active" : "Paused");
        holder.tvActive.setTextColor(limit.isActive ? 0xFF4CAF50 : 0xFFFF9800);

        holder.btnEdit.setOnClickListener(v   -> editListener.onEdit(limit));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(limit));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvCategory, tvLimit, tvActive;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvLimitCategory);
            tvLimit    = v.findViewById(R.id.tvLimitAmount);
            tvActive   = v.findViewById(R.id.tvLimitStatus);
            btnEdit    = v.findViewById(R.id.btnEditLimit);
            btnDelete  = v.findViewById(R.id.btnDeleteLimit);
        }
    }

    private static final DiffUtil.ItemCallback<BudgetLimit> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<BudgetLimit>() {
            @Override
            public boolean areItemsTheSame(@NonNull BudgetLimit a, @NonNull BudgetLimit b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull BudgetLimit a, @NonNull BudgetLimit b) {
                return a.id == b.id
                    && a.limitAmount == b.limitAmount
                    && a.isActive    == b.isActive
                    && a.period.equals(b.period);
            }
        };
}
