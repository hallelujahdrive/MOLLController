package jp.ac.gunma_ct.elc.mollcontroller;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * TODO: document your custom view class.
 */
public class MenuItemView extends TextView {
    private static final int[] CHECKED_STATE_SET = new int[]{android.R.attr.state_checked};
    private static final int[] DISABLED_STATE_SET = new int[]{-16842910};

    private int mIconSize;

    private ColorStateList mIconTintList;

    public MenuItemView(Context context) {
        this(context, null);
    }

    public MenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.menuItemViewStyle);
    }

    public MenuItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void init(AttributeSet attrs, int defStyle){
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MenuItemView, defStyle, 0);

        mIconSize=getResources().getDimensionPixelSize(android.support.design.R.dimen.navigation_icon_size);

        //iconTintListのset
        if(a.hasValue(R.styleable.MenuItemView_itemIconTint)){
            mIconTintList = a.getColorStateList(R.styleable.MenuItemView_itemIconTint);
        }else {
            mIconTintList = createDefaultColorStateList(android.R.attr.textColorSecondary);
        }

        if(getBackground() == null) {
            setBackgroundDrawable(createDefaultBackground());
        }

        setIcon(a.getDrawable(R.styleable.MenuItemView_android_icon));

    }

    private StateListDrawable createDefaultBackground() {
        TypedValue value = new TypedValue();
        if(this.getContext().getTheme().resolveAttribute(android.support.design.R.attr.colorControlHighlight, value, true)) {
            StateListDrawable drawable = new StateListDrawable();
            drawable.addState(CHECKED_STATE_SET, new ColorDrawable(value.data));
            drawable.addState(EMPTY_STATE_SET, new ColorDrawable(0));
            return drawable;
        } else {
            return null;
        }
    }

    public void setIcon(Drawable icon) {
        if(icon !=null) {
            icon= DrawableCompat.wrap(icon);
            icon.mutate();
            icon.setBounds(0, 0, mIconSize, mIconSize);
            DrawableCompat.setTintList(icon, this.mIconTintList);
        }

        setCompoundDrawables(icon,null,null,null);
    }

    private ColorStateList createDefaultColorStateList(int baseColorThemeAttr) {
        TypedValue value = new TypedValue();
        if(!getContext().getTheme().resolveAttribute(baseColorThemeAttr, value, true)) {
            return null;
        } else {
            ColorStateList baseColor = getResources().getColorStateList(value.resourceId);
            if(!this.getContext().getTheme().resolveAttribute(android.support.design.R.attr.colorPrimary, value, true)) {
                return null;
            } else {
                int colorPrimary = value.data;
                int defaultColor = baseColor.getDefaultColor();
                return new ColorStateList(new int[][]{DISABLED_STATE_SET, CHECKED_STATE_SET, EMPTY_STATE_SET}, new int[]{baseColor.getColorForState(DISABLED_STATE_SET, defaultColor), colorPrimary, defaultColor});
            }
        }
    }
}