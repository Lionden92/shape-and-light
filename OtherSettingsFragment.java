package com.pathfinder.shapeandlight;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by Lionden on 10/6/2016.
 */
public class OtherSettingsFragment extends Fragment {

    private static final String LOG_TAG = OtherSettingsFragment.class.getSimpleName();

    protected SharedPreferences mPrefs;
    protected ScrollView mScrollView;
    private static final String OTHER_SETTINGS_SCRL_YPOS = "other_settings_scrollview_y_pos";

    // Other settings view objects and params.
    protected SLToggleButton mButBestLooks;
    protected SLToggleButton mButMedLooks;
    protected SLToggleButton mButLowLooks;
    protected SLToggleButton mButMinLight1;
    protected SLToggleButton mButMinLight2;
    protected SLToggleButton mButMinLight3;
    protected SLToggleButton mButMinLight4;
    protected SLToggleButton mButMinLight5;
    protected SLToggleButton mButAltSoundImp;
    protected SLToggleButton mButDisableTicks;
    protected SLToggleButton mButLessSens;
    protected SLToggleButton mButMedSens;
    protected SLToggleButton mButMoreSens;
    protected SLToggleButton mButFlipVert;
    protected SLToggleButton mButAltLock;
    protected SLToggleButton mButPerfHides;
    protected SLToggleButton mButShowWelc;
    protected Button mButAddOffset;
    protected Button mButSubOffset;
    protected Button mButCtrlSensHelp;
    protected Button mButVisSettingsHelp;
    protected Button mButRoutMusHelp;
    protected Button mButSoundFxHelp;
    protected TextView mOffsetText;
    protected int mVisualsLevel;
    protected float mMinLightFactor;
    protected boolean mAltSoundImp;
    protected boolean mDisableTicks;
    protected int mEyeSensFlag;
    protected boolean mFlipVertical;
    protected boolean mAltLock;
    protected boolean mPerfHidesUI;
    protected boolean mShowWelcome;
    protected int mPerfOffset;

    protected Dialog mHelpDialog;

