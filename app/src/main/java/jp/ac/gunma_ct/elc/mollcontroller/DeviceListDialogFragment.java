package jp.ac.gunma_ct.elc.mollcontroller;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.HashSet;

/**
 * Created by Sakai on 2015/06/22.
 */
public class DeviceListDialogFragment extends BaseDialogFragment {

    private static final int DEFAULT_SCAN_PERIOD = 10;

    private ListView mListView;
    private View mHeaderView;

    private BluetoothAdapter mBluetoothAdapter;
    private long mScanPeriod;
    private boolean mScanning;

    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    private Handler mHandler=new Handler();

    public static DeviceListDialogFragment newInstance(int id){
        DeviceListDialogFragment dialogFragment = new DeviceListDialogFragment();
        Bundle args=new Bundle();
        args.putInt(ARG_ID,id);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState){

        mListener = (OnDialogInteractionListener) getTargetFragment();

        //スキャン時間の設定
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mScanPeriod = sp.getInt(getString(R.string.key_scan_period),DEFAULT_SCAN_PERIOD) * 1000;

        mHeaderView=getActivity().getLayoutInflater().inflate(R.layout.device_list_header_view,null);
        mHeaderView.setClickable(false);

        final AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_device_list);

        mListView=new ListView(getActivity());
        mListView.setDivider(null);

        mListView.addHeaderView(mHeaderView, null, false);

        final DeviceAdapter adapter=new DeviceAdapter(getActivity(),0);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent();
                i.putExtra(ARG_ID,getArguments().getInt(ARG_ID));
                i.putExtra(ARG_DEVICE,adapter.getItem(position - (mScanning ? 1 : 0)));
                mListener.onDialogResult(getTargetRequestCode(), RESULT_OK,i);
                dismiss();
            }
        });

        builder.setView(mListView);

        final BluetoothManager bluetoothManager=(BluetoothManager)getActivity().getSystemService(getActivity().BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (device != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //アダプタに追加
                            adapter.add(device);
                        }
                    });
                }
            }
        };


        scanDevice(true);

        return builder.create();
    }

    private void scanDevice(final boolean enable){
        if(enable){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanEnd();
                }
            }, mScanPeriod);

            mScanning=true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);

        }else{
            scanEnd();
        }
    }

    private void scanEnd() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mListView.removeHeaderView(mHeaderView);
        if(mListView.getCount() == 0){
            Intent i = new Intent();
            i.putExtra(ARG_ID, getArguments().getInt(ARG_ID));
            mListener.onDialogResult(getTargetRequestCode(), RESULT_CANCELED,i );
            dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog){
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        super.onDismiss(dialog);

    }

    private class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {

        private HashSet<String> mHashSet=new HashSet<>();

        public DeviceAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public void add(BluetoothDevice device){
            if(mHashSet.add(device.getAddress())){
                super.add(device);
            }
        }

        @Override
        public View getView(int position,View convertView,ViewGroup parent){
            if(convertView==null){
                convertView=new MenuItemView(getActivity());
            }

            BluetoothDevice device=getItem(position);

            ((MenuItemView) convertView).setText(device.getName());
            ((MenuItemView) convertView).setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth));

            return convertView;
        }
    }
}
