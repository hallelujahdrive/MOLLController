package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ListAdapter;

public class SettingsActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //action baの設定
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar=getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager=getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, new SettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private static final String TAG_ABOUT_MOLL_CONTROLLER = "ABOUT_MOLL_CONTROLLER";

        //Activityのリクエストコード
        private static final int REQUEST_CODE_RINGTONE_PICKER = 0;

        //MOLL Controllerについて
        private static final String ABOUT_MOLL_CONTROLLER_URL = "file:///android_asset/about_moll_controller.html";

        private SharedPreferences mSp;

        private Preference mAlarmPreference;
        //アラーム音のUri
        private Uri mUri;


        private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener =new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                //ListPreferenceならsummeryにセット
                Preference preference = findPreference(key);
                if(preference instanceof SeekBarPreference) {
                    SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                    preference.setSummary(seekBarPreference.getStringValue() == null ? "" : seekBarPreference.getStringValue());
                    //ResultCodeのset
                    getActivity().setResult(RESULT_OK);
                }

            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_preference);

            mSp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String uriString = mSp.getString(getString(R.string.key_notification), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
            mUri = uriString == "" ? null : Uri.parse(uriString);

            //アラーム音の選択
            mAlarmPreference = findPreference(getString(R.string.key_notification));
            if(mAlarmPreference != null){
                //Summaryの設定
                mAlarmPreference.setSummary(getRingtoneTitle(mUri));
                //リスナの設定
                mAlarmPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        //Preferenceのタイトル
                        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.title_notification));
                        //アラームの表示(ぶっちゃけTYPE_RINGTONEと違いわからない)
                        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                        //デフォルトの選択
                        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mUri);
                        startActivityForResult(i, REQUEST_CODE_RINGTONE_PICKER);
                        return true;
                    }
                });
            }

            //MOLL Controllerについて
            Preference aboutMollContrillerPreference = findPreference(getString(R.string.key_about_moll_controller));
            if(aboutMollContrillerPreference != null) {
                aboutMollContrillerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        //多重起動の防止
                        if (getFragmentManager().findFragmentByTag(TAG_ABOUT_MOLL_CONTROLLER) == null) {
                            webViewDialogFragment dialogFragment = webViewDialogFragment.newInstance(getString(R.string.title_about_moll_controller), ABOUT_MOLL_CONTROLLER_URL);
                            dialogFragment.show(getFragmentManager(), TAG_ABOUT_MOLL_CONTROLLER);
                        }
                        return true;
                    }
                });
            }

            //ライセンス情報
            Preference licencePreference = findPreference(getString(R.string.key_license));
            if(licencePreference != null) {
                licencePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        Intent i = new Intent(getActivity(), LicenseActivity.class);
                        startActivity(i);
                        return true;
                    }
                });
            }


            //Summaryの設定
            ListAdapter adapter = getPreferenceScreen().getRootAdapter();
            for(int i=0; i<adapter.getCount();i++){
                Object item = adapter.getItem(i);
                if (item instanceof SeekBarPreference){
                    SeekBarPreference seekBarPreference = (SeekBarPreference) item;
                    seekBarPreference.setSummary(seekBarPreference.getStringValue() == null ? "" : seekBarPreference.getStringValue());
                }
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data){
            switch (requestCode){
                case REQUEST_CODE_RINGTONE_PICKER:
                    if(resultCode == RESULT_OK) {
                        mUri = (Uri) data.getExtras().get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        //Summaryのセット
                        mAlarmPreference.setSummary(getRingtoneTitle(mUri));
                        //SharedPreferencesにput
                        SharedPreferences.Editor editor = mSp.edit();
                        editor.putString(getString(R.string.key_notification), mUri == null ? "" : mUri.toString());

                        editor.apply();
                    }
                    break;
            }
        }

        @Override
        public void onResume(){
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }

        @Override
        public void onPause(){
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }

        //アラームのタイトルの取得
        private String getRingtoneTitle(Uri uri){
            //とりあえずなしで初期化
            String title = getString(R.string.text_no_ringtone);

            if(uri != null) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(getActivity(), uri);
                title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                if(uri.equals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))){
                    title = String.format(getString(R.string.text_default_ringtone_title), title);
                }
            }

            return title;
        }
    }
}
