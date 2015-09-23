package jp.ac.gunma_ct.elc.mollcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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
public class AutoFragment extends BaseFragment implements SensorEventListener{

    //回転角度(暫定)
    private static final double MAX_DEG = Math.PI/3;
    //到達時のRSSIの閾値
    private static final int RSSI_THRESHOLD = -40;

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

    private int mRssi = 0;

    //角速度合計
    private double mTotalAngularVelocity;
    //センサの呼び出し回数
    private int mCount;
    private boolean register = false;
    private boolean isSearching = false;

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

        //更新間隔の設定
        setInterval();

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
                } else {
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
                } else {
                    mTagBluetoothGatt.disconnect();
                }
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerListener(isSearching = !isSearching);
            }
        });

        return view;
    }

    private void connectGatt(int id,BluetoothDevice device){
        switch (id){
            case R.id.moll_device_view:
                //デバイス名の表示
                mMollDeviceView.setBluetoothDevice(device);
                //接続
                if(mMollDeviceView.getBluetoothDevice() != null) {
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

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (status == BluetoothGatt.GATT_SUCCESS) {
                                        mMollDeviceView.setConnectionStatus(true);
                                        Toast.makeText(getActivity(), R.string.message_connected, Toast.LENGTH_LONG).show();
                                        //SearchButtonの有効
                                        if(mTagDeviceView.getConnectedStatus()){
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
                if(mTagDeviceView.getBluetoothDevice()!=null) {
                    mTagBluetoothGatt = mTagDeviceView.getBluetoothDevice().connectGatt(getActivity(), false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            connectionStateChange(mTagDeviceView, newState);
                        }

                        @Override
                        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status){
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
                            if(isSearching){
                                writeCharacteristic(rssi);
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
                if(deviceView == mTagDeviceView ) {
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
                                        while (mTagDeviceView.getConnectedStatus()) {
                                            mTagBluetoothGatt.readRemoteRssi();
                                            try {
                                                Thread.sleep(mInterval);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }).start();
                            }
                            //メッセージ表示
                            Toast.makeText(getActivity(), R.string.message_connected, Toast.LENGTH_LONG).show();
                            //SearchButtonの有効
                            if(mMollDeviceView.getConnectedStatus()){
                                mSearchButton.setEnabled(true);
                            }
                        }
                    });
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                if(getActivity()!=null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //切断
                            deviceView.setConnectionStatus(false);
                            //StatusLayoutの削除
                            if(deviceView == mTagDeviceView && mStatusLayout.getParent() != null) {
                                mTagDeviceLayout.removeView(mStatusLayout);
                            }
                            //メッセージ表示
                            Toast.makeText(getActivity(), R.string.message_disconnected, Toast.LENGTH_LONG).show();
                            //SearchButtonの無効
                            mSearchButton.setEnabled(false);
                        }
                    });
                    break;
                }
        }
    }

    private String getDistance(int rssi){
        //定数
        double a = -70;
        double b = 3.5;
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        //numberFormat.setMaximumFractionDigits(1);

        double distance = Math.pow(10, (a - rssi) / (10 * b));

        return numberFormat.format(distance);
    }

    private void writeCharacteristic(int rssi){
        byte command;

        if(rssi < RSSI_THRESHOLD) {
            if (mRssi == 0 || rssi >= mRssi) {
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

        }else{
            //到達したんじゃね？
            command = STOP;

            isSearching = false;
            registerListener(false);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mSearchButton.setEnabled(false);
                    //Toastの表示
                    Toast.makeText(getActivity(), String.format(getString(R.string.message_found),mTagDeviceView.getBluetoothDevice().getName()),Toast.LENGTH_LONG).show();
                }
            });
        }

        //コマンドの送信
        if (mTxCharacteristic != null) {
            mTxCharacteristic.setValue(new byte[]{command});
            mMollBluetoothGatt.writeCharacteristic(mTxCharacteristic);
        }

        mRssi = rssi;
    }

    //リスナの登録
    private void registerListener(boolean enabled){
        register = enabled;
        if(enabled) {
            for (Sensor sensor : mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE)) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }else{
            mSensorManager.unregisterListener(this);
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
