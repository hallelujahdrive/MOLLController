package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
        fragmentManager.beginTransaction().replace(R.id.container,new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener =new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                //ListPreferenceならsummeryにセット
                Preference preference=findPreference(key);
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
            addPreferencesFromResource(R.xml.preference);

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
        public void onResume(){
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }

        @Override
        public void onPause(){
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }
    }
}
