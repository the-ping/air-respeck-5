package com.specknet.airrespeck;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.specknet.airrespeck.utils.ButtonDesc;

import java.util.ArrayList;
import java.util.List;


public class MenuFragment extends Fragment {

    private List<ButtonDesc> mButtons;

    private OnMenuSelectedListener mListener;

    private LinearLayout mMenuContainer;
    private LinearLayout.LayoutParams mMenuItemLayoutParameters;

    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMenuSelectedListener) {
            mListener = (OnMenuSelectedListener) context;
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

        mButtons = new ArrayList<ButtonDesc>();
        mButtons.add(new ButtonDesc(ButtonDesc.buttonType.HOME, getString(R.string.menu_home),
                R.drawable.ic_home));
        mButtons.add(new ButtonDesc(ButtonDesc.buttonType.DASHBOARD, getString(R.string.menu_dashboard),
                R.drawable.ic_dashboard));
        mButtons.add(new ButtonDesc(ButtonDesc.buttonType.SETTINGS, getString(R.string.menu_settings),
                R.drawable.ic_settings));
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

        createButton(mButtons.get(0));
        createButton(mButtons.get(1));
        createButton(mButtons.get(2));
    }

    /**
     * Interface to communicate with the host activity
     * The host activity must implement this interface
     */
    public interface OnMenuSelectedListener {
        void onButtonSelected(int buttonId);
    }

    public void onButtonClick(View v, int buttonId) {
        // Send events to the host activity
        mListener.onButtonSelected(buttonId);
    }

    private void createButton(final ButtonDesc buttonDesc) {
        Button button = new Button(getActivity());
        button.setText(buttonDesc.getLabel());
        button.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        button.setCompoundDrawablesWithIntrinsicBounds(0, buttonDesc.getImage(), 0, 0);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v, buttonDesc.getType().ordinal());
            }
        });

        mMenuContainer.addView(button, mMenuItemLayoutParameters);
    }
}
