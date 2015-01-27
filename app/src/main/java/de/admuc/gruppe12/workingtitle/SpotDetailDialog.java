package de.admuc.gruppe12.workingtitle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;

/**
 * Created by matti on 27.01.2015.
 */
public class SpotDetailDialog extends DialogFragment {
    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;
    private float userRating = 0;
    private String spotName = "";
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
        builder.setView(main = inflater.inflate(R.layout.create_news_spot, null))
                .setPositiveButton("Send rating", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // save input data
                        spotName = ((EditText) main.findViewById(R.id.locationName)).getText().toString();
                        RatingBar ratingBar = (RatingBar) main.findViewById(R.id.ratingBar);
                        userRating = ratingBar.getRating();

                        // Send the positive button event back to the host activity
                        mListener.onDialogPositiveClick(SpotDetailDialog.this, spotName, userRating);
                    }
                })
                .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(SpotDetailDialog.this);

                    }
                });
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String spotName, float spotRating);

        public void onDialogNegativeClick(DialogFragment dialog);
    }
}
