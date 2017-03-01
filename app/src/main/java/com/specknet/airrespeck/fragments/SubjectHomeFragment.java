package com.specknet.airrespeck.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.Constants;

/**
 * Created by Darius on 08.02.2017.
 */

public class SubjectHomeFragment extends BaseFragment {

    private ImageView connectedStatusRESpeck;
    private ImageView connectedStatusAirspeck;

    /**
     * Required empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubjectHomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_subject_home, container, false);

        // Load connection symbols
        connectedStatusRESpeck = (ImageView) view.findViewById(R.id.connected_status_respeck);
        connectedStatusAirspeck = (ImageView) view.findViewById(R.id.connected_status_airspeck);

        mConnectingLayout = (LinearLayout) view.findViewById(R.id.connecting_layout);

        return view;
    }

    @Override
    public int getIcon() {
        return Constants.MENU_ICON_HOME;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void updateRESpeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            // "Flash" with symbol when updating to indicate data coming in
            connectedStatusRESpeck.setImageResource(R.drawable.vec_wireless);
            connectedStatusRESpeck.setVisibility(View.INVISIBLE);

            Log.i("DF", "connection symbol update");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectedStatusRESpeck.setVisibility(View.VISIBLE);
                }
            }, 100);

        } else {
            connectedStatusRESpeck.setImageResource(R.drawable.vec_xmark);
        }
    }

    public void updateAirspeckConnectionSymbol(boolean isConnected) {
        if (isConnected) {
            // "Flash" with symbol when updating to indicate data coming in
            connectedStatusAirspeck.setImageResource(R.drawable.vec_wireless);
            connectedStatusAirspeck.setVisibility(View.INVISIBLE);

            Log.i("DF", "connection symbol update");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectedStatusAirspeck.setVisibility(View.VISIBLE);
                }
            }, 100);

        } else {
            connectedStatusAirspeck.setImageResource(R.drawable.vec_xmark);
        }
    }
}


