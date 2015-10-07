package jp.ac.gunma_ct.elc.mollcontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Created by Chiharu on 2015/08/14.
 */
public class ManualFragment extends BaseFragment implements View.OnTouchListener{

    private static final String ARG_IS_CHECKED = "IS_CHECKED";

    protected static final String TAG_DEVICE_VIEW = "DEVICE_VIEW";

    protected static final int REQUEST_CODE_DEVICE_VIEW = 3;

    private ImageButton mStopButton;
    private ImageButton mForwardButton;
    private ImageButton mBackButton;
    private ImageButton mTurnLeftButton;
    private ImageButton mTurnRightButton;
    private ImageButton mLeftForwardButton;
    private ImageButton mRightForwardButton;
    private ImageButton mLeftBackButton;
    private ImageButton mRightBackButton;


    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private boolean mConnected = false;

    public static ManualFragment newInstance(int sectionNumber) {
        ManualFragment fragment = new ManualFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER,sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //フルスクリーンに
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View view = inflater.inflate(R.layout.fragment_manual, container, false);

        FloatingActionButton settingsButton = (FloatingActionButton) view.findViewById(R.id.settings_button);

        mStopButton = (ImageButton) view.findViewById(R.id.stop_button);
        mForwardButton = (ImageButton) view.findViewById(R.id.forward_button);
        mBackButton = (ImageButton) view.findViewById(R.id.back_button);
        mTurnLeftButton = (ImageButton) view.findViewById(R.id.turn_left_button);
        mTurnRightButton = (ImageButton) view.findViewById(R.id.turn_right_button);
        mLeftForwardButton = (ImageButton) view.findViewById(R.id.left_forward_button);
        mRightForwardButton = (ImageButton) view.findViewById(R.id.right_forward_button);
        mLeftBackButton = (ImageButton) view.findViewById(R.id.left_back_button);
        mRightBackButton = (ImageButton) view.findViewById(R.id.right_back_button);

        //Listenerの登録
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnected) {
                    //接続していればDeviceViewFragment
                    openDeviceViewDialog();
                } else {
                    //接続してなければDeviceListFragmentを開く
                    openDeviceListDialog(R.id.moll_device_view);
                }
            }
        });

        //最初はボタンは全部無効
        setButtonsEnabled(false);

        mStopButton.setOnTouchListener(this);
        mForwardButton.setOnTouchListener(this);
        mBackButton.setOnTouchListener(this);
        mTurnLeftButton.setOnTouchListener(this);
        mTurnRightButton.setOnTouchListener(this);
        mLeftForwardButton.setOnTouchListener(this);
        mRightForwardButton.setOnTouchListener(this);
        mLeftBackButton.setOnTouchListener(this);
        mRightBackButton.setOnTouchListener(this);

        return view;
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, Intent data) {
        super.onDialogResult(requestCode, resultCode, data);
        Bundle extras = data.getExtras();
        switch (requestCode) {
            case REQUEST_CODE_DEVICE_DATA:
                if (resultCode == BaseDialogFragment.RESULT_OK) {
                    connectGatt((BluetoothDevice) extras.getParcelable(ARG_DEVICE));
                }
                break;
            case REQUEST_CODE_DEVICE_VIEW:
                switch (resultCode){
                    case BaseDialogFragment.RESULT_FIRST_USER + DeviceViewDialogFragment.RESULT_CHECKED_CHANGED:
                        if(extras.getBoolean(ARG_IS_CHECKED)){
                            mBluetoothGatt.connect();
                        }else{
                            mBluetoothGatt.disconnect();
                        }
                        break;
                    case BaseDialogFragment.RESULT_FIRST_USER + DeviceViewDialogFragment.RESULT_BUTTON_CLICK:
                        openDeviceListDialog(R.id.moll_device_view);
                }
        }
    }

    @Override
    public void setUpMoll() {
        if (mBluetoothGatt != null) {
            setUpMoll(mBluetoothGatt);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int command = STOP;

        switch (event.getAction()){
            //ボタンを押した
            case MotionEvent.ACTION_DOWN:
                switch (v.getId()) {
                    case R.id.stop_button:
                        break;
                    case R.id.forward_button:
                        command = FORWARD;
                        break;
                    case R.id.back_button:
                        command = BACK;
                        break;
                    case R.id.turn_left_button:
                        command = TURN_LEFT;
                        break;
                    case R.id.turn_right_button:
                        command = TURN_RIGHT;
                        break;
                    case R.id.left_forward_button:
                        command = LEFT_FORWARD;
                        break;
                    case R.id.right_forward_button:
                        command = RIGHT_FORWARD;
                        break;
                    case R.id.left_back_button:
                        command = LEFT_BACK;
                        break;
                    case R.id.right_back_button:
                        command = RIGHT_BACK;
                        break;
                }
                //コマンドの送信
                move(mBluetoothGatt, command);
                break;
            //ボタンを離した
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //コマンドの送信
                move(mBluetoothGatt, STOP);
                break;
        }
        return false;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        if(mBluetoothGatt != null){
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }
    }

    //ImageButtonのtint(<Lollipop)
    private void setButtonTint(ImageButton imageButton, boolean enabled){
        int tintColor;
        if(enabled){
            tintColor = getResources().getColor(R.color.secondary_text_default_material_light);
        }else{
            tintColor = getResources().getColor(R.color.secondary_text_disabled_material_light);
        }
        imageButton.getDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
    }

    private void openDeviceViewDialog(){
        if(getFragmentManager().findFragmentByTag(TAG_DEVICE_VIEW) == null){
            DeviceViewDialogFragment dialogFragment = DeviceViewDialogFragment.newInstance(mDevice);
            dialogFragment.setTargetFragment(this,REQUEST_CODE_DEVICE_VIEW);
            dialogFragment.show(getFragmentManager(), TAG_DEVICE_VIEW);
        }
    }

    private void connectGatt(BluetoothDevice device){
        mDevice = device;
        //接続
        mBluetoothGatt = mDevice.connectGatt(getActivity(), false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                switch (newState){
                    case BluetoothProfile.STATE_CONNECTED:
                        //
                        mBluetoothGatt.discoverServices();
                        mConnected = true;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //Toastの表示
                                Toast.makeText(getActivity(), R.string.message_connected, Toast.LENGTH_LONG).show();
                                //DeviceViewDialogへ反映
                                DeviceViewDialogFragment dialogFragment = (DeviceViewDialogFragment) getFragmentManager().findFragmentByTag(TAG_DEVICE_VIEW);
                                if(dialogFragment != null){
                                    dialogFragment.setConnectionStatus(true);
                                }
                            }
                        });
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        mConnected = false;
                        if(!mDestroyed) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setButtonsEnabled(false);
                                    //Toastの表示
                                    if (getActivity() != null) {
                                        Toast.makeText(getActivity(), R.string.message_disconnected, Toast.LENGTH_LONG).show();
                                        //DeviceViewDialogへ反映
                                        DeviceViewDialogFragment dialogFragment = (DeviceViewDialogFragment) getFragmentManager().findFragmentByTag(TAG_DEVICE_VIEW);
                                        if (dialogFragment != null) {
                                            dialogFragment.setConnectionStatus(false);
                                        }
                                    }
                                }
                            });
                        }
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, final int status) {

                //送信用のCharacteristicがこれで取得できるってばっちゃが言ってた
                BluetoothGattService service = mBluetoothGatt.getService(RBL_SERVICE);
                mTxCharacteristic = service.getCharacteristic(RBL_DEVICE_TX_UUID);
                //Mollのsetup
                setUpMoll(mBluetoothGatt);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            setButtonsEnabled(true);
                        } else {
                            Toast.makeText(getActivity(), R.string.message_fault, Toast.LENGTH_LONG).show();
                            mBluetoothGatt.discoverServices();
                        }
                    }
                });
            }
        });
    }

    private void setButtonsEnabled(boolean enabled){
        mStopButton.setEnabled(enabled);
        mForwardButton.setEnabled(enabled);
        mBackButton.setEnabled(enabled);
        mTurnLeftButton.setEnabled(enabled);
        mTurnRightButton.setEnabled(enabled);
        mLeftForwardButton.setEnabled(enabled);
        mRightForwardButton.setEnabled(enabled);
        mLeftBackButton.setEnabled(enabled);
        mRightBackButton.setEnabled(enabled);

        //Lollipop以前の時,Tintが使えないのでこうする
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setButtonTint(mStopButton, enabled);
            setButtonTint(mForwardButton, enabled);
            setButtonTint(mBackButton, enabled);
            setButtonTint(mTurnLeftButton, enabled);
            setButtonTint(mTurnRightButton, enabled);
            setButtonTint(mLeftForwardButton, enabled);
            setButtonTint(mRightForwardButton, enabled);
        }
    }
}
