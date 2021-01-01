package cafe.adriel.androidaudiorecorder;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentRecorder extends FragmentBase {
    private Handler handler;
    private Runnable handlerCallback;
    private TextView tvStatus, tvTimer;

    public int recorderSecondsElapsed= 0;

    public FragmentRecorder() {
        //Required empty public constructor
    }

    public static FragmentRecorder newInstance(Bundle args, boolean isBrightColor) {
        FragmentRecorder fragment = new FragmentRecorder();
        fragment.setArguments(args);
        fragment.mIsBrightColor= isBrightColor;
        fragment.iTag= 0;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView= inflater.inflate(R.layout.fragment_recorder, container, false);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);

        if(mIsBrightColor) {
            tvStatus.setTextColor(Color.BLUE);
            tvTimer.setTextColor(Color.BLUE);
        }

        handler= new Handler();
        handlerCallback= new Runnable() {
            @Override
            public void run() {
                updateTimer();
            }
        };
        reset();
        return mRootView;
    }

    public void reset() {
        try {
            recorderSecondsElapsed= 0;
            tvStatus.setText("");
            tvTimer.setText(Util.formatSeconds(recorderSecondsElapsed));
            handler.removeCallbacks(handlerCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void stop() {
        try {
            tvStatus.setText("");//R.string.aar_stopped
            tvTimer.setText(Util.formatSeconds(recorderSecondsElapsed));
            handler.removeCallbacks(handlerCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void recording() {
        try {
            tvStatus.setText(R.string.aar_recording);
            tvTimer.setText(Util.formatSeconds(recorderSecondsElapsed));
            updateTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void paused() {
        try {
            tvStatus.setText(R.string.aar_paused);
            tvTimer.setText(Util.formatSeconds(recorderSecondsElapsed));
            handler.removeCallbacks(handlerCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void updateTimer() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvTimer.setText(Util.formatSeconds(recorderSecondsElapsed));
                    recorderSecondsElapsed++;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.postDelayed(handlerCallback, 1000);
    }
}
