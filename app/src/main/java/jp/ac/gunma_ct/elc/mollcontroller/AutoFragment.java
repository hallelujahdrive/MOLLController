package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
public class AutoFragment extends BaseFragment {

    private DeviceView mMollDeviceView;
    private DeviceView mTagDeviceView;

    private LinearLayout mTagDeviceLayout;

    private LinearLayout mStatusLayout;
    private TextView mRssiTextView;
    private TextView mDistanceTextView;

    private BluetoothGatt mMollBluetoothGatt;
    private BluetoothGatt mTagBluetoothGatt;

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

        View rootView = inflater.inflate(R.layout.fragment_auto, container, false);

        mMollDeviceView = (DeviceView) rootView.findViewById(R.id.moll_device_view);
        mTagDeviceView = (DeviceView) rootView.findViewById(R.id.tag_device_view);
        mTagDeviceLayout = (LinearLayout) rootView.findViewById(R.id.tag_device_layout);
        mStatusLayout = (LinearLayout) rootView.findViewById(R.id.status_layout);
        mRssiTextView = (TextView) rootView.findViewById(R.id.rssi_text_view);
        mDistanceTextView = (TextView) rootView.findViewById(R.id.distance_text_view);

        mTagDeviceLayout.removeView(mStatusLayout);

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

        return rootView;
    }

    private void connectGatt(int id,BluetoothDevice device){
        switch (id){
            case R.id.moll_device_view:
                //デバイス名の表示
                mMollDeviceView.setBluetoothDevice(device);
                //接続
                if(mMollDeviceView.getBluetoothDevice()!=null) {
                    mMollBluetoothGatt = mMollDeviceView.getBluetoothDevice().connectGatt(getActivity(), false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            super.onConnectionStateChange(gatt, status, newState);

                            connectionStateChange(mMollDeviceView, newState);
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
                        }
                    });
                }
                break;
        }
    }

    private void connectionStateChange(final DeviceView deviceView, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //接続
                        deviceView.setConnectionStatus(true);
                        //StatusLayoutの追加
                        if (deviceView == mTagDeviceView && mStatusLayout.getParent() == null) {
                            mTagDeviceLayout.addView(mStatusLayout, 1);

                            //暫定.1秒おきに更新
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while (mTagDeviceView.getConnected()) {
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
                    }
                });
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
    }
}
