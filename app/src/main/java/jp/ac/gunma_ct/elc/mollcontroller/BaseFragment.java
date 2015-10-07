package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import java.nio.ByteBuffer;
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

    private static final short DEFAULT_VELOCITY = 120;
    private static final int DEFAULT_SENSOR_THRESHOLD = 500;
    private static final int DEFAULT_TURN_PERIOD = 500;
    private static final int DEFAULT_BACK_PERIOD = 500;

    protected static final int REQUEST_CODE_DEVICE_LIST = 0;
    protected static final int REQUEST_CODE_RESCAN = 1;
    protected static final int REQUEST_CODE_DEVICE_DATA = 2;

    //命令タイプ
    private static final byte SET_UP = 0;
    private static final byte MOVE = 1;
    private static final byte SET_LED = 2;

    //move
    private static final int leftP = 1;
    private static final int leftN = 2;
    private static final int rightP = 3;
    private static final int rightN = 4;

    //コマンド
    protected static final byte STOP = 0;
    protected static final byte FORWARD = 1;
    protected static final byte BACK = 2;
    protected static final byte TURN_LEFT = 3;
    protected static final byte TURN_RIGHT = 4;
    protected static final byte LEFT_FORWARD = 5;
    protected static final byte RIGHT_FORWARD = 6;
    protected static final byte LEFT_BACK = 7;
    protected static final byte RIGHT_BACK = 8;


    //LEDのピン出力
    protected static final byte LOW = 0;
    protected static final byte HIGH = 1;

    public static final UUID RBL_SERVICE = UUID.fromString("713D0000-503E-4C75-BA94-3148F18D941E");
    public static final UUID RBL_DEVICE_RX_UUID = UUID.fromString("713D0002-503E-4C75-BA94-3148F18D941E");
    public static final UUID RBL_DEVICE_TX_UUID = UUID.fromString("713D0003-503E-4C75-BA94-3148F18D941E");
    public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID SERIAL_NUMBER_STRING = UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb");

    protected BluetoothGattCharacteristic mTxCharacteristic;

    protected static Handler handler = new Handler();

    //デフォルトの速度
    protected byte mDefVelocityL;
    protected byte mDefVelocityR;

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

    //Mollのセットアップ
    protected void setUpMoll(BluetoothGatt bluetoothGatt){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        byte[] value = new byte[15];
        //フラグの挿入
        value[0] = SET_UP;
        //速度の値の挿入
        value[1] = mDefVelocityL = getVelocity(R.string.key_velocity_left);
        value[2] = mDefVelocityR = getVelocity(R.string.key_velocity_right);
        int i = 3;
        //センサの値の挿入
        for(byte data : intToBytes(sp.getInt(getString(R.string.key_sensor_threshold), DEFAULT_SENSOR_THRESHOLD))){
            value[i++] = data;
        }
        //後退時間の値の挿入
        for(byte data : intToBytes(sp.getInt(getString(R.string.key_back_period), DEFAULT_BACK_PERIOD))){
            value[i++] = data;
        }
        //転回時間の値の挿入
        for(byte data : intToBytes(sp.getInt(getString(R.string.key_turn_period), DEFAULT_TURN_PERIOD))){
            value[i++] = data;
        }

        writeCharacteristic(bluetoothGatt, value);
    }

    //走行
    protected void move(BluetoothGatt bluetoothGatt, int command){
        byte[] value = {MOVE, 0, 0, 0, 0};

        switch (command){
            case STOP:
                break;
            case FORWARD:
                value[leftP] = mDefVelocityL;
                value[rightP] = mDefVelocityR;
                break;
            case BACK:
                value[leftN] = mDefVelocityL;
                value[rightN] = mDefVelocityR;
                break;
            case TURN_LEFT:
                value[leftN] = mDefVelocityL;
                value[rightP] = mDefVelocityR;
                break;
            case TURN_RIGHT:
                value[leftP] = mDefVelocityL;
                value[rightN] = mDefVelocityR;
                break;
            case LEFT_FORWARD:
                value[rightP] = mDefVelocityR;
                break;
            case RIGHT_FORWARD:
                value[leftP] = mDefVelocityL;
                break;
            case LEFT_BACK:
                value[rightN] = mDefVelocityR;
                break;
            case RIGHT_BACK:
                value[leftN] = mDefVelocityL;
                break;
        }

        writeCharacteristic(bluetoothGatt, value);

    }

    protected void move(BluetoothGatt bluetoothGatt, Integer velocityL, Integer velocityR){
        byte[] value = {MOVE, 0, 0, 0, 0};

        //データの挿入
        byte left = velocityL.byteValue();
        byte right = velocityR.byteValue();
        //左
        if(velocityL >= 0){
            value[leftP] = velocityL.byteValue();
        }else{
            value[leftN] = Integer.valueOf(-velocityL).byteValue();
        }
        //右
        if(velocityR >= 0){
            value[rightP] = velocityR.byteValue();
        }else{
            value[rightN] = Integer.valueOf(-velocityR).byteValue();
        }

        writeCharacteristic(bluetoothGatt, value);
    }

    //LEDの設定
    protected void setLed(BluetoothGatt bluetoothGatt, byte red, byte green, byte blue){
        byte[] value = {SET_LED, red, green, blue};
        writeCharacteristic(bluetoothGatt, value);
    }

    //送信
    protected void writeCharacteristic(BluetoothGatt bluetoothGatt, byte[] value) {
        if (mTxCharacteristic != null) {
            mTxCharacteristic.setValue(value);
            bluetoothGatt.writeCharacteristic(mTxCharacteristic);
        }
    }

    private byte getVelocity(int resId){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean individual = sp.getBoolean(getString(R.string.key_velocity_individual), false);
        Integer velocity = individual ? sp.getInt(getString(resId), DEFAULT_VELOCITY) : sp.getInt(getString(R.string.key_velocity), DEFAULT_VELOCITY);
        return velocity.byteValue();
    }

    private byte[] intToBytes(int value){
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(value);

        return byteBuffer.array();
    }

    public abstract void setUpMoll();
}
