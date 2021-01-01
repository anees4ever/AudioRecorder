package cafe.adriel.androidaudiorecorder;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cleveroad.audiovisualization.GLAudioVisualizationView;

import java.io.File;
import java.util.ArrayList;

import cafe.adriel.androidaudioconverter.model.AudioFormat;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import omrecorder.AudioChunk;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;

public class AudioRecorderActivity extends AppCompatActivity
        implements PullTransport.OnAudioChunkPulledListener,
                    FragmentInteractionListener {
    public static String RECORDING_DIR= "";
    private String mDestinationMP3File;
    private String mRecordingWAVFile;

    private File mfDestinationMP3File;
    private File mfRecordingWAVFile;

    private AudioSource mSource;
    private AudioChannel mChannel;
    private AudioSampleRate mSampleRate;
    private int mColor;
    private boolean mAutoStart;
    private boolean mKeepDisplayOn;

    private Recorder mRecorder;
    private VisualizerHandler mVisualizerHandler;

    private GLAudioVisualizationView vwAudioVisualizer;
    private ImageButton ibtnRestart, ibtnRecord, ibtnSave;

    private FragmentRecorder fragmentRecorder;
    private FragmentPlayer fragmentPlayer;
    private FragmentAudioTrim fragmentAudioTrim;
    private int mCurrentFragmentTag= 0;

    private boolean mIsRecording;
    private boolean mPreviewWarned= false;

    private ArrayList<PlayerItem> mPlayerItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aar_activity_audio_recorder);

        try {
            if(savedInstanceState != null) {
                mDestinationMP3File = savedInstanceState.getString(AndroidAudioRecorder.EXTRA_FILE_PATH);
                mSource = (AudioSource) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_SOURCE);
                mChannel = (AudioChannel) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_CHANNEL);
                mSampleRate = (AudioSampleRate) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
                mColor = savedInstanceState.getInt(AndroidAudioRecorder.EXTRA_COLOR);
                mAutoStart = savedInstanceState.getBoolean(AndroidAudioRecorder.EXTRA_AUTO_START);
                mKeepDisplayOn = savedInstanceState.getBoolean(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON);
            } else {
                mDestinationMP3File = getIntent().getStringExtra(AndroidAudioRecorder.EXTRA_FILE_PATH);
                mSource = (AudioSource) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SOURCE);
                mChannel = (AudioChannel) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_CHANNEL);
                mSampleRate = (AudioSampleRate) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
                mColor = getIntent().getIntExtra(AndroidAudioRecorder.EXTRA_COLOR, Color.BLACK);
                mAutoStart = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_AUTO_START, false);
                mKeepDisplayOn = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON, false);
            }

            if(mKeepDisplayOn){
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            if (getSupportActionBar() != null) {
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setElevation(0);
                getSupportActionBar().setBackgroundDrawable(
                        new ColorDrawable(Util.getDarkerColor(mColor)));
                getSupportActionBar().setHomeAsUpIndicator(
                        ContextCompat.getDrawable(this, R.drawable.aar_ic_menu_clear));
            }

            vwAudioVisualizer = new GLAudioVisualizationView.Builder(this)
                    .setLayersCount(1)
                    .setWavesCount(6)
                    .setWavesHeight(R.dimen.aar_wave_height)
                    .setWavesFooterHeight(R.dimen.aar_footer_height)
                    .setBubblesPerLayer(20)
                    .setBubblesSize(R.dimen.aar_bubble_size)
                    .setBubblesRandomizeSize(true)
                    .setBackgroundColor(Util.getDarkerColor(mColor))
                    .setLayerColors(new int[]{mColor})
                    .build();

            RelativeLayout rlContainer = (RelativeLayout) findViewById(R.id.rlContainer);

            ibtnSave = (ImageButton) findViewById(R.id.ibtnSave);
            ibtnRestart = (ImageButton) findViewById(R.id.ibtnRestart);
            ibtnRecord = (ImageButton) findViewById(R.id.ibtnRecord);

            rlContainer.setBackgroundColor(Util.getDarkerColor(mColor));
            rlContainer.addView(vwAudioVisualizer, 0);

            initRecordingDirectory();

            boolean isBrightColor= Util.isBrightColor(mColor);
            if(isBrightColor) {
                ContextCompat.getDrawable(this, R.drawable.aar_ic_menu_clear)
                        .setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                ContextCompat.getDrawable(this, R.drawable.aar_ic_menu_check)
                        .setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                ibtnRestart.setColorFilter(Color.BLUE);
                ibtnRecord.setColorFilter(Color.BLUE);
                ibtnSave.setColorFilter(Color.BLUE);
            }
            fragmentRecorder= FragmentRecorder.newInstance(null, isBrightColor);
            fragmentPlayer= FragmentPlayer.newInstance(null, isBrightColor);
            fragmentAudioTrim= FragmentAudioTrim.newInstance(null, isBrightColor);

            mPlayerItems= new ArrayList<>();

            boolean drawPlayer= false;
            try {
                mRecordingWAVFile= RECORDING_DIR + "/recorded_audio.wav";
                mfRecordingWAVFile= new File(mRecordingWAVFile);
                if(mfRecordingWAVFile.exists()) {
                    mfRecordingWAVFile.delete();
                }
                mfDestinationMP3File= new File(mDestinationMP3File);
                if(mfDestinationMP3File.exists()) {
                    File mDuplicateFile= getDuplicateFile(AudioFormat.MP3);
                    Util.copyFile(mfDestinationMP3File, mDuplicateFile);
                    PlayerItem playerItem= new PlayerItem(this);
                    playerItem.set(mPlayerItems.size(), mDuplicateFile.getAbsolutePath());
                    mPlayerItems.add(playerItem);
                    drawPlayer= true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(drawPlayer) {
                playAudio();
            } else {
                recordAudio();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void drawFragment(FragmentBase fragment) {
        try {
            mCurrentFragmentTag= fragment.iTag;
            FragmentManager fragmentManager= getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_content, fragment);
            fragmentTransaction.commit();
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(mAutoStart && !mIsRecording){
            onClickRecording(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            vwAudioVisualizer.onResume();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        try {
            if (mIsRecording) {
                pauseRecording();
            } else {
                if(mCurrentFragmentTag==1) {
                    fragmentPlayer.pause(-1);
                } else if (mCurrentFragmentTag==2) {
                    fragmentAudioTrim.stop();
                } else {
                    fragmentRecorder.stop();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            vwAudioVisualizer.onPause();
        } catch (Exception e){
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        restartRecording();
        setResult(RESULT_CANCELED);
        try {
            vwAudioVisualizer.release();
        } catch (Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(AndroidAudioRecorder.EXTRA_FILE_PATH, mDestinationMP3File);
        outState.putInt(AndroidAudioRecorder.EXTRA_COLOR, mColor);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.aar_audio_recorder, menu);
        //saveMenuItem = menu.findItem(R.id.action_save);
        //saveMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.aar_ic_check));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAudioChunkPulled(AudioChunk audioChunk) {
        float amplitude = mIsRecording ? (float) audioChunk.maxAmplitude() : 0f;
        mVisualizerHandler.onDataReceived(amplitude);
    }

    public void onClickRecording(View v) {
        if(mCurrentFragmentTag==2) {
            Toast.makeText(this, "Please complete editing", Toast.LENGTH_SHORT).show();
        } else {
            if (mIsRecording) {
                pauseRecording();
            } else {
                resumeRecording();
            }
        }
    }
    public void onClickRestart(View v){
        Util.confirm(this, "Reset?", "Are you sure to clear all recording?", new Runnable() {
            @Override
            public void run() {
                restartRecording();
            }
        });
    }
    public void onClickSave(View v) {
        try {
            if(mCurrentFragmentTag==0) {
                Toast.makeText(this, "Please record an audio", Toast.LENGTH_SHORT).show();
            } else if(mCurrentFragmentTag==2) {
                Toast.makeText(this, "Please complete editing", Toast.LENGTH_SHORT).show();
            } else {
                selectAudio();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resumeRecording() {
        try {
            mIsRecording = true;
            ibtnSave.setVisibility(View.GONE);

            ibtnRestart.setVisibility(View.INVISIBLE);
            ibtnRecord.setImageResource(R.drawable.aar_ic_pause);

            mVisualizerHandler = new VisualizerHandler();
            vwAudioVisualizer.linkTo(mVisualizerHandler);

            if (mRecorder == null) {
                mRecorder = OmRecorder.wav(new PullTransport.Default(Util.getMic(mSource, mChannel, mSampleRate), this), new File(mRecordingWAVFile));
            }
            mRecorder.resumeRecording();

            if(mCurrentFragmentTag!=0) {
                recordAudio();
            }
            Util.wait(100, new Runnable() {
                @Override
                public void run() {
                    fragmentRecorder.recording();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void pauseRecording() {
        try {
            mIsRecording = false;
            if (!isFinishing()) {
                ibtnSave.setVisibility(View.VISIBLE);
            }

            ibtnRestart.setVisibility(View.VISIBLE);
            ibtnRecord.setImageResource(R.drawable.aar_ic_rec);

            vwAudioVisualizer.release();
            if (mVisualizerHandler != null) {
                mVisualizerHandler.stop();
            }

            if (mRecorder != null) {
                //mRecorder.pauseRecording();
                mRecorder.stopRecording();
                mRecorder = null;
            }

            if(mCurrentFragmentTag==0) {
                //fragmentRecorder.paused();
                fragmentRecorder.stop();
            }
            File mNewWavFile= getDuplicateFile(AudioFormat.WAV);
            mfRecordingWAVFile.renameTo(mNewWavFile);
            if(mfRecordingWAVFile.exists()) {
                mfRecordingWAVFile.delete();
            }
            PlayerItem playerItem= new PlayerItem(this);
            playerItem.set(mPlayerItems.size(), mNewWavFile.getAbsolutePath());
            mPlayerItems.add(playerItem);
            playAudio();
            cancelPreviewWarned();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void stopRecording(){
        try {
            boolean wasRecording = mIsRecording;
            vwAudioVisualizer.release();
            if (mVisualizerHandler != null) {
                mVisualizerHandler.stop();
            }

            if (mRecorder != null) {
                mRecorder.stopRecording();
                mRecorder = null;
            }
            if (wasRecording && mCurrentFragmentTag == 0) {
                fragmentRecorder.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void restartRecording() {
        try {
            if (mIsRecording) {
                stopRecording();
            } else {
                try {
                    if (mCurrentFragmentTag == 1) {
                        fragmentPlayer.pause(-1);
                    } else if (mCurrentFragmentTag == 2) {
                        fragmentAudioTrim.stop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mVisualizerHandler = new VisualizerHandler();
                vwAudioVisualizer.linkTo(mVisualizerHandler);
                vwAudioVisualizer.release();
                if (mVisualizerHandler != null) {
                    mVisualizerHandler.stop();
                }
            }
            ibtnSave.setVisibility(View.INVISIBLE);
            ibtnRestart.setVisibility(View.INVISIBLE);
            ibtnRecord.setImageResource(R.drawable.aar_ic_rec);
            recordAudio();
            fragmentRecorder.reset();

            mPlayerItems.clear();
            initRecordingDirectory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void selectAudio() {
        try {
            stopRecording();
            if(!mPreviewWarned) {
                mPreviewWarned= true;
                AlertDialog.Builder builder= new AlertDialog.Builder(this);
                builder.setTitle("Confirm")
                        .setMessage("Preview audio before processing. REDO is not possible.")
                        .setNeutralButton(android.R.string.ok, null)
                        .setPositiveButton("Preview", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    fragmentPlayer.playInSequence();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .show();
            } else {
                AudioBatchProcessor audioBatchProcessor = new AudioBatchProcessor(this);
                audioBatchProcessor.setFiles(mPlayerItems)
                        .setDestination(mfDestinationMP3File)
                        .setOnFinish(new Runnable() {
                            @Override
                            public void run() {
                                setResult(RESULT_OK);
                                finish();
                            }
                        })
                        .start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getDuplicateFile(AudioFormat format){
        File file= new File(RECORDING_DIR, "temp_audio_" + System.currentTimeMillis() + "." + format.getFormat());
        if(file.exists()) {
            return getDuplicateFile(format);
        } else {
            return file;
        }
    }

    private void initRecordingDirectory() {
        try {
            File file= new File(getExternalCacheDir(), "recorder");
            RECORDING_DIR= file.getAbsolutePath();
            try { file.mkdirs(); } catch (Exception e) {e.printStackTrace(); }
            clearDirectory(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearDirectory(File file) {
        try {
            for(File f: file.listFiles()) {
                if(f.isDirectory()) {
                    clearDirectory(f);
                }
                f.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reset() {
        onClickRestart(ibtnRestart);
    }

    @Override
    public void recordAudio() {
        try {
            ibtnRestart.setVisibility(View.INVISIBLE);
            ibtnSave.setVisibility(View.INVISIBLE);
            ibtnRecord.setVisibility(View.VISIBLE);
            ibtnRecord.setImageResource(mIsRecording?R.drawable.aar_ic_pause:R.drawable.aar_ic_rec);
            drawFragment(fragmentRecorder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void trimAudio(int position) {
        try {
            ibtnRestart.setVisibility(View.INVISIBLE);
            ibtnSave.setVisibility(View.INVISIBLE);
            ibtnRecord.setVisibility(View.INVISIBLE);
            ibtnRecord.setImageResource(R.drawable.aar_ic_rec);
            fragmentAudioTrim.mCurrentItemPosition = position;
            drawFragment(fragmentAudioTrim);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playAudio() {
        try {
            ibtnRestart.setVisibility(View.VISIBLE);
            ibtnSave.setVisibility(View.VISIBLE);
            ibtnRecord.setVisibility(View.VISIBLE);
            ibtnRecord.setImageResource(R.drawable.aar_ic_rec);
            drawFragment(fragmentPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<PlayerItem> getPlayerItems() {
        return mPlayerItems;
    }

    @Override
    public void cancelPreviewWarned() {
        mPreviewWarned= false;
    }
}
