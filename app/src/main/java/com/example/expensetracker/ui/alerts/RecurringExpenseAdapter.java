package com.example.expensetracker.ui.alerts;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.entity.RecurringExpense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecurringExpenseAdapter
        extends ListAdapter<RecurringExpense, RecurringExpenseAdapter.ViewHolder> {

    public interface OnDeleteListener { void onDelete(RecurringExpense expense); }
    public interface OnEditListener   { void onEdit(RecurringExpense expense);   }

    private final Context          context;
    private final OnDeleteListener deleteListener;
    private final OnEditListener   editListener;

    private final SimpleDateFormat sdf =
        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public RecurringExpenseAdapter(Context context,
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
                .inflate(R.layout.item_recurring_expense, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecurringExpense re = getItem(position);
        holder.tvName.setText(re.name);
        holder.tvAmount.setText(String.format("\u20B9%.0f / %s", re.amount, re.frequency));
        holder.tvNextDue.setText("Due: " + sdf.format(new Date(re.nextDueDate)));

        holder.btnEdit.setOnClickListener(v   -> editListener.onEdit(re));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(re));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvName, tvAmount, tvNextDue;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvRecurringName);
            tvAmount  = v.findViewById(R.id.tvRecurringAmount);
            tvNextDue = v.findViewById(R.id.tvRecurringDue);
            btnEdit   = v.findViewById(R.id.btnEditRecurring);
            btnDelete = v.findViewById(R.id.btnDeleteRecurring);
        }
    }

    private static final DiffUtil.ItemCallback<RecurringExpense> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<RecurringExpense>() {
            @Override
            public boolean areItemsTheSame(@NonNull RecurringExpense a,
                                           @NonNull RecurringExpense b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull RecurringExpense a,
                                              @NonNull RecurringExpense b) {
                return a.id        == b.id
                    && a.nextDueDate == b.nextDueDate
                    && a.amount      == b.amount
                    && a.name.equals(b.name)
                    && a.frequency.equals(b.frequency);
            }
        };
}
