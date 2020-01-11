package com.ed.exoplayerdemo1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.RepeatMode;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
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
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();
    private SimpleExoPlayerView mExoPlayerView;
    private SimpleExoPlayer mExoPlayer;
    private Context context;
    private ImageView allScreen;

    private ConcatenatingMediaSource videoSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        context = this;

        initView();

        initPlayer();
        sourceInjectPlayer();
        setPlayOrPause(true);
    }

    private void initView() {
        allScreen = findViewById(R.id.exo_all_screen);
        allScreen.setOnClickListener(this);
    }

    /**
     * 初始化player
     */
    private void initPlayer() {
        if (mExoPlayerView == null) {
            //创建带宽
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            //创建轨道选择工厂
            TrackSelection.Factory videoTackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(bandwidthMeter);
            //创建缓冲控制器，用于控制MediaSource何时缓冲更多的媒体资源以及缓冲多少媒体资源
            LoadControl loadControl = new DefaultLoadControl();
            //1. 创建轨道选择器，从MediaSource中提取各个轨道的二进制数据，交给Render渲染。
            MappingTrackSelector trackSelector =
                    new DefaultTrackSelector(videoTackSelectionFactory);

            //2.创建播放器实例
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
            //3.创建SimpleExoPlayerView
            mExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exoView);
            //4.将player和view绑定
            mExoPlayerView.setPlayer(mExoPlayer);

            //添加播放器监听的listener
            mExoPlayer.addListener(eventListener);
            mExoPlayer.addVideoListener(videoListener);
            //控制媒体是否以及如何循环,也可以通过媒体资源控制循环次数,REPEAT_MODE_ONE单个资源无限播放（两个资源只重复播放第一个），
            mExoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
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
        videoSource = buildConcatenatingMediaSource();
        if (mExoPlayer != null) {
            //添加数据源到播放器中
            mExoPlayer.prepare(videoSource);
        }
    }

    /**
     * 开始和暂停播放
     */
    private void setPlayOrPause(boolean play) {
        if (mExoPlayer != null) {
            //开始播放
            mExoPlayer.setPlayWhenReady(play);
        }

    }


    /**
     * 创建单个多媒体资源，可以传递不同类型的数据，如视屏、音频
     */
    private MediaSource buildMediaSource() {
        Uri uri = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");

        //创建加载数据的工厂
        DataSource.Factory dataSourceFactory= new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getApplicationInfo().name));
        // MediaSource代表要播放的媒体,可以是本地资源，可以是网络资源
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .setTag(uri)
                .createMediaSource(uri);
        return videoSource;
    }

    /**
     * 创建多个媒体资源，可以传递多个资源，无缝播放
     * 可以是不同类型的数据，如视屏、音频
     */
    private ConcatenatingMediaSource buildConcatenatingMediaSource() {
        Uri uri = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");
        Uri uri1 = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4");

        //测量播放过程中的带宽。 如果不需要，可以为null。自适应流的核心就是选择最合适当前播放环境的轨道,自适应播放根据测量的下载速度来估计网络带宽
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        //创建加载数据的工厂
        DataSource.Factory dataSourceFactory= new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getApplicationInfo().name), bandwidthMeter);
        // 生成用于解析媒体数据的Extractor实例。
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // MediaSource代表要播放的媒体,可以是本地资源，可以是网络资源
        MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory,
                null, null);

        MediaSource videoSource1 = new ExtractorMediaSource(uri1, dataSourceFactory, extractorsFactory,
                null, null);
        return new ConcatenatingMediaSource(videoSource,videoSource1);

       /* 播放序列（A，A，B）
       MediaSource firstSource =
                new ProgressiveMediaSource.Factory(...).createMediaSource(firstVideoUri);
        MediaSource secondSource =
                new ProgressiveMediaSource.Factory(...).createMediaSource(secondVideoUri);
        // Plays the first video twice.
        LoopingMediaSource firstSourceTwice = new LoopingMediaSource(firstSource, 2);
        // Plays the first video twice, then the second video.
        ConcatenatingMediaSource concatenatedSource =
                new ConcatenatingMediaSource(firstSourceTwice, secondSource);*/
    }


    /**
     * 剪辑某段视频
     * 示例将视频播放剪辑为以5秒开始并以10秒结束
     */
    private MediaSource clippingMediaSource() {
        Uri uri = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");

        //创建加载数据的工厂
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, getApplicationInfo().name));
        // 创建资源
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        // Clip to start at 5 seconds and end at 10 seconds.
        ClippingMediaSource clippingSource =
                new ClippingMediaSource(
                        videoSource,
                        /* startPositionUs= */ 5_000_000,
                        /* endPositionUs= */ 15_000_000);

        // 控制播放的次数，前提是mExoPlayer.setRepeatMode不能做设置,无缝循环固定次数
        //LoopingMediaSource loopingSource = new LoopingMediaSource(clippingSource, 2);
        return clippingSource;
    }

    /**
     * 给定视频文件和单独的字幕文件， MergingMediaSource 可用于将它们合并为单个源以进行回放。
     */
    private void mergingMediaSource() {
        Uri uri = Uri.parse(
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, getApplicationInfo().name));
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
/*
        // Build the subtitle MediaSource.
        Format subtitleFormat = Format.createTextSampleFormat(
                null, // An identifier for the track. May be null.
                MimeTypes.APPLICATION_SUBRIP, // The mime type. Must be set correctly.
                null, // Selection flags for the track.
                null); // The subtitle language. May be null.
        MediaSource subtitleSource =
                new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(subtitleUri, subtitleFormat, C.TIME_UNSET);
        // Plays the video with the sideloaded subtitle.
        MergingMediaSource mergedSource =
                new MergingMediaSource(videoSource, subtitleSource);*/
    }

    /**
     * 播放器监听器
     */
    private ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {

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

    /**
     * 修改播放器列表.如果当前播放 MediaSource 被移除，
     * 则播放器将自动移动到播放第一个剩余的后继者,或者如果不存在这样的后继者则转换到结束状态
     */
    private void updateMediaSource(ConcatenatingMediaSource videoSource){
        if(videoSource!=null&&videoSource.getSize()>0){
             videoSource.removeMediaSource(0);
        }
    }


    /**
     * 识别播放列表项
     * MediaSource 可以在工厂类中使用自定义标签设置每个项目 MediaSource ，这可以是uri，
     * 标题或任何其他自定义对象。可以使用查询当前正在播放的项目的标签 player.getCurrentTag 。
     * player.getCurrentTimeline 返回的当前值Timeline 还包含所有标记作为 Timeline.Window 对象的一部分
     */
    private void identifyMediaSource(MediaSource videoSource){
        if(videoSource!=null&&mExoPlayer!=null){
            //(Timeline)mExoPlayer.getCurrentTag();
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        switch (i) {
            case R.id.exo_all_screen:
                identifyMediaSource(videoSource);
                break;

            default:
        }
    }
}

