package com.pathfinder.shapeandlight;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.app.ActionBar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
 * Created by Lionden on 9/13/2016.
 */
public class SLMenuActivity extends FragmentActivity {
    private static final String LOG_TAG = SLMenuActivity.class.getSimpleName();
    protected static final String SELECTED_SETTINGS = "selected_settings";
    protected static final int OBJ_SETTINGS = 0;
    protected static final int OTHER_SETTINGS = 1;
    protected static final int MUSIC_TRACK = 2;
    protected static final int INFO_SCREEN = 3;

    protected SharedPreferences mPrefs;
    protected ImageButton mButObjSettingsSel;
    protected ImageButton mButOtherSettingsSel;
    protected ImageButton mButMusicSelect;
    protected ImageButton mButInfoSelect;
    protected Button mButDone;

    protected int mSelected;
    protected int mSelectedColor;
    protected int mUnselectedColor;

    protected Fragment mFragment;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_screen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        mSelectedColor = getResources().getColor(R.color.colorPrimaryDark);
        mUnselectedColor = getResources().getColor(R.color.colorPrimaryMedium);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSelected = mPrefs.getInt(SELECTED_SETTINGS, MUSIC_TRACK);
        mButObjSettingsSel = findViewById(R.id.obj_settings_select);
        mButOtherSettingsSel = findViewById(R.id.other_settings_select);
        mButInfoSelect = findViewById(R.id.infoscreen_select);
        mButMusicSelect = findViewById(R.id.music_select);
        mButDone = findViewById(R.id.settings_done);
        highlightAndSelectSettings();
        mButMusicSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelected = MUSIC_TRACK;
                setNewSettingsSelection();
            }
        });
        mButObjSettingsSel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelected = OBJ_SETTINGS;
                setNewSettingsSelection();
            }
        });
        mButOtherSettingsSel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelected = OTHER_SETTINGS;
                setNewSettingsSelection();
            }
        });
        mButInfoSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelected = INFO_SCREEN;
                setNewSettingsSelection();
            }
        });
        mButDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
        // Code to display the banner ad in the settings screen.
        AdView mAdView = findViewById(R.id.menuscreen_adview);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.hide();
        }
    }

    @SuppressLint("ApplySharedPref")
    @SuppressWarnings("deprecation")
    protected void setNewSettingsSelection() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(SELECTED_SETTINGS, mSelected);
        editor.commit();
        mButObjSettingsSel.setBackgroundColor(mUnselectedColor);
        mButOtherSettingsSel.setBackgroundColor(mUnselectedColor);
        mButInfoSelect.setBackgroundColor(mUnselectedColor);
        mButMusicSelect.setBackgroundColor(mUnselectedColor);
        highlightAndSelectSettings();
    }

    // Sets the settings screen fragment to be displayed in the settings pane, and highlights
    // the appropriate button, depending on currently-active settings selection.
    protected void highlightAndSelectSettings() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        switch (mSelected) {
            case OBJ_SETTINGS:
                mButObjSettingsSel.setBackgroundColor(mSelectedColor);
                mFragment = new ObjectSettingsFragment();
                break;
            case OTHER_SETTINGS:
                mButOtherSettingsSel.setBackgroundColor(mSelectedColor);
                mFragment = new OtherSettingsFragment();
                break;
            case MUSIC_TRACK:
                mButMusicSelect.setBackgroundColor(mSelectedColor);
                mFragment = new MusicFragment();
                break;
            case INFO_SCREEN:
                mButInfoSelect.setBackgroundColor(mSelectedColor);
                mFragment = new AboutFragment();
                break;
            }
        fragmentTransaction.replace(R.id.settings_detail_container, mFragment);
        fragmentTransaction.commit();
    }


}
