package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Sakai on 2015/06/29.
 */
public class DeviceDataDialogFragment extends DialogFragment {

    private static final String ARG_ID="ID";
    private static final String ARG_DEVICE="DEVICE";

    private TextView mNameTextView;
    private TextView mAddressTextView;
    private TextView mTypeTextView;

    public static DeviceDataDialogFragment newInstance(int id,BluetoothDevice device){
        DeviceDataDialogFragment dialogFragment=new DeviceDataDialogFragment();
        Bundle args=new Bundle();
        args.putInt(ARG_ID,id);
        args.putParcelable(ARG_DEVICE, device);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState){

        final BluetoothDevice device=getArguments().getParcelable(ARG_DEVICE);

        final AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.title_device_data);

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_device_data,null);

        mNameTextView = (TextView) view.findViewById(R.id.name_text_view);
        mAddressTextView = (TextView) view.findViewById(R.id.address_text_view);
        mTypeTextView = (TextView) view.findViewById(R.id.type_text_view);

        mNameTextView.setText(device.getName());
        mAddressTextView.setText(device.getAddress());

        int id;
        switch (device.getType()){
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                id=R.string.device_type_classic;
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                id=R.string.device_type_le;
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                id=R.string.device_type_dual;
                break;
            default:
                id=R.string.device_type_unknown;
                break;
        }

        mTypeTextView.setText(id);

        builder.setView(view);

        builder.setPositiveButton(R.string.action_connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.putExtra(ARG_ID,getArguments().getInt(ARG_ID));
                i.putExtra(ARG_DEVICE,device);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,i);
            }
        }).setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            //ダイアログを閉じる
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return builder.create();
    }
}
