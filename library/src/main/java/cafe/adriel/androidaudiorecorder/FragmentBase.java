package cafe.adriel.androidaudiorecorder;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public class FragmentBase extends Fragment {
    protected FragmentInteractionListener mListener;
    protected Context mContext;
    protected View mRootView;
    protected boolean mIsBrightColor= false;

    public int iTag= 0;

    public FragmentBase() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext= context;
        if (context instanceof FragmentInteractionListener) {
            mListener = (FragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public final <T extends View> T findViewById(int id) {
        return mRootView.findViewById(id);
    }
}
