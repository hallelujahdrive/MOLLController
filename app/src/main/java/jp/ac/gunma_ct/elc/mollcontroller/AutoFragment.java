package jp.ac.gunma_ct.elc.mollcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;

/**
 * Created by Chiharu on 2015/08/14.
 */
public class AutoFragment extends BaseFragment implements SensorEventListener {

    private static final int REQUEST_CODE_SEARCH_START_CONFIRM = 3;
    private static final int REQUEST_CODE_SEARCH_ABORT_CONFIRM = 4;
    //Dialog顔してるけどActivityなんでそこんとこよろしく
    private static final int REQUEST_CODE_FOUND_NOTIFICATION = 5;

    private static final String TAG_SEARCH_START_CONFIRM = "SEARCH_START_CONFIRM";
    private static final String TAG_SEARCH_ABORT_CONFIRM = "SEARCH_ABORT_CONFIRM";

    //回転角度(暫定)
    private static final double MAX_DEG = Math.PI / 4;

    //デフォルトの値
    private static final int DEFAULT_INTERVAL = 1000;
    private static final int DEFAULT_HISTORIES = 1;
    private static final int DEFAULT_THRESHOLD = -40;

    private DeviceView mMollDeviceView;
    private DeviceView mTagDeviceView;

    private LinearLayout mTagDeviceLayout;

    private LinearLayout mStatusLayout;
    private TextView mRssiTextView;
    private TextView mDistanceTextView;
    private FloatingActionButton mSearchButton;

    private BluetoothGatt mMollBluetoothGatt;
    private BluetoothGatt mTagBluetoothGatt;

    private SensorManager mSensorManager;

    //更新間隔
    private long mInterval;
    private int mThreshold;

    //通信強度の前回の取得時刻
    private long mLast;

    //通信強度の履歴
    private int[] mRssi;
    private int mHistories;

    //角速度合計
    private double mTotalAngularVelocity;
    //センサの呼び出し回数
    private int mCount;
    private boolean register = false;
    private boolean mSearching = false;

    public static AutoFragment newInstance(int sectionNumber) {
        AutoFragment fragment = new AutoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        //再生成しない
        fragment.setRetainInstance(true);
        return fragment;
    }

    public AutoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //フルスクリーンの解除
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //設定の読み込み
        setUp();

        //SensorManagerの取得
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        View view = inflater.inflate(R.layout.fragment_auto, container, false);

        mMollDeviceView = (DeviceView) view.findViewById(R.id.moll_device_view);
        mTagDeviceView = (DeviceView) view.findViewById(R.id.tag_device_view);
        mTagDeviceLayout = (LinearLayout) view.findViewById(R.id.tag_device_layout);
        mStatusLayout = (LinearLayout) view.findViewById(R.id.status_layout);
        mRssiTextView = (TextView) view.findViewById(R.id.rssi_text_view);
        mDistanceTextView = (TextView) view.findViewById(R.id.distance_text_view);
        mSearchButton = (FloatingActionButton) view.findViewById(R.id.search_button);

        mTagDeviceLayout.removeView(mStatusLayout);
        mSearchButton.setEnabled(false);

