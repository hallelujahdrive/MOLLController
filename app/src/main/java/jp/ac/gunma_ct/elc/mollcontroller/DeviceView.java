package jp.ac.gunma_ct.elc.mollcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Chiharu on 2015/08/16.
 */
public class DeviceView extends GridLayout {

    private ImageView mIconImageView;
    private TextView mTitleTextView;
    private TextView mNameView;
    private TextView mStatusTextView;
    private SwitchCompat mConnectionSwitch;
    private Button mSettingsButton;

    private BluetoothDevice mDevice;

    private boolean mConnected = false;

    public DeviceView(Context context) {
        this(context, null);
    }

    public DeviceView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.deviceViewStyle);
    }

    public DeviceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs, defStyleAttr);
    }

    //初期化
    public void init(AttributeSet attrs,int defStyle){

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DeviceView, defStyle, 0);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.device_view,this);

        mIconImageView = (ImageView) view.findViewById(R.id.icon_image_view);
        mTitleTextView = (TextView) view.findViewById(R.id.title_text_view);
        mNameView = (TextView) view.findViewById(R.id.name_text_view);
        mStatusTextView = (TextView) view.findViewById(R.id.status_text_view);
        mConnectionSwitch = (SwitchCompat) view.findViewById(R.id.connection_switch);
        mSettingsButton = (Button) view.findViewById(R.id.settings_button);

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary,outValue,true);

        mTitleTextView.setText(a.getText(R.styleable.DeviceView_title));

        a.recycle();
    }

    public void setConnectionStatus(boolean connected){
        Drawable icon;
        int id;

        if(mConnected = connected){
            icon=getResources().getDrawable(R.drawable.ic_action_bluetooth_connected);
            id=R.string.status_connected;
        }else{
            icon=getResources().getDrawable(R.drawable.ic_action_bluetooth_disabled);
            id=R.string.status_disconnected;
        }
        if(icon!=null){
            mIconImageView.setImageDrawable(icon);
        }

        mConnectionSwitch.setChecked(connected);
        mStatusTextView.setText(id);

    }

    public void setBluetoothDevice(BluetoothDevice device){
        mDevice = device;
        mNameView.setText(mDevice.getName());
        //switchを有効に
        mConnectionSwitch.setEnabled(true);
    }

    @Nullable
    public BluetoothDevice getBluetoothDevice(){
        return mDevice;
    }

    public boolean getConnected(){
        return mConnected;
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener l){
        mConnectionSwitch.setOnCheckedChangeListener(l);
    }

    public void setOnButtonClickListener(OnClickListener l){
        mSettingsButton.setOnClickListener(l);
    }
}
