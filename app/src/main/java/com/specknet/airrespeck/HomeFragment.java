package com.specknet.airrespeck;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class HomeFragment extends Fragment {

    private ReadingsFragment mReadingsFragment;
    private ReadingFragment mReadingFragment;
    private FeedbackFragment mFeedbackFragment;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReadingsFragment = new ReadingsFragment();
        mReadingFragment = new ReadingFragment();
        mFeedbackFragment = new FeedbackFragment();

        FragmentTransaction trans = getChildFragmentManager().beginTransaction();
        //trans.add(R.id.readings, mReadingsFragment, "READINGS");
        trans.add(R.id.readings, mReadingFragment, "READINGS");
        trans.add(R.id.feedback, mFeedbackFragment, "FEEDBACK");
        trans.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

    }

    public void setRespiratoryRate(final int value) {
        //mReadingsFragment.setRespiratoryRate(value);
        mReadingFragment.setReadingVal(value, 0);
    }

    public void setPM10(final int value) {
        //mReadingsFragment.setPM10(value);
        mReadingFragment.setReadingVal(value, 1);
    }

    public void setPM2_5(final int value) {
        //mReadingsFragment.setPM2_5(value);
        mReadingFragment.setReadingVal(value, 2);
    }
}
