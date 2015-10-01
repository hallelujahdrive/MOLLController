package jp.ac.gunma_ct.elc.mollcontroller;

import android.content.res.TypedArray;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class LicenseActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String ARG_TITLE = "TITLE";
    private static final String ARG_URL = "URL";

    private static final String TAG_LICENSE = "LICENSE";

    private static final String[] HTML_URLS = {
            "file:///android_asset/open_source_license.html",
            "file:///android_asset/creative_commons_license.html"
    };

    private ListView mListView;
    private ListAdapter mAdapter;

    private ArrayList<String> mTitleArray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        mListView = (ListView) findViewById(android.R.id.list);

        //action baの設定
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar=getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TypedArray typedArray = getResources().obtainTypedArray(R.array.license_title);
        for(int i=0;i<typedArray.length();i++){
            mTitleArray.add(typedArray.getString(i));
        }

        // Set the adapter
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, mTitleArray);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //多重起動の防止
        if(getFragmentManager().findFragmentByTag(TAG_LICENSE) == null) {
            webViewDialogFragment dialogFragment = webViewDialogFragment.newInstance(mTitleArray.get(position), HTML_URLS[position]);
            dialogFragment.show(getFragmentManager(), TAG_LICENSE);
        }
    }
}