    public OtherSettingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Other settings param inits.
        mVisualsLevel = mPrefs.getInt(SLSurfaceView.VISUALS_LEVEL, SLSimRenderer.LOW_VISUALS);
        mMinLightFactor = mPrefs.getFloat(SLSurfaceView.MIN_LIGHT_FACTOR,
                SLSimRenderer.MIN_LIGHT_FACTOR_3);
        mEyeSensFlag = mPrefs.getInt(SLSurfaceView.EYE_SENSITIVITY, SLSimRenderer.MED_SENSITIVITY);
        mAltSoundImp = mPrefs.getBoolean(SLSurfaceView.ALT_SOUND_SYSTEM, false);
        mDisableTicks = !mPrefs.getBoolean(SLSurfaceView.PLAY_UI_TICKS, true);
        mFlipVertical = mPrefs.getBoolean(SLSurfaceView.EYE_FLIP_VERT, false);
        mAltLock = mPrefs.getBoolean(SLSurfaceView.EYE_ALT_LOCK, true);
        mPerfHidesUI = mPrefs.getBoolean(SLSurfaceView.PERFORM_HIDE_UI, false);
        mShowWelcome = mPrefs.getBoolean(SLSurfaceView.SHOW_WELCOME_DIALOG, true);
        mPerfOffset = mPrefs.getInt(SLSurfaceView.PERFORM_OFFSET_INT, 0);
        mHelpDialog = new Dialog(getActivity(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.other_settings_fragment, container, false);
        mScrollView = rootView.findViewById(R.id.settings_scroller);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(0, mPrefs.getInt(OTHER_SETTINGS_SCRL_YPOS, 0));
                mScrollView.setVisibility(View.VISIBLE);
            }
        });
        Drawable onIcon;
        Drawable offIcon;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onIcon = getResources().getDrawable(R.drawable.ic_check_box_on_03, null);
            offIcon = getResources().getDrawable(R.drawable.ic_check_box_blank, null);
        } else {
            onIcon = getResources().getDrawable(R.drawable.ic_check_box_on_03);
            offIcon = getResources().getDrawable(R.drawable.ic_check_box_blank);
        }
        Drawable radioOnIcon = getResources().getDrawable(R.drawable.ic_custom_radio_lit_02);
        Drawable radioOffWeakest = getResources().getDrawable(R.drawable.ic_weakest_radio_02);
        Drawable radioOffWeaker = getResources().getDrawable(R.drawable.ic_weaker_radio_02);
        Drawable radioOffMedium = getResources().getDrawable(R.drawable.ic_medium_radio_02);
        Drawable radioOffStronger = getResources().getDrawable(R.drawable.ic_stronger_radio_03);
        Drawable radioOffStrongest = getResources().getDrawable(R.drawable.ic_strongest_radio_03);
        // Other settings view object instantiations.
        mButBestLooks = rootView.findViewById(R.id.visuals_high_select);
        mButMedLooks = rootView.findViewById(R.id.visuals_med_select);
        mButLowLooks = rootView.findViewById(R.id.visuals_low_select);
        mButMinLight1 = rootView.findViewById(R.id.background_light_1);
        mButMinLight2 = rootView.findViewById(R.id.background_light_2);
        mButMinLight3 = rootView.findViewById(R.id.background_light_3);
        mButMinLight4 = rootView.findViewById(R.id.background_light_4);
        mButMinLight5 = rootView.findViewById(R.id.background_light_5);
        mButAltSoundImp = rootView.findViewById(R.id.alt_sound_imp_checkbox);
        mButDisableTicks = rootView.findViewById(R.id.disable_ui_ticks_checkbox);
        mButLessSens = rootView.findViewById(R.id.less_sens_select);
        mButMedSens = rootView.findViewById(R.id.medium_sens_select);
        mButMoreSens = rootView.findViewById(R.id.more_sens_select);
        mButFlipVert = rootView.findViewById(R.id.flip_vert_checkbox);
        mButAltLock = rootView.findViewById(R.id.alt_lock_checkbox);
        mButPerfHides = rootView.findViewById(R.id.perf_hides_ui_checkbox);
        mButShowWelc = rootView.findViewById(R.id.show_welcome_screen);
        mButSubOffset = rootView.findViewById(R.id.offset_minus_button);
        mButAddOffset = rootView.findViewById(R.id.offset_plus_button);
        mButCtrlSensHelp = rootView.findViewById(R.id.ctrlsens_help_button);
        mButVisSettingsHelp = rootView.findViewById(R.id.visuals_help_button);
        mButRoutMusHelp = rootView.findViewById(R.id.rout_mus_help_button);
        mButSoundFxHelp = rootView.findViewById(R.id.sound_fx_help_button);
        mOffsetText = rootView.findViewById(R.id.offset_value_text);
        // Other settings view object setup/initializations.
        mButBestLooks.setIcons(radioOnIcon, radioOffStrongest);
        mButBestLooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVisualsLevel = SLSimRenderer.HIGH_VISUALS;
                setVisuals();
            }
        });
        mButMedLooks.setIcons(radioOnIcon, radioOffStronger);
        mButMedLooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVisualsLevel = SLSimRenderer.MED_VISUALS;
                setVisuals();
            }
        });
        mButLowLooks.setIcons(radioOnIcon, radioOffMedium);
        mButLowLooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVisualsLevel = SLSimRenderer.LOW_VISUALS;
                setVisuals();
            }
        });
        setVisuals();
        mButMinLight1.setIcons(radioOnIcon, radioOffWeakest);
        mButMinLight1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMinLightFactor = SLSimRenderer.MIN_LIGHT_FACTOR_1;
                setMinLighting();
            }
        });
        mButMinLight2.setIcons(radioOnIcon, radioOffWeaker);
        mButMinLight2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMinLightFactor = SLSimRenderer.MIN_LIGHT_FACTOR_2;
                setMinLighting();
            }
        });
        mButMinLight3.setIcons(radioOnIcon, radioOffMedium);
        mButMinLight3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMinLightFactor = SLSimRenderer.MIN_LIGHT_FACTOR_3;
                setMinLighting();
            }
        });
        mButMinLight4.setIcons(radioOnIcon, radioOffStronger);
        mButMinLight4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMinLightFactor = SLSimRenderer.MIN_LIGHT_FACTOR_4;
                setMinLighting();
            }
        });
        mButMinLight5.setIcons(radioOnIcon, radioOffStrongest);
        mButMinLight5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMinLightFactor = SLSimRenderer.MIN_LIGHT_FACTOR_5;
                setMinLighting();
            }
        });
        setMinLighting();
        mButAltSoundImp.setIcons(onIcon, offIcon);
        mButAltSoundImp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAltSoundImp = !mAltSoundImp;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.ALT_SOUND_SYSTEM, mAltSoundImp);
                editor.apply();
                mButAltSoundImp.setOn(mAltSoundImp);
            }
        });
        mButAltSoundImp.setOn(mAltSoundImp);
        mButDisableTicks.setIcons(onIcon, offIcon);
        mButDisableTicks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDisableTicks = !mDisableTicks;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.PLAY_UI_TICKS, !mDisableTicks);
                editor.apply();
                mButDisableTicks.setOn(mDisableTicks);
            }
        });
        mButDisableTicks.setOn(mDisableTicks);

        mButLessSens.setIcons(radioOnIcon, radioOffWeakest);
        mButLessSens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEyeSensFlag = SLSimRenderer.LOW_SENSITIVITY;
                setEyeSensitivity();
            }
        });
        mButMedSens.setIcons(radioOnIcon, radioOffMedium);
        mButMedSens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEyeSensFlag = SLSimRenderer.MED_SENSITIVITY;
                setEyeSensitivity();
            }
        });
        mButMoreSens.setIcons(radioOnIcon, radioOffStrongest);
        mButMoreSens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEyeSensFlag = SLSimRenderer.HIGH_SENSITIVITY;
                setEyeSensitivity();
            }
        });
        setEyeSensitivity();
        mButFlipVert.setIcons(onIcon, offIcon);
        mButFlipVert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFlipVertical = !mFlipVertical;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.EYE_FLIP_VERT, mFlipVertical);
                editor.apply();
                mButFlipVert.setOn(mFlipVertical);
            }
        });
        mButFlipVert.setOn(mFlipVertical);
        mButAltLock.setIcons(onIcon, offIcon);
        mButAltLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAltLock = !mAltLock;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.EYE_ALT_LOCK, mAltLock);
                editor.apply();
                mButAltLock.setOn(mAltLock);
            }
        });
        mButAltLock.setOn(mAltLock);
        mButPerfHides.setIcons(onIcon, offIcon);
        mButPerfHides.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPerfHidesUI = !mPerfHidesUI;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.PERFORM_HIDE_UI, mPerfHidesUI);
                editor.apply();
                mButPerfHides.setOn(mPerfHidesUI);
            }
        });
        mButPerfHides.setOn(mPerfHidesUI);
        mButShowWelc.setIcons(onIcon, offIcon);
        mButShowWelc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShowWelcome = !mShowWelcome;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.SHOW_WELCOME_DIALOG, mShowWelcome);
                editor.apply();
                mButShowWelc.setOn(mShowWelcome);
            }
        });
        mButSubOffset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPerfOffset -= 50;
                if (mPerfOffset < -1000) mPerfOffset = -1000;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putInt(SLSurfaceView.PERFORM_OFFSET_INT, mPerfOffset);
                editor.apply();
                mOffsetText.setText(Integer.toString(mPerfOffset));
            }
        });
        mButAddOffset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPerfOffset += 50;
                if (mPerfOffset > 1000) mPerfOffset = 1000;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putInt(SLSurfaceView.PERFORM_OFFSET_INT, mPerfOffset);
                editor.apply();
                mOffsetText.setText(Integer.toString(mPerfOffset));
            }
        });
        mOffsetText.setText(Integer.toString(mPerfOffset));
        mButShowWelc.setOn(mShowWelcome);
        mButCtrlSensHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.eyecontrol_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        mButVisSettingsHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.visualsettings_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        mButRoutMusHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.routine_mus_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        mButSoundFxHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.sound_fx_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        return rootView;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(OTHER_SETTINGS_SCRL_YPOS, mScrollView.getScrollY());
        editor.commit();
    }

    public void setVisuals() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(SLSurfaceView.VISUALS_LEVEL, mVisualsLevel);
        editor.apply();
        mButBestLooks.setOn(mVisualsLevel == SLSimRenderer.HIGH_VISUALS);
        mButMedLooks.setOn(mVisualsLevel == SLSimRenderer.MED_VISUALS);
        mButLowLooks.setOn(mVisualsLevel == SLSimRenderer.LOW_VISUALS);
    }

    public void setMinLighting() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putFloat(SLSurfaceView.MIN_LIGHT_FACTOR, mMinLightFactor);
        editor.apply();
        float tol = 0.1f;
        mButMinLight1.setOn(mMinLightFactor > SLSimRenderer.MIN_LIGHT_FACTOR_1 - tol
                && mMinLightFactor < SLSimRenderer.MIN_LIGHT_FACTOR_1 + tol);
        mButMinLight2.setOn(mMinLightFactor > SLSimRenderer.MIN_LIGHT_FACTOR_2 - tol
                && mMinLightFactor < SLSimRenderer.MIN_LIGHT_FACTOR_2 + tol);
        mButMinLight3.setOn(mMinLightFactor > SLSimRenderer.MIN_LIGHT_FACTOR_3 - tol
                && mMinLightFactor < SLSimRenderer.MIN_LIGHT_FACTOR_3 + tol);
        mButMinLight4.setOn(mMinLightFactor > SLSimRenderer.MIN_LIGHT_FACTOR_4 - tol
                && mMinLightFactor < SLSimRenderer.MIN_LIGHT_FACTOR_4 + tol);
        mButMinLight5.setOn(mMinLightFactor > SLSimRenderer.MIN_LIGHT_FACTOR_5 - tol
                && mMinLightFactor < SLSimRenderer.MIN_LIGHT_FACTOR_5 + tol);
    }

    public void setEyeSensitivity() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(SLSurfaceView.EYE_SENSITIVITY, mEyeSensFlag);
        editor.apply();
        mButLessSens.setOn(mEyeSensFlag == SLSimRenderer.LOW_SENSITIVITY);
        mButMedSens.setOn(mEyeSensFlag == SLSimRenderer.MED_SENSITIVITY);
        mButMoreSens.setOn(mEyeSensFlag == SLSimRenderer.HIGH_SENSITIVITY);
    }

}
