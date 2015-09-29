package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import java.io.IOException;

/**
 * Created by Chiharu on 2015/09/29.
 */
public class FoundNotificationDialogFragment extends BaseDialogFragment {

    public static FoundNotificationDialogFragment newInstance(BluetoothDevice device){
        FoundNotificationDialogFragment dialogFragment = new FoundNotificationDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DEVICE, device);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //メッセージの表示
        builder.setMessage(String.format(getString(R.string.message_found), ((BluetoothDevice) getArguments().getParcelable(ARG_DEVICE)).getName()));

        //ボタンの設定
        final Intent i = new Intent();
        builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogResult(getTargetRequestCode(), RESULT_OK , i);
            }
        }).setNegativeButton(R.string.action_retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogResult(getTargetRequestCode(), RESULT_CANCELED, i);
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        //Listenerの登録
        mListener = (OnDialogInteractionListener) activity;
        //音量調節をアラームに
        activity.setVolumeControlStream(AudioManager.STREAM_ALARM);

    }

    @Override
    public void onDetach(){
        super.onDetach();
        //リスナの解除
        mListener = null;
    }

    @Override
    public void onDismiss(DialogInterface dialog){
        super.onDismiss(dialog);
        getActivity().finish();
    }
}
