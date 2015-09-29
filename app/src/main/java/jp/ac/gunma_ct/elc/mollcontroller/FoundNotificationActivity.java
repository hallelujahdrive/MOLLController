package jp.ac.gunma_ct.elc.mollcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;

public class FoundNotificationActivity extends AppCompatActivity implements BaseDialogFragment.OnDialogInteractionListener {

    private static final int REQUEST_CODE_FOUND_NOTIFICATION = 5;
    private static final String TAG_FOUND_NOTIFICATION = "FOUND_NOTIFICATION";

    protected static final String ARG_DEVICE = "DEVICE";

    private MediaPlayer mMediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Windowのフラグ
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        //オブジェクトの受け取り
        Intent i = getIntent();
        Bundle extras = i.getExtras();
        BluetoothDevice device = extras.getParcelable(ARG_DEVICE);

        //Dialogの表示
        FoundNotificationDialogFragment dialogFragment = FoundNotificationDialogFragment.newInstance(device);
        dialogFragment.setTargetFragment(null, REQUEST_CODE_FOUND_NOTIFICATION);
        dialogFragment.show(getFragmentManager(), TAG_FOUND_NOTIFICATION);

        //アラーム音の再生
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final String uriString = sp.getString(getString(R.string.key_notification), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        //なしの場合は弾く
        if(uriString != "") {
            //読み込みは念のため別スレッドでやります
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mMediaPlayer = new MediaPlayer();
                    try {
                        mMediaPlayer.setDataSource(getApplication(), Uri.parse(uriString));
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
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_CODE_FOUND_NOTIFICATION:
                switch (resultCode){
                    case BaseDialogFragment.RESULT_OK:
                        setResult(RESULT_OK);
                        break;
                    case BaseDialogFragment.RESULT_CANCELED:
                        setResult(RESULT_CANCELED);
                        break;
                }
                finish();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mMediaPlayer != null){
            //アラームの停止
            mMediaPlayer.stop();
        }
    }
}
