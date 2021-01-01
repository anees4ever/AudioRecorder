package cafe.adriel.androidaudiorecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class AudioBatchProcessor {
    private final Context mContext;
    private int mTotalFiles= 0;
    private int mCurrentConverting= -1;

    private File mDestination, mMergedFile;

    private ArrayList<PlayerItem> mPlayerItems;
    private ArrayList<PlayerItem> mAACFiles;
    private ProgressDialog progressDialog;
    private Handler handlerConvertionProgress;
    private Runnable callbackConvertionProgress;
    private Runnable mOnFinish;

    public AudioBatchProcessor(Context context) {
        mContext= context;
        progressDialog= new ProgressDialog(mContext);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        mPlayerItems= new ArrayList<>();
        mAACFiles= new ArrayList<>();
        mMergedFile= AudioRecorderActivity.getDuplicateFile(AudioFormat.MP4);
        handlerConvertionProgress= new Handler();
        callbackConvertionProgress= new Runnable() {
            @Override
            public void run() {
                checkConvertionProgress();
            }
        };
    }
    public AudioBatchProcessor setFiles(ArrayList<PlayerItem> playerItems) {
        mPlayerItems= playerItems;
        return this;
    }
    public AudioBatchProcessor setDestination(File destination) {
        mDestination= destination;
        return this;
    }
    public AudioBatchProcessor setOnFinish(Runnable onFinish) {
        mOnFinish= onFinish;
        return this;
    }
    public void start() {
        try {
            setProgress("Preparing");
            mTotalFiles= mPlayerItems.size();
            mCurrentConverting= -1;
            mAACFiles.clear();
            try { handlerConvertionProgress.removeCallbacks(callbackConvertionProgress); } catch (Exception e) { e.printStackTrace(); }
            if(mTotalFiles==1 && !mPlayerItems.get(0).trim()) {
                if(mPlayerItems.get(0).isMP3()) {
                    mPlayerItems.get(0).mFile.renameTo(mDestination);
                    finishProcess();
                } else {
                    convertToMP3(mPlayerItems.get(0).mFile);
                }
            } else {
                checkConvertionProgress();
                startConvertAAC();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setProgress(String progressStr) {
        try {
            if(progressDialog==null) {
                return;
            }
            progressDialog.setMessage(progressStr);
            if(!progressDialog.isShowing()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void hideProgress() {
        try {
            if(progressDialog!=null) {
                progressDialog.dismiss();
            }
            progressDialog= null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkConvertionProgress() {
        try {
            String progressStr= String.format(Locale.getDefault(),"Converting Recorded Files: %d/%d", mCurrentConverting+1, mTotalFiles);
            setProgress(progressStr);
            handlerConvertionProgress.postDelayed(callbackConvertionProgress, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void startConvertAAC() {
        try {
            if(mCurrentConverting >= mPlayerItems.size() - 1) {
                finishConvertionAAC();
            } else {
                mCurrentConverting++;
                convertToAAC(mPlayerItems.get(mCurrentConverting));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void convertToAAC(final PlayerItem playerItem) {
        try {
            AudioUtils.Converter converter= new AudioUtils.Converter(mContext);
            converter.setFile(playerItem.mFile)
                    .setDelete(false)
                    .setShowProgress(false)
                    .setListener(new AudioUtils.Listener() {
                        @Override
                        public void onStart() {}
                        @Override
                        public void onFinish(File mCompletedFile) {
                            try {
                                PlayerItem newPlayerItem= new PlayerItem(mContext);
                                newPlayerItem.mPosition= mAACFiles.size();
                                newPlayerItem.mFileName= mCompletedFile.getAbsolutePath();
                                newPlayerItem.mFile= new File(newPlayerItem.mFileName);
                                newPlayerItem.mTrimStart= playerItem.mTrimStart;
                                newPlayerItem.mTrimEnd= playerItem.mTrimEnd;
                                newPlayerItem.mDuration= playerItem.mDuration;
                                mAACFiles.add(newPlayerItem);
                                startConvertAAC();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishConvertionAAC() {
        handlerConvertionProgress.removeCallbacks(callbackConvertionProgress);
        mergeAudioFiles();
    }

    private void mergeAudioFiles() {
        try {
            setProgress("Merging Recorded Files");
            List<Track> audioTracks = new LinkedList<>();
            for(PlayerItem playerItem: mAACFiles) {
                Track track= new AACTrackImpl(new FileDataSourceImpl(playerItem.mFile));
                if(playerItem.trim()) {
                    long startSample = findNextSyncSample(track, playerItem.mTrimStart);
                    long endSample = findNextSyncSample(track, playerItem.mTrimEnd);
                    audioTracks.add(new ClippedTrack(track, startSample, endSample));
                } else {
                    audioTracks.add(track);
                }
            }
            Movie movie = new Movie();
            movie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            Container out = new DefaultMp4Builder().build(movie);
            FileChannel fc = new RandomAccessFile(mMergedFile, "rw").getChannel();
            out.writeContainer(fc);
            fc.close();

            convertToMP3(mMergedFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void convertToMP3(File file) {
        try {
            setProgress("Converting Merged File");

            AudioUtils.Converter converter= new AudioUtils.Converter(mContext);
            converter.setFile(file)
                    .setDelete(false)
                    .setShowProgress(false)
                    .setFormat(AudioFormat.MP3)
                    .setListener(new AudioUtils.Listener() {
                        @Override
                        public void onStart() {}
                        @Override
                        public void onFinish(File mCompletedFile) {
                            try {
                                mCompletedFile.renameTo(mDestination);
                                finishProcess();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishProcess() {
        try {
            hideProgress();
            for(PlayerItem playerItem: mAACFiles) {
                if(playerItem.mFile.exists()) {
                    playerItem.mFile.delete();
                }
            }
            if(mMergedFile.exists()) {
                mMergedFile.delete();
            }
            if(mOnFinish!=null) {
                mOnFinish.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long findNextSyncSample(Track track, double cutHereValue) {
        long timeScale= track.getTrackMetaData().getTimescale();
        long currentSample = 0;
        double currentTime = 0;
        long[] durations = track.getSampleDurations();
        long[] syncSamples = track.getSyncSamples();
        for (int i = 0; i < durations.length; i++) {
            long delta = durations[i];
            double cutHere= cutHereValue / delta;
            if ((syncSamples == null || syncSamples.length > 0 || Arrays.binarySearch(syncSamples, currentSample + 1) >= 0)
                    && currentTime > cutHere) {
                return i;
            }
            currentTime += (double) delta / (double) timeScale;
            currentSample++;
        }
        return currentSample;
    }
}