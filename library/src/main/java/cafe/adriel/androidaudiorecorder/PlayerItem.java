package cafe.adriel.androidaudiorecorder;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.File;

import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class PlayerItem {
    public interface PlayerHandler {
        boolean isPlaying();
        void play(int position);
        void pause(int position);
        void trim(int position);
        void move(int position, int direction);
        void remove(int position);
        PlayerItem getItemAt(int position);
        int getCount();
    }
    private final Context mContext;

    public int mPosition= -1;
    public String mFileName= "";
    public File mFile= null;
    public long mDuration= 0;
    public float mTrimStart= 0;
    public float mTrimEnd= 0;

    public PlayerItem(Context context) {
        mContext= context;
    }
    public PlayerItem(Context context, int position, String fileName) {
        mContext= context;
    }
    public static PlayerItem from(PlayerItem from) {
        if(from==null) {
            return null;
        }
        PlayerItem to= new PlayerItem(from.mContext);
        to.mPosition= from.mPosition;
        to.mFileName= from.mFileName;
        to.mFile= new File(from.mFile.getAbsolutePath());
        to.mDuration= from.mDuration;
        to.mTrimStart= from.mTrimStart;
        to.mTrimEnd= from.mTrimEnd;
        return to;
    }

    public PlayerItem set(int position, String fileName) {
        try {
            mPosition= position;
            mFileName= fileName;
            mFile= new File(mFileName);
            updateDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public PlayerItem remove() {
        try {
            if(mFile.exists()) {
                mFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private void updateDuration() {
        try {
            final MediaPlayer player= new MediaPlayer();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    try { mDuration= mp.getDuration(); } catch (Exception e) { e.printStackTrace(); }
                    mTrimEnd= mDuration;
                    try { mp.stop(); } catch (Exception e) { e.printStackTrace(); }
                    try { mp.reset(); } catch (Exception e) { e.printStackTrace(); }
                    try { mp.release(); } catch (Exception e) { e.printStackTrace(); }
                }
            });
            player.setDataSource(mFileName);
            player.prepare();
            try {
                try { mDuration= player.getDuration(); } catch (Exception e) { e.printStackTrace(); }
                mTrimEnd= mDuration;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean trim() {
        return mTrimStart>0 || mTrimEnd < mDuration;
    }
    public boolean isWAV() {
        return mFileName.toLowerCase().endsWith(AudioFormat.WAV.getFormat());
    }
    public boolean isMP3() {
        return mFileName.toLowerCase().endsWith(AudioFormat.MP3.getFormat());
    }
}
