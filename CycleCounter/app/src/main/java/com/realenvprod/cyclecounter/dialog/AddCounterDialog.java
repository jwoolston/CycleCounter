package com.realenvprod.cyclecounter.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.realenvprod.cyclecounter.MainActivity;
import com.realenvprod.cyclecounter.R;
import com.realenvprod.cyclecounter.counter.Counter;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class AddCounterDialog extends DialogFragment {

    private static final String TAG = "AddCounterDialog";

    private static final String ARG_COUNTER = "counter";

    public static AddCounterDialog newInstance(@NonNull Counter counter) {
        final AddCounterDialog dialog = new AddCounterDialog();
        final Bundle args = new Bundle();
        args.putParcelable(AddCounterDialog.ARG_COUNTER, counter);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog getDialog() {
        final Counter counter = getArguments().getParcelable(ARG_COUNTER);
        if (counter == null) {
            throw new IllegalArgumentException("Bundled counter object was null!");
        }
        return new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog)
                .setTitle("New Cycle Sensor Discovered.")
                .setIcon(R.drawable.ic_add_circle_outline_black_24dp)
                .setMessage("A new cycle sensor (" + counter.getAddress() + ") has been discovered. Would you like to "
                            + "add it to the database?")
                .setCancelable(true)
                .setPositiveButton("YES", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((MainActivity) getActivity()).showAddCounterFragment(counter);
                    }
                })
                .setNegativeButton("NO", null)
                .create();
    }
}
