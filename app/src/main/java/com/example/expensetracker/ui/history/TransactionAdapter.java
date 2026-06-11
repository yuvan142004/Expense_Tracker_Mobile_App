package com.example.expensetracker.ui.history;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.Category;
import com.example.expensetracker.data.entity.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionAdapter
        extends ListAdapter<Transaction, TransactionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    private final Context            context;
    private final OnItemClickListener listener;
    private final SimpleDateFormat    sdf =
        new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public TransactionAdapter(Context context, OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.context  = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction txn = getItem(position);

        // Amount with ₹ symbol and type indicator
        String amountStr = String.format("\u20B9%.0f", txn.amount);
        holder.tvAmount.setText(amountStr);
        holder.tvAmount.setTextColor(
            "CREDIT".equals(txn.transactionType) ? Color.parseColor("#4CAF50")
                                                 : Color.parseColor("#F44336"));

        // Merchant or source
        String label = txn.merchantName != null && !txn.merchantName.isEmpty()
                ? txn.merchantName : txn.source;
        holder.tvMerchant.setText(label);

        // Date
        holder.tvDate.setText(sdf.format(new Date(txn.timestamp)));

        // Payment method (show only if not CASH, which is default)
        if (txn.paymentMethod != null && !txn.paymentMethod.isEmpty() && !"CASH".equals(txn.paymentMethod)) {
            String displayMethod = getPaymentMethodDisplay(txn.paymentMethod);
            holder.tvPaymentMethod.setText(" · " + displayMethod);
            holder.tvPaymentMethod.setVisibility(View.VISIBLE);
        } else {
            holder.tvPaymentMethod.setVisibility(View.GONE);
        }

        // Category name + color (async)
        AppDatabase.dbExecutor.execute(() -> {
            Category cat = AppDatabase.getInstance(context)
                    .categoryDao().getCategoryByIdSync(txn.categoryId);
            if (cat != null) {
                holder.itemView.post(() -> {
                    holder.tvCategory.setText(cat.name);
                    try {
                        holder.tvCategoryDot.setBackgroundColor(
                            Color.parseColor(cat.colorHex));
                    } catch (Exception ignored) {}
                });
            }
        });

        // Tap → edit category
        holder.itemView.setOnClickListener(v -> listener.onItemClick(txn));
    }

    /**
     * Convert payment method code to user-friendly display text
     */
    private String getPaymentMethodDisplay(String method) {
        switch (method) {
            case "UPI":
                return "💵 UPI";
            case "CARD":
                return "💳 Card";
            case "NET_BANKING":
                return "🏦 Net Banking";
            case "CASH":
            default:
                return "Cash";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMerchant, tvCategory, tvDate, tvAmount, tvCategoryDot, tvPaymentMethod;
        ViewHolder(View itemView) {
            super(itemView);
            tvMerchant       = itemView.findViewById(R.id.tvMerchant);
            tvCategory       = itemView.findViewById(R.id.tvCategory);
            tvDate           = itemView.findViewById(R.id.tvDate);
            tvAmount         = itemView.findViewById(R.id.tvAmount);
            tvCategoryDot    = itemView.findViewById(R.id.tvCategoryDot);
            tvPaymentMethod  = itemView.findViewById(R.id.tvPaymentMethod);
        }
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Transaction>() {
            @Override
            public boolean areItemsTheSame(@NonNull Transaction a, @NonNull Transaction b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull Transaction a, @NonNull Transaction b) {
                return a.id == b.id && a.categoryId == b.categoryId
                    && a.amount == b.amount && a.isEdited == b.isEdited;
            }
        };
}
