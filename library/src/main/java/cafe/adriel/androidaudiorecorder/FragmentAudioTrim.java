package cafe.adriel.androidaudiorecorder;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.AppendTrack;
import org.mp4parser.muxer.tracks.ClippedTrack;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class FragmentAudioTrim extends FragmentBase implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        View.OnClickListener {

    private Handler handler;
    private Runnable handlerCallback;
    private boolean AllowChanges= true;

    public int mCurrentItemPosition= -1;
    public PlayerItem mCurrentItem= null;

    private MediaPlayer mPlayer;
    private RangeSeekBar<Float> sbTrim;
    private ImageButton btnSRW, btnSFW;
    private ImageButton btnERW, btnEFW;
    private EditText edTrimFrom, edTrimTo;
    private TextView tvTo, tvPreview, tvPlayerTime, tvPlayerDuration;
    private ImageButton ibtnPlay, ibtnStop;
    private SeekBar sbPlayer;
    private ImageButton ibtnCancel, ibtnSave;

    public FragmentAudioTrim() {
        //Required empty public constructor
    }

    public static FragmentAudioTrim newInstance(Bundle args, boolean isBrightColor) {
        FragmentAudioTrim fragment = new FragmentAudioTrim();
        fragment.setArguments(args);
        fragment.mIsBrightColor= isBrightColor;
        fragment.iTag= 2;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView= inflater.inflate(R.layout.fragment_audio_trim, container, false);

        sbTrim= findViewById(R.id.sbTrim);
        btnSRW= findViewById(R.id.btnSRW);
        btnSFW= findViewById(R.id.btnSFW);
        btnERW= findViewById(R.id.btnERW);
        btnEFW= findViewById(R.id.btnEFW);
        edTrimFrom= findViewById(R.id.edTrimFrom);
        edTrimTo= findViewById(R.id.edTrimTo);
        tvTo= findViewById(R.id.tvTo);
        tvPreview= findViewById(R.id.tvPreview);
        tvPlayerTime= findViewById(R.id.tvPlayerTime);
        tvPlayerDuration= findViewById(R.id.tvPlayerDuration);
        ibtnPlay= findViewById(R.id.ibtnPlay);
        ibtnStop= findViewById(R.id.ibtnStop);
        sbPlayer= findViewById(R.id.sbPlayer);
        ibtnCancel= findViewById(R.id.ibtnCancel);
        ibtnSave= findViewById(R.id.ibtnSave);

        btnSRW.setOnClickListener(this);
        btnSFW.setOnClickListener(this);
        btnERW.setOnClickListener(this);
        btnEFW.setOnClickListener(this);

        ibtnPlay.setOnClickListener(this);
        ibtnStop.setOnClickListener(this);

        ibtnCancel.setOnClickListener(this);
        ibtnSave.setOnClickListener(this);

        TextWatcher textWatcher= new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                if (!AllowChanges) {
                    return;
                }
                try {
                    AllowChanges= false;
                    float x = Float.parseFloat(edTrimFrom.getText().toString().isEmpty() ? "0" : edTrimFrom.getText().toString());
                    float y = Float.parseFloat(edTrimTo.getText().toString().isEmpty() ? "0" : edTrimTo.getText().toString());
                    sbTrim.setSelectedMinValue(x);
                    sbTrim.setSelectedMaxValue(y);
                    reset();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    AllowChanges= true;
                }
            }
        };
        //edTrimFrom.addTextChangedListener(textWatcher);
        //edTrimTo.addTextChangedListener(textWatcher);

        RangeSeekBar.OnRangeSeekBarChangeListener<Float> rangeChangeListener= new RangeSeekBar.OnRangeSeekBarChangeListener<Float>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<Float> bar, Float minValue, Float maxValue) {
                seekbarChanged();
            }
        };
        sbTrim.setOnRangeSeekBarChangeListener(rangeChangeListener);

        if(mIsBrightColor) {
            btnSRW.setColorFilter(Color.BLUE);
            btnSFW.setColorFilter(Color.BLUE);
            btnERW.setColorFilter(Color.BLUE);
            btnEFW.setColorFilter(Color.BLUE);

            edTrimFrom.setTextColor(Color.BLUE);
            edTrimTo.setTextColor(Color.BLUE);
            tvTo.setTextColor(Color.BLUE);
            tvPreview.setTextColor(Color.BLUE);
            tvPlayerTime.setTextColor(Color.BLUE);
            tvPlayerDuration.setTextColor(Color.BLUE);

            ibtnPlay.setColorFilter(Color.BLUE);
            ibtnStop.setColorFilter(Color.BLUE);
            ibtnCancel.setColorFilter(Color.BLUE);
            ibtnSave.setColorFilter(Color.BLUE);
        }

        handler= new Handler();
        handlerCallback= new Runnable() {
            @Override
            public void run() {
                updateTimer();
            }
        };

        mCurrentItem= mListener.getPlayerItems().get(mCurrentItemPosition);

        sbTrim.setRangeValues((float) 0, (float) mCurrentItem.mDuration, (float)0.1);
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                try {
                    sbTrim.setSelectedMinValue(mCurrentItem.mTrimStart);
                    sbTrim.setSelectedMaxValue(mCurrentItem.mTrimEnd);

                    seekbarChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return mRootView;
    }

    @Override
    public void onClick(View v) {
        try {
            if (v.getId() == R.id.ibtnPlay) {
                onClickPlaying(v);
            } else if (v.getId() == R.id.ibtnStop) {
                onClickStop(v);
            }

            else if (v.getId() == R.id.btnSRW) {
                onClickChangeValues(1);
            } else if (v.getId() == R.id.btnSFW) {
                onClickChangeValues(2);
            } else if (v.getId() == R.id.btnERW) {
                onClickChangeValues(3);
            } else if (v.getId() == R.id.btnEFW) {
                onClickChangeValues(4);
            }


            else if (v.getId() == R.id.ibtnSave) {
                onClickSave(v);
            } else if (v.getId() == R.id.ibtnCancel) {
                onClickCancel(v);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickPlaying(View v){
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                if(isPlaying()){
                    mPlayer.pause();
                    handler.removeCallbacks(handlerCallback);
                } else {
                    start();
                }
            }
        });
    }
    public void onClickStop(View v){
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                reset();
            }
        });
    }
    public void onClickChangeValues(final int what) {
        try {
            switch (what) {
                case 1: sbTrim.setSelectedMinValue(sbTrim.getSelectedMinValue()-100); break;
                case 2: sbTrim.setSelectedMinValue(sbTrim.getSelectedMinValue()+100); break;
                case 3: sbTrim.setSelectedMaxValue(sbTrim.getSelectedMaxValue()-100); break;
                case 4: sbTrim.setSelectedMaxValue(sbTrim.getSelectedMaxValue()+100); break;
            }
            seekbarChanged();
        } catch (Exception e) {
            //ignore
        }
    }
    private void seekbarChanged() {
        if (!AllowChanges) {
            return;
        }
        try {
            AllowChanges= false;
            edTrimFrom.setText(Util.formatSeconds(sbTrim.getSelectedMinValue().intValue()/1000));
            edTrimTo.setText(Util.formatSeconds(sbTrim.getSelectedMaxValue().intValue()/1000));
            reset();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AllowChanges= true;
        }
    }
    public void onClickSave(View v) {
        try {
            stop();
            mCurrentItem.mTrimStart= sbTrim.getSelectedMinValue();
            mCurrentItem.mTrimEnd= sbTrim.getSelectedMaxValue();
            mListener.playAudio();
            mListener.cancelPreviewWarned();
        } catch (Exception e) {
            //ignore
        }
    }
    public void onClickCancel(View v){
        try {
            stop();
            mListener.playAudio();
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        reset();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        refreshPlayer();
    }

    public void refreshPlayer() {
        try {
            tvPlayerTime.setText(Util.formatSeconds(sbTrim.getSelectedMinValue().intValue() / 1000));
            tvPlayerDuration.setText(Util.formatSeconds(sbTrim.getSelectedMaxValue().intValue() / 1000));
            sbPlayer.setMax(sbTrim.getSelectedMaxValue().intValue()-sbTrim.getSelectedMinValue().intValue());
            mPlayer.seekTo(sbTrim.getSelectedMinValue().intValue());
        } catch (Exception e) {
            //ignore
        }
    }
    public void reset() {
        try {
            if(isPlaying()) {
                stop();
            }
            mPlayer= new MediaPlayer();
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setDataSource(mCurrentItem.mFileName);
            mPlayer.prepare();

            ibtnPlay.setImageResource(R.drawable.aar_ic_play);
            sbPlayer.setProgress(0);

            handler.removeCallbacks(handlerCallback);

            refreshPlayer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            if(mPlayer==null) {
                reset();
            }
            mPlayer.start();
            ibtnPlay.setImageResource(R.drawable.aar_ic_pause);
            updateTimer();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void stop(){
        try {
            ibtnPlay.setImageResource(R.drawable.aar_ic_play);
            handler.removeCallbacks(handlerCallback);
            if (mPlayer != null) {
                try {
                    mPlayer.stop();
                    mPlayer.reset();
                    mPlayer= null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying(){
        try {
            return mPlayer != null && mPlayer.isPlaying();
        } catch (Exception e){
            return false;
        }
    }

    public void updateTimer() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(isPlaying()) {
                        sbPlayer.setProgress(mPlayer.getCurrentPosition()-sbTrim.getSelectedMinValue().intValue());
                        tvPlayerTime.setText(Util.formatSeconds(mPlayer.getCurrentPosition()/1000));
                        if(mPlayer.getCurrentPosition()>=sbTrim.getSelectedMaxValue().intValue()) {
                            reset();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.postDelayed(handlerCallback, 100);
    }
}
