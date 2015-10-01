package jp.ac.gunma_ct.elc.mollcontroller;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Chiharu on 2015/08/24.
 */
public class SeekBarPreference extends DialogPreference {

    private int mMin;
    private int mMax;
    private int mMagnification;
    private String mUnit;

    private int mValue;

    private SeekBar mSeekBar;
    private TextView mTextView;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs);
    }

    //初期設定
    public void init(AttributeSet attrs){
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);

        mMin = a.getInteger(R.styleable.SeekBarPreference_min,0);
        mMax = a.getInteger(R.styleable.SeekBarPreference_max,100);
        mMagnification = a.getInteger(R.styleable.SeekBarPreference_magnification, 1);
        mUnit = a.getString(R.styleable.SeekBarPreference_unit);

        a.recycle();
    }

    @Override
    public View onCreateDialogView(){

        View view = LayoutInflater.from(getContext()).inflate(R.layout.seek_bar_preference, null);

        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mTextView = (TextView) view.findViewById(R.id.text_view);

        //最大値の設定
        mSeekBar.setMax(mMax);

        //値の挿入
        mSeekBar.setProgress((mValue = getPersistedInt(0)) / mMagnification);
        mTextView.setText(String.valueOf(mValue));

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //progressの最小値を1にする
                if (progress < mMin) {
                    progress = mMin;
                    mSeekBar.setProgress(progress);
                }
                //TextViewに反映
                mTextView.setText(String.valueOf(progress * mMagnification));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //値を設定
                mValue = seekBar.getProgress() * mMagnification;

            }
        });

        return view;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if(restoreValue){
            mValue = getPersistedInt(0);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult){
        if(positiveResult){
            persistInt(mValue);
        }
        super.onDialogClosed(positiveResult);
    }

    public String getStringValue(){
        //単位を付けて返す
        return String.valueOf(mValue)+(mUnit == null?"":mUnit);
    }
}
