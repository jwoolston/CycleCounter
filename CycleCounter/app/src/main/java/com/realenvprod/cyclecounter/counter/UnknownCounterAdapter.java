package com.realenvprod.cyclecounter.counter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.realenvprod.cyclecounter.R;

import java.util.ArrayList;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class UnknownCounterAdapter extends RecyclerView.Adapter<UnknownCounterAdapter.ViewHolder> {

    private static final String TAG = "UnknownCounterAdapter";

    private static UnknownCounterAdapter instance;

    private final ArrayList<Counter> counters = new ArrayList<>();

    public static synchronized UnknownCounterAdapter getInstance() {
        if (instance == null) {
            instance = new UnknownCounterAdapter();
        }
        return instance;
    }

    private UnknownCounterAdapter() {

    }

    public boolean addCounter(@NonNull Counter counter) {
        if (!counters.contains(counter)) {
            Log.d(TAG, "Adding counter: " + counter + " to unknown devices adapter.");
            counters.add(counter);
            return true;
        } else {
            return false;
        }
    }

    public void removeCounter(@NonNull Counter counter) {
        counters.remove(counter);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.counter_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Log.d(TAG, "Binding view for position " + position + ": " + counters.get(position));
        holder.setData(counters.get(position));
    }

    @Override
    public int getItemCount() {
        return counters.size();
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
            aliasView.setText(counter.alias);
            addressView.setText(counter.address);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + aliasView.getText() + "'";
        }
    }
}
