package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_CODE_SETTINGS = 1;

    private static final int VALUE_DEFAULT_SCAN_PERIOD = 10;
    private static final int VALUE_DEFAULT_INTERVAL = 1000;
    private static final int VALUE_DEFAULT_SEARCH_END_THRESHOLD = -40;
    private static final int VALUE_DEFAULT_SENSOR_THRESHOLD = 500;

    private static final String KEY_PREFERENCE_EXIST = "PREFERENCE_EXIST";

    private static final String TAG_NO_BLUETOOTH = "NO_BLUETOOTH";
    private static final String TAG_AUTO = "AUTO";
    private static final String TAG_MANUAL = "MANUAL";

    private SharedPreferences mSp;

    private String mCurrentTag;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private LinearLayout mToolbarLayout;

    private BluetoothAdapter mBluetoothAdapter;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SharedPreferenceの取得
        mSp = PreferenceManager.getDefaultSharedPreferences(getApplication());
        getSettings();

        //画面サイズの取得
        Point point=new Point();
        getWindowManager().getDefaultDisplay().getSize(point);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mToolbarLayout= (LinearLayout) findViewById(R.id.toolbar_layout);

        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                point);

        //青歯なかったら終了な
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            NoBluetoothDialog dialogFragment = new NoBluetoothDialog();
            dialogFragment.show(getFragmentManager(), TAG_NO_BLUETOOTH);
        } else {
            //青歯があったら続行
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(getApplicationContext().BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            //青歯無効だったら有効にさせる
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        if(position < 0) {
            startSettingsActivity();
        }else{
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            final ActionBar actionBar = getSupportActionBar();

            switch (position) {
                //自動探索
                case 0:
                    transaction.replace(R.id.container, AutoFragment.newInstance(position),TAG_AUTO);
                    //ActionBarの表示
                    if (actionBar != null) {
                        mToolbarLayout.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                    }
                    //タグの更新
                    mCurrentTag = TAG_AUTO;
                    break;
                //コントローラ
                case 1:
                    transaction.replace(R.id.container, ManualFragment.newInstance(position),TAG_MANUAL);
                    //ActionBarの非表示
                    if (actionBar != null) {
                        mToolbarLayout.animate().translationY(-mToolbarLayout.getHeight()).setInterpolator(new AccelerateInterpolator(2)).start();
                    }
                    //タグの更新
                    mCurrentTag = TAG_MANUAL;
                    break;
            }
            transaction.commit();
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.title_auto);
                break;
            case 1:
                mTitle = getString(R.string.title_manual);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);

        //タイトルの再設定
        restoreActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            //getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startSettingsActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                //終了
                if(resultCode == Activity.RESULT_CANCELED){
                    finish();
                }
                break;
            case REQUEST_CODE_SETTINGS:
                switch (resultCode){
                    case Activity.RESULT_OK:
                        //Intervalの更新
                        android.app.Fragment fragment = getFragmentManager().findFragmentByTag(mCurrentTag);
                        if(fragment != null){
                            ((BaseFragment)fragment).setUpMoll();
                            if(mCurrentTag == TAG_AUTO) {
                                ((AutoFragment) fragment).setSettings();
                            }
                        }
                }
                break;
        }
    }

    private void getSettings(){
        //初回起動ならデフォルトを設定
        if(!mSp.getBoolean(KEY_PREFERENCE_EXIST,false)){
            putDefaultSettings();
        }
    }

    private void putDefaultSettings(){
        SharedPreferences.Editor editor = mSp.edit();

        editor.putBoolean(KEY_PREFERENCE_EXIST,true);
        editor.putInt(getString(R.string.key_scan_period), VALUE_DEFAULT_SCAN_PERIOD);
        editor.putInt(getString(R.string.key_interval), VALUE_DEFAULT_INTERVAL);
        editor.putInt(getString(R.string.key_search_end_threshold), VALUE_DEFAULT_SEARCH_END_THRESHOLD);
        editor.putInt(getString(R.string.key_sensor_threshold), VALUE_DEFAULT_SENSOR_THRESHOLD);
        editor.putString(getString(R.string.key_alarm), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());

        //ﾁｮｹﾞﾌﾟﾙｨｨｨｨ
        editor.apply();
    }

    private void startSettingsActivity(){
        Intent i = new Intent(this,SettingsActivity.class);
        startActivityForResult(i, REQUEST_CODE_SETTINGS);
    }


    public static class NoBluetoothDialog extends DialogFragment {

        @Override
        public AlertDialog onCreateDialog(Bundle savedInstanceState){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.message_no_bluetooth)
                    .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getActivity().finish();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
}
