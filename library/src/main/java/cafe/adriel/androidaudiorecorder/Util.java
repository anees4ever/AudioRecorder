package cafe.adriel.androidaudiorecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioFormat;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;


public class Util {
    private static final Handler HANDLER = new Handler();

    private Util() {
    }

    public static void wait(int millis, Runnable callback){
        HANDLER.postDelayed(callback, millis);
    }

    public static omrecorder.AudioSource getMic(AudioSource source,
                                                AudioChannel channel,
                                                AudioSampleRate sampleRate) {
        return new omrecorder.AudioSource.Smart(
                source.getSource(),
                AudioFormat.ENCODING_PCM_16BIT,
                channel.getChannel(),
                sampleRate.getSampleRate());
    }

    public static boolean isBrightColor(int color) {
        if(android.R.color.transparent == color) {
            return true;
        }
        int [] rgb = {Color.red(color), Color.green(color), Color.blue(color)};
        int brightness = (int) Math.sqrt(
                rgb[0] * rgb[0] * 0.241 +
                rgb[1] * rgb[1] * 0.691 +
                rgb[2] * rgb[2] * 0.068);
        return brightness >= 200;
    }

    public static int getDarkerColor(int color) {
        float factor = 0.8f;
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }

    public static String formatSeconds(int seconds) {
        return getTwoDecimalsValue(seconds / 3600) + ":"
                + getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60);
    }

    public static String formatMilliSeconds(long milliSeconds) {
        int seconds= (int) milliSeconds / 1000;
        return getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60);
    }

    private static String getTwoDecimalsValue(int value) {
        if (value >= 0 && value <= 9) {
            return "0" + value;
        } else {
            return value + "";
        }
    }

    public static void copyFile(File from, File to) {
        copyFile(from, to, false);
    }
    public static void copyFile(File from, File to, boolean delete) {
        try {
            if(from.exists() && from.isFile()) {
                FileInputStream fi= new FileInputStream(from);
                FileOutputStream fo= new FileOutputStream(to);
                try {
                    FileChannel src= fi.getChannel();
                    FileChannel dst= fo.getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                } finally {
                    fi.close();
                    fo.close();
                }
                if(delete) {
                    from.delete();
                }
            }
        } catch(Exception e) {
            Log.e("copyFile", e.toString());
        }
    }

    public static void confirm(final Context context, final String title, final String message, final Runnable confirmed) {
        try {
            AlertDialog.Builder builder= new AlertDialog.Builder(context);
            builder.setTitle(title)
                    .setMessage(message)
                    .setNeutralButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                if (confirmed != null) {
                                    confirmed.run();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .create()
                    .show();
        } catch(Exception e) {
            Log.e("confirm", e.toString());
        }
    }
}