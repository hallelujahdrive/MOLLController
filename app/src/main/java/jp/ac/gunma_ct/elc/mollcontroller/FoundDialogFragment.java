package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import java.io.IOException;

/**
 * Created by Chiharu on 2015/09/29.
 */
public class FoundDialogFragment extends BaseDialogFragment {

    private MediaPlayer mMediaPlayer = null;

    public static FoundDialogFragment newInstance(BluetoothDevice device){
        FoundDialogFragment dialogFragment = new FoundDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DEVICE, device);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {

        mListener = (OnDialogInteractionListener) getTargetFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //メッセージの表示
        //builder.setMessage(String.format(getString(R.string.message_found), ((BluetoothDevice) getArguments().getParcelable(ARG_DEVICE)).getName()));
        builder.setMessage(String.format(getString(R.string.message_found),"Dummy"));

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
        //音量調節をアラームに
        activity.setVolumeControlStream(AudioManager.STREAM_ALARM);
        //アラーム音
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        final String uriString = sp.getString(getString(R.string.key_alarm), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        //なしの場合は弾く
        if(uriString != "") {
            //読み込みは念のため別スレッドでやります
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mMediaPlayer = new MediaPlayer();
                    try {
                        mMediaPlayer.setDataSource(activity, Uri.parse(uriString));
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

                        //アラームの再生
                        mMediaPlayer.setLooping(true);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        System.out.println("あたっち");
    }

    @Override
    public void onDetach(){
        super.onDetach();
        if(mMediaPlayer != null){
            //アラームの停止
            mMediaPlayer.stop();
        }
        System.out.println("でたっち");
    }
}
