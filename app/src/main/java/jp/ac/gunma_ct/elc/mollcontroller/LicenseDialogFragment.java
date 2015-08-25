package jp.ac.gunma_ct.elc.mollcontroller;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by Chiharu on 2015/08/25.
 */
public class LicenseDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "TITLE";
    private static final String ARG_URL = "URL";

    public static LicenseDialogFragment newInstance(String title,String url){
        LicenseDialogFragment dialogFragment = new LicenseDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE,title);
        args.putString(ARG_URL,url);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //タイトルの設定
        builder.setTitle(getArguments().getString(ARG_TITLE));

        //ライセンスの表示
        WebView webView = new WebView(getActivity());
        webView.loadUrl(getArguments().getString(ARG_URL));

        builder.setView(webView);

        return builder.create();
    }
}
