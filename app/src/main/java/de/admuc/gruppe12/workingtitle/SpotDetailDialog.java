package de.admuc.gruppe12.workingtitle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * Created by matti on 27.01.2015.
 */
public class SpotDetailDialog extends DialogFragment {
    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;
    private View main = null;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // I need a nice little layout here
        builder.setView(main = inflater.inflate(R.layout.spot_detail, null))
                .setPositiveButton("Send rating", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // save input data
                        int id = getArguments().getInt("id");
                        RatingBar ratingBar = (RatingBar) main.findViewById(R.id.spot_detail_ratingBar);
                        float userRating = ratingBar.getRating();

                        // Send the positive button event back to the host activity
                        // dummy string is need so I can use the same interface for two buttonListener ;)
                        mListener.onDialogPositiveClick(SpotDetailDialog.this, id, "dummyString", userRating);
                    }
                })
                .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(SpotDetailDialog.this);


                    }
                });

        // find the views
        TextView spot_detail_rating = ((TextView) main.findViewById(R.id.spot_detail_average_rating));
        TextView spot_detail_name = ((TextView) main.findViewById(R.id.spot_detail_name));

        // get the data from the bundle to show in the dialog
        Bundle b = getArguments();
        float current_rating = b.getLong("rating");
        String title = b.getString("title");
        // display data in the view
        spot_detail_rating.setText(String.valueOf(String.format("%.1f", current_rating)));
        spot_detail_name.setText(title);

        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, int id, String spotName, float spotRating);

        public void onDialogNegativeClick(DialogFragment dialog);
    }
}
