package com.pathfinder.shapeandlight;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;


public class MusicFragment extends Fragment {

    private static final String LOG_TAG = MusicFragment.class.getSimpleName();
    protected SharedPreferences mPrefs;
    protected ScrollView mScrollView;
    private static final String MUSIC_TRACK_SCRL_YPOS = "music_track_scrollview_y_pos";
    private static final String PREV_TRACK_ON_SELECT = "preview_track_on_selection";
    protected Button mButBeautSilence;
    protected Button mButBlueDanube;
    protected Button mButFantasyIce;
    protected Button mButMidnightCity;
    protected Button mButStreetlights;
    protected Button mButSunriseWithout;
    protected Button mButTimeReflection;
    protected int mTrack;
    protected boolean mPrevOnSelect;
    protected SLToggleButton mPrevSelectBut;

    protected MediaPlayer mPreviewPlayer;
    private float mPreviewVol;
    private float mFadeIncVol;
    private final Handler mFadeHandler = new Handler();
    private static final long START_FADE = 5500;
    private static final long FADE_INC = 133;
    private static final float FADE_INC_FRACTION = 0.034f;

    protected final Runnable mIncrementPreviewFade = new Runnable() {
        @Override
        public void run() {
            mPreviewVol -= mFadeIncVol;
            if (mPreviewVol > 0.0f) {
                if (mPreviewPlayer != null && mPreviewPlayer.isPlaying())
                    mPreviewPlayer.setVolume(mPreviewVol, mPreviewVol);
                mFadeHandler.postDelayed(mIncrementPreviewFade, FADE_INC);
            } else {
                if (mPreviewPlayer != null && mPreviewPlayer.isPlaying())
                    mPreviewPlayer.pause();
            }
        }
    };

