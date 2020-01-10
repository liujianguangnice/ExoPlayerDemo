package com.ed.exoplayerdemo1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.RepeatMode;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.IOException;
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
        setPlayOrPause(true);
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
            MappingTrackSelector trackSelector =
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
            mExoPlayer.addVideoListener(videoListener);
            //控制媒体是否以及如何循环,也可以通过媒体资源控制循环次数
            mExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            //控制播放列表改组
            mExoPlayer.setShuffleModeEnabled(false);
            //调整播放速度和音高setPlaybackParameters

            /**将播放器事件记录到控制台EventLogger，EventLogger|ExoPlayerImpl
             *EventLogger: state [131.89, 128.27, window=0, period=0, true, ENDED]
             *[float]：自 player 创建以来的挂钟时间。
             *[float]：当前播放位置。
             *[window=int]：当前窗口索引。
             *[period=int]：该窗口中的当前时段。
             *[boolean]： playWhenReady标志。
             *[string]：当前播放状态。
             *
             * 当可用或选定的曲目发生变化时，将记录曲目信息。这在回放开始时至少发生一次,轨道tracks和渲染器renderer都会变化，日志会有体现
             * 播放自适应流时，播放期间会记录正在播放的格式的更改以及所选曲目的属性
             * */
            mExoPlayer.addAnalyticsListener(new EventLogger(trackSelector));

        }
    }

    /**
     * 加载MediaSource数据
     */
    private void sourceInjectPlayer() {
        //ConcatenatingMediaSource videoSource = buildMediaSource();
        MediaSource videoSource = clippingMediaSource();
        if (mExoPlayer != null) {
            //MediaSource在播放开始的时候，通过ExoPlayer.prepare方法注入
            mExoPlayer.prepare(videoSource);
        }
    }

    /**
     * 开始和暂停播放
     */
    private void setPlayOrPause(boolean play) {
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
        return new ConcatenatingMediaSource(videoSource);

    }


    /**
     * 剪辑某段视频
     * 示例将视频播放剪辑为以5秒开始并以10秒结束
     */
    private MediaSource clippingMediaSource(){
        Uri uri = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");

        //测量播放过程中的带宽。 如果不需要，可以为null。自适应流的核心就是选择最合适当前播放环境的轨道,自适应播放根据测量的下载速度来估计网络带宽
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // 生成加载媒体数据的DataSource实例。
        DataSource.Factory dataSourceFactory
                = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "useExoplayer"), bandwidthMeter);
        // 生成用于解析媒体数据的Extractor实例。
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory,
                null, null);
        // Clip to start at 5 seconds and end at 10 seconds.
        ClippingMediaSource clippingSource =
                new ClippingMediaSource(
                        videoSource,
                        /* startPositionUs= */ 5_000_000,
                        /* endPositionUs= */ 15_000_000);

        // 控制播放的次数，前提是mExoPlayer.setRepeatMode不能做设置
        //LoopingMediaSource loopingSource = new LoopingMediaSource(clippingSource, 2);
        return clippingSource;
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
            Log.i(TAG, "onTracksChanged:播放器轨道选择器切换中...");
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.i(TAG, "onLoadingChanged:播放器正在加载中...");
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "播放器状态变化: 准备工作完成是否播放 = " + String.valueOf(playWhenReady)
                    + " 当前播放器状态 = " + playbackState);
            switch (playbackState) {
                case ExoPlayer.STATE_ENDED:
                    Log.i(TAG, "播放器播放完所有媒体");
                    //Stop playback and return to start position
                    mExoPlayer.seekTo(0);
                    setPlayOrPause(false);
                    break;
                case ExoPlayer.STATE_READY:
                    Log.i(TAG, "播放器准备完毕，当前位置: " + mExoPlayer.getCurrentPosition()
                            + " 最大时长: " + stringForTime((int) mExoPlayer.getDuration()));
                    setProgress(0);
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    Log.i(TAG, "播放器当前位置无法立即进行播放，数据加载中..");
                    break;
                case ExoPlayer.STATE_IDLE:
                    Log.i(TAG, "播放器初始状态，播放器停止或播放失败");
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlaybackError: " + error.getMessage());
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                IOException cause = error.getSourceException();
                if (cause instanceof HttpDataSource.HttpDataSourceException) {
                    // An HTTP error occurred.
                    HttpDataSource.HttpDataSourceException httpError = (HttpDataSource.HttpDataSourceException) cause;
                    // This is the request for which the error occurred.
                    DataSpec requestDataSpec = httpError.dataSpec;
                    // It's possible to find out more about the error both by casting and by
                    // querying the cause.
                    if (httpError instanceof HttpDataSource.InvalidResponseCodeException) {
                        // Cast to InvalidResponseCodeException and retrieve the response code,
                        // message and headers.
                    } else {
                        // Try calling httpError.getCause() to retrieve the underlying cause,
                        // although note that it may be null.
                    }

                    android.util.Log.i(TAG, "onPlayerError:HTTP网络问题导致播放失败 ");
                }
            }
        }

        public void onPositionDiscontinuity() {
            Log.i(TAG, "onPositionDiscontinuity");
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "MainActivity.onPlaybackParametersChanged." + playbackParameters.toString());
        }
    };


    private VideoListener videoListener = new VideoListener() {
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            android.util.Log.i(TAG, "onVideoSizeChanged: ");
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {
            android.util.Log.i(TAG, "onSurfaceSizeChanged: ");
        }

        @Override
        public void onRenderedFirstFrame() {
            android.util.Log.i(TAG, "onRenderedFirstFrame: ");
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
            setPlayOrPause(true);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause.");
        super.onPause();
        if (mExoPlayer != null) {
            setPlayOrPause(false);
        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop.");
        super.onStop();
        if (mExoPlayer != null) {
            setPlayOrPause(false);
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

