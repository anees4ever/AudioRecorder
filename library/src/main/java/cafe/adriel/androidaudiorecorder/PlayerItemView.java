package cafe.adriel.androidaudiorecorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerItemView implements View.OnClickListener {
    private final Context mContext;
    private final PlayerItem.PlayerHandler mPlayerHandler;

    public int mPosition;

    public View mRootView;
    public SeekBar sbPlayer;
    public TextView tvPlayerDuration;
    public ImageButton ibtnPlay, ibtnTrim, ibtnMoreOptions;

    public PlayerItemView(Context context, PlayerItem.PlayerHandler playerHandler) {
        mContext= context;
        mPlayerHandler= playerHandler;
        render();
    }

    private void render() {
        try {
            mRootView= LayoutInflater.from(mContext).inflate(R.layout.fragment_player_item, null);
            sbPlayer= mRootView.findViewById(R.id.sbPlayer);
            tvPlayerDuration = mRootView.findViewById(R.id.tvPlayerDuration);
            ibtnPlay = mRootView.findViewById(R.id.ibtnPlay);
            ibtnTrim = mRootView.findViewById(R.id.ibtnTrim);
            ibtnMoreOptions = mRootView.findViewById(R.id.ibtnMoreOptions);

            sbPlayer.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
            tvPlayerDuration.setTextColor(Color.BLUE);
            ibtnPlay.setColorFilter(Color.BLUE);
            ibtnTrim.setColorFilter(Color.BLUE);
            ibtnMoreOptions.setColorFilter(Color.BLUE);

            ibtnPlay.setOnClickListener(this);
            ibtnTrim.setOnClickListener(this);
            ibtnMoreOptions.setOnClickListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public View getView() {
        return mRootView;
    }

    public PlayerItemView setData(PlayerItem playerItem) {
        try {
            mPosition = playerItem.mPosition;
            int durn= getCorrectedMax(playerItem);
            tvPlayerDuration.setText(Util.formatMilliSeconds(durn));
            sbPlayer.setMax(durn);
            sbPlayer.setProgress(0);
            ibtnPlay.setImageResource(R.drawable.aar_ic_play);
            ibtnPlay.setTag("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public void setProgress(long duration, long position) {
        try {
            PlayerItem playerItem= getItemAt(mPosition);
            if(playerItem!=null && playerItem.mDuration==0) {
                if(playerItem.mTrimEnd==0) {
                    playerItem.mTrimEnd= duration;
                }
                playerItem.mDuration= duration;
            }
            int progress= getCorrectedProgress(playerItem, position);
            sbPlayer.setMax(getCorrectedMax(playerItem));
            sbPlayer.setProgress(progress);
            tvPlayerDuration.setText(Util.formatMilliSeconds(progress));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getCorrectedMax(PlayerItem playerItem) {
        return (int) (playerItem.mTrimEnd-playerItem.mTrimStart);
    }
    private int getCorrectedProgress(PlayerItem playerItem, long position) {
        return (int) (position-playerItem.mTrimStart);
    }

    public void paused() {
        try {
            ibtnPlay.setImageResource(R.drawable.aar_ic_play);
            ibtnPlay.setTag("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stoped() {
        try {
            PlayerItem playerItem= getItemAt(mPosition);
            sbPlayer.setProgress(0);
            tvPlayerDuration.setText(Util.formatMilliSeconds(getCorrectedMax(playerItem)));

            ibtnPlay.setImageResource(R.drawable.aar_ic_play);
            ibtnPlay.setTag("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean stopNow(long position) {
        try {
            return position >= getItemAt(mPosition).mTrimEnd;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        try {
            if (v.getId() == R.id.ibtnPlay) {
                onClickPlaying(v);
            } else if (v.getId() == R.id.ibtnTrim) {
                onClickTrim(v);
            } else if (v.getId() == R.id.ibtnMoreOptions) {
                onClickOptions(v);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickPlaying(View v){
        try {
            if (TextUtils.equals(ibtnPlay.getTag().toString(), "0")) {
                play();
                ibtnPlay.setImageResource(R.drawable.aar_ic_pause);
                ibtnPlay.setTag("1");
            } else {
                pause();
                ibtnPlay.setImageResource(R.drawable.aar_ic_play);
                ibtnPlay.setTag("0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickTrim(View v){
        pause();
        trim();
    }

    public void onClickOptions(View v){
        try {
            PopupMenu popupMenu= new PopupMenu(mContext, v);
            popupMenu.getMenuInflater().inflate(R.menu.aar_audio_player, popupMenu.getMenu());
            int count= getCount();
            if(count<=1) {
                popupMenu.getMenu().removeItem(R.id.action_move_up);
                popupMenu.getMenu().removeItem(R.id.action_move_down);
            } else if(mPosition==0) {
                popupMenu.getMenu().removeItem(R.id.action_move_up);
            } else if (mPosition==count-1) {
                popupMenu.getMenu().removeItem(R.id.action_move_down);
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        if(item.getItemId()==R.id.action_move_up) {
                            move(-1);
                        } else if(item.getItemId()==R.id.action_move_down) {
                            move(1);
                        } else if(item.getItemId()==R.id.action_remove) {
                            remove();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            popupMenu.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void play() {
        try {
            if(mPlayerHandler!=null) {
                mPlayerHandler.play(mPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void pause() {
        try {
            if(mPlayerHandler!=null) {
                mPlayerHandler.pause(mPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void trim() {
        try {
            if(mPlayerHandler!=null) {
                mPlayerHandler.trim(mPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void move(int direction) {
        try {
            if(mPlayerHandler!=null) {
                mPlayerHandler.move(mPosition, direction);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void remove() {
        try {
            if(mPlayerHandler!=null) {
                mPlayerHandler.remove(mPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private PlayerItem getItemAt(int position) {
        try {
            if(mPlayerHandler!=null) {
                return mPlayerHandler.getItemAt(position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private int getCount() {
        try {
            if(mPlayerHandler!=null) {
                return mPlayerHandler.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}
