package jp.ac.gunma_ct.elc.mollcontroller;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by Chiharu on 2015/09/27.
 */
public class ConfirmDialogFragment extends BaseDialogFragment {

    private static final String ARG_MESSAGE = "MESSAGE";
    private static final String ARG_POSITIVE_BUTTON_TEXT = "POSITIVE_BUTTON_TEXT";

    public static ConfirmDialogFragment newInstance(String message, int positiveButtonText){
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState){

        Bundle args  = getArguments();

        mListener = (OnDialogInteractionListener) getTargetFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //メッセージの表示
        builder.setMessage(args.getString(ARG_MESSAGE));

        //ボタンの設定
        final Intent i = new Intent();
        builder.setPositiveButton(args.getInt(ARG_POSITIVE_BUTTON_TEXT), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogResult(getTargetRequestCode(), RESULT_OK, i);
                dismiss();
            }
        }).setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogResult(getTargetRequestCode(), RESULT_CANCELED, i);
                dismiss();
            }
        });

        return builder.create();
    }
}
