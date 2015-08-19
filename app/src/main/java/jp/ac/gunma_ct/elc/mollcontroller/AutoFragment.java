package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

/**
 * Created by Chiharu on 2015/08/14.
 */
public class AutoFragment extends Fragment implements DeviceListDialogFragment.OnDialogInteractionListener,DeviceDataDialogFragment.OnDialogInteractionListener{

    private static final String ARG_SECTION_NUMBER = "SELECTION_NUMBER";

    private static final String TAG_DEVICE_LIST = "DEVICE_LIST";
    private static final String TAG_DEVICE_DATA = "DEVICE_DATA";

    private static final int REQUEST_CODE=0;

    private static Handler handler = new Handler();

    private DeviceView mMollDeviceView;
    private DeviceView mTagDeviceView;

    private BluetoothGatt mMollBluetoothGatt;
    private BluetoothGatt mTagBluetoothGatt;

    public static AutoFragment newInstance(int sectionNumber) {
        AutoFragment fragment = new AutoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER,sectionNumber);
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
        View rootView = inflater.inflate(R.layout.fragment_auto, container, false);

        mMollDeviceView= (DeviceView) rootView.findViewById(R.id.moll_device_view);
        mTagDeviceView= (DeviceView) rootView.findViewById(R.id.tag_device_view);

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

    public void openDeviceListDialog(int id) {
        DeviceListDialogFragment dialogFragment=DeviceListDialogFragment.newInstance(id);
        dialogFragment.setTargetFragment(this, REQUEST_CODE);
        dialogFragment.show(getActivity().getFragmentManager(), TAG_DEVICE_LIST);
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
                                    Toast.makeText(getActivity(),String.valueOf(rssi),Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
                break;
        }
    }

    private void connectionStateChange(final DeviceView deviceView,int newState){
        switch (newState){
            case BluetoothProfile.STATE_CONNECTED:
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //接続
                        deviceView.setConnectionStatus(true);
                        mTagBluetoothGatt.readRemoteRssi();
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
                            //メッセージ表示
                            Toast.makeText(getActivity(), R.string.message_disconnected, Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }
        }
    }

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

    @Override
    public void onListCallback(int id,BluetoothDevice device) {
        DeviceDataDialogFragment dialogFragment = DeviceDataDialogFragment.newInstance(id,device);
        dialogFragment.setTargetFragment(this, REQUEST_CODE);
        dialogFragment.show(getActivity().getFragmentManager(), TAG_DEVICE_DATA);
    }



    @Override
    public void onDataCallback(int id,BluetoothDevice device) {
        connectGatt(id,device);
    }
}
