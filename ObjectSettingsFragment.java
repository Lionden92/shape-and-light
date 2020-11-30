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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by Lionden on 10/6/2016.
 */
public class ObjectSettingsFragment extends Fragment {

    private static final String LOG_TAG = ObjectSettingsFragment.class.getSimpleName();

    protected SharedPreferences mPrefs;
    protected ScrollView mScrollView;
    private static final String OBJ_SETTINGS_SCRL_YPOS = "obj_settings_scrollview_y_pos";

    // Shape settings view objects and params.
    protected ImageView mThumb01;
    protected ImageView mThumb02;
    protected ImageView mThumb03;
    protected ImageView mThumb04;
    protected ImageView mThumb05;
    protected Button mButShapeDecalHlp;
    protected Button mButDecalAttr;
    protected int mSelectedDecal;

    // Light settings view objects and params.
    protected ImageView mLight0Ic;
    protected ImageView mLight1Ic;
    protected ImageView mLight2Ic;
    protected SLToggleButton mButLight0Tog;
    protected SLToggleButton mButLight1Tog;
    protected SLToggleButton mButLight2Tog;
    protected SLToggleButton mButLight0Str;
    protected SLToggleButton mButLight1Str;
    protected SLToggleButton mButLight2Str;
    protected SLToggleButton mButLight0Br;
    protected SLToggleButton mButLight1Br;
    protected SLToggleButton mButLight2Br;
    protected TextView mLight0StrTxt;
    protected TextView mLight1StrTxt;
    protected TextView mLight2StrTxt;
    protected TextView mLight0BrTxt;
    protected TextView mLight1BrTxt;
    protected TextView mLight2BrTxt;
    protected Button mButLightPtsHlp;
    protected boolean mIsLight0On;
    protected boolean mIsLight1On;
    protected boolean mIsLight2On;
    protected boolean mIsLight0Strobe;
    protected boolean mIsLight1Strobe;
    protected boolean mIsLight2Strobe;
    protected boolean mIsLight0Bright;
    protected boolean mIsLight1Bright;
    protected boolean mIsLight2Bright;

    // Gravity settings view objects and params.
    protected SLToggleButton mMovRingCheckBox;
    protected SLToggleButton mMovRingExclusiveBox;
    protected SLToggleButton mButCtrRingSpring;
    protected SLToggleButton mButCtrRingSpringSq;
    protected SLToggleButton mButCtrRingWeakest;
    protected SLToggleButton mButCtrRingWeaker;
    protected SLToggleButton mButCtrRingMedium;
    protected SLToggleButton mButCtrRingStronger;
    protected SLToggleButton mButCtrRingStrongest;
    protected SLToggleButton mButMovRingSpring;
    protected SLToggleButton mButMovRingSpringSq;
    protected SLToggleButton mButMovRingWeakest;
    protected SLToggleButton mButMovRingWeaker;
    protected SLToggleButton mButMovRingMedium;
    protected SLToggleButton mButMovRingStronger;
    protected SLToggleButton mButMovRingStrongest;
    protected TextView mGravTypeTitle;
    protected TextView mGravStrengthTitle;
    protected TextView mGravTypeSpring;
    protected TextView mGravTypeSpringSq;
    protected TextView mMovExclusiveTitle;
    protected Button mGravRingHelpDialog;
    protected Button mMobRingHelpDialog;
    protected boolean mCtrRingSpringSq;
    protected float mCtrRingStrength;
    protected boolean mMovRingSpringSq;
    protected float mMovRingStrength;
    protected int mGravRingCount;
    protected boolean mMovRingExclusive;


    protected Dialog mHelpDialog;