        mMollDeviceView.setOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDeviceListDialog(R.id.moll_device_view);
            }
        });
        mTagDeviceView.setOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDeviceListDialog(R.id.tag_device_view);
            }
        });

        mMollDeviceView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mMollBluetoothGatt.connect()) {
                        //接続失敗
                        mMollDeviceView.setConnectionStatus(false);
                    }
                } else if (mMollBluetoothGatt != null) {
                    mMollBluetoothGatt.disconnect();
                }
            }
        });
        mTagDeviceView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mTagBluetoothGatt.connect()) {
                        //接続失敗
                        mTagDeviceView.setConnectionStatus(false);
                    }
                } else if (mTagBluetoothGatt != null) {
                    mTagBluetoothGatt.disconnect();
                }
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSearching) {
                    //探索中断ダイアログを表示
                    openSearchAbortConfirmDialog();
                } else {
                    //探索開始ダイアログを表示
                    openSearchStartConfirmDialog();
                }
            }
        });

        return view;
    }

    private void connectGatt(int id, BluetoothDevice device) {
        switch (id) {
            case R.id.moll_device_view:
                //デバイス名の表示
                mMollDeviceView.setBluetoothDevice(device);
                //接続
                if (mMollDeviceView.getBluetoothDevice() != null) {
                    mMollBluetoothGatt = mMollDeviceView.getBluetoothDevice().connectGatt(getActivity(), false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            connectionStateChange(mMollDeviceView, newState);
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, final int status) {

                            //送信用のCharacteristicがこれで取得できるってばっちゃが言ってた
                            BluetoothGattService service = mMollBluetoothGatt.getService(RBL_SERVICE);
                            mTxCharacteristic = service.getCharacteristic(RBL_DEVICE_TX_UUID);
                            //Mollのsetup
                            setUpMoll(mMollBluetoothGatt);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (status == BluetoothGatt.GATT_SUCCESS) {
                                        mMollDeviceView.setConnectionStatus(true);
                                        Toast.makeText(getActivity(), R.string.message_connected, Toast.LENGTH_LONG).show();

                                        //SearchButtonの有効
                                        if (mTagDeviceView.getConnectedStatus()) {
                                            mSearchButton.setEnabled(true);
                                        }
                                    } else {
                                        Toast.makeText(getActivity(), R.string.message_fault, Toast.LENGTH_LONG).show();
                                        //SearchButtonの無効
                                        mSearchButton.setEnabled(false);
                                    }
                                }
                            });
                        }
                    });
                }
                break;
            case R.id.tag_device_view:
                //デバイス名の表示
                mTagDeviceView.setBluetoothDevice(device);
                //接続
                if (mTagDeviceView.getBluetoothDevice() != null) {
                    mTagBluetoothGatt = mTagDeviceView.getBluetoothDevice().connectGatt(getActivity(), false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            connectionStateChange(mTagDeviceView, newState);
                        }

                        @Override
                        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
                            //取得時刻
                            mLast = System.currentTimeMillis();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //信号強度の表示
                                    mRssiTextView.setText(String.valueOf(rssi));
                                    //距離の表示
                                    mDistanceTextView.setText(getDistance(rssi));

                                }
                            });

                            //探索中なら書き込み
                            if (mSearching) {
                                setCommand(rssi);
                            }
                        }
                    });
                }
                break;
        }
    }

    private void connectionStateChange(final DeviceView deviceView, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                //Tagの時だけ.mollはonServicesDiscoveredで処理
                if (deviceView == mTagDeviceView) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //接続
                            deviceView.setConnectionStatus(true);
                            //StatusLayoutの追加
                            if (mStatusLayout.getParent() == null) {
                                mTagDeviceLayout.addView(mStatusLayout, 1);

                                //更新
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (deviceView.getConnectedStatus() && !mDestroyed) {
                                            if(mLast == 0 || (mLast - System.currentTimeMillis()) <= mInterval * 2) {
                                                mTagBluetoothGatt.readRemoteRssi();
                                                try {
                                                    Thread.sleep(mInterval);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }else{
                                                sendCommand(mMollBluetoothGatt, STOP);
                                                //切断要求
                                                mTagBluetoothGatt.disconnect();
                                                handler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //Toastの表示
                                                        Toast.makeText(getActivity(), R.string.message_fault, Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                                break;
                                            }
                                        }
                                    }
                                }).start();
                            }
                            //メッセージ表示
                            Toast.makeText(getActivity(), R.string.message_connected, Toast.LENGTH_LONG).show();
                            //SearchButtonの有効
                            if (mMollDeviceView.getConnectedStatus()) {
                                mSearchButton.setEnabled(true);
                            }
                        }
                    });
                } else {
                    mMollBluetoothGatt.discoverServices();
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                if (!mDestroyed) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //切断
                            deviceView.setConnectionStatus(false);
                            //StatusLayoutの削除
                            if (deviceView == mTagDeviceView && mStatusLayout.getParent() != null) {
                                mTagDeviceLayout.removeView(mStatusLayout);
                            }
                            //メッセージ表示
                            Toast.makeText(getActivity(), R.string.message_disconnected, Toast.LENGTH_LONG).show();
                            //SearchButtonの無効
                            mSearchButton.setEnabled(false);
                        }
                    });
                }
                break;
        }
    }

    //設定の読み込み
    public void setUp() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mInterval = sp.getInt(getString(R.string.key_interval), DEFAULT_INTERVAL);
        int histories = sp.getInt(getString(R.string.key_histories), DEFAULT_HISTORIES);
        mThreshold = sp.getInt(getString(R.string.key_search_end_threshold), DEFAULT_THRESHOLD);

        mRssi = new int[histories];
        mHistories = 0;
    }

    private String getDistance(int rssi) {
        //定数
        double a = -70;
        double b = 3.5;
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        //numberFormat.setMaximumFractionDigits(1);

        double distance = Math.pow(10, (a - rssi) / (10 * b));

        return numberFormat.format(distance);
    }

    private void setCommand(int rssi) {
        byte command;

        if (rssi < mThreshold) {
            if (getRssiAverage() == 0 || rssi >= getRssiAverage()) {
                command = FORWARD;
            } else {
                //回転開始の初期化
                mTotalAngularVelocity = 0;
                mCount = 0;
                //左回転
                command = TURN_LEFT;
                if (mTxCharacteristic != null) {
                    mTxCharacteristic.setValue(new byte[]{command});
                    mMollBluetoothGatt.writeCharacteristic(mTxCharacteristic);
                }
                //回転開始時刻
                long startMillis = System.currentTimeMillis();
                double deg = 0;
                while (deg < MAX_DEG) {
                    //平均角速度(rad/s) * 回転時間(s)
                    deg = mTotalAngularVelocity / mCount * (startMillis - System.currentTimeMillis()) / 1000;
                }
            }

        } else {
            //到達したんじゃね？
            command = STOP;
            //LEDを青に
            setLed(mMollBluetoothGatt, LOW, LOW, HIGH);

            mSearching = false;
            registerListener(false);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openFoundNotificationDialog();
                }
            });
        }

        //コマンドの送信
        sendCommand(mMollBluetoothGatt, command);
        //電波強度の更新
        if(getRssiAverage() == 0){
            resetRssi(rssi);
        }else {
            updateRssi(rssi);
        }
    }

    private void resetRssi(int rssi){
        for(int i = 0 ; i < mRssi.length ; i++){
            mRssi[i] = rssi;
        }
    }

    //平均通信強度の取得
    private double getRssiAverage(){
        int total = 0;
        for(int rssi : mRssi){
            total += rssi;
        }

        return ((double)total)/mRssi.length;
    }

    //通信強度の更新
    private void updateRssi(int rssi){
        mRssi[mHistories] = rssi;
        if(++mHistories >= mRssi.length){
            mHistories = 0;
        }
    }

    //探索開始のダイアログ
    private void openSearchStartConfirmDialog(){
        //多重起動の防止
        if(getFragmentManager().findFragmentByTag(TAG_SEARCH_START_CONFIRM) == null) {
            ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(String.format(getString(R.string.message_search_start), mTagDeviceView.getBluetoothDevice().getName()), R.string.action_start);
            dialogFragment.setTargetFragment(this, REQUEST_CODE_SEARCH_START_CONFIRM);
            dialogFragment.show(getFragmentManager(), TAG_SEARCH_START_CONFIRM);
        }
    }

    //探索停止のダイアログ
    private void openSearchAbortConfirmDialog(){
        //多重起動の防止
        if(getFragmentManager().findFragmentByTag(TAG_SEARCH_ABORT_CONFIRM) == null) {
            ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(String.format(getString(R.string.message_search_abort), mTagDeviceView.getBluetoothDevice().getName()), R.string.action_abort);
            dialogFragment.setTargetFragment(this, REQUEST_CODE_SEARCH_ABORT_CONFIRM);
            dialogFragment.show(getFragmentManager(), TAG_SEARCH_ABORT_CONFIRM);
        }
    }

    //探索終了
    private void openFoundNotificationDialog(){
        //実はActivity
        Intent i = new Intent(getActivity(), FoundNotificationActivity.class);
        i.putExtra(ARG_DEVICE, mMollDeviceView.getBluetoothDevice());

        startActivityForResult(i, REQUEST_CODE_FOUND_NOTIFICATION);
    }

    //センサのリスナの登録
    private void registerListener(boolean enabled){
        register = enabled;
        if(enabled) {
            for (Sensor sensor : mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE)) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            }
        }else{
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_CODE_FOUND_NOTIFICATION:
                //緑に戻す
                setLed(mMollBluetoothGatt, LOW, HIGH, LOW);
                //再検索
                if(resultCode == BaseDialogFragment.RESULT_CANCELED){
                    openSearchStartConfirmDialog();
                }
                break;
        }
    }

        @Override
    public void onDialogResult(int requestCode, int resultCode, Intent data) {
        super.onDialogResult(requestCode, resultCode, data);
        Bundle extras = data.getExtras();
        switch (requestCode) {
            case REQUEST_CODE_DEVICE_DATA:
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    connectGatt(extras.getInt(ARG_ID), (BluetoothDevice) extras.getParcelable(ARG_DEVICE));
                }
                break;
            case REQUEST_CODE_SEARCH_START_CONFIRM:
                if(resultCode == BaseDialogFragment.RESULT_OK){
                    //初期化
                    resetRssi(0);
                    //センサのリスナの登録
                    registerListener(true);
                    mSearching = true;
                    //Toastの表示
                    Toast.makeText(getActivity(), R.string.message_search_started, Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_CODE_SEARCH_ABORT_CONFIRM:
                if(resultCode == BaseDialogFragment.RESULT_OK){
                    //停止コマンドの送信
                    sendCommand(mMollBluetoothGatt, STOP);
                    //センサのリスナの解除
                    registerListener(false);
                    mSearching = false;
                }
                break;
            case REQUEST_CODE_FOUND_NOTIFICATION:
                //緑に戻す
                setLed(mMollBluetoothGatt, LOW, HIGH, LOW);
                //再検索
                if(resultCode == BaseDialogFragment.RESULT_CANCELED){
                    openSearchStartConfirmDialog();
                }
                break;
        }
    }

    @Override
    public void setUpMoll() {
        if(mMollBluetoothGatt != null) {
            setUpMoll(mMollBluetoothGatt);
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        if(mMollBluetoothGatt != null){
            mMollBluetoothGatt.disconnect();
            mMollBluetoothGatt = null;
        }
        if(mTagBluetoothGatt != null){
            mTagBluetoothGatt.disconnect();
            mTagBluetoothGatt = null;
        }
        if(register){
            registerListener(false);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            //values[2]はz軸の回転
            mTotalAngularVelocity += event.values[2];
            mCount++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
