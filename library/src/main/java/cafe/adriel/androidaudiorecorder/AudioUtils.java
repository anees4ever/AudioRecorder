package cafe.adriel.androidaudiorecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.AppendTrack;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class AudioUtils {
    public static ProgressDialog progressDialog= null;
    public static void initProgress(Context context) {
        try {
            if(progressDialog!=null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog= null;
            }
            progressDialog= new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void setProgress(String progressStr) {
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
    public static void hideProgress() {
        try {
            if(progressDialog!=null) {
                progressDialog.dismiss();
            }
            progressDialog= null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface Listener {
        void onStart();
        void onFinish(File mCompletedFile);
    }
    public static class Converter {
        WeakReference<Context> mContext;
        File mFile;
        Listener mListener;
        String progressLabel= "Processing recorded file";
        AudioFormat mFormat= AudioFormat.AAC;
        boolean mDeleteAfter= true;
        boolean mProgress= false;
        public Converter(Context context) {
            mContext= new WeakReference<>(context);
        }
        public Converter setFile(File file) {
            mFile= file;
            return this;
        }
        public Converter setListener(Listener listener) {
            mListener= listener;
            return this;
        }
        public Converter setLabel(String label) {
            progressLabel= label;
            return this;
        }
        public Converter setFormat(AudioFormat format) {
            mFormat= format;
            return this;
        }
        public Converter setDelete(boolean deleteAfter) {
            mDeleteAfter= deleteAfter;
            return this;
        }
        public Converter setShowProgress(boolean progress) {
            mProgress= progress;
            return this;
        }
        private void convert() {
            try {
                if(mProgress) {
                    initProgress(mContext.get());
                    setProgress(progressLabel);
                }
                if(mListener!=null) {
                    mListener.onStart();
                }
                AndroidAudioConverter.with(mContext.get())
                        .setFile(mFile)
                        .setFormat(mFormat)
                        .setCallback(new IConvertCallback() {
                            @Override
                            public void onSuccess(File convertedFile) {
                                if(mDeleteAfter) {
                                    mFile.delete();
                                }
                                if(mProgress) {
                                    hideProgress();
                                }
                                if (mListener != null) {
                                    mListener.onFinish(convertedFile);
                                }
                            }

                            @Override
                            public void onFailure(Exception error) {
                                if(mProgress) {
                                    hideProgress();
                                }
                                Toast.makeText(mContext.get(), error.toString(), Toast.LENGTH_SHORT).show();
                                if (mListener != null) {
                                    mListener.onFinish(null);
                                }
                            }
                        })
                        .convert();
            } catch (Exception e) {
                if(mProgress) {
                    hideProgress();
                }
                Toast.makeText(mContext.get(), e.toString(), Toast.LENGTH_SHORT).show();
                if (mListener != null) {
                    mListener.onFinish(null);
                }
            }
        }

        public void run() {
            convert();
        }
    }

    public static class Merger extends AsyncTask<Void, Void, Void> {
        WeakReference<Context> mContext;
        ArrayList<File> mFiles;
        File mOutputFile, mDestination;
        Listener mListener;
        String progressLabel= "Merging Recorded file";
        public Merger(Context context) {
            mContext= new WeakReference<>(context);
            mFiles = new ArrayList<>();
            mOutputFile= new File(mContext.get().getExternalCacheDir(), "/operating_audio_copy.mp4");
        }
        public Merger setFiles(File... files) {
            mFiles.addAll(Arrays.asList(files));
            return this;
        }
        public Merger setDestination(File file) {
            mDestination= file;
            return this;
        }
        public Merger setListener(Listener listener) {
            mListener= listener;
            return this;
        }
        public Merger setLabel(String label) {
            progressLabel= label;
            return this;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            initProgress(mContext.get());
            setProgress(progressLabel);
            if(mListener!=null) {
                mListener.onStart();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if(mOutputFile.exists()) {
                    mOutputFile.delete();
                }
                List<Track> audioTracks = new LinkedList<>();
                for(File file: mFiles) {
                    if (file.exists()) {
                        Track audio= new AACTrackImpl(new FileDataSourceImpl(file.getAbsolutePath()));
                        audioTracks.add(audio);
                    }
                }
                Movie result = new Movie();
                if (!audioTracks.isEmpty()) {
                    result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                    Container out = new DefaultMp4Builder().build(result);
                    FileChannel fc = new RandomAccessFile(mOutputFile, "rw").getChannel();
                    out.writeContainer(fc);
                    fc.close();
                }
                //Movie movie= MovieCreator.build(mOutputFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            hideProgress();
            convert();
        }

        private void convert() {
            try {
                if(mOutputFile.exists()) {
                    AudioUtils.Converter converter= new AudioUtils.Converter(mContext.get());
                    converter.setFile(mOutputFile)
                            .setListener(new AudioUtils.Listener() {
                                @Override
                                public void onStart() {}
                                @Override
                                public void onFinish(File mCompletedFile) {
                                    mOutputFile.delete();
                                    moveFile(mCompletedFile);
                                }
                            })
                            .setLabel("Processing merged file")
                            .run();
                } else {
                    if(mListener!=null) {
                        mListener.onFinish(null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void moveFile(File mCompletedFile) {
            try {
                if(mCompletedFile.exists()) {
                    if(mDestination.exists()) {
                        mDestination.delete();
                    }
                    Util.copyFile(mCompletedFile, mDestination, true);
                    if(mListener!=null) {
                        mListener.onFinish(mDestination);
                    }
                } else {
                    if(mListener!=null) {
                        mListener.onFinish(null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            executeOnExecutor(Executors.newSingleThreadExecutor());
        }
    }
}
