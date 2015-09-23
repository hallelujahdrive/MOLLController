package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import java.util.UUID;

/**
 * Created by Chiharu on 2015/09/21.
 */
public abstract class BaseFragment extends Fragment implements BaseDialogFragment.OnDialogInteractionListener{

    protected static final String ARG_SECTION_NUMBER = "SELECTION_NUMBER";

    protected static final String ARG_ID = "ID";
    protected static final String ARG_DEVICE = "DEVICE";

    protected static final String TAG_DEVICE_LIST = "DEVICE_LIST";
    protected static final String TAG_RESCAN = "RESCAN";
    protected static final String TAG_DEVICE_DATA = "DEVICE_DATA";

    protected static final int REQUEST_CODE_DEVICE_LIST = 0;
    protected static final int REQUEST_CODE_RESCAN = 1;
    protected static final int REQUEST_CODE_DEVICE_DATA = 2;

    protected static final byte STOP = 0;
    protected static final byte FORWARD = 1;
    protected static final byte BACK = 2;
    protected static final byte LEFT = 3;
    protected static final byte RIGHT = 4;
    protected static final byte TURN_LEFT = 5;
    protected static final byte TURN_RIGHT = 6;


    public static final UUID RBL_SERVICE = UUID.fromString("713D0000-503E-4C75-BA94-3148F18D941E");
    public static final UUID RBL_DEVICE_RX_UUID = UUID.fromString("713D0002-503E-4C75-BA94-3148F18D941E");
    public static final UUID RBL_DEVICE_TX_UUID = UUID.fromString("713D0003-503E-4C75-BA94-3148F18D941E");
    public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID SERIAL_NUMBER_STRING = UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb");

    protected BluetoothGattCharacteristic mTxCharacteristic;

    protected static Handler handler = new Handler();

    protected boolean mDestroyed = false;

    @Override
    public void onDialogResult(int requestCode, int resultCode, Intent data) {
        Bundle extras = data.getExtras();
        switch (requestCode){
            case REQUEST_CODE_DEVICE_LIST:
                if(resultCode == BaseDialogFragment.RESULT_OK) {
                    //DeviceDialogを開く
                    //多重起動の防止
                    if (getFragmentManager().findFragmentByTag(TAG_DEVICE_DATA) == null) {
                        DeviceDataDialogFragment dialogFragment = DeviceDataDialogFragment.newInstance(extras.getInt(ARG_ID), (BluetoothDevice) extras.getParcelable(ARG_DEVICE));
                        dialogFragment.setTargetFragment(this, REQUEST_CODE_DEVICE_DATA);
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
                if(resultCode == BaseDialogFragment.RESULT_OK){
                    openDeviceListDialog(extras.getInt(ARG_ID));
                }
                break;
        }
    }

    //アクティビティ呼ばないとタイトルがセットされない
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        ((MainActivity) context).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDestroy(){
        //殺しにかかる
        mDestroyed = true;

        super.onDestroy();
    }

    protected void openDeviceListDialog(int id) {
        //多重起動の防止
        if(getFragmentManager().findFragmentByTag(TAG_DEVICE_LIST) == null) {
            DeviceListDialogFragment dialogFragment = DeviceListDialogFragment.newInstance(id);
            dialogFragment.setTargetFragment(this, REQUEST_CODE_DEVICE_LIST);
            dialogFragment.show(getFragmentManager(), TAG_DEVICE_LIST);
        }
    }
}
