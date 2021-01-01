package cafe.adriel.androidaudiorecorder;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class FragmentPlayer extends FragmentBase implements View.OnClickListener,
        PlayerItem.PlayerHandler, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {
    private Handler handlerTimer;
    private Runnable handlerTimerCallback;
    private MediaPlayer mPlayer;

    private LinearLayout llPlayerItems;
    private ImageButton ibtnPlayAll, ibtnClearAll;
    private TextView tvPlayAll, tvClearAll, tvRecordingCount;


    private int mCurrentPosition= -1;
    private PlayerItemView mCurrentItem= null;

    ArrayList<PlayerItem> mPlayerItems= null;
    ArrayList<PlayerItemView> mPlayerItemViews= null;

    boolean playingInSequence= false;

    public FragmentPlayer() {
        //Required empty public constructor
    }

    public static FragmentPlayer newInstance(Bundle args, boolean isBrightColor) {
        FragmentPlayer fragment = new FragmentPlayer();
        fragment.setArguments(args);
        fragment.mIsBrightColor= isBrightColor;
        fragment.iTag= 1;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView= inflater.inflate(R.layout.fragment_player, container, false);

        llPlayerItems = findViewById(R.id.llPlayerItems);
        ibtnPlayAll = findViewById(R.id.ibtnPlayAll);
        ibtnClearAll = findViewById(R.id.ibtnClearAll);
        tvPlayAll = findViewById(R.id.tvPlayAll);
        tvClearAll = findViewById(R.id.tvClearAll);
        tvRecordingCount = findViewById(R.id.tvRecordingCount);

        ibtnPlayAll.setOnClickListener(this);
        tvPlayAll.setOnClickListener(this);
        ibtnClearAll.setOnClickListener(this);
        tvClearAll.setOnClickListener(this);

        if(mIsBrightColor) {
            ibtnPlayAll.setColorFilter(Color.BLUE);
            ibtnClearAll.setColorFilter(Color.BLUE);
            tvPlayAll.setTextColor(Color.BLUE);
            tvClearAll.setTextColor(Color.BLUE);
            tvRecordingCount.setTextColor(Color.BLUE);
        }

        handlerTimer= new Handler();
        handlerTimerCallback= new Runnable() {
            @Override
            public void run() {
                updateTimer();
            }
        };

        refreshView();
        return mRootView;
    }

    @Override
    public void onClick(View v) {
        try {
            if (v.getId() == R.id.ibtnPlayAll || v.getId() == R.id.tvPlayAll) {
                playInSequence();
            } else if (v.getId() == R.id.ibtnClearAll || v.getId() == R.id.tvClearAll) {
                confirmAndClearAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playInSequence() {
        try {
            if (mCurrentItem!=null) {
                if(isPlaying()) {
                    pause(mCurrentPosition);
                }
                mCurrentItem.stoped();
            }
            playingInSequence= true;
            playAt(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmAndClearAll() {
        try {
            mListener.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshView() {
        try {
            mPlayerItems= mListener.getPlayerItems();
            mPlayerItemViews= new ArrayList<>();
            llPlayerItems.removeAllViews();
            tvRecordingCount.setText(String.format(Locale.getDefault(), "%d recordings", mPlayerItems.size()));
            for(PlayerItem playerItem: mPlayerItems) {
                PlayerItemView playerItemView= new PlayerItemView(mContext, this);
                playerItemView.setData(playerItem);
                llPlayerItems.addView(playerItemView.getView());
                mPlayerItemViews.add(playerItemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTimer() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(isPlaying() && mCurrentPosition >=0 && mCurrentItem != null) {
                        long curPos= mPlayer.getCurrentPosition();
                        mCurrentItem.setProgress(mPlayer.getDuration(), curPos);
                        if(mCurrentItem.stopNow(curPos)) {
                            mCurrentItem.stoped();
                            if(playingInSequence) {
                                if(mCurrentPosition<mPlayerItemViews.size()-1) {
                                    playAt(mCurrentPosition+1);
                                } else {
                                    pause(mCurrentPosition);
                                }
                            } else {
                                pause(mCurrentPosition);
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        handlerTimer.postDelayed(handlerTimerCallback, 100);
    }


    private void refreshPlayerItemsPosition() {
        try {
            for(int i=0; i<mPlayerItems.size(); i++) {
                mPlayerItems.get(i).mPosition= i;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            for(int i=0; i<mPlayerItemViews.size(); i++) {
                mPlayerItemViews.get(i).mPosition= i;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAt(int position) {
        try {
            if(isPlaying()) {
                pause(mCurrentPosition);
            }
            mCurrentPosition= position;
            mCurrentItem= mPlayerItemViews.get(position);
            PlayerItem playerItem= mPlayerItems.get(mCurrentPosition);
            mPlayer= new MediaPlayer();
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setDataSource(playerItem.mFileName);
            mPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            PlayerItem playerItem= mPlayerItems.get(mCurrentPosition);
            int progress= mCurrentItem!=null?mCurrentItem.sbPlayer.getProgress():0;
            if(progress>0 && progress > playerItem.mTrimStart) {
                mp.seekTo(progress);
            } else {
                mp.seekTo((int) playerItem.mTrimStart);
            }
            mp.start();
            updateTimer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            handlerTimer.removeCallbacks(handlerTimerCallback);
            try {
                if(mCurrentItem!=null) {
                    mCurrentItem.stoped();
                }
                if(playingInSequence) {
                    if(mCurrentPosition<mPlayerItemViews.size()-1) {
                        playAt(mCurrentPosition+1);
                        return;
                    }
                }
                mCurrentItem= null;
                mCurrentPosition= -1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resume() {
        try {
            if(mPlayer!=null) {
                mPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPlaying() {
        try {
            return mPlayer != null && mPlayer.isPlaying();
        } catch (Exception e){
            return false;
        }
    }

    @Override
    public void play(int position) {
        try {
            playingInSequence= false;
            if(position>=0 && position==mCurrentPosition) {
                resume();
            } else {
                try {
                    if(mCurrentItem!=null) {
                        mCurrentItem.paused();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                playAt(position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause(int position) {
        try {
            playingInSequence= false;
            if (isPlaying()) {
                mPlayer.pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void trim(int position) {
        try {
            playingInSequence= false;
            mListener.trimAudio(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void move(int position, int direction) {
        try {
            int from= -1;
            int to= -1;
            if(direction==-1) {
                if(position>0) {
                    from= position;
                    to= position-1;
                } else {
                    return;
                }
            } else {
                if(position<mPlayerItems.size()-1) {
                    from= position;
                    to= position+1;
                } else {
                    return;
                }
            }
            if(from>=0 && to>=0) {
                PlayerItem itemFrom= PlayerItem.from(mPlayerItems.get(from));
                PlayerItem itemTo= PlayerItem.from(mPlayerItems.get(to));
                mPlayerItems.remove(from);
                mPlayerItems.add(from, itemTo);

                mPlayerItems.remove(to);
                mPlayerItems.add(to, itemFrom);

                refreshPlayerItemsPosition();
                refreshView();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(final int position) {
        try {
            Util.confirm(mContext, "Remove?", "Are you sure to remove this recording?", new Runnable() {
                @Override
                public void run() {
                    try {
                        mPlayerItems.get(position).remove();
                        mPlayerItems.remove(position);
                        llPlayerItems.removeViewAt(position);
                        mPlayerItemViews.remove(position);

                        refreshPlayerItemsPosition();
                        tvRecordingCount.setText(String.format(Locale.getDefault(), "%d recordings", mPlayerItems.size()));
                        if(mPlayerItems.size()==0) {
                            mListener.recordAudio();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerItem getItemAt(int position) {
        return mPlayerItems!=null && mPlayerItems.size() > 0 &&
                position >= 0 && position<= mPlayerItems.size()-1?mPlayerItems.get(position):null;
    }

    @Override
    public int getCount() {
        return mPlayerItems.size();
    }
}
