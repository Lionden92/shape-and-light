package com.pathfinder.shapeandlight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by Lionden on 10/3/2016.
 */
@SuppressLint("AppCompatCustomView")
public class SLToggleButton extends Button {

    protected boolean mOn;
    protected Drawable mOnIcon;
    protected Drawable mOffIcon;

    public SLToggleButton(Context context) {
        super(context);
    }

    public SLToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SLToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setIcons(Drawable onIcon, Drawable offIcon) {
        mOnIcon = onIcon;
        mOffIcon = offIcon;
    }

    public void setOn(boolean on) {
        mOn = on;
        Drawable icon = on ? mOnIcon : mOffIcon;
        setBackground(icon);
    }

    public boolean isOn() {
        return mOn;
    }


}