    public MusicFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mTrack = mPrefs.getInt(SLSurfaceView.MUSIC_TRACK, SLSimRenderer.MIDNIGHT_CITY);
        mPrevOnSelect = mPrefs.getBoolean(PREV_TRACK_ON_SELECT, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.music_fragment, container, false);
        mScrollView = rootView.findViewById(R.id.music_track_scrollview);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(0, mPrefs.getInt(MUSIC_TRACK_SCRL_YPOS, 0));
                mScrollView.setVisibility(View.VISIBLE);
            }
        });
        mButBeautSilence = rootView.findViewById(R.id.button_beautiful_silence);
        mButBlueDanube = rootView.findViewById(R.id.button_blue_danube);
        mButFantasyIce = rootView.findViewById(R.id.button_fantasy_ice);
        mButMidnightCity = rootView.findViewById(R.id.button_midnight_city);
        mButStreetlights = rootView.findViewById(R.id.button_streetlights);
        mButSunriseWithout = rootView.findViewById(R.id.button_sunrise_without);
        mButTimeReflection = rootView.findViewById(R.id.button__time_reflection);
        mButBeautSilence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.BEAUTIFUL_SIL);
            }
        });
        mButBlueDanube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.BLUE_DANUBE);
            }
        });
        mButFantasyIce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.FANTASY_ICE);
            }
        });
        mButMidnightCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.MIDNIGHT_CITY);
            }
        });
        mButStreetlights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.STREET_LIGHTS);
            }
        });
        mButSunriseWithout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.SUNRISE_WITHOUT);
            }
        });
        mButTimeReflection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNewTrack(SLSimRenderer.TIME_REFLECTION);
            }
        });
        highlightSelectedTrack(false);
        mPrevSelectBut = rootView.findViewById(R.id.prev_track_select_butt);
        Drawable onIcon;
        Drawable offIcon;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onIcon = getResources().getDrawable(R.drawable.ic_check_box_on_03, null);
            offIcon = getResources().getDrawable(R.drawable.ic_check_box_blank, null);
        } else {
            onIcon = getResources().getDrawable(R.drawable.ic_check_box_on_03);
            offIcon = getResources().getDrawable(R.drawable.ic_check_box_blank);
        }
        mPrevSelectBut.setIcons(onIcon, offIcon);
        mPrevSelectBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrevOnSelect = !mPrevOnSelect;
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(PREV_TRACK_ON_SELECT, mPrevOnSelect);
                editor.apply();
                mPrevSelectBut.setOn(mPrevOnSelect);
            }
        });
        mPrevSelectBut.setOn(mPrevOnSelect);
        return rootView;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onPause() {
        super.onPause();
        mFadeHandler.removeCallbacks(mIncrementPreviewFade);
        if (mPreviewPlayer != null) {
            mPreviewPlayer.release();
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(MUSIC_TRACK_SCRL_YPOS, mScrollView.getScrollY());
        editor.commit();
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @SuppressWarnings("deprecation")
    protected void setNewTrack(int track) {
        mTrack = track;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(SLSurfaceView.MUSIC_TRACK, track);
        editor.commit();
        int textColor = getResources().getColor(R.color.colorOffWhite);
        mButBeautSilence.setBackgroundColor(getResources().getColor(R.color.blueScale01));
        mButBeautSilence.setTextColor(textColor);
        mButBlueDanube.setBackgroundColor(getResources().getColor(R.color.blueScale02));
        mButBlueDanube.setTextColor(textColor);
        mButFantasyIce.setBackgroundColor(getResources().getColor(R.color.blueScale03));
        mButFantasyIce.setTextColor(textColor);
        mButMidnightCity.setBackgroundColor(getResources().getColor(R.color.blueScale04));
        mButMidnightCity.setTextColor(textColor);
        mButStreetlights.setBackgroundColor(getResources().getColor(R.color.blueScale05));
        mButStreetlights.setTextColor(textColor);
        mButSunriseWithout.setBackgroundColor(getResources().getColor(R.color.blueScale06));
        mButSunriseWithout.setTextColor(textColor);
        mButTimeReflection.setBackgroundColor(getResources().getColor(R.color.blueScale07));
        mButTimeReflection.setTextColor(textColor);
        highlightSelectedTrack(true);
    }

    @SuppressWarnings("deprecation")
    protected void highlightSelectedTrack(boolean selecting) {
        int color = getResources().getColor(R.color.blueSky);
        int textColor = getResources().getColor(R.color.colorBlack);
        boolean prevwTrack = selecting && mPrevOnSelect;
        switch (mTrack) {
            case SLSimRenderer.BEAUTIFUL_SIL:
                mButBeautSilence.setBackgroundColor(color);
                mButBeautSilence.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_beautiful_silence_2, 0.6f, 0);
                break;
            case SLSimRenderer.BLUE_DANUBE:
                mButBlueDanube.setBackgroundColor(color);
                mButBlueDanube.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_blue_danube_3, 0.99f, 600);
                break;
            case SLSimRenderer.FANTASY_ICE:
                mButFantasyIce.setBackgroundColor(color);
                mButFantasyIce.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_fantasy_on_ice_2, 0.5f, 0);
                break;
            case SLSimRenderer.MIDNIGHT_CITY:
                mButMidnightCity.setBackgroundColor(color);
                mButMidnightCity.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_midnight_city_2, 0.6f, 0);
                break;
            case SLSimRenderer.STREET_LIGHTS:
                mButStreetlights.setBackgroundColor(color);
                mButStreetlights.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_streetlights_people_2, 0.8f, 0);
                break;
            case SLSimRenderer.SUNRISE_WITHOUT:
                mButSunriseWithout.setBackgroundColor(color);
                mButSunriseWithout.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_sunrise_without_you_2, 0.8f, 1100);
                break;
            case SLSimRenderer.TIME_REFLECTION:
                mButTimeReflection.setBackgroundColor(color);
                mButTimeReflection.setTextColor(textColor);
                if (prevwTrack) previewTrack(R.raw.mus_time_of_reflection_2, 0.6f, 0);
                break;
        }
    }

    private void previewTrack(int musResID, float vol, int seek) {
        mFadeHandler.removeCallbacks(mIncrementPreviewFade);
        mPreviewVol = vol;
        mFadeIncVol = FADE_INC_FRACTION * vol;
        if (mPreviewPlayer != null) mPreviewPlayer.release();
        mPreviewPlayer = MediaPlayer.create(getActivity(), musResID);
        mPreviewPlayer.setVolume(vol, vol);
        mPreviewPlayer.seekTo(seek);
        mPreviewPlayer.start();
        mFadeHandler.postDelayed(mIncrementPreviewFade, START_FADE);
    }


}
