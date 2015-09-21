package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.DialogFragment;
import android.content.Intent;

/**
 * Created by Chiharu on 2015/09/21.
 */
public class BaseDialogFragment extends DialogFragment {

    public static final int RESULT_OK = -1;
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_FIRST_USER = 1;

    protected static final String ARG_ID = "ID";
    protected static final String ARG_DEVICE = "DEVICE";

    protected OnDialogInteractionListener mListener;

    public interface OnDialogInteractionListener {
        void onDialogResult(int requestCode,int resultCode,Intent data);
    }
}
