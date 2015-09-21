package jp.ac.gunma_ct.elc.mollcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CompoundButton;

/**
 * Created by Chiharu on 2015/09/21.
 */
public class DeviceViewDialogFragment extends BaseDialogFragment {

    public static final int RESULT_CHECKED_CHANGED = 0;
    public static final int RESULT_BUTTON_CLICK = 1;

    private static final String ARG_IS_CHECKED = "IS_CHECKED";

    private DeviceView mDeviceView;

    public static DeviceViewDialogFragment newInstance(BluetoothDevice device){
        DeviceViewDialogFragment dialogFragment=new DeviceViewDialogFragment();
        Bundle args=new Bundle();
        args.putParcelable(ARG_DEVICE, device);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState){

        mListener = (OnDialogInteractionListener) getTargetFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //DeviceViewの設定
        mDeviceView = new DeviceView(getActivity());
        mDeviceView.setBluetoothDevice((BluetoothDevice) getArguments().getParcelable(ARG_DEVICE), true);
        mDeviceView.setTitle(R.string.title_moll);

        mDeviceView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                System.out.println(isChecked);
                Intent i = new Intent();
                i.putExtra(ARG_IS_CHECKED, isChecked);
                mListener.onDialogResult(getTargetRequestCode(), RESULT_FIRST_USER + RESULT_CHECKED_CHANGED, i);
            }
        });

        mDeviceView.setOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDialogResult(getTargetRequestCode(), RESULT_FIRST_USER + RESULT_BUTTON_CLICK, new Intent());
                dismiss();
            }
        });

        builder.setView(mDeviceView);

        return builder.create();
    }

    public void setConnectionStatus(boolean connected){
        mDeviceView.setConnectionStatus(connected);
    }
}
