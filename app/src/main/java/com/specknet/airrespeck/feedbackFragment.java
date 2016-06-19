package com.specknet.airrespeck;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class FeedbackFragment extends Fragment {

    private TextView mFeedback;

    public FeedbackFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mFeedback = (TextView) view.findViewById(R.id.feedback);

        updateFeedback();
    }

    public void updateFeedback() {
        // TODO add dynamic feedback based on readings and user profile
        mFeedback.setText("Safe to exercise");
        mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_running_man, 0, 0);
        //mFeedback.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_stop, 0, 0);
    }
}
