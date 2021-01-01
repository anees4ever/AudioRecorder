package cafe.adriel.androidaudiorecorder;

import java.util.ArrayList;

public interface FragmentInteractionListener {
    void reset();
    void recordAudio();
    void trimAudio(int position);
    void playAudio();
    ArrayList<PlayerItem> getPlayerItems();
    void cancelPreviewWarned();
}
