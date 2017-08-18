package com.realenvprod.cyclecounter.counter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.db.CounterDatabaseContract.CounterEntry;
import com.realenvprod.cyclecounter.fragment.KnownCounterFragment.OnCounterListItemInteractionListener;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Counter} and makes a call to the
 * specified {@link OnCounterListItemInteractionListener}.
 */
public class CounterAdapter extends RecyclerView.Adapter<CounterAdapter.ViewHolder> {

    private       Context                              context;
    private       Cursor                               cursor;
    private       boolean                              dataValid;
    private       int                                  rowIdColumn;
    private       DataSetObserver                      dataSetObserver;
    private final OnCounterListItemInteractionListener listener;

    public CounterAdapter(@NonNull Context context, @Nullable OnCounterListItemInteractionListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        dataValid = cursor != null;
        rowIdColumn = dataValid ? cursor.getColumnIndex(CounterEntry._ID) : -1;
        dataSetObserver = new NotifyingDataSetObserver(this);
        if (cursor != null) {
            cursor.registerDataSetObserver(dataSetObserver);
        }
    }

    @Nullable
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be closed.
     */
    public void changeCursor(@Nullable Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike {@link #changeCursor(Cursor)}, the returned old Cursor
     * is <em>not</em> closed.
     */
    @Nullable
    public Cursor swapCursor(@Nullable Cursor newCursor) {
        if (newCursor == cursor) {
            return null;
        }
        final Cursor oldCursor = cursor;
        if (oldCursor != null && dataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
        }
        cursor = newCursor;
        if (cursor != null) {
            if (dataSetObserver != null) {
                cursor.registerDataSetObserver(dataSetObserver);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow(CounterEntry._ID);
            dataValid = true;
            notifyDataSetChanged();
        } else {
            rowIdColumn = -1;
            dataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
    }

    public void setDataValid(boolean valid) {
        this.dataValid = valid;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.counter_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        holder.setData(new Counter(cursor));

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != listener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an counter has been selected.
                    listener.onCounterListItemInteraction(holder.counter);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (dataValid && cursor != null) {
            return cursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        if (dataValid && cursor != null && cursor.moveToPosition(position)) {
            return cursor.getLong(rowIdColumn);
        }
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View     view;
        private final TextView aliasView;
        private final TextView addressView;
        private       Counter  counter;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            aliasView = (TextView) view.findViewById(R.id.alias);
            addressView = (TextView) view.findViewById(R.id.address);
        }

        public void setData(@NonNull Counter counter) {
            this.counter = counter;
            aliasView.setText(counter.getAlias());
            addressView.setText(counter.getAddress());
        }

        @Override
        public String toString() {
            return super.toString() + " '" + aliasView.getText() + "'";
        }
    }

    private static class NotifyingDataSetObserver extends DataSetObserver {
        private final CounterAdapter adapter;

        public NotifyingDataSetObserver(@NonNull CounterAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onChanged() {
            super.onChanged();
            adapter.setDataValid(true);
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            adapter.setDataValid(false);
        }
    }
}
