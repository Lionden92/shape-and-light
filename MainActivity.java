package com.pathfinder.shapeandlight;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements SLSurfaceView.SLMainLauncher {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int ACTIVITY_SETTINGS = 1;
    protected boolean mShowWelcomeDialog;
    protected boolean mShowInfoButtonDialog;
    private SharedPreferences mPrefs;
    protected SLSurfaceView mSurfView;
    protected static final int WELCOME_DIALOG_DELAY = 3000;
    private static final long NANOS_PER_MILLI = 1000000;
    protected static final long SPLASH_TIME = 1750 * NANOS_PER_MILLI;
    protected final Handler mSLHandler = new Handler();
    protected final Runnable mWelcomeRunnable = new Runnable() {
        @Override
        public void run() {
            launchWelcomeDialog();
        }
    };
    protected SplashDialog mSplashDialog;
    protected volatile long mSplashStartTime;
    protected final Runnable mCloseSplashRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSplashDialog != null) {
                if (mSplashDialog.isShowing()) {
                    mSplashDialog.dismiss();
                }
            }
        }
    };
    private boolean mAltSoundSys;
    private boolean mSoundsActive;
    private long mBubbleEmitPlayTime;
    private final static long BUBBLE_EMIT_SOUND_LENGTH = 60 * NANOS_PER_MILLI;
    private final static int BUBBLE_EMIT_SND = 1;
    private final static int SPARKLE_EMIT_SND = 2;
    private final static int SHAPE_TAP_SND = 3;
    private final static int LIGHT_TAP_SND = 4;
    private final static int UI_TICK_SND = 5;
    private static final int KB_80 = 81920;
    private static final int KB_40 = 40960;
    private static final int KB_25 = 25600;
    private static final int[] SAMPLE_RATES = {44100, 44056, 37800, 32000, 22050, 16000, 11025, 8000};
    private static int sample_rate_index;
    private static final float AUDIO_SAMPLE_RT_FLT = 44100.0f;
    private SLSound mBubbleEmitSnd;
    private SLSound mOtherSnds;
    private SoundPool mSoundPool;
    private int mBubbleEmitPlayer;
    private int mSparkleEmitPlayer;
    private int mShapeTapPlayer;
    private int mLightTapPlayer;
    private int mUITickPlayer;
    private boolean mPlayUITicks;
    private volatile MediaPlayer mMusicPlayer;
    private boolean mMusicPaused;
    private MediaPlayer mSparkleCracklePlayer;
    private int mSparkleCracklePos;
    private boolean mCracklePaused;
    private volatile boolean mIsMusicPlayerActive;
    private final Runnable mStartMusicRunnable = new Runnable() {
        @Override
        public void run() {
            mMusicPlayer.seekTo(0);
            if (!mMusicPlayer.isPlaying()) {
                mMusicPlayer.start();
            }
        }
    };
    private final Runnable mStopMusicRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMusicPlayer.isPlaying()) {
                mMusicPlayer.pause();
            }
        }
    };
    private final Runnable mToggleMusicRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMusicPlayer.isPlaying()) {
                mMusicPlayer.pause();
            } else {
                mMusicPlayer.seekTo(0);
                mMusicPlayer.start();
            }
        }
    };
    private final Runnable mUpdateMusicPlayingStateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsMusicPlayerActive) {
                mSurfView.mRenderer.mMusicPlaying = mMusicPlayer.isPlaying();
            }
        }
    };
    private final Runnable mUpdateMusicPosRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsMusicPlayerActive && mMusicPlayer.isPlaying()) {
                mSurfView.mRenderer.mMusPosReadTime = System.nanoTime();
                int musPos;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    MediaTimestamp tStamp = mMusicPlayer.getTimestamp();
                    if (tStamp != null) musPos = (int)(tStamp.getAnchorMediaTimeUs() / 1000);
                    else musPos = mMusicPlayer.getCurrentPosition();
                } else {
                    musPos = mMusicPlayer.getCurrentPosition();
                }
                mSurfView.mRenderer.mMusicPosition = musPos;
            }
        }
    };
    private final Runnable mPlayUITickRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSoundsActive) {
                if (mAltSoundSys) mOtherSnds.play(UI_TICK_SND, 0.33f, 0.33f, 1.0f);
                else mSoundPool.play(mUITickPlayer, 0.33f, 0.33f, 1, 0, 1.0f);
            }
        }
    };
    private final Runnable mStopCrackleRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSoundsActive && mSparkleCracklePlayer != null) {
                mSparkleCracklePlayer.pause();
            }
        }
    };
    private final Runnable mPauseCrackleRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSoundsActive && mSparkleCracklePlayer != null) {
                if (mSparkleCracklePlayer.isPlaying()) {
                    mSparkleCracklePos = mSparkleCracklePlayer.getCurrentPosition();
                    mSparkleCracklePlayer.pause();
                    mCracklePaused = true;
                }
            }
        }
    };
    private final Runnable mResumeCrackleRunnable= new Runnable() {
        @Override
        public void run() {
            if (mSoundsActive && mSparkleCracklePlayer != null) {
                if (mCracklePaused) {
                    mSparkleCracklePlayer.seekTo(mSparkleCracklePos);
                    mSparkleCracklePlayer.start();
                    mCracklePaused = false;
                }
            }
        }
    };
    private final Runnable mPauseMusicRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsMusicPlayerActive && mMusicPlayer != null) {
                if (mMusicPlayer.isPlaying()) {
                    mMusicPlayer.pause();
                    mMusicPaused = mMusicPlayer.getCurrentPosition()
                            < mMusicPlayer.getDuration() - 1000;
                }
            }
        }
    };
    private final Runnable mResumeMusicRunnable= new Runnable() {
        @Override
        public void run() {
            if (mIsMusicPlayerActive && mMusicPlayer != null) {
                if (mMusicPaused) {
                    mMusicPlayer.start();
                    mMusicPaused = false;
                }
            }
        }
    };

    private class SplashDialog extends Dialog {

        SplashDialog(Context context, int themeResId) {
            super(context, themeResId);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            mSplashStartTime = System.nanoTime();
        }
    }

    private class SLSound {

        boolean sPlayed;
        int sCurrentSound;
        AudioTrack sAudioTrack;

        SLSound(int flag) {
            sPlayed = false;
            sCurrentSound = flag;
            assignSoundToTrack(flag);
        }

        AudioTrack setAudioTrack(int sndRes, boolean inStereo, int maxBufferSize) {
            InputStream inputStream = getResources().openRawResource(sndRes);
            byte[] soundData = new byte[maxBufferSize];
            int soundDataLength = 0;
            try {
                soundDataLength = inputStream.read(soundData, 0, maxBufferSize);
            } catch (IOException e) {
                return null;
            }
            if (soundDataLength == 0) return null;
            AudioTrack audioTrack = null;
            while (audioTrack == null && sample_rate_index < SAMPLE_RATES.length) {
                audioTrack = createAudioTrack(SAMPLE_RATES[sample_rate_index],
                        inStereo, soundDataLength);
                if (audioTrack == null) sample_rate_index++;
            }
            if (audioTrack != null) audioTrack.write(soundData, 0, soundDataLength);
            return audioTrack;
        }

        AudioTrack createAudioTrack(int sampleRate, boolean inStereo, int dataLength) {
            try {
                return new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        inStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        dataLength,
                        AudioTrack.MODE_STATIC
                );
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.getMessage());
                return null;
            }
        }

        void play(int flag, float lVol, float rVol, float rate) {
            if (sCurrentSound != flag) {
                sCurrentSound = flag;
                sPlayed = false;
                if (sAudioTrack != null) sAudioTrack.release();
                assignSoundToTrack(flag);
            }
            if (sAudioTrack != null) {
                if (sPlayed) {
                    if (sAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        sAudioTrack.stop();
                    }
                    sAudioTrack.reloadStaticData();
                } else sPlayed = true;
                sAudioTrack.setPlaybackRate((int) (AUDIO_SAMPLE_RT_FLT * rate));
                sAudioTrack.setStereoVolume(lVol, rVol);
                sAudioTrack.play();
            }
        }

        void assignSoundToTrack(int flag) {
            if (flag == BUBBLE_EMIT_SND)
                sAudioTrack = setAudioTrack(R.raw.smoke_bubble_emit_st_3_raw, true, KB_40);
            else if (flag == SPARKLE_EMIT_SND)
                sAudioTrack = setAudioTrack(R.raw.sparkler_emit_st_3_raw, true, KB_80);
            else if (flag == SHAPE_TAP_SND)
                sAudioTrack = setAudioTrack(R.raw.shape_tap_st_3_raw, true, KB_40);
            else if (flag == LIGHT_TAP_SND)
                sAudioTrack = setAudioTrack(R.raw.lightpoint_tap_st_2_raw, true, KB_40);
            else
                sAudioTrack = setAudioTrack(R.raw.ui_tick_04_raw, false, KB_25);
        }

        void release() {
            if (sAudioTrack != null) sAudioTrack.release();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Get screen size to determine whether to use lower-res images for
        // the splash screen (thus inclusion in the GLSurfaceView constructor).
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int widthDP = Math.round(dm.widthPixels / dm.density);
        int heightDP = Math.round(dm.heightPixels/ dm.density);
        SLSimRenderer.is_high_dp = Math.max(widthDP, heightDP) >= 820;
        // Set up splash screen dialog and show.
        mSplashDialog = new SplashDialog(this,
                android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        int splashLayout = dm.widthPixels >= 1920 ? R.layout.splash_screen_1024
                : R.layout.splash_screen_512;
        mSplashDialog.setContentView(splashLayout);
        mSplashDialog.show();
        mSurfView = new SLSurfaceView(this, this);
        setContentView(mSurfView);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setBackgroundColor(Color.BLACK);
        mSLHandler.removeCallbacks(mWelcomeRunnable);
        mSLHandler.postDelayed(mWelcomeRunnable, WELCOME_DIALOG_DELAY);
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-1575937469133662~1548180431");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.hide();
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSurfView.mPrefs = mPrefs;
        mSurfView.onResume();
        mAltSoundSys = mPrefs.getBoolean(SLSurfaceView.ALT_SOUND_SYSTEM, false);
        mPlayUITicks = mPrefs.getBoolean(SLSurfaceView.PLAY_UI_TICKS, true);
        sample_rate_index = mPrefs.getInt(SLSurfaceView.SAMPLING_RATE_INDEX, 0);
        mSoundsActive = true;
        if (mAltSoundSys) {
            // Load all sounds into memory at startup instead of as used at runtime.
            mBubbleEmitSnd = new SLSound(BUBBLE_EMIT_SND);
            mOtherSnds  = new SLSound(SPARKLE_EMIT_SND);
            mOtherSnds = new SLSound(SHAPE_TAP_SND);
            mOtherSnds = new SLSound(LIGHT_TAP_SND);
            mOtherSnds = new SLSound(UI_TICK_SND);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
                SoundPool.Builder sndPoolBuilder = new SoundPool.Builder();
                AudioAttributes attributes = attrBuilder
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build();
                mSoundPool = sndPoolBuilder.setMaxStreams(4).setAudioAttributes(attributes).build();
            } else {
                mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
            }
            mBubbleEmitPlayer = mSoundPool.load(this, R.raw.smoke_bubble_emit_st_3_mp3, 1);
            mSparkleEmitPlayer = mSoundPool.load(this, R.raw.sparkler_emit_st_3_mp3, 1);
            mShapeTapPlayer = mSoundPool.load(this, R.raw.shape_tap_st_3_mp3, 1);
            mLightTapPlayer = mSoundPool.load(this, R.raw.lightpoint_tap_st_2_mp3, 1);
            mUITickPlayer = mSoundPool.load(this, R.raw.ui_tick_04_mp3, 0);
        }
        mSparkleCracklePlayer = MediaPlayer.create(this, R.raw.sparkler_crackle_02b);
        mCracklePaused = false;
        mShowWelcomeDialog = mPrefs.getBoolean(SLSurfaceView.SHOW_WELCOME_DIALOG, true);
        mShowInfoButtonDialog = mPrefs.getBoolean(SLSurfaceView.SHOW_INFO_BUTTON_DIALOG, true);
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    protected  void onPause() {
        super.onPause();
        mIsMusicPlayerActive = false;
        mSoundsActive = false;
        if (mMusicPlayer != null) mMusicPlayer.release();
        if (mSparkleCracklePlayer != null) mSparkleCracklePlayer.release();
        if (mBubbleEmitSnd != null) mBubbleEmitSnd.release();
        if (mOtherSnds != null) mOtherSnds.release();
        if (mSoundPool != null) mSoundPool.release();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(SLSurfaceView.SHOW_WELCOME_DIALOG, mShowWelcomeDialog);
        editor.putBoolean(SLSurfaceView.SHOW_INFO_BUTTON_DIALOG, mShowInfoButtonDialog);
        editor.putInt(SLSurfaceView.SAMPLING_RATE_INDEX, sample_rate_index);
        editor.commit();
        mSurfView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Empty overridden method to preempt saving of unused state info from cluttering cache/data
        // directories.
    }

    public void launchWelcomeDialog() {
        if (mShowWelcomeDialog) {
            final Dialog welcomePopup =
                    new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
            welcomePopup.setContentView(R.layout.welcome_dialog);
            welcomePopup.setCanceledOnTouchOutside(true);
            welcomePopup.show();
            Button dontShowAgainButt = (Button) welcomePopup.findViewById(R.id.dont_show_welcome_butt);
            Button dismissButt = (Button) welcomePopup.findViewById(R.id.dismiss_welcome_butt);
            dontShowAgainButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mShowWelcomeDialog = false;
                    welcomePopup.dismiss();
                }
            });
            dismissButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    welcomePopup.dismiss();
                }
            });
        }
    }

    @Override
    public void launchHelpButtonDialog() {
        if (mShowInfoButtonDialog) {
            final Dialog infoButtPopop =
                    new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
            infoButtPopop.setContentView(R.layout.help_button_dialog);
            infoButtPopop.setCanceledOnTouchOutside(true);
            infoButtPopop.show();
            Button dontShowAgainButt = (Button) infoButtPopop
                    .findViewById(R.id.dont_show_helpdialog_butt);
            Button dismissButt = (Button) infoButtPopop
                    .findViewById(R.id.dismiss_helpdialog_butt);
            dontShowAgainButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mShowInfoButtonDialog = false;
                    infoButtPopop.dismiss();
                }
            });
            dismissButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    infoButtPopop.dismiss();
                }
            });
        }
    }

    @Override
    public void launchSLMenuActivity() {
        Intent i = new Intent(this, SLMenuActivity.class);
        startActivityForResult(i, ACTIVITY_SETTINGS);
    }

    @Override
    public void launchHelpPopup(int flag) {
        Dialog helpPopup = new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        int resID;
        switch (flag) {
            case SLSurfaceView.SETTINGS_HELP:
                resID = R.layout.menu_help_dialog;
                break;
            case SLSurfaceView.RESET_HELP:
                resID = R.layout.reset_help_dialog;
                break;
            case SLSurfaceView.EYELOCK_HELP:
                resID = R.layout.eyelock_help_dialog;
                break;
            case SLSurfaceView.MUSIC_HELP:
                resID = R.layout.music_help_dialog;
                break;
            case SLSurfaceView.EMITTER_HELP:
                resID = R.layout.emitter_help_dialog;
                break;
            case SLSurfaceView.EYEMOVMODE_HELP:
                resID = R.layout.movedir_help_dialog;
                break;
            case SLSurfaceView.EYEMOVCONTR_HELP:
                resID = R.layout.eyemovctrl_help_dialog;
                break;
            case SLSurfaceView.EYEDIRCONTR_HELP:
                resID = R.layout.eyedirctrl_help_dialog;
                break;
            case SLSurfaceView.OBJSELECT_HELP:
                resID = R.layout.objectselect_help_screen;
                break;
            case SLSurfaceView.OBJMANIP_HELP:
                resID = R.layout.objectmanip_help_screen;
                break;
            case SLSurfaceView.ORIENTAX_HELP:
                resID = R.layout.orientaxes_help_dialog;
                break;
            case SLSurfaceView.PAUSE_HELP:
                resID = R.layout.pause_help_dialog;
                break;
            case SLSurfaceView.ABOUT_APP_DIALOG:
                resID = R.layout.about_dialog;
                break;
            default:
                resID = R.layout.menu_help_dialog;
        }
        helpPopup.setContentView(resID);
        helpPopup.setCanceledOnTouchOutside(true);
        helpPopup.show();
    }

    @Override
    public boolean isSplashTimeUp(long currentTime) {
        return currentTime - mSplashStartTime > SPLASH_TIME;
    }

    @Override
    public void closeSplashScreen() {
        mSLHandler.post(mCloseSplashRunnable);
    }

    @Override
    public void playBubbleSound(float leftV, float rightV, float rate) {
        // If the sound has been played back at a time ago less than its length return w/o playing.
        if (System.nanoTime() - mBubbleEmitPlayTime < BUBBLE_EMIT_SOUND_LENGTH) return;
        final float left = leftV;
        final float right = rightV;
        final float pitch = rate;
        mSLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSoundsActive) {
                    mBubbleEmitPlayTime = System.nanoTime();
                    if (mAltSoundSys) mBubbleEmitSnd.play(BUBBLE_EMIT_SND, left, right, pitch);
                    else mSoundPool.play(mBubbleEmitPlayer, left, right, 1, 0, pitch);
                }
            }
        });
    }

    @Override
    public void playSparkleEmitSound(float leftV, float rightV) {
        final float left = leftV;
        final float right = rightV;
        mSLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSoundsActive){
                    if (mAltSoundSys) mOtherSnds.play(SPARKLE_EMIT_SND, left, right, 0.95f);
                    else mSoundPool.play(mSparkleEmitPlayer, left, right, 1, 0, 0.95f);
                }
            }
        });
    }

    @Override
    public void playSparkleCrackleSound(float leftV, float rightV) {
        final float left = leftV;
        final float right = rightV;
        mSLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSoundsActive && mSparkleCracklePlayer != null) {
                    mSparkleCracklePlayer.seekTo(0);
                    mSparkleCracklePlayer.setVolume(left, right);
                    if (!mSparkleCracklePlayer.isPlaying()) mSparkleCracklePlayer.start();
                }
            }
        });
    }

    @Override
    public void updateSparkleCrackleSound(float leftV, float rightV) {
        final float left = leftV;
        final float right = rightV;
        mSLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSoundsActive && mSparkleCracklePlayer != null)
                    mSparkleCracklePlayer.setVolume(left, right);
            }
        });
    }

    @Override
    public void stopSparkleCrackleSound() {
        mSLHandler.post(mStopCrackleRunnable);
    }

    @Override
    public void playFreeMoverTapSound(float leftV, float rightV, float rate, int player) {
        final float left = leftV;
        final float right = rightV;
        final float pitch = rate;
        final int tap = player;
        mSLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSoundsActive) {
                    if (tap == 1) {
                        if (mAltSoundSys) mOtherSnds.play(SHAPE_TAP_SND, left, right, pitch);
                        else mSoundPool.play(mShapeTapPlayer, left, right, 1, 0, pitch);
                    } else {
                        if (mAltSoundSys) mOtherSnds.play(LIGHT_TAP_SND, left, right, pitch);
                        else mSoundPool.play(mLightTapPlayer, left, right, 1, 0, pitch);
                    }
                }
            }
        });
    }

    @Override
    public void playUITickSound() {
        if (mPlayUITicks) mSLHandler.post(mPlayUITickRunnable);
    }

    @Override
    public void playUITickSndDirect() {
        if (mPlayUITicks) mPlayUITickRunnable.run();
    }

    @Override
    public void createMusicPlayer(int track, float volume) {
        final int musicTrack = track;
        final Context context = this;
        final float vol = volume;
        mSLHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsMusicPlayerActive = true;
                mMusicPlayer = MediaPlayer.create(context, musicTrack);
                mMusicPlayer.setVolume(vol, vol);
            }
        });
    }

    @Override
    public void startMusic() {
        mSurfView.mRenderer.mMusicPlaying = true;
        mSLHandler.removeCallbacks(mStartMusicRunnable);
        mSLHandler.post(mStartMusicRunnable);
    }

    @Override
    public void stopMusic() {
        mSurfView.mRenderer.mMusicPlaying = false;
        mSLHandler.removeCallbacks(mStopMusicRunnable);
        mSLHandler.post(mStopMusicRunnable);
    }

    @Override
    public void toggleMusic() {
        mSurfView.mRenderer.mMusicPlaying = !mSurfView.mRenderer.mMusicPlaying;
        mSLHandler.removeCallbacks(mToggleMusicRunnable);
        mSLHandler.post(mToggleMusicRunnable);
    }

    @Override
    public void updateMusicPlayingState() {
        mSLHandler.removeCallbacks(mUpdateMusicPlayingStateRunnable);
        mSLHandler.post(mUpdateMusicPlayingStateRunnable);
    }

    @Override
    public void updateMusicPosition() {
        mSLHandler.removeCallbacks(mUpdateMusicPosRunnable);
        mSLHandler.post(mUpdateMusicPosRunnable);
    }

    @Override
    public void pauseMusic() {
        mSLHandler.removeCallbacks(mPauseMusicRunnable);
        mSLHandler.post(mPauseMusicRunnable);
    }

    @Override
    public void resumeMusic() {
        mSLHandler.removeCallbacks(mResumeMusicRunnable);
        mSLHandler.post(mResumeMusicRunnable);
    }

    @Override
    public void pauseCrackle() {
        mSLHandler.removeCallbacks(mPauseCrackleRunnable);
        mSLHandler.post(mPauseCrackleRunnable);
    }

    @Override
    public void resumeCrackle() {
        mSLHandler.removeCallbacks(mResumeCrackleRunnable);
        mSLHandler.post(mResumeCrackleRunnable);
    }


}
