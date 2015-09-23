package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Chiharu on 2015/08/23.
 */
public class RescanDialogFragment extends BaseDialogFragment {

    private static final String ARG_ID ="ID";

    public static RescanDialogFragment newInstance(int id){
        RescanDialogFragment dialogFragment = new RescanDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID,id);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    //button
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState){

        mListener = (OnDialogInteractionListener) getTargetFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //メッセージ
        builder.setMessage(R.string.message_device_not_found);

        builder.setPositiveButton(R.string.action_rescan, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.putExtra(ARG_ID,getArguments().getInt(ARG_ID));
                mListener.onDialogResult(getTargetRequestCode(), RESULT_OK,i);
            }
        }).setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return builder.create();
    }
}
