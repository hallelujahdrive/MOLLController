package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Chiharu on 2015/08/14.
 */
public class AutoFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "SELECTION_NUMBER";

    private static final String ARG_ID = "ID";
    private static final String ARG_DEVICE = "DEVICE";

    private static final String TAG_DEVICE_LIST = "DEVICE_LIST";
    private static final String TAG_RESCAN = "RESCAN";
    private static final String TAG_DEVICE_DATA = "DEVICE_DATA";

    private static final int REQUEST_CODE_LIST = 0;
    private static final int REQUEST_CODE_RESCAN = 1;
    private static final int REQUEST_CODE_DATA = 2;

    private static final int DEFAULT_INTERVAL = 1000;

    private static Handler handler = new Handler();

    private DeviceView mMollDeviceView;
    private DeviceView mTagDeviceView;

    private LinearLayout mTagDeviceLayout;

    private LinearLayout mStatusLayout;
    private TextView mRssiTextView;
    private TextView mDistanceTextView;

    private BluetoothGatt mMollBluetoothGatt;
    private BluetoothGatt mTagBluetoothGatt;

    private long mInterval;

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

    private void openDeviceListDialog(int id) {
        //多重起動の防止
        if(getFragmentManager().findFragmentByTag(TAG_DEVICE_LIST) == null) {
            DeviceListDialogFragment dialogFragment = DeviceListDialogFragment.newInstance(id);
            dialogFragment.setTargetFragment(this, REQUEST_CODE_LIST);
            dialogFragment.show(getFragmentManager(), TAG_DEVICE_LIST);
        }
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
                            super.onConnectionStateChange(gatt, status, newState);

                            connectionStateChange(mTagDeviceView, newState);
                        }

                        @Override
                        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //信号強度の表示
                                    mRssiTextView.setText(String.valueOf(rssi));
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle extras = data.getExtras();
        switch (requestCode){
            case REQUEST_CODE_LIST:
                if(resultCode == Activity.RESULT_OK) {
                    //DeviceDialogを開く
                    //多重起動の防止
                    if (getFragmentManager().findFragmentByTag(TAG_DEVICE_DATA) == null) {
                        DeviceDataDialogFragment dialogFragment = DeviceDataDialogFragment.newInstance(extras.getInt(ARG_ID), (BluetoothDevice) extras.getParcelable(ARG_DEVICE));
                        dialogFragment.setTargetFragment(this, REQUEST_CODE_DATA);
                        dialogFragment.show(getFragmentManager(), TAG_DEVICE_DATA);
                    }
                }else{
                    //RescanDialogを開く
                    //多重起動の防止
                    if(getFragmentManager().findFragmentByTag(TAG_RESCAN) == null){
                        RescanDialogFragment dialogFragment = RescanDialogFragment.newInstance(extras.getInt(ARG_ID));
                        dialogFragment.setTargetFragment(this, REQUEST_CODE_RESCAN);
                        dialogFragment.show(getFragmentManager(), TAG_RESCAN);
                    }
                }
                break;
            case REQUEST_CODE_RESCAN:
                if(resultCode == Activity.RESULT_OK){
                    openDeviceListDialog(extras.getInt(ARG_ID));
                }
                break;
            case REQUEST_CODE_DATA:
                if(resultCode == Activity.RESULT_OK){
                    connectGatt(extras.getInt(ARG_ID), (BluetoothDevice) extras.getParcelable(ARG_DEVICE));
                }
                break;
        }
    }

    //非推奨メソッドだけど、何故かonAttach(Context context)を呼ぶとバグる
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
                ((MainActivity) activity).onSectionAttached(
                        getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach(){
        super.onDetach();
        if(mMollBluetoothGatt!=null){
            mMollBluetoothGatt.disconnect();
        }
        if(mTagBluetoothGatt!=null){
            mTagBluetoothGatt.disconnect();
        }
    }

    public void setInterval(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mInterval = sp.getInt(getString(R.string.key_interval),DEFAULT_INTERVAL);
    }
}