    public ObjectSettingsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Shape & Lights param inits.
        mIsLight0On = mPrefs.getBoolean(SLSurfaceView.LIGHT0_ON, true);
        mIsLight1On = mPrefs.getBoolean(SLSurfaceView.LIGHT1_ON, true);
        mIsLight2On = mPrefs.getBoolean(SLSurfaceView.LIGHT2_ON, true);
        mIsLight0Strobe = mPrefs.getBoolean(SLSurfaceView.LIGHT0_STROBE, false);
        mIsLight1Strobe = mPrefs.getBoolean(SLSurfaceView.LIGHT1_STROBE, false);
        mIsLight2Strobe = mPrefs.getBoolean(SLSurfaceView.LIGHT2_STROBE, false);
        mIsLight0Bright = mPrefs.getBoolean(SLSurfaceView.LIGHT0_BRIGHT, false);
        mIsLight1Bright = mPrefs.getBoolean(SLSurfaceView.LIGHT1_BRIGHT, false);
        mIsLight2Bright = mPrefs.getBoolean(SLSurfaceView.LIGHT2_BRIGHT, true);
        mSelectedDecal = mPrefs.getInt(SLSurfaceView.SHAPE_DECAL, SLSimRenderer.STAR_DECAL);
        // Gravity ring param inits.
        mCtrRingSpringSq = mPrefs.getBoolean(SLSurfaceView.CTR_RING_SPRINGSQ, false);
        mMovRingSpringSq = mPrefs.getBoolean(SLSurfaceView.MOV_RING_SPRINGSQ, true);
        mCtrRingStrength = mPrefs.getFloat(SLSurfaceView.CTR_RING_STRENGTH, 2.0f);
        mMovRingStrength = mPrefs.getFloat(SLSurfaceView.MOV_RING_STRENGTH, 3.0f);
        mGravRingCount = mPrefs.getInt(SLSurfaceView.GRAV_RING_COUNT, 2);
        mMovRingExclusive = mPrefs.getBoolean(SLSurfaceView.MOV_RING_EXCLUSIVE, false);
        mHelpDialog = new Dialog(getActivity(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
    }


    @SuppressWarnings("deprecation")
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.obj_settings_fragment, container, false);
        mScrollView = rootView.findViewById(R.id.settings_scroller);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(0, mPrefs.getInt(OBJ_SETTINGS_SCRL_YPOS, 0));
                mScrollView.setVisibility(View.VISIBLE);
            }
        });
        // Shape & Lights view object instantiations.
        mThumb01 = rootView.findViewById(R.id.decal_thumb_01);
        mThumb02 = rootView.findViewById(R.id.decal_thumb_02);
        mThumb03 = rootView.findViewById(R.id.decal_thumb_03);
        mThumb04 = rootView.findViewById(R.id.decal_thumb_04);
        mThumb05 = rootView.findViewById(R.id.decal_thumb_05);
        mLight0Ic = rootView.findViewById(R.id.light_01_icon);
        mLight1Ic = rootView.findViewById(R.id.light_02_icon);
        mLight2Ic = rootView.findViewById(R.id.light_03_icon);
        mButLight0Tog = rootView.findViewById(R.id.light_01_on);
        mButLight1Tog = rootView.findViewById(R.id.light_02_on);
        mButLight2Tog = rootView.findViewById(R.id.light_03_on);
        mButLight0Str = rootView.findViewById(R.id.light_01_strobe);
        mButLight1Str = rootView.findViewById(R.id.light_02_strobe);
        mButLight2Str = rootView.findViewById(R.id.light_03_strobe);
        mLight0StrTxt = rootView.findViewById(R.id.light_01_strobe_text);
        mLight1StrTxt = rootView.findViewById(R.id.light_02_strobe_text);
        mLight2StrTxt = rootView.findViewById(R.id.light_03_strobe_text);
        mButLight0Br = rootView.findViewById(R.id.light_01_bright);
        mButLight1Br = rootView.findViewById(R.id.light_02_bright);
        mButLight2Br = rootView.findViewById(R.id.light_03_bright);
        mLight0BrTxt = rootView.findViewById(R.id.light_01_bright_text);
        mLight1BrTxt = rootView.findViewById(R.id.light_02_bright_text);
        mLight2BrTxt = rootView.findViewById(R.id.light_03_bright_text);
        mButShapeDecalHlp = rootView.findViewById(R.id.shape_decal_help_button);
        mButLightPtsHlp = rootView.findViewById(R.id.lightpts_help_button);
        mButDecalAttr = rootView.findViewById(R.id.shape_decal_attr_button);
        // Shape & Lights view object setup/initializations.
        setLight0IconColors();
        setLight1IconColors();
        setLight2IconColors();
        setThumbStates();
        mThumb01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedDecal = SLSimRenderer.STAR_DECAL;
                setShapeDecal();
            }
        });
        mThumb02.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedDecal = SLSimRenderer.ROSE_DECAL;
                setShapeDecal();
            }
        });
        mThumb03.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedDecal = SLSimRenderer.EYE_DECAL;
                setShapeDecal();
            }
        });
        mThumb04.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedDecal = SLSimRenderer.BFLY_DECAL;
                setShapeDecal();
            }
        });
        mThumb05.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedDecal = SLSimRenderer.FISH_DECAL;
                setShapeDecal();
            }
        });
        Drawable onIcon;
        Drawable offIcon;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            onIcon = getResources().getDrawable(R.drawable.ic_check_box_on_03, null);
            offIcon = getResources().getDrawable(R.drawable.ic_check_box_blank, null);
        } else {
            onIcon = getResources().getDrawable(R.drawable.ic_check_box_on_03);
            offIcon = getResources().getDrawable(R.drawable.ic_check_box_blank);
        }
        mButLight0Tog.setIcons(onIcon, offIcon);
        mButLight0Tog.setOn(mIsLight0On);
        mButLight0Tog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight0On = !mIsLight0On;
                mButLight0Tog.setOn(mIsLight0On);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT0_ON, mIsLight0On);
                editor.apply();
                // Enable/disable strobe check box.
                enableLightOptions(mIsLight0On, mLight0StrTxt, mButLight0Str, mIsLight0Strobe,
                        mLight0BrTxt, mButLight0Br, mIsLight0Bright);
                setLight0IconColors();
            }
        });
        mButLight1Tog.setIcons(onIcon, offIcon);
        mButLight1Tog.setOn(mIsLight1On);
        mButLight1Tog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight1On = !mIsLight1On;
                mButLight1Tog.setOn(mIsLight1On);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT1_ON, mIsLight1On);
                editor.apply();
                // Enable/disable strobe check box.
                enableLightOptions(mIsLight1On, mLight1StrTxt, mButLight1Str, mIsLight1Strobe,
                        mLight1BrTxt, mButLight1Br, mIsLight1Bright);
                setLight1IconColors();
            }
        });
        mButLight2Tog.setIcons(onIcon, offIcon);
        mButLight2Tog.setOn(mIsLight2On);
        mButLight2Tog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight2On = !mIsLight2On;
                mButLight2Tog.setOn(mIsLight2On);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT2_ON, mIsLight2On);
                editor.apply();
                // Enable/disable strobe check box.
                enableLightOptions(mIsLight2On, mLight2StrTxt, mButLight2Str, mIsLight2Strobe,
                        mLight2BrTxt, mButLight2Br, mIsLight2Bright);
                setLight2IconColors();
            }
        });
        mButLight0Str.setIcons(onIcon, offIcon);
        mButLight0Str.setOn(mIsLight0Strobe);
        mButLight0Str.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight0Strobe = !mIsLight0Strobe;
                mButLight0Str.setOn(mIsLight0Strobe);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT0_STROBE, mIsLight0Strobe);
                editor.apply();
            }
        });
        mButLight1Str.setIcons(onIcon, offIcon);
        mButLight1Str.setOn(mIsLight1Strobe);
        mButLight1Str.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight1Strobe = !mIsLight1Strobe;
                mButLight1Str.setOn(mIsLight1Strobe);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT1_STROBE, mIsLight1Strobe);
                editor.apply();
            }
        });
        mButLight2Str.setIcons(onIcon, offIcon);
        mButLight2Str.setOn(mIsLight2Strobe);
        mButLight2Str.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight2Strobe = !mIsLight2Strobe;
                mButLight2Str.setOn(mIsLight2Strobe);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT2_STROBE, mIsLight2Strobe);
                editor.apply();
            }
        });
        mButLight0Br.setIcons(onIcon, offIcon);
        mButLight0Br.setOn(mIsLight0Bright);
        mButLight0Br.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight0Bright = !mIsLight0Bright;
                mButLight0Br.setOn(mIsLight0Bright);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT0_BRIGHT, mIsLight0Bright);
                editor.apply();
            }
        });
        mButLight1Br.setIcons(onIcon, offIcon);
        mButLight1Br.setOn(mIsLight1Bright);
        mButLight1Br.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight1Bright = !mIsLight1Bright;
                mButLight1Br.setOn(mIsLight1Bright);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT1_BRIGHT, mIsLight1Bright);
                editor.apply();
            }
        });
        mButLight2Br.setIcons(onIcon, offIcon);
        mButLight2Br.setOn(mIsLight2Bright);
        mButLight2Br.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsLight2Bright = !mIsLight2Bright;
                mButLight2Br.setOn(mIsLight2Bright);
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.LIGHT2_BRIGHT, mIsLight2Bright);
                editor.apply();
            }
        });
        enableLightOptions(mIsLight0On, mLight0StrTxt, mButLight0Str, mIsLight0Strobe,
                mLight0BrTxt, mButLight0Br, mIsLight0Bright);
        enableLightOptions(mIsLight1On, mLight1StrTxt, mButLight1Str, mIsLight1Strobe,
                mLight1BrTxt, mButLight1Br, mIsLight1Bright);
        enableLightOptions(mIsLight2On, mLight2StrTxt, mButLight2Str, mIsLight2Strobe,
                mLight2BrTxt, mButLight2Br, mIsLight2Bright);
        mButShapeDecalHlp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.shape_decal_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        mButLightPtsHlp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.lightpoints_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        mButDecalAttr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.decal_attr_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        // Gravity ring settings view object instantiations.
        mButCtrRingSpring = rootView.findViewById(R.id.spring_select);
        mButCtrRingSpringSq = rootView.findViewById(R.id.springsq_select);
        mButCtrRingWeakest = rootView.findViewById(R.id.weakest_select);
        mButCtrRingWeaker = rootView.findViewById(R.id.weaker_select);
        mButCtrRingMedium = rootView.findViewById(R.id.medium_select);
        mButCtrRingStronger = rootView.findViewById(R.id.stronger_select);
        mButCtrRingStrongest = rootView.findViewById(R.id.strongest_select);
        mMovRingCheckBox = rootView.findViewById(R.id.enableFreeGravCheck);
        mMovRingExclusiveBox = rootView.findViewById(R.id.exclusiveFreeGravCheck);
        mButMovRingSpring = rootView.findViewById(R.id.spring_select2);
        mButMovRingSpringSq = rootView.findViewById(R.id.springsq_select2);
        mButMovRingWeakest = rootView.findViewById(R.id.weakest_select2);
        mButMovRingWeaker = rootView.findViewById(R.id.weaker_select2);
        mButMovRingMedium = rootView.findViewById(R.id.medium_select2);
        mButMovRingStronger = rootView.findViewById(R.id.stronger_select2);
        mButMovRingStrongest = rootView.findViewById(R.id.strongest_select2);
        mMovExclusiveTitle = rootView.findViewById(R.id.exclusiveFreeGravText);
        mGravTypeTitle = rootView.findViewById(R.id.gravity_type_title);
        mGravStrengthTitle = rootView.findViewById(R.id.gravity_strength_title);
        mGravTypeSpring = rootView.findViewById(R.id.gravity_type_spring);
        mGravTypeSpringSq = rootView.findViewById(R.id.gravity_type_springsq);
        mGravRingHelpDialog = rootView.findViewById(R.id.ctr_gravring_help_button);
        mMobRingHelpDialog = rootView.findViewById(R.id.mob_gravring_help_button);
        // Gravity ring settings view object setup/initializations.
        Drawable radioOnIcon = getResources().getDrawable(R.drawable.ic_custom_radio_lit_02);
        Drawable radioOffWeakest = getResources().getDrawable(R.drawable.ic_weakest_radio_02);
        Drawable radioOffWeaker = getResources().getDrawable(R.drawable.ic_weaker_radio_02);
        Drawable radioOffMedium = getResources().getDrawable(R.drawable.ic_medium_radio_02);
        Drawable radioOffStronger = getResources().getDrawable(R.drawable.ic_stronger_radio_03);
        Drawable radioOffStrongest = getResources().getDrawable(R.drawable.ic_strongest_radio_03);
        Drawable radioSpringOn = getResources().getDrawable(R.drawable.ic_spring_radio_lit);
        Drawable radioSpringOff = getResources().getDrawable(R.drawable.ic_spring_radio);
        Drawable radioSpringSqOn = getResources().getDrawable(R.drawable.ic_springsq_radio_lit);
        Drawable radioSpringSqOff = getResources().getDrawable(R.drawable.ic_springsq_radio);
        mButCtrRingSpring.setIcons(radioSpringOn, radioSpringOff);
        mButCtrRingSpring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingSpringSq = false;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.CTR_RING_SPRINGSQ, mCtrRingSpringSq);
                editor.apply();
                setCtrRingToggles();
            }
        });
        mButCtrRingSpringSq.setIcons(radioSpringSqOn, radioSpringSqOff);
        mButCtrRingSpringSq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingSpringSq = true;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.CTR_RING_SPRINGSQ, mCtrRingSpringSq);
                editor.apply();
                setCtrRingToggles();
            }
        });
        mButCtrRingWeakest.setIcons(radioOnIcon, radioOffWeakest);
        mButCtrRingWeakest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingStrength = 1.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.CTR_RING_STRENGTH, mCtrRingStrength);
                editor.apply();
                setCtrRingToggles();
            }
        });
        mButCtrRingWeaker.setIcons(radioOnIcon, radioOffWeaker);
        mButCtrRingWeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingStrength = 2.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.CTR_RING_STRENGTH, mCtrRingStrength);
                editor.apply();
                setCtrRingToggles();
            }
        });
        mButCtrRingMedium.setIcons(radioOnIcon, radioOffMedium);
        mButCtrRingMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingStrength = 3.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.CTR_RING_STRENGTH, mCtrRingStrength);
                editor.apply();
                setCtrRingToggles();
            }
        });
        mButCtrRingStronger.setIcons(radioOnIcon, radioOffStronger);
        mButCtrRingStronger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingStrength = 4.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.CTR_RING_STRENGTH, mCtrRingStrength);
                editor.apply();
                setCtrRingToggles();
            }
        });
        mButCtrRingStrongest.setIcons(radioOnIcon, radioOffStrongest);
        mButCtrRingStrongest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtrRingStrength = 5.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.CTR_RING_STRENGTH, mCtrRingStrength);
                editor.apply();
                setCtrRingToggles();
            }
        });
        setCtrRingToggles();
        mButMovRingSpring.setIcons(radioSpringOn, radioSpringOff);
        mButMovRingSpring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingSpringSq = false;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.MOV_RING_SPRINGSQ, mMovRingSpringSq);
                editor.apply();
                setMovRingToggles();
            }
        });
        mButMovRingSpringSq.setIcons(radioSpringSqOn, radioSpringSqOff);
        mButMovRingSpringSq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingSpringSq = true;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.MOV_RING_SPRINGSQ, mMovRingSpringSq);
                editor.apply();
                setMovRingToggles();
            }
        });
        mButMovRingWeakest.setIcons(radioOnIcon, radioOffWeakest);
        mButMovRingWeakest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingStrength = 1.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.MOV_RING_STRENGTH, mMovRingStrength);
                editor.apply();
                setMovRingToggles();
            }
        });
        mButMovRingWeaker.setIcons(radioOnIcon, radioOffWeaker);
        mButMovRingWeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingStrength = 2.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.MOV_RING_STRENGTH, mMovRingStrength);
                editor.apply();
                setMovRingToggles();
            }
        });
        mButMovRingMedium.setIcons(radioOnIcon, radioOffMedium);
        mButMovRingMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingStrength = 3.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.MOV_RING_STRENGTH, mMovRingStrength);
                editor.apply();
                setMovRingToggles();
            }
        });
        mButMovRingStronger.setIcons(radioOnIcon, radioOffStronger);
        mButMovRingStronger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingStrength = 4.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.MOV_RING_STRENGTH, mMovRingStrength);
                editor.apply();
                setMovRingToggles();
            }
        });
        mButMovRingStrongest.setIcons(radioOnIcon, radioOffStrongest);
        mButMovRingStrongest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingStrength = 5.0f;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putFloat(SLSurfaceView.MOV_RING_STRENGTH, mMovRingStrength);
                editor.apply();
                setMovRingToggles();
            }
        });
        mMovRingExclusiveBox.setIcons(onIcon, offIcon);
        mMovRingExclusiveBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMovRingExclusive = !mMovRingExclusive;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SLSurfaceView.MOV_RING_EXCLUSIVE, mMovRingExclusive);
                editor.apply();
                mMovRingExclusiveBox.setOn(mMovRingExclusive);
            }
        });
        setMovRingToggles();
        mMovRingCheckBox.setIcons(onIcon, offIcon);
        mMovRingCheckBox.setOn(mGravRingCount == 2);
        enableMovRingToggles(mMovRingCheckBox.isOn());
        mMovRingCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGravRingCount = mGravRingCount == 1 ? 2 : 1;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putInt(SLSurfaceView.GRAV_RING_COUNT, mGravRingCount);
                editor.apply();
                mMovRingCheckBox.setOn(mGravRingCount == 2);
                enableMovRingToggles(mMovRingCheckBox.isOn());

            }
        });
        mGravRingHelpDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.ctrgravring_help_dialog);
                mHelpDialog.setCanceledOnTouchOutside(true);
                mHelpDialog.show();
            }
        });
        mMobRingHelpDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelpDialog.setContentView(R.layout.mobgravring_help_dialog);
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
        editor.putInt(OBJ_SETTINGS_SCRL_YPOS, mScrollView.getScrollY());
        editor.commit();
    }

    // Set the light point bright & strobe option buttons & titles enabled/disabled.
    private void enableLightOptions(boolean enable, TextView textStr, SLToggleButton boxStr, boolean strobe,
                                    TextView textBr, SLToggleButton boxBr, boolean bright) {
        int textColor;
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textColor = getResources().getColor(R.color.greyScale03, null);
            } else {
                textColor = getResources().getColor(R.color.greyScale03);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textColor = getResources().getColor(R.color.greyScale01, null);
            } else {
                textColor = getResources().getColor(R.color.greyScale01);
            }
        }
        textStr.setTextColor(textColor);
        textBr.setTextColor(textColor);
        boxStr.setClickable(enable);
        boxBr.setClickable(enable);
        Drawable disCheckOn;
        Drawable disCheckOff;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            disCheckOn = getResources().getDrawable(R.drawable.ic_check_box_on_disabled, null);
            disCheckOff = getResources().getDrawable(R.drawable.ic_check_box_off_disabled, null);
        } else {
            disCheckOn = getResources().getDrawable(R.drawable.ic_check_box_on_disabled);
            disCheckOff = getResources().getDrawable(R.drawable.ic_check_box_off_disabled);
        }
        if (enable) {
            boxStr.setOn(strobe);
            boxBr.setOn(bright);
        } else {
            boxStr.setBackground(strobe ? disCheckOn : disCheckOff);
            boxBr.setBackground(bright ? disCheckOn : disCheckOff);
        }
    }

    private void setLight0IconColors() {
        Drawable background;
        if (mIsLight0On) {
            float redFl = mPrefs.getFloat("light_0_col_0", 1.0f);
            float grnFl = mPrefs.getFloat("light_0_col_1", 0.0f);
            float bluFl = mPrefs.getFloat("light_0_col_2", 0.0f);
            background = SLHelper.getColorDrawable(redFl, grnFl, bluFl);
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                background = getResources().getDrawable(R.color.colorPrimary, null);
            } else {
                background = getResources().getDrawable(R.color.colorPrimary);
            }
        }
        mLight0Ic.setBackground(background);
    }

    private void setLight1IconColors() {
        Drawable background;
        if (mIsLight1On) {
            float redFl = mPrefs.getFloat("light_1_col_0", 0.0f);
            float grnFl = mPrefs.getFloat("light_1_col_1", 1.0f);
            float bluFl = mPrefs.getFloat("light_1_col_2", 0.0f);
            background = SLHelper.getColorDrawable(redFl, grnFl, bluFl);
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                background = getResources().getDrawable(R.color.colorPrimary, null);
            } else {
                background = getResources().getDrawable(R.color.colorPrimary);
            }
        }
        mLight1Ic.setBackground(background);
    }

    private void setLight2IconColors() {
        Drawable background;
        if (mIsLight2On) {
            float redFl = mPrefs.getFloat("light_2_col_0", 0.0f);
            float grnFl = mPrefs.getFloat("light_2_col_1", 0.0f);
            float bluFl = mPrefs.getFloat("light_2_col_2", 1.0f);
            background = SLHelper.getColorDrawable(redFl, grnFl, bluFl);
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                background = getResources().getDrawable(R.color.colorPrimary, null);
            } else {
                background = getResources().getDrawable(R.color.colorPrimary);
            }
        }
        mLight2Ic.setBackground(background);
    }


    private void setShapeDecal() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(SLSurfaceView.SHAPE_DECAL, mSelectedDecal);
        editor.apply();
        setThumbStates();
    }

    private void setThumbStates() {
        mThumb01.setBackground(null);
        mThumb02.setBackground(null);
        mThumb03.setBackground(null);
        mThumb04.setBackground(null);
        mThumb05.setBackground(null);
        Drawable selected;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            selected = getResources().getDrawable(R.drawable.thumb_selected_01, null);
        } else {
            selected = getResources().getDrawable(R.drawable.thumb_selected_01);
        }
        switch (mSelectedDecal) {
            case SLSimRenderer.STAR_DECAL:
                mThumb01.setBackground(selected);
                break;
            case SLSimRenderer.ROSE_DECAL:
                mThumb02.setBackground(selected);
                break;
            case SLSimRenderer.EYE_DECAL:
                mThumb03.setBackground(selected);
                break;
            case SLSimRenderer.BFLY_DECAL:
                mThumb04.setBackground(selected);
                break;
            case SLSimRenderer.FISH_DECAL:
                mThumb05.setBackground(selected);
                break;
        }
    }

    public void setCtrRingToggles() {
        mButCtrRingSpring.setOn(!mCtrRingSpringSq);
        mButCtrRingSpringSq.setOn(mCtrRingSpringSq);
        mButCtrRingWeakest.setOn(mCtrRingStrength == 1.0f);
        mButCtrRingWeaker.setOn(mCtrRingStrength == 2.0f);
        mButCtrRingMedium.setOn(mCtrRingStrength == 3.0f);
        mButCtrRingStronger.setOn(mCtrRingStrength == 4.0f);
        mButCtrRingStrongest.setOn(mCtrRingStrength == 5.0f);
    }

    public void setMovRingToggles() {
        mButMovRingSpring.setOn(!mMovRingSpringSq);
        mButMovRingSpringSq.setOn(mMovRingSpringSq);
        mButMovRingWeakest.setOn(mMovRingStrength == 1.0f);
        mButMovRingWeaker.setOn(mMovRingStrength == 2.0f);
        mButMovRingMedium.setOn(mMovRingStrength == 3.0f);
        mButMovRingStronger.setOn(mMovRingStrength == 4.0f);
        mButMovRingStrongest.setOn(mMovRingStrength == 5.0f);
    }


    @SuppressWarnings("deprecation")
    public void enableMovRingToggles(boolean enabled) {
        int textColor = enabled ? getResources().getColor(R.color.greyScale03)
                : getResources().getColor(R.color.greyScale01);
        mMovRingExclusiveBox.setClickable(enabled);
        mButMovRingSpring.setClickable(enabled);
        mButMovRingSpringSq.setClickable(enabled);
        mButMovRingWeakest.setClickable(enabled);
        mButMovRingWeaker.setClickable(enabled);
        mButMovRingMedium.setClickable(enabled);
        mButMovRingStronger.setClickable(enabled);
        mButMovRingStrongest.setClickable(enabled);
        mMovExclusiveTitle.setTextColor(textColor);
        mGravTypeTitle.setTextColor(textColor);
        mGravStrengthTitle.setTextColor(textColor);
        mGravTypeSpring.setTextColor(textColor);
        mGravTypeSpringSq.setTextColor(textColor);
        if (enabled) {
            mMovRingExclusiveBox.setOn(mMovRingExclusive);
            setMovRingToggles();
        } else {
            Drawable disCheckOn = getResources().getDrawable(R.drawable.ic_check_box_on_disabled);
            Drawable disCheckOff = getResources().getDrawable(R.drawable.ic_check_box_off_disabled);
            Drawable disabledIcon = getResources().getDrawable(R.drawable.ic_weakest_radio_02);
            mMovRingExclusiveBox.setBackground(mMovRingExclusive ? disCheckOn : disCheckOff);
            mButMovRingSpring.setBackground(disabledIcon);
            mButMovRingSpringSq.setBackground(disabledIcon);
            mButMovRingWeakest.setBackground(disabledIcon);
            mButMovRingWeaker.setBackground(disabledIcon);
            mButMovRingMedium.setBackground(disabledIcon);
            mButMovRingStronger.setBackground(disabledIcon);
            mButMovRingStrongest.setBackground(disabledIcon);
        }
    }

}
