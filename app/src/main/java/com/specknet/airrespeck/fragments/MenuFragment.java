package com.specknet.airrespeck.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.models.MenuButton;
import com.specknet.airrespeck.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class MenuFragment extends Fragment {

    private List<MenuButton> mButtons;

    private OnMenuSelectedListener mListener;

    private LinearLayout mMenuContainer;
    private LinearLayout.LayoutParams mMenuItemLayoutParameters;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MenuFragment() {

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

        mButtons = new ArrayList<MenuButton>();
        mButtons.add(new MenuButton(MenuButton.buttonType.HOME, getString(R.string.menu_home),
                Utils.menuIconsResId[0]));
        mButtons.add(new MenuButton(MenuButton.buttonType.AIR_QUALITY, getString(R.string.menu_air_quality),
                Utils.menuIconsResId[2]));
        mButtons.add(new MenuButton(MenuButton.buttonType.DASHBOARD, getString(R.string.menu_graphs),
                Utils.menuIconsResId[3]));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

    /**
     * Send button click events to the host activity.
     * @param v View View containing the button.
     * @param buttonId int Index of button clicked.
     */
    public void onButtonClick(View v, int buttonId) {
        mListener.onButtonSelected(buttonId);
    }

    /**
     * Create a button and add it to {@link #mMenuContainer}
     * @param menuButton MenuButton Instance of MenuButton class with the button data.
     */
    private void createButton(final MenuButton menuButton) {
        Button button = new Button(getActivity());
        button.setText(menuButton.getLabel());
        button.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        //button.setCompoundDrawablesWithIntrinsicBounds(0, menuButton.getImage(), 0, 0);

        Drawable drawable = ContextCompat.getDrawable(getContext(), menuButton.getImage());
        drawable.setBounds(0, 0, (int)(drawable.getIntrinsicWidth() * 1.0f),
                                 (int)(drawable.getIntrinsicHeight() * 1.0f));
        //ScaleDrawable sd = new ScaleDrawable(drawable, 0, scaleWidth, scaleHeight);
        //button.setCompoundDrawables(sd.getDrawable(), null, null, null);
        button.setCompoundDrawables(null, drawable, null, null);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v, menuButton.getType().ordinal());
            }
        });

        mMenuContainer.addView(button, mMenuItemLayoutParameters);
    }
}
