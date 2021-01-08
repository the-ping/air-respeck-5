package com.specknet.airrespeck.fragments;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.specknet.airrespeck.R;
import com.specknet.airrespeck.utils.Constants;
import com.specknet.airrespeck.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioSubmission extends Fragment {

    public AudioSubmission() {
        // Required empty public constructor
    }

    private Button play, submit, record, cancel;
    private MediaRecorder myAudioRecorder;
    private String outputFile;
    private MediaPlayer mediaPlayer;
    private ImageView image;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_audio_submission, container, false);

        record = (Button) view.findViewById(R.id.audiorecord);
        play = (Button) view.findViewById(R.id.audioplay);
        submit = (Button) view.findViewById(R.id.audiosubmit);
        cancel = (Button) view.findViewById(R.id.audiocancel);
        image = (ImageView) view.findViewById(R.id.audioimg);

        submit.setEnabled(false);
        play.setEnabled(false);

        String directory = Utils.getInstance().getDataDirectory(getActivity()) +
                Constants.MEDIA_DIRECTORY_NAME;
        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        outputFile = directory.concat(filename.concat(".3gp"));

        myAudioRecorder = new MediaRecorder();

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (play.getText().equals("Play")) {
                    try {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.stop();
                                record.setEnabled(true);
                                play.setText("Play");
                                submit.setEnabled(true);
                                cancel.setEnabled(true);
                                image.setImageResource(R.drawable.ic_stop_black_24dp);
                            }

                        });
                        mediaPlayer.setDataSource(outputFile);
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        Toast.makeText(getActivity().getApplicationContext(), "Playing Audio", Toast.LENGTH_SHORT).show();
                        record.setEnabled(false);
                        play.setText("Stop Playing");
                        submit.setEnabled(false);
                        image.setImageResource(R.drawable.ic_play_arrow_black_24dp);

                    } catch (Exception e) {
                        // make something
                    }
                } else if (play.getText().equals("Stop Playing")){
                    mediaPlayer.stop();
                    record.setEnabled(true);
                    play.setText("Play");
                    submit.setEnabled(true);
                    cancel.setEnabled(true);
                    image.setImageResource(R.drawable.ic_stop_black_24dp);
                    Toast.makeText(getActivity().getApplicationContext(), "Audio Stopped", Toast.LENGTH_SHORT).show();
                }
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (record.getText().equals("Record")) {
                    try {
                        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                        myAudioRecorder.setOutputFile(outputFile);
                        myAudioRecorder.prepare();
                        myAudioRecorder.start();
                    } catch (IllegalStateException ise) {
                        // make something ...
                    } catch (IOException e) {
                        // make something...
                    }
                    record.setText("Stop Recording");
                    Toast.makeText(getContext().getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
                    play.setEnabled(false);
                    submit.setEnabled(false);
                    cancel.setEnabled(false);
                    image.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);


                } else if (record.getText().equals("Stop Recording")){
                    myAudioRecorder.stop();
                    record.setText("Record");
                    Toast.makeText(getContext().getApplicationContext(), "Recording completed", Toast.LENGTH_SHORT).show();
                    play.setEnabled(true);
                    submit.setEnabled(true);
                    cancel.setEnabled(true);
                    image.setImageResource(R.drawable.ic_stop_black_24dp);

                }
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity().getApplicationContext(), "Audio submitted successfully", Toast.LENGTH_SHORT).show();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Apagar o ficheiro de som se ele existir.
                File file = new File(outputFile);
                file.delete();
                getActivity().getSupportFragmentManager().popBackStack();

            }
        });

        // Inflate the layout for this fragment
        return view;
    }
}
