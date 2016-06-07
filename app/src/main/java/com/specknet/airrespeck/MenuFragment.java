package com.specknet.airrespeck;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;


public class MenuFragment extends Fragment {

    private FragmentActivity mListener;
    private LinearLayout mMenuContainer;
    private LinearLayout.LayoutParams mMenuItemLayoutParameters;

    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mListener = (FragmentActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMenuItemLayoutParameters = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mMenuContainer = (LinearLayout)view.findViewById(R.id.menu_layout);

        createButton(getString(R.string.home), R.drawable.ic_home);
        createButton(getString(R.string.dashboard), R.drawable.ic_dashboard);
        createButton(getString(R.string.settings), R.drawable.ic_settings);
    }

    private void createButton(final String label, final int image) {
        Button button = new Button(mListener);
        button.setText(label);
        button.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        button.setCompoundDrawablesWithIntrinsicBounds(0, image, 0, 0);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(this, xActivity.class));
            }
        });

        mMenuContainer.addView(button, mMenuItemLayoutParameters);
    }
}
