package com.ed.exoplayerdemo1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.util.Formatter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private SimpleExoPlayerView mExoPlayerView;
    private SimpleExoPlayer mExoPlayer;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mContext = this;

        initPlayer();
        sourceInjectPlayer();
        setPlayPause(true);
    }

    /**
     * 初始化player
     */
    private void initPlayer() {
        if (mExoPlayerView == null) {
            //1. 创建一个默认的 TrackSelector,轨道选择器，用于选择MediaSource提供的轨道（tracks），供每个可用的渲染器使用。
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector =
                    new DefaultTrackSelector(videoTackSelectionFactory);
            //用于控制MediaSource何时缓冲更多的媒体资源以及缓冲多少媒体资源,在创建播放器的时候被注入
            LoadControl loadControl = new DefaultLoadControl();
            //2.创建ExoPlayer
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
            //3.创建SimpleExoPlayerView
            mExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exoView);
            //4.为exoPlayerView设置播放器
            mExoPlayerView.setPlayer(mExoPlayer);

            //添加播放器监听的listener
            mExoPlayer.addListener(eventListener);
            mExoPlayer.setPlayWhenReady(false);
        }
    }

    /**
     * 加载MediaSource数据
     */
    private void sourceInjectPlayer() {
        ConcatenatingMediaSource videoSource = buildMediaSource();
        if (mExoPlayer != null) {
            //MediaSource在播放开始的时候，通过ExoPlayer.prepare方法注入
            mExoPlayer.prepare(videoSource);
        }
    }

    /**
     * 播放或停止播放器
     */
    private void setPlayPause(boolean play) {
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(play);
        }

    }


    /**
     * 创建多媒体资源，可以传递多个资源，
     * 可以是不同类型的数据，如视屏、音频
     */
    private ConcatenatingMediaSource buildMediaSource() {
        Uri uri = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");
        Uri uri1 = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4");

        //测量播放过程中的带宽。 如果不需要，可以为null。自适应流的核心就是选择最合适当前播放环境的轨道,自适应播放根据测量的下载速度来估计网络带宽
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // 生成加载媒体数据的DataSource实例。
        DataSource.Factory dataSourceFactory
                = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "useExoplayer"), bandwidthMeter);
        // 生成用于解析媒体数据的Extractor实例。
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // MediaSource代表要播放的媒体,可以是本地资源，可以是网络资源
        MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory,
                null, null);

        MediaSource videoSource1 = new ExtractorMediaSource(uri1, dataSourceFactory, extractorsFactory,
                null, null);
        return new ConcatenatingMediaSource(videoSource, videoSource1);

    }

    /**
     * 播放器监听器
     */
    private ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            Log.i(TAG, "onTimelineChanged");
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.i(TAG, "onTracksChanged:播放器轨道选择器切换...");
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.i(TAG, "onLoadingChanged:播放器正在加载中...");
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged播放状态变化中: 准备工作完成是否播放 = " + String.valueOf(playWhenReady)
                    + " 当前播放器状态 = " + playbackState);
            switch (playbackState) {
                case ExoPlayer.STATE_ENDED:
                    Log.i(TAG, "Playback ended!");
                    //Stop playback and return to start position
                    setPlayPause(false);
                    mExoPlayer.seekTo(0);
                    break;
                case ExoPlayer.STATE_READY:
                    Log.i(TAG, "播放器准备完毕! pos: " + mExoPlayer.getCurrentPosition()
                            + " max: " + stringForTime((int) mExoPlayer.getDuration()));
                    setProgress(0);
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    Log.i(TAG, "Playback buffering!");
                    break;
                case ExoPlayer.STATE_IDLE:
                    Log.i(TAG, "ExoPlayer idle!");
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlaybackError: " + error.getMessage());
        }

        public void onPositionDiscontinuity() {
            Log.i(TAG, "onPositionDiscontinuity");
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "MainActivity.onPlaybackParametersChanged." + playbackParameters.toString());
        }
    };

    /**
     * 时间格式化
     */
    private String stringForTime(int timeMs) {
        StringBuilder mFormatBuilder;
        Formatter mFormatter;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
            //return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume.");
        if (mExoPlayer != null) {
            setPlayPause(true);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause.");
        super.onPause();
        if (mExoPlayer != null) {
            setPlayPause(false);
        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop.");
        super.onStop();
        if (mExoPlayer != null) {
            setPlayPause(false);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExoPlayer != null) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.stop();
            mExoPlayer.removeListener(eventListener);
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }
}

