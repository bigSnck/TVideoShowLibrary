package com.yt.tvideolibrary.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorInfo;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.bean.InfoCode;
import com.aliyun.player.nativeclass.MediaInfo;
import com.aliyun.player.nativeclass.PlayerConfig;
import com.aliyun.player.nativeclass.Thumbnail;
import com.aliyun.player.nativeclass.TrackInfo;
import com.aliyun.player.source.UrlSource;
import com.aliyun.player.source.VidAuth;
import com.aliyun.player.source.VidSts;

import com.aliyun.thumbnail.ThumbnailBitmapInfo;
import com.aliyun.thumbnail.ThumbnailHelper;
import com.aliyun.utils.VcPlayerLog;

import com.yt.tvideolibrary.R;
import com.yt.tvideolibrary.constants.PlayParameter;
import com.yt.tvideolibrary.listener.AliyunScreenMode;
import com.yt.tvideolibrary.listener.LockPortraitListener;
import com.yt.tvideolibrary.listener.OnAutoPlayListener;
import com.yt.tvideolibrary.listener.OnChangeQualityListener;
import com.yt.tvideolibrary.listener.OnScreenCostingSingleTagListener;
import com.yt.tvideolibrary.listener.OnStoppedListener;
import com.yt.tvideolibrary.theme.ITheme;
import com.yt.tvideolibrary.utils.DensityUtil;
import com.yt.tvideolibrary.utils.FixedToastUtils;
import com.yt.tvideolibrary.utils.ImageLoader;
import com.yt.tvideolibrary.utils.NetWatchdog;
import com.yt.tvideolibrary.utils.OrientationWatchDog;
import com.yt.tvideolibrary.utils.ScreenUtils;
import com.yt.tvideolibrary.utils.TimeFormater;
import com.yt.tvideolibrary.view.control.ControlView;
import com.yt.tvideolibrary.view.gesture.GestureDialogManager;
import com.yt.tvideolibrary.view.gesture.GestureView;
import com.yt.tvideolibrary.view.guide.GuideView;
import com.yt.tvideolibrary.view.interfaces.ViewAction;


import com.yt.tvideolibrary.view.quality.QualityView;
import com.yt.tvideolibrary.view.speed.SpeedView;
import com.yt.tvideolibrary.view.thumbnail.ThumbnailView;
import com.yt.tvideolibrary.view.tipsview.LoadingView;
import com.yt.tvideolibrary.view.tipsview.TipsView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * UI播放器的主要实现类。 通过ITheme控制各个界面的主题色。 通过各种view的组合实现UI的界面。这些view包括： 用户手势操作的{@link GestureView} 控制播放，显示信息的{@link
 * ControlView} 显示清晰度列表的{@link QualityView} 倍速选择界面{@link SpeedView} 用户使用引导页面{@link GuideView} 用户提示页面{@link TipsView}
 * 以及封面等。 view 的初始化是在{@link #initVideoView}方法中实现的。 然后是对各个view添加监听方法，处理对应的操作，从而实现与播放器的共同操作
 */
public class AliyunVodPlayerView extends RelativeLayout implements ITheme {
    /**
     * 精准seek开启判断逻辑：当视频时长小于5分钟的时候。
     */
    private static final int ACCURATE = 5 * 60 * 1000;
    private static final String TAG = AliyunVodPlayerView.class.getSimpleName();

    /**
     * 判断VodePlayer 是否加载完成
     */
    private Map<MediaInfo, Boolean> hasLoadEnd = new HashMap<>();

    //视频画面
    private SurfaceView mSurfaceView;
    //手势操作view
    private GestureView mGestureView;
    //皮肤view
    private ControlView mControlView;
    //清晰度view
    private QualityView mQualityView;
    //倍速选择view
    private SpeedView mSpeedView;
    //引导页view
    private GuideView mGuideView;
    //封面view
    private ImageView mCoverView;
    /**
     * 缩略图View
     */
    private ThumbnailView mThumbnailView;

    //播放器
    private AliPlayer mAliyunVodPlayer;
    //手势对话框控制
    private GestureDialogManager mGestureDialogManager;
    //网络状态监听
    private NetWatchdog mNetWatchdog;
    //屏幕方向监听
    private OrientationWatchDog mOrientationWatchDog;
    //Tips view
    private TipsView mTipsView;
    //锁定竖屏
    private LockPortraitListener mLockPortraitListener = null;
    //是否锁定全屏
    private boolean mIsFullScreenLocked = false;
    //当前屏幕模式
    private AliyunScreenMode mCurrentScreenMode = AliyunScreenMode.Small;
    //是不是在seek中
    private boolean inSeek = false;
    //播放是否完成
    private boolean isCompleted = false;
    //媒体信息
    private MediaInfo mAliyunMediaInfo;
    //解决bug,进入播放界面快速切换到其他界面,播放器仍然播放视频问题
    private VodPlayerLoadEndHandler vodPlayerLoadEndHandler = new VodPlayerLoadEndHandler(this);
    //原视频的buffered
    private long mVideoBufferedPosition = 0;
    //原视频的currentPosition
    private long mCurrentPosition = 0;
    //当前播放器的状态 默认为idle状态
    private int mPlayerState = IPlayer.idle;
    //原视频时长
    private long mSourceDuration;

    //目前支持的几种播放方式
    private VidAuth mAliyunPlayAuth;
    private UrlSource mAliyunLocalSource;
    private VidSts mAliyunVidSts;

    //对外的各种事件监听
    private IPlayer.OnInfoListener mOutInfoListener = null;
    private IPlayer.OnErrorListener mOutErrorListener = null;
    //    private IPlayer.OnRePlayListener mOutRePlayListener = null;
    private OnAutoPlayListener mOutAutoPlayListener = null;
    private IPlayer.OnPreparedListener mOutPreparedListener = null;
    private IPlayer.OnCompletionListener mOutCompletionListener = null;
    private IPlayer.OnSeekCompleteListener mOuterSeekCompleteListener = null;
    private OnChangeQualityListener mOutChangeQualityListener = null;
    private IPlayer.OnRenderingStartListener mOutFirstFrameStartListener = null;
    private OnScreenCostingSingleTagListener mOnScreenCostingSingleTagListener = null;
    private OnScreenBrightnessListener mOnScreenBrightnessListener = null;
    private OnTimeExpiredErrorListener mOutTimeExpiredErrorListener = null;
    //    private IPlayer.OnUrlTimeExpiredListener mOutUrlTimeExpiredListener = null;
    //对外view点击事件监听
    private OnPlayerViewClickListener mOnPlayerViewClickListener = null;
    // 连网断网监听
    private NetConnectedListener mNetConnectedListener = null;
    // 横屏状态点击更多
    private ControlView.OnShowMoreClickListener mOutOnShowMoreClickListener;
    //播放按钮点击监听
    private OnPlayStateBtnClickListener onPlayStateBtnClickListener;
    //停止按钮监听
    private OnStoppedListener mOnStoppedListener;
    /**
     * 对外SEI消息通知
     */
    private IPlayer.OnSeiDataListener mOutSeiDataListener = null;
    //当前屏幕亮度
    private int mScreenBrightness;
    /**
     * 缩略图帮助类
     */
    private ThumbnailHelper mThumbnailHelper;
    //获取缩略图是否成功
    private boolean mThumbnailPrepareSuccess = false;

    private float currentSpeed;
    private float currentVolume;
    private int currentScreenBrigtness;

    private Context mContext;
    public AliyunVodPlayerView(Context context) {
        this(context,null);

    }

    public AliyunVodPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs,0);

    }

    public AliyunVodPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext=context;
        initVideoView();
    }

    /**
     * 初始化view
     */
    private void initVideoView() {
        //初始化播放用的surfaceView
        initSurfaceView();
        //初始化播放器
        initAliVcPlayer();
        //初始化封面
        initCoverView();
        //初始化手势view
        initGestureView();
        //初始化控制栏
        initControlView();
        //初始化清晰度view
        initQualityView();
        //初始化缩略图
        initThumbnailView();
        //初始化倍速view
        initSpeedView();
        //初始化指引view
        initGuideView();
        //初始化提示view
        initTipsView();
        //初始化网络监听器
        initNetWatchdog();
        //初始化屏幕方向监听
        initOrientationWatchdog();
        //初始化手势对话框控制
        initGestureDialogManager();
        //默认为蓝色主题
        setTheme(Theme.Blue);
        //先隐藏手势和控制栏，防止在没有prepare的时候做操作。
        hideGestureAndControlViews();
    }

    /**
     * 更新UI播放器的主题
     *
     * @param theme 支持的主题
     */
    @Override
    public void setTheme(Theme theme) {
        //通过判断子View是否实现了ITheme的接口，去更新主题
        int childCounts = getChildCount();
        for (int i = 0; i < childCounts; i++) {
            View view = getChildAt(i);
            if (view instanceof ITheme) {
                ((ITheme) view).setTheme(theme);
            }
        }
    }

    /**
     * 切换播放速度
     *
     * @param speedValue 播放速度
     */
    public void changeSpeed(SpeedView.SpeedValue speedValue) {
        if (speedValue == SpeedView.SpeedValue.Normal) {
            currentSpeed = 1.0f;
        } else if (speedValue == SpeedView.SpeedValue.OneQuartern) {
            currentSpeed = 1.25f;
        } else if (speedValue == SpeedView.SpeedValue.OneHalf) {
            currentSpeed = 1.5f;
        } else if (speedValue == SpeedView.SpeedValue.Twice) {
            currentSpeed = 2.0f;
        }
        mAliyunVodPlayer.setSpeed(currentSpeed);
    }

    public void setCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentVolume(float progress) {
        this.currentVolume = progress;
        mAliyunVodPlayer.setVolume(progress);
    }

    public float getCurrentVolume() {
        if (mAliyunVodPlayer != null) {
            return mAliyunVodPlayer.getVolume();
        }
        return 0;
    }


    /**
     * UI播放器支持的主题
     */
    public static enum Theme {
        /**
         * 蓝色主题
         */
        Blue,
        /**
         * 绿色主题
         */
        Green,
        /**
         * 橙色主题
         */
        Orange,
        /**
         * 红色主题
         */
        Red
    }

    /**
     * 隐藏手势和控制栏
     */
    private void hideGestureAndControlViews() {
        if (mGestureView != null) {
            mGestureView.hide(ViewAction.HideType.Normal);
        }
        if (mControlView != null) {
            mControlView.hide(ViewAction.HideType.Normal);
        }
    }

    /**
     * 初始化网络监听
     */
    private void initNetWatchdog() {
        Context context = getContext();
        mNetWatchdog = new NetWatchdog(context);
        mNetWatchdog.setNetChangeListener(new MyNetChangeListener(this));
        mNetWatchdog.setNetConnectedListener(new MyNetConnectedListener(this));
    }

    private void onWifiTo4G() {
        VcPlayerLog.d(TAG, "onWifiTo4G");

        //如果已经显示错误了，那么就不用显示网络变化的提示了。
        if (mTipsView.isErrorShow()) {
            return;
        }

        //wifi变成4G，先暂停播放
        if (!isLocalSource()) {
            pause();
        }


        //隐藏其他的动作,防止点击界面去进行其他操作
        mGestureView.hide(ControlView.HideType.Normal);
        mControlView.hide(ControlView.HideType.Normal);

        //显示网络变化的提示
        if (!isLocalSource() && mTipsView != null) {
            mTipsView.showNetChangeTipView();
        }
    }

    private void on4GToWifi() {
        VcPlayerLog.d(TAG, "on4GToWifi");
        //如果已经显示错误了，那么就不用显示网络变化的提示了。
        if (mTipsView.isErrorShow()) {
            return;
        }

        //隐藏网络变化的提示
        if (mTipsView != null) {
            mTipsView.hideNetErrorTipView();
        }
    }

    private void onNetDisconnected() {
        VcPlayerLog.d(TAG, "onNetDisconnected");
        //网络断开。
        // NOTE： 由于安卓这块网络切换的时候，有时候也会先报断开。所以这个回调是不准确的。
    }

    private static class MyNetChangeListener implements NetWatchdog.NetChangeListener {

        private WeakReference<AliyunVodPlayerView> viewWeakReference;

        public MyNetChangeListener(AliyunVodPlayerView aliyunVodPlayerView) {
            viewWeakReference = new WeakReference<AliyunVodPlayerView>(aliyunVodPlayerView);
        }

        @Override
        public void onWifiTo4G() {
            AliyunVodPlayerView aliyunVodPlayerView = viewWeakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.onWifiTo4G();
            }
        }

        @Override
        public void on4GToWifi() {
            AliyunVodPlayerView aliyunVodPlayerView = viewWeakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.on4GToWifi();
            }
        }

        @Override
        public void onNetDisconnected() {
            AliyunVodPlayerView aliyunVodPlayerView = viewWeakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.onNetDisconnected();
            }
        }
    }

    /**
     * 初始化缩略图
     */
    private void initThumbnailView() {
        mThumbnailView = new ThumbnailView(getContext());
        mThumbnailView.setVisibility(View.GONE);
        addSubViewByCenter(mThumbnailView);
    }

    /**
     * 初始化屏幕方向旋转。用来监听屏幕方向。结果通过OrientationListener回调出去。
     */
    private void initOrientationWatchdog() {
        final Context context = getContext();
        mOrientationWatchDog = new OrientationWatchDog(context);
        mOrientationWatchDog.setOnOrientationListener(new InnerOrientationListener(this));
    }

    private static class InnerOrientationListener implements OrientationWatchDog.OnOrientationListener {

        private WeakReference<AliyunVodPlayerView> playerViewWeakReference;

        public InnerOrientationListener(AliyunVodPlayerView playerView) {
            playerViewWeakReference = new WeakReference<AliyunVodPlayerView>(playerView);
        }

        @Override
        public void changedToLandForwardScape(boolean fromPort) {
            AliyunVodPlayerView playerView = playerViewWeakReference.get();
            if (playerView != null) {
                playerView.changedToLandForwardScape(fromPort);
            }
        }

        @Override
        public void changedToLandReverseScape(boolean fromPort) {
            AliyunVodPlayerView playerView = playerViewWeakReference.get();
            if (playerView != null) {
                playerView.changedToLandReverseScape(fromPort);
            }
        }

        @Override
        public void changedToPortrait(boolean fromLand) {
            AliyunVodPlayerView playerView = playerViewWeakReference.get();
            if (playerView != null) {
                playerView.changedToPortrait(fromLand);
            }
        }
    }

    /**
     * 屏幕方向变为横屏。
     *
     * @param fromPort 是否从竖屏变过来
     */
    private void changedToLandForwardScape(boolean fromPort) {
        //如果不是从竖屏变过来，也就是一直是横屏的时候，就不用操作了
        if (!fromPort) {
            return;
        }
        changeScreenMode(AliyunScreenMode.Full, false);
        if (orientationChangeListener != null) {
            orientationChangeListener.orientationChange(fromPort, mCurrentScreenMode);
        }
    }

    /**
     * 屏幕方向变为横屏。
     *
     * @param fromPort 是否从竖屏变过来
     */
    private void changedToLandReverseScape(boolean fromPort) {
        //如果不是从竖屏变过来，也就是一直是横屏的时候，就不用操作了
        if (!fromPort) {
            return;
        }
        changeScreenMode(AliyunScreenMode.Full, true);
        if (orientationChangeListener != null) {
            orientationChangeListener.orientationChange(fromPort, mCurrentScreenMode);
        }
    }

    /**
     * 屏幕方向变为竖屏
     *
     * @param fromLand 是否从横屏转过来
     */
    private void changedToPortrait(boolean fromLand) {
        //屏幕转为竖屏
        if (mIsFullScreenLocked) {
            return;
        }

        if (mCurrentScreenMode == AliyunScreenMode.Full) {
            //全屏情况转到了竖屏
            if (getLockPortraitMode() == null) {
                //没有固定竖屏，就变化mode
                if (fromLand) {
                    changeScreenMode(AliyunScreenMode.Small, false);
                } else {
                    //如果没有转到过横屏，就不让他转了。防止竖屏的时候点横屏之后，又立即转回来的现象
                }
            } else {
                //固定竖屏了，竖屏还是竖屏，不用动
            }
        } else if (mCurrentScreenMode == AliyunScreenMode.Small) {
            //竖屏的情况转到了竖屏
        }
        if (orientationChangeListener != null) {
            orientationChangeListener.orientationChange(fromLand, mCurrentScreenMode);
        }
    }

    /**
     * 初始化手势的控制类
     */
    private void initGestureDialogManager() {
        Context context = getContext();
        if (context instanceof Activity) {
            mGestureDialogManager = new GestureDialogManager((Activity) context);
        }
    }

    /**
     * 初始化提示view
     */
    private void initTipsView() {

        mTipsView = new TipsView(getContext());
        //设置tip中的点击监听事件
        mTipsView.setOnTipClickListener(new TipsView.OnTipClickListener() {
            @Override
            public void onContinuePlay() {
                VcPlayerLog.d(TAG, "playerState = " + mPlayerState);
                //继续播放。如果没有prepare或者stop了，需要重新prepare
                mTipsView.hideAll();
                if (mPlayerState == IPlayer.paused || mPlayerState == IPlayer.prepared){
                    start();
                }else {
                    if (mAliyunPlayAuth != null) {
                        prepareAuth(mAliyunPlayAuth);
                    } else if (mAliyunVidSts != null) {
                        prepareVidsts(mAliyunVidSts);
                    } else if (mAliyunLocalSource != null) {
                        prepareLocalSource(mAliyunLocalSource);
                    }
                }
            }

            @Override
            public void onStopPlay() {
                // 结束播放
                mTipsView.hideAll();
                stop();

                Context context = getContext();
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
            }

            @Override
            public void onRetryPlay() {
                //重试
                reTry();
            }

            @Override
            public void onReplay() {
                //重播
                rePlay();
            }

            @Override
            public void onRefreshSts() {
                if (mOutTimeExpiredErrorListener != null) {
                    mOutTimeExpiredErrorListener.onTimeExpiredError();
                }
            }
        });
        addSubView(mTipsView);
    }

    /**
     * 重试播放，会从当前位置开始播放
     */
    public void reTry() {

        isCompleted = false;
        inSeek = false;

        int currentPosition = mControlView.getVideoPosition();
        VcPlayerLog.d(TAG, " currentPosition = " + currentPosition);

        if (mTipsView != null) {
            mTipsView.hideAll();
        }
        if (mControlView != null) {
            mControlView.reset();
            //防止被reset掉，下次还可以获取到这些值
            mControlView.setVideoPosition(currentPosition);
        }
        if (mGestureView != null) {
            mGestureView.reset();
        }

        if (mAliyunVodPlayer != null) {

            //显示网络加载的loading。。
            if (mTipsView != null) {
                mTipsView.showNetLoadingTipView();
            }

            /*
                isLocalSource()判断不够,有可能是sts播放,也有可能是url播放,还有可能是sd卡的视频播放,
                如果是后两者,需要走if,否则走else
            */
            if (isLocalSource() || isUrlSource()) {
                mAliyunVodPlayer.setDataSource(mAliyunLocalSource);
                mAliyunVodPlayer.prepare();
            } else {
                mAliyunVodPlayer.setDataSource(mAliyunVidSts);
                mAliyunVodPlayer.prepare();
            }
            isAutoAccurate(currentPosition);
        }

    }

    /**
     * 重播，将会从头开始播放
     */
    public void rePlay() {

        isCompleted = false;
        inSeek = false;

        if (mTipsView != null) {
            mTipsView.hideAll();
        }
        if (mControlView != null) {
            mControlView.reset();
        }
        if (mGestureView != null) {
            mGestureView.reset();
        }

        if (mAliyunVodPlayer != null) {
            //显示网络加载的loading。。
            if (mTipsView != null) {
                mTipsView.showNetLoadingTipView();
            }
            //重播是从头开始播
            mAliyunVodPlayer.prepare();
        }

    }

    /**
     * 重置。包括一些状态值，view的状态等
     */
    private void reset() {
        isCompleted = false;
        inSeek = false;
        mCurrentPosition = 0;
        mVideoBufferedPosition = 0;

        if (mTipsView != null) {
            mTipsView.hideAll();
        }
        if (mControlView != null) {
            mControlView.reset();
        }
        if (mGestureView != null) {
            mGestureView.reset();
        }
        stop();
    }

    /**
     * 初始化封面
     */
    private void initCoverView() {
        mCoverView = new ImageView(getContext());
        //这个是为了给自动化测试用的id
        mCoverView.setId(R.id.custom_id_min);
        addSubView(mCoverView);
    }

    /**
     * 初始化控制栏view
     */
    private void initControlView() {
        mControlView = new ControlView(getContext());
        addSubView(mControlView);
        //设置播放按钮点击
        mControlView.setOnPlayStateClickListener(new ControlView.OnPlayStateClickListener() {
            @Override
            public void onPlayStateClick() {
                switchPlayerState();
            }
        });
        //设置进度条的seek监听
        mControlView.setOnSeekListener(new ControlView.OnSeekListener() {
            @Override
            public void onSeekEnd(int position) {

                if (mControlView != null) {
                    mControlView.setVideoPosition(position);
                }
                if (isCompleted) {
                    //播放完成了，不能seek了
                    inSeek = false;
                } else {
                    //拖动结束后，开始seek
                    seekTo(position);

                    if (onSeekStartListener != null) {
                        onSeekStartListener.onSeekStart(position);
                    }
                    hideThumbnailView();
                }
            }

            @Override
            public void onSeekStart(int position) {
                //拖动开始
                inSeek = true;
                if(mThumbnailPrepareSuccess){
                    showThumbnailView();
                }
            }

            @Override
            public void onProgressChanged(int progress) {
                requestBitmapByPosition(progress);
            }
        });
        //菜单按钮点击
        mControlView.setOnMenuClickListener(new ControlView.OnMenuClickListener() {
            @Override
            public void onMenuClick() {
                //点击之后显示倍速界面
                //根据屏幕模式，显示倍速界面
                mSpeedView.show(mCurrentScreenMode);
            }
        });
        mControlView.setOnDownloadClickListener(new ControlView.OnDownloadClickListener() {
            @Override
            public void onDownloadClick() {
                //点击下载之后弹出不同清晰度选择下载dialog
                // 如果当前播放视频时url类型, 不允许下载
                if ("localSource".equals(PlayParameter.PLAY_PARAM_TYPE)) {
                    FixedToastUtils.show(getContext(), getResources().getString(R.string.alivc_video_not_support_download));
                    return;
                }
                if (mOnPlayerViewClickListener != null) {
                    mOnPlayerViewClickListener.onClick(mCurrentScreenMode, PlayViewType.Download);
                }
            }
        });
        //清晰度按钮点击
        mControlView.setOnQualityBtnClickListener(new ControlView.OnQualityBtnClickListener() {

            @Override
            public void onQualityBtnClick(View v, List<TrackInfo> qualities, String currentQuality) {
                //显示清晰度列表
                mQualityView.setQuality(qualities, currentQuality);
                mQualityView.showAtTop(v);
            }

            @Override
            public void onHideQualityView() {
                mQualityView.hide();
            }
        });
        //点击锁屏的按钮
        mControlView.setOnScreenLockClickListener(new ControlView.OnScreenLockClickListener() {
            @Override
            public void onClick() {
                lockScreen(!mIsFullScreenLocked);
            }
        });
        //点击全屏/小屏按钮
        mControlView.setOnScreenModeClickListener(new ControlView.OnScreenModeClickListener() {
            @Override
            public void onClick() {
                AliyunScreenMode targetMode;
                if (mCurrentScreenMode == AliyunScreenMode.Small) {
                    targetMode = AliyunScreenMode.Full;
                } else {
                    targetMode = AliyunScreenMode.Small;
                }

                changeScreenMode(targetMode, false);

            }
        });
        //点击了标题栏的返回按钮
        mControlView.setOnBackClickListener(new ControlView.OnBackClickListener() {
            @Override
            public void onClick() {

                if (mCurrentScreenMode == AliyunScreenMode.Full) {
                    //设置为小屏状态
                    changeScreenMode(AliyunScreenMode.Small, false);
                } else if (mCurrentScreenMode == AliyunScreenMode.Small) {
                    //小屏状态下，就结束活动
                    Context context = getContext();
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                }

            }
        });

        // 横屏下显示更多
        mControlView.setOnShowMoreClickListener(new ControlView.OnShowMoreClickListener() {
            @Override
            public void showMore() {
                if (mOutOnShowMoreClickListener != null) {
                    mOutOnShowMoreClickListener.showMore();
                }
            }
        });


    }
    /**
     * 根据位置请求缩略图
     */
    private void requestBitmapByPosition(int targetPosition) {
        if (mThumbnailHelper != null && mThumbnailPrepareSuccess) {
            mThumbnailHelper.requestBitmapAtPosition(targetPosition);
        }
    }
    /**
     * 隐藏缩略图
     */
    private void hideThumbnailView() {
        if (mThumbnailView != null) {
            mThumbnailView.hideThumbnailView();
        }
    }

    /**
     * 显示缩略图
     */
    private void showThumbnailView() {
        if (mThumbnailView != null && mThumbnailPrepareSuccess) {
            mThumbnailView.showThumbnailView();
            //根据屏幕大小调整缩略图的大小
            ImageView thumbnailImageView = mThumbnailView.getThumbnailImageView();
            if (thumbnailImageView != null) {
                ViewGroup.LayoutParams layoutParams = thumbnailImageView.getLayoutParams();
                layoutParams.width = (int) (ScreenUtils.getWidth(getContext()) / 3);
                layoutParams.height = layoutParams.width / 2 - DensityUtil.px2dip(getContext(), 10);
                thumbnailImageView.setLayoutParams(layoutParams);
            }
        }
    }
    /**
     * 目标位置计算算法
     *
     * @param duration        视频总时长
     * @param currentPosition 当前播放位置
     * @param deltaPosition   与当前位置相差的时长
     * @return
     */
    public int getTargetPosition(long duration, long currentPosition, long deltaPosition) {
        // seek步长
        long finalDeltaPosition;
        // 根据视频时长，决定seek步长
        long totalMinutes = duration / 1000 / 60;
        int hours = (int) (totalMinutes / 60);
        int minutes = (int) (totalMinutes % 60);

        // 视频时长为1小时以上，小屏和全屏的手势滑动最长为视频时长的十分之一
        if (hours >= 1) {
            finalDeltaPosition = deltaPosition / 10;
        }// 视频时长为31分钟－60分钟时，小屏和全屏的手势滑动最长为视频时长五分之一
        else if (minutes > 30) {
            finalDeltaPosition = deltaPosition / 5;
        }// 视频时长为11分钟－30分钟时，小屏和全屏的手势滑动最长为视频时长三分之一
        else if (minutes > 10) {
            finalDeltaPosition = deltaPosition / 3;
        }// 视频时长为4-10分钟时，小屏和全屏的手势滑动最长为视频时长二分之一
        else if (minutes > 3) {
            finalDeltaPosition = deltaPosition / 2;
        }// 视频时长为1秒钟至3分钟时，小屏和全屏的手势滑动最长为视频结束
        else {
            finalDeltaPosition = deltaPosition;
        }

        long targetPosition = finalDeltaPosition + currentPosition;
        if (targetPosition < 0) {
            targetPosition = 0;
        }
        if (targetPosition > duration) {
            targetPosition = duration;
        }
        return (int) targetPosition;
    }

    public interface createSuccessListener {
        void createSuccess();
    }

    private createSuccessListener mListener;

    public void setCreateSuccessListener(createSuccessListener listener) {
        this.mListener = listener;
    }


    /**
     * 锁定屏幕。锁定屏幕后，只有锁会显示，其他都不会显示。手势也不可用
     *
     * @param lockScreen 是否锁住
     */
    public void lockScreen(boolean lockScreen) {
        mIsFullScreenLocked = lockScreen;
        if (mControlView != null) {
            mControlView.setScreenLockStatus(mIsFullScreenLocked);
        }
        if (mGestureView != null) {
            mGestureView.setScreenLockStatus(mIsFullScreenLocked);
        }
    }

    /**
     * 初始化清晰度列表
     */
    private void initQualityView() {
        mQualityView = new QualityView(getContext());
        addSubView(mQualityView);
        //清晰度点击事件
        mQualityView.setOnQualityClickListener(new QualityView.OnQualityClickListener() {
            @Override
            public void onQualityClick(TrackInfo qualityTrackInfo) {
                //进行清晰度的切换
                mAliyunVodPlayer.selectTrack(qualityTrackInfo.getIndex());
            }
        });
    }

    /**
     * 初始化倍速view
     */
    private void initSpeedView() {
        mSpeedView = new SpeedView(getContext());
        addSubView(mSpeedView);

        //倍速点击事件
        mSpeedView.setOnSpeedClickListener(new SpeedView.OnSpeedClickListener() {
            @Override
            public void onSpeedClick(SpeedView.SpeedValue value) {
                float speed = 1.0f;
                if (value == SpeedView.SpeedValue.Normal) {
                    speed = 1.0f;
                } else if (value == SpeedView.SpeedValue.OneQuartern) {
                    speed = 1.25f;
                } else if (value == SpeedView.SpeedValue.OneHalf) {
                    speed = 1.5f;
                } else if (value == SpeedView.SpeedValue.Twice) {
                    speed = 2.0f;
                }

                //改变倍速
                if (mAliyunVodPlayer != null) {
                    mAliyunVodPlayer.setSpeed(speed);
                }

                mSpeedView.setSpeed(value);
            }

            @Override
            public void onHide() {
                //当倍速界面隐藏之后，显示菜单按钮
            }
        });

    }

    /**
     * 初始化引导view
     */
    private void initGuideView() {
        mGuideView = new GuideView(getContext());
        addSubView(mGuideView);
    }

    /**
     * 切换播放状态。点播播放按钮之后的操作
     */
    private void switchPlayerState() {

        if (mPlayerState == IPlayer.started) {
            pause();

        } else if (mPlayerState == IPlayer.paused || mPlayerState == IPlayer.prepared) {
            start();
        }
        if (onPlayStateBtnClickListener != null) {
            onPlayStateBtnClickListener.onPlayBtnClick(mPlayerState);
        }
    }

    /**
     * 初始化手势view
     */
    private void initGestureView() {
        mGestureView = new GestureView(getContext());
        addSubView(mGestureView);

        //设置手势监听
        mGestureView.setOnGestureListener(new GestureView.GestureListener() {

            @Override
            public void onHorizontalDistance(float downX, float nowX) {
                //水平滑动调节seek。
                // seek需要在手势结束时操作。
                long duration = mAliyunVodPlayer.getDuration();
                long position = mCurrentPosition;
                long deltaPosition = 0;
                int targetPosition = 0;
                if (mPlayerState == IPlayer.prepared ||
                        mPlayerState == IPlayer.paused ||
                        mPlayerState == IPlayer.started) {
                    //在播放时才能调整大小
                    deltaPosition = (long) (nowX - downX) * duration / getWidth();
                    targetPosition = getTargetPosition(duration, position, deltaPosition);
                }

                if (mGestureDialogManager != null) {
                    inSeek = true;
                    mControlView.setVideoPosition(targetPosition);
                    requestBitmapByPosition(targetPosition);
                    showThumbnailView();
                }
            }

            @Override
            public void onLeftVerticalDistance(float downY, float nowY) {
                //左侧上下滑动调节亮度
                int changePercent = (int) ((nowY - downY) * 100 / getHeight());

                if (mGestureDialogManager != null) {
                    mGestureDialogManager.showBrightnessDialog(AliyunVodPlayerView.this, mScreenBrightness);
                    int brightness = mGestureDialogManager.updateBrightnessDialog(changePercent);
                    if (mOnScreenBrightnessListener != null) {
                        mOnScreenBrightnessListener.onScreenBrightness(brightness);
                    }
                    mScreenBrightness = brightness;
                }
            }

            @Override
            public void onRightVerticalDistance(float downY, float nowY) {
                //右侧上下滑动调节音量
                float volume = mAliyunVodPlayer.getVolume();
                int changePercent = (int) ((nowY - downY) * 100 / getHeight());
                if (mGestureDialogManager != null) {
                    mGestureDialogManager.showVolumeDialog(AliyunVodPlayerView.this, volume * 100);
                    float targetVolume = mGestureDialogManager.updateVolumeDialog(changePercent);
                    currentVolume = targetVolume;
                    //通过返回值改变音量
                    mAliyunVodPlayer.setVolume((targetVolume / 100.00f));
                }
            }

            @Override
            public void onGestureEnd() {
                //手势结束。
                //seek需要在结束时操作。
                if (mGestureDialogManager != null) {
                    mGestureDialogManager.dismissBrightnessDialog();
                    mGestureDialogManager.dismissVolumeDialog();
                    if (inSeek) {
                        int seekPosition = mControlView.getVideoPosition();
                        if (seekPosition >= mAliyunVodPlayer.getDuration()) {
                            seekPosition = (int) (mAliyunVodPlayer.getDuration() - 1000);
                        }
                        if (seekPosition >= 0) {
                            seekTo(seekPosition);
                            hideThumbnailView();
                        }else {
                            inSeek = false;
                        }
                    }

                }
            }

            @Override
            public void onSingleTap() {
                //单击事件，显示控制栏
                if (mControlView != null){
                    if (mControlView.getVisibility() != VISIBLE) {
                        mControlView.show();
                    } else {
                        mControlView.hide(ControlView.HideType.Normal);
                    }
                }

            }

            @Override
            public void onDoubleTap() {
                switchPlayerState();
            }
        });
    }

    /**
     * 初始化播放器显示view
     */
    private void initSurfaceView() {
        mSurfaceView = new SurfaceView(getContext().getApplicationContext());
        addSubView(mSurfaceView);

        SurfaceHolder holder = mSurfaceView.getHolder();
        //增加surfaceView的监听
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                VcPlayerLog.d(TAG, " surfaceCreated = surfaceHolder = " + surfaceHolder);
                if (mAliyunVodPlayer != null) {
                    mAliyunVodPlayer.setDisplay(surfaceHolder);
                    //防止黑屏
                    mAliyunVodPlayer.redraw();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width,
                                       int height) {
                VcPlayerLog.d(TAG,
                        " surfaceChanged surfaceHolder = " + surfaceHolder + " ,  width = " + width + " , height = "
                                + height);
                if (mAliyunVodPlayer != null) {
                    mAliyunVodPlayer.redraw();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                VcPlayerLog.d(TAG, " surfaceDestroyed = surfaceHolder = " + surfaceHolder);
                if (mAliyunVodPlayer != null) {
                    mAliyunVodPlayer.setDisplay(null);
                }
            }
        });
    }

    /**
     * 初始化播放器
     */
    private void initAliVcPlayer() {

        mAliyunVodPlayer = AliPlayerFactory.createAliPlayer(mContext);
        mAliyunVodPlayer.enableLog(true);
        //设置准备回调
        mAliyunVodPlayer.setOnPreparedListener(new VideoPlayerPreparedListener(this));
        //播放器出错监听
        mAliyunVodPlayer.setOnErrorListener(new VideoPlayerErrorListener(this));
        //播放器加载回调
        mAliyunVodPlayer.setOnLoadingStatusListener(new VideoPlayerLoadingStatusListener(this));
        //播放器状态
        mAliyunVodPlayer.setOnStateChangedListener(new VideoPlayerStateChangedListener(this));
        //播放结束
        mAliyunVodPlayer.setOnCompletionListener(new VideoPlayerCompletionListener(this));
        //播放信息监听
        mAliyunVodPlayer.setOnInfoListener(new VideoPlayerInfoListener(this));
        //第一帧显示
        mAliyunVodPlayer.setOnRenderingStartListener(new VideoPlayerRenderingStartListener(this));
        //trackChange监听
        mAliyunVodPlayer.setOnTrackChangedListener(new VideoPlayerTrackChangedListener(this));
        //seek结束事件
        mAliyunVodPlayer.setOnSeekCompleteListener(new VideoPlayerOnSeekCompleteListener(this));
        mAliyunVodPlayer.setOnSeiDataListener(new VideoPlayerOnSeiDataListener(this));
    }

    /**
     * 获取从源中设置的标题 。 如果用户设置了标题，优先使用用户设置的标题。 如果没有，就使用服务器返回的标题
     *
     * @param title 服务器返回的标题
     * @return 最后的标题
     */
    private String getTitle(String title) {
        String finalTitle = title;
        if (mAliyunLocalSource != null) {
            finalTitle = mAliyunLocalSource.getTitle();
        } else if (mAliyunPlayAuth != null) {
            finalTitle = mAliyunPlayAuth.getTitle();
        } else if (mAliyunVidSts != null) {
            finalTitle = mAliyunVidSts.getTitle();
        }

        if (TextUtils.isEmpty(finalTitle)) {
            return title;
        } else {
            return finalTitle;
        }
    }

    /**
     * 获取从源中设置的封面 。 如果用户设置了封面，优先使用用户设置的封面。 如果没有，就使用服务器返回的封面
     *
     * @param postUrl 服务器返回的封面
     * @return 最后的封面
     */
    private String getPostUrl(String postUrl) {
        String finalPostUrl = postUrl;
        if (mAliyunLocalSource != null) {
            finalPostUrl = mAliyunLocalSource.getCoverPath();
        } else if (mAliyunPlayAuth != null) {

        }

        if (TextUtils.isEmpty(finalPostUrl)) {
            return postUrl;
        } else {
            return finalPostUrl;
        }
    }

    /**
     * 判断是否是本地资源
     *
     * @return
     */
    private boolean isLocalSource() {
        String scheme = null;
        if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            return false;
        }
        if ("localSource".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            Uri parse = Uri.parse(PlayParameter.PLAY_PARAM_URL);
            scheme = parse.getScheme();
        }
        return scheme == null;
    }

    /**
     * 判断是否是Url播放资源
     */
    private boolean isUrlSource() {
        String scheme = null;
        if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            return false;
        } else {
            Uri parse = Uri.parse(PlayParameter.PLAY_PARAM_URL);
            scheme = parse.getScheme();
            return scheme != null;
        }
    }


    /**
     * 获取视频时长
     *
     * @return 视频时长
     */
    public int getDuration() {
        if (mAliyunVodPlayer != null) {
            return (int) mAliyunVodPlayer.getDuration();
        }

        return 0;
    }

    /**
     * 获取当前位置
     *
     * @return 当前位置
     */
//    public int getCurrentPosition() {
//        if (mAliyunVodPlayer != null && mAliyunVodPlayer.isPlaying()) {
//            return (int) mAliyunVodPlayer.getCurrentPosition();
//        }
//
//        return 0;
//    }

    /**
     * 显示错误提示
     *
     * @param errorCode  错误码
     * @param errorEvent 错误事件
     * @param errorMsg   错误描述
     */
    public void showErrorTipView(int errorCode, String errorEvent, String errorMsg) {
        pause();
        stop();
        if (mControlView != null) {
            mControlView.setPlayState(ControlView.PlayState.NotPlaying);
        }

        if (mTipsView != null) {
            //隐藏其他的动作,防止点击界面去进行其他操作
            mGestureView.hide(ViewAction.HideType.End);
            mControlView.hide(ViewAction.HideType.End);
            mCoverView.setVisibility(GONE);
            mTipsView.showErrorTipView(errorCode, errorEvent, errorMsg);
        }
    }

    private void hideErrorTipView() {

        if (mTipsView != null) {
            //隐藏其他的动作,防止点击界面去进行其他操作
            mTipsView.hideErrorTipView();
        }
    }

    /**
     * addSubView 添加子view到布局中
     *
     * @param view 子view
     */
    private void addSubView(View view) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        //添加到布局中
        addView(view, params);
    }

    /**
     * 添加子View到布局中央
     */
    private void addSubViewByCenter(View view) {
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(view, params);
    }

    /**
     * 添加子View到布局中,在某个View的下方
     *
     * @param view            需要添加的View
     * @param belowTargetView 在这个View的下方
     */
    private void addSubViewBelow(final View view, final View belowTargetView) {
        belowTargetView.post(new Runnable() {
            @Override
            public void run() {
                LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                int measuredHeight = belowTargetView.getMeasuredHeight();
                params.topMargin = measuredHeight;
                //添加到布局中
                addView(view, params);
            }
        });
    }


    /**
     * 改变屏幕模式：小屏或者全屏。
     *
     * @param targetMode
     */
    public void changeScreenMode(AliyunScreenMode targetMode, boolean isReverse) {
        VcPlayerLog.d(TAG, "mIsFullScreenLocked = " + mIsFullScreenLocked + " ， targetMode = " + targetMode);

        AliyunScreenMode finalScreenMode = targetMode;

        if (mIsFullScreenLocked) {
            finalScreenMode = AliyunScreenMode.Full;
        }

        //这里可能会对模式做一些修改
        if (targetMode != mCurrentScreenMode) {
            mCurrentScreenMode = finalScreenMode;
        }

        if (mControlView != null) {
            mControlView.setScreenModeStatus(finalScreenMode);
        }

        if (mSpeedView != null) {
            mSpeedView.setScreenMode(finalScreenMode);
        }

        if (mGuideView != null) {
            mGuideView.setScreenMode(finalScreenMode);
        }

        Context context = getContext();
        if (context instanceof Activity) {
            if (finalScreenMode == AliyunScreenMode.Full) {
                if (getLockPortraitMode() == null) {
                    //不是固定竖屏播放。
//                    ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    if (isReverse) {
                        ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    } else {
                        ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }

                    //SCREEN_ORIENTATION_LANDSCAPE只能固定一个横屏方向
                } else {
                    //如果是固定全屏，那么直接设置view的布局，宽高
                    ViewGroup.LayoutParams aliVcVideoViewLayoutParams = getLayoutParams();
                    aliVcVideoViewLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
            } else if (finalScreenMode == AliyunScreenMode.Small) {

                if (getLockPortraitMode() == null) {
                    //不是固定竖屏播放。
                    ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    //如果是固定全屏，那么直接设置view的布局，宽高
                    ViewGroup.LayoutParams aliVcVideoViewLayoutParams = getLayoutParams();
                    aliVcVideoViewLayoutParams.height = (int) (ScreenUtils.getWidth(context) * 9.0f / 16);
                    aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
            }
        }
    }

    /**
     * 获取当前屏幕模式：小屏、全屏
     *
     * @return 当前屏幕模式
     */
    public AliyunScreenMode getScreenMode() {
        return mCurrentScreenMode;
    }

    /**
     * 设置准备事件监听
     *
     * @param onPreparedListener 准备事件
     */
    public void setOnPreparedListener(IPlayer.OnPreparedListener onPreparedListener) {
        mOutPreparedListener = onPreparedListener;
    }

    /**
     * 设置错误事件监听
     *
     * @param onErrorListener 错误事件监听
     */
    public void setOnErrorListener(IPlayer.OnErrorListener onErrorListener) {
        mOutErrorListener = onErrorListener;
    }

    /**
     * 设置信息事件监听
     *
     * @param onInfoListener 信息事件监听
     */
    public void setOnInfoListener(IPlayer.OnInfoListener onInfoListener) {
        mOutInfoListener = onInfoListener;
    }

    /**
     * 设置播放完成事件监听
     *
     * @param onCompletionListener 播放完成事件监听
     */
    public void setOnCompletionListener(IPlayer.OnCompletionListener onCompletionListener) {
        mOutCompletionListener = onCompletionListener;
    }

    /**
     * 设置改变清晰度事件监听
     *
     * @param l 清晰度事件监听
     */
    public void setOnChangeQualityListener(OnChangeQualityListener l) {
        mOutChangeQualityListener = l;
    }


    /**
     * 设置重播事件监听
     *
     * @param onRePlayListener 重播事件监听
     */
//    public void setOnRePlayListener(IPlayer.OnRePlayListener onRePlayListener) {
//        mOutRePlayListener = onRePlayListener;
//    }

    /**
     * 设置自动播放事件监听
     *
     * @param l 自动播放事件监听
     */
    public void setOnAutoPlayListener(OnAutoPlayListener l) {
        mOutAutoPlayListener = l;
    }

    public interface OnTimeExpiredErrorListener {
        void onTimeExpiredError();
    }

    /**
     * 设置源超时监听
     *
     * @param l 源超时监听
     */
    public void setOnTimeExpiredErrorListener(OnTimeExpiredErrorListener l) {
        mOutTimeExpiredErrorListener = l;
    }

    /**
     * 设置鉴权过期监听，在鉴权过期前一分钟回调
     *
     * @param listener
     */
//    public void setOnUrlTimeExpiredListener(IPlayer.OnUrlTimeExpiredListener listener) {
//        this.mOutUrlTimeExpiredListener = listener;
//    }

    /**
     * 设置首帧显示事件监听
     *
     * @param onFirstFrameStartListener 首帧显示事件监听
     */
    public void setOnFirstFrameStartListener(IPlayer.OnRenderingStartListener onFirstFrameStartListener) {
        mOutFirstFrameStartListener = onFirstFrameStartListener;
    }

    /**
     * 设置seek结束监听
     *
     * @param onSeekCompleteListener seek结束监听
     */
    public void setOnSeekCompleteListener(IPlayer.OnSeekCompleteListener onSeekCompleteListener) {
        mOuterSeekCompleteListener = onSeekCompleteListener;
    }

    /**
     * 设置停止播放监听
     *
     * @param onStoppedListener 停止播放监听
     */
    public void setOnStoppedListener(OnStoppedListener onStoppedListener) {
        this.mOnStoppedListener = onStoppedListener;
    }

    /**
     * 设置加载状态监听
     *
     * @param onLoadingListener 加载状态监听
     */
    public void setOnLoadingListener(IPlayer.OnLoadingStatusListener onLoadingListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnLoadingStatusListener(onLoadingListener);
        }
    }
    public void setSeiDataListener(IPlayer.OnSeiDataListener onSeiDataListener){
        this.mOutSeiDataListener = onSeiDataListener;
    }
    /**
     * 设置缓冲监听
     *
     * @param onBufferingUpdateListener 缓冲监听
     */
//    public void setOnBufferingUpdateListener(IPlayer.OnBufferingUpdateListener onBufferingUpdateListener) {
//        if (mAliyunVodPlayer != null) {
//            mAliyunVodPlayer.setOnBufferingUpdateListener(onBufferingUpdateListener);
//        }
//    }

    /**
     * 设置视频宽高变化监听
     *
     * @param onVideoSizeChangedListener 视频宽高变化监听
     */
    public void setOnVideoSizeChangedListener(IPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnVideoSizeChangedListener(onVideoSizeChangedListener);
        }
    }

    /**
     * 设置循环播放开始监听
     *
     * @param onCircleStartListener 循环播放开始监听
     */
//    public void setOnCircleStartListener(IPlayer.OnCircleStartListener onCircleStartListener) {
//        if (mAliyunVodPlayer != null) {
//            mAliyunVodPlayer.setOnCircleStartListener(onCircleStartListener);
//        }
//    }

    /**
     * 设置PlayAuth的播放方式
     *
     * @param aliyunPlayAuth auth
     */
    public void setAuthInfo(VidAuth aliyunPlayAuth) {
        if (mAliyunVodPlayer == null) {
            return;
        }
        //重置界面
        clearAllSource();
        reset();

        mAliyunPlayAuth = aliyunPlayAuth;

        if (mControlView != null) {
            mControlView.setForceQuality(aliyunPlayAuth.isForceQuality());
        }

        //4G的话先提示
        if (!isLocalSource() && NetWatchdog.is4GConnected(getContext())) {
            if (mTipsView != null) {
                mTipsView.showNetChangeTipView();
            }
        } else {
            //具体的准备操作
            prepareAuth(aliyunPlayAuth);
        }
    }

    /**
     * 通过playAuth prepare
     *
     * @param aliyunPlayAuth 源
     */
    private void prepareAuth(VidAuth aliyunPlayAuth) {
        if (mTipsView != null) {
            mTipsView.showNetLoadingTipView();
        }
        if (mControlView != null) {
            mControlView.setIsMtsSource(false);
        }
        if (mQualityView != null) {
            mQualityView.setIsMtsSource(false);
        }
        mAliyunVodPlayer.setDataSource(aliyunPlayAuth);
        mAliyunVodPlayer.prepare();
    }

    /**
     * 清空之前设置的播放源
     */
    private void clearAllSource() {
        mAliyunPlayAuth = null;
        mAliyunVidSts = null;
        mAliyunLocalSource = null;
    }

    /**
     * 设置本地播放源
     *
     * @param aliyunLocalSource 本地播放源
     */
    public void setLocalSource(UrlSource aliyunLocalSource) {
        if (mAliyunVodPlayer == null) {
            return;
        }

        clearAllSource();
        reset();

        mAliyunLocalSource = aliyunLocalSource;

        if (mControlView != null) {
            mControlView.setForceQuality(true);
        }
        if (!isLocalSource() && NetWatchdog.is4GConnected(getContext())) {
            if (mTipsView != null) {
                mTipsView.showNetChangeTipView();
            }
        } else {
            prepareLocalSource(aliyunLocalSource);
        }

    }

    /**
     * prepare本地播放源
     *
     * @param aliyunLocalSource 本地播放源
     */
    private void prepareLocalSource(UrlSource aliyunLocalSource) {
        if (mControlView != null) {
            mControlView.setForceQuality(true);
        }
        if (mControlView != null) {
            mControlView.setIsMtsSource(false);
        }

        if (mQualityView != null) {
            mQualityView.setIsMtsSource(false);
        }
        mAliyunVodPlayer.setAutoPlay(true);
        mAliyunVodPlayer.setDataSource(aliyunLocalSource);
        mAliyunVodPlayer.prepare();
    }

    /**
     * 准备vidsts源
     *
     * @param vidSts 源
     */
    public void setVidSts(VidSts vidSts) {
        if (mAliyunVodPlayer == null) {
            return;
        }

        clearAllSource();
        reset();

        mAliyunVidSts = vidSts;

        if (mControlView != null) {
            mControlView.setForceQuality(vidSts.isForceQuality());
        }
        if (NetWatchdog.is4GConnected(getContext())) {
            if (mTipsView != null) {
                mTipsView.showNetChangeTipView();
            }
        } else {
            prepareVidsts(mAliyunVidSts);
        }
    }

    /**
     * 准备vidsts 源
     */
    private void prepareVidsts(VidSts vidSts) {
        if (mTipsView != null) {
            mTipsView.showNetLoadingTipView();
        }
        if (mControlView != null) {
            mControlView.setIsMtsSource(false);
        }

        if (mQualityView != null) {
            mQualityView.setIsMtsSource(false);
        }
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setDataSource(vidSts);
            mAliyunVodPlayer.prepare();
        }
    }

    /**
     * 设置封面信息
     *
     * @param uri url地址
     */
    public void setCoverUri(String uri) {
        if (mCoverView != null && !TextUtils.isEmpty(uri)) {
            (new ImageLoader(mCoverView)).loadAsync(uri);
            mCoverView.setVisibility(isPlaying() ? GONE : VISIBLE);
        }
    }

    /**
     * 设置封面id
     *
     * @param resId 资源id
     */
    public void setCoverResource(int resId) {
        if (mCoverView != null) {
            mCoverView.setImageResource(resId);
            mCoverView.setVisibility(isPlaying() ? GONE : VISIBLE);
        }
    }

    /**
     * 设置边播边存
     *
     * @param enable      是否开启。开启之后会根据maxDuration和maxSize决定有无缓存。
     * @param saveDir     保存目录
     * @param maxDuration 单个文件最大时长 秒
     * @param maxSize     所有文件最大大小 MB
     */
    public void setPlayingCache(boolean enable, String saveDir, int maxDuration, long maxSize) {
        if (mAliyunVodPlayer != null) {
//            mAliyunVodPlayer.setPlayingCache(enable, saveDir, maxDuration, maxSize);
        }
    }

    /**
     * 设置缩放模式
     *
     * @param scallingMode 缩放模式
     */
    public void setVideoScalingMode(IPlayer.ScaleMode scallingMode) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setScaleMode(scallingMode);
        }
    }


    public LoadingView getLoadView(){
        return mControlView.getLoadView();
    }

    /**
     * 当VodPlayer 没有加载完成的时候,调用onStop 去暂停视频,
     * 会出现暂停失败的问题。
     */
    private static class VodPlayerLoadEndHandler extends Handler {

        private WeakReference<AliyunVodPlayerView> weakReference;

        private boolean intentPause;

        public VodPlayerLoadEndHandler(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                intentPause = true;
            }
            if (msg.what == 1) {
                AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
                if (aliyunVodPlayerView != null && intentPause) {
                    aliyunVodPlayerView.onStop();
                    intentPause = false;
                }
            }
        }
    }

    /**
     * 在activity调用onResume的时候调用。 解决home回来后，画面方向不对的问题
     */
    public void onResume() {
        if (mIsFullScreenLocked) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                changeScreenMode(AliyunScreenMode.Small, false);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                changeScreenMode(AliyunScreenMode.Full, false);
            }
        }

        if (mNetWatchdog != null) {
            mNetWatchdog.startWatch();
        }

        if (mOrientationWatchDog != null) {
            mOrientationWatchDog.startWatch();
        }

        //从其他界面过来的话，也要show。
//        if (mControlView != null) {
//            mControlView.show();
//        }

        //onStop中记录下来的状态，在这里恢复使用
        resumePlayerState();
    }


    /**
     * 暂停播放器的操作
     */
    public void onStop() {
        if (mNetWatchdog != null) {
            mNetWatchdog.stopWatch();
        }
        if (mOrientationWatchDog != null) {
            mOrientationWatchDog.stopWatch();
        }

        //保存播放器的状态，供resume恢复使用。
        savePlayerState();
    }

    /**
     * Activity回来后，恢复之前的状态
     */
    private void resumePlayerState() {
        if (mAliyunVodPlayer == null) {
            return;
        }
        //恢复前台后需要进行判断,如果是本地资源,则继续播放,如果是4g则给予提示,不会继续播放,否则继续播放
        if (!isLocalSource() && NetWatchdog.is4GConnected(getContext())) {

        } else {
            start();
        }

    }

    /**
     * 保存当前的状态，供恢复使用
     */
    private void savePlayerState() {
        if (mAliyunVodPlayer == null) {
            return;
        }
        //然后再暂停播放器
        //如果希望后台继续播放，不需要暂停的话，可以注释掉pause调用。
        pause();
    }

    /**
     * 获取媒体信息
     *
     * @return 媒体信息
     */
    public MediaInfo getMediaInfo() {
        if (mAliyunVodPlayer != null) {
            return mAliyunVodPlayer.getMediaInfo();
        }

        return null;
    }

    /**
     * 活动销毁，释放
     */
    public void onDestroy() {
        stop();
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.release();
            mAliyunVodPlayer = null;
        }

        mSurfaceView = null;
        mGestureView = null;
        mControlView = null;
        mCoverView = null;
        mGestureDialogManager = null;
        if (mNetWatchdog != null) {
            mNetWatchdog.stopWatch();
        }
        mNetWatchdog = null;
        mTipsView = null;
        mAliyunMediaInfo = null;
        if (mOrientationWatchDog != null) {
            mOrientationWatchDog.destroy();
        }
        mOrientationWatchDog = null;
        if (hasLoadEnd != null) {
            hasLoadEnd.clear();
        }
    }

    /**
     * 是否处于播放状态：start或者pause了
     *
     * @return 是否处于播放状态
     */
    public boolean isPlaying() {
        return mPlayerState == IPlayer.started;
    }

    /**
     * 获取播放器状态
     *
     * @return 播放器状态
     */
    public int getPlayerState() {
        return mPlayerState;
    }

    /**
     * 开始播放
     */
    public void start() {
        if (mControlView != null) {
            mControlView.show();
            mControlView.setPlayState(ControlView.PlayState.Playing);
        }

        if (mAliyunVodPlayer == null) {
            return;
        }

        if (mGestureView != null) {
            mGestureView.show();
        }

        if (mPlayerState == IPlayer.paused || mPlayerState == IPlayer.prepared) {
            mAliyunVodPlayer.start();
        }

    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (mControlView != null) {
            mControlView.setPlayState(ControlView.PlayState.NotPlaying);
        }
        if (mAliyunVodPlayer == null) {
            return;
        }

        if (mPlayerState == IPlayer.started || mPlayerState == IPlayer.prepared) {
            mAliyunVodPlayer.pause();
        }
    }

    /**
     * 停止播放
     */
    private void stop() {
        Boolean hasLoadedEnd = null;
        MediaInfo mediaInfo = null;
        if (mAliyunVodPlayer != null && hasLoadEnd != null) {
            mediaInfo = mAliyunVodPlayer.getMediaInfo();
            hasLoadedEnd = hasLoadEnd.get(mediaInfo);
        }

        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.stop();
        }

        if (mControlView != null) {
            mControlView.setPlayState(ControlView.PlayState.NotPlaying);
        }
        if (hasLoadEnd != null) {
            hasLoadEnd.remove(mediaInfo);
        }
    }

    /**
     * seek操作
     *
     * @param position 目标位置
     */
    public void seekTo(int position) {
        if (mAliyunVodPlayer == null) {
            return;
        }

        inSeek = true;
        realySeekToFunction(position);
    }

    private void realySeekToFunction(int position) {
        isAutoAccurate(position);
        mAliyunVodPlayer.start();
        if (mControlView != null) {
            mControlView.setPlayState(ControlView.PlayState.Playing);
        }
    }

    /**
     * 判断是否开启精准seek
     */
    private void isAutoAccurate(int position) {
        if (getDuration() <= ACCURATE) {
            mAliyunVodPlayer.seekTo(position, IPlayer.SeekMode.Accurate);
        } else {
            mAliyunVodPlayer.seekTo(position, IPlayer.SeekMode.Inaccurate);
        }
    }

    /**
     * 设置是否显示标题栏
     *
     * @param show true:是
     */
    public void setTitleBarCanShow(boolean show) {
        if (mControlView != null) {
            mControlView.setTitleBarCanShow(show);
        }
    }

    /**
     * 设置是否显示控制栏
     *
     * @param show true:是
     */
    public void setControlBarCanShow(boolean show) {
        if (mControlView != null) {
            mControlView.setControlBarCanShow(show);
        }

    }

    /**
     * 开启底层日志
     */
    public void enableNativeLog() {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.enableLog(true);
        }
    }

    /**
     * 关闭底层日志
     */
    public void disableNativeLog() {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.enableLog(false);
        }
    }

    /**
     * 设置线程池
     *
     * @param executorService 线程池
     */
//    public void setThreadExecutorService(ExecutorService executorService) {
//        if (mAliyunVodPlayer != null) {
//            mAliyunVodPlayer.setThreadExecutorService(executorService);
//        }
//    }

    /**
     * 获取SDK版本号
     *
     * @return SDK版本号
     */
    public String getSDKVersion() {
        return AliPlayerFactory.getSdkVersion();
    }

    /**
     * 获取播放surfaceView
     *
     * @return 播放surfaceView
     */
    public SurfaceView getPlayerView() {
        return mSurfaceView;
    }

    /**
     * 设置自动播放
     *
     * @param auto true 自动播放
     */
    public void setAutoPlay(boolean auto) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setAutoPlay(auto);
        }
    }

    /**
     * 获取底层的一些debug信息
     *
     * @return debug信息
     */
    public Map<String, String> getAllDebugInfo() {
        if (mAliyunVodPlayer != null) {
//            return mAliyunVodPlayer.getAllDebugInfo();
        }
        return null;
    }

    /**
     * 设置锁定竖屏监听
     *
     * @param listener 监听器
     */
    public void setLockPortraitMode(LockPortraitListener listener) {
        mLockPortraitListener = listener;
    }

    /**
     * 锁定竖屏
     *
     * @return 竖屏监听器
     */
    public LockPortraitListener getLockPortraitMode() {
        return mLockPortraitListener;
    }

    /**
     * 让home键无效
     *
     * @param keyCode 按键
     * @param event   事件
     * @return 是否处理。
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (((mCurrentScreenMode == AliyunScreenMode.Full)) && (keyCode != KeyEvent.KEYCODE_HOME)
                && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            changedToPortrait(true);
            return false;
        }
        if (mIsFullScreenLocked && (keyCode != KeyEvent.KEYCODE_HOME)) {
            return false;
        }
        return true;
    }

    /**
     * 截图功能
     *
     * @return 图片
     */
    public void snapShot() {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.snapshot();
        }
    }

    /**
     * 设置循环播放
     *
     * @param circlePlay true:循环播放
     */
    public void setCirclePlay(boolean circlePlay) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setLoop(circlePlay);
        }
    }

    /**
     * 设置播放时的镜像模式
     *
     * @param mode 镜像模式
     */
    public void setRenderMirrorMode(IPlayer.MirrorMode mode) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setMirrorMode(mode);
        }
    }

    /**
     * 设置播放时的旋转方向
     *
     * @param rotate 旋转角度
     */
    public void setRenderRotate(IPlayer.RotateMode rotate) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setRotateMode(rotate);
        }
    }

    /**
     * 播放按钮点击listener
     */
    public interface OnPlayStateBtnClickListener {
        void onPlayBtnClick(int playerState);
    }

    /**
     * 设置播放状态点击监听
     */
    public void setOnPlayStateBtnClickListener(OnPlayStateBtnClickListener listener) {
        this.onPlayStateBtnClickListener = listener;
    }

    private OnSeekStartListener onSeekStartListener;

    /**
     * seek开始监听
     */

    public interface OnSeekStartListener {
        void onSeekStart(int position);
    }

    public void setOnSeekStartListener(OnSeekStartListener listener) {
        this.onSeekStartListener = listener;
    }

    /**
     * Player View Click Type
     */
    public enum PlayViewType {
        /**
         * click download view
         */
        Download,
        /**
         * click screen cast
         */
        ScreenCast
    }

    public interface OnPlayerViewClickListener {
        void onClick(AliyunScreenMode screenMode, PlayViewType viewType);
    }

    /**
     * 设置播放器view点击事件监听，目前只对外暴露下载按钮和投屏按钮
     */
    public void setmOnPlayerViewClickListener(
            OnPlayerViewClickListener mOnPlayerViewClickListener) {
        this.mOnPlayerViewClickListener = mOnPlayerViewClickListener;
    }

    /**
     * 屏幕方向改变监听接口
     */
    public interface OnOrientationChangeListener {
        /**
         * 屏幕方向改变
         *
         * @param from        从横屏切换为竖屏, 从竖屏切换为横屏
         * @param currentMode 当前屏幕类型
         */
        void orientationChange(boolean from, AliyunScreenMode currentMode);
    }

    private OnOrientationChangeListener orientationChangeListener;

    public void setOrientationChangeListener(
            OnOrientationChangeListener listener) {
        this.orientationChangeListener = listener;
    }

    /**
     * 断网/连网监听
     */
    private class MyNetConnectedListener implements NetWatchdog.NetConnectedListener {
        public MyNetConnectedListener(AliyunVodPlayerView aliyunVodPlayerView) {
        }

        @Override
        public void onReNetConnected(boolean isReconnect) {
            if (mNetConnectedListener != null) {
                mNetConnectedListener.onReNetConnected(isReconnect);
            }
        }

        @Override
        public void onNetUnConnected() {
            if (mNetConnectedListener != null) {
                mNetConnectedListener.onNetUnConnected();
            }
        }
    }

    public void setNetConnectedListener(NetConnectedListener listener) {
        this.mNetConnectedListener = listener;
    }

    /**
     * 判断是否有网络的监听
     */
    public interface NetConnectedListener {
        /**
         * 网络已连接
         */
        void onReNetConnected(boolean isReconnect);

        /**
         * 网络未连接
         */
        void onNetUnConnected();
    }

    public interface OnFinishListener {
        void onFinishClick();
    }

    /**
     * 横屏下显示更多
     */
    public interface OnShowMoreClickListener {
        void showMore();
    }

    public void setOnShowMoreClickListener(
            ControlView.OnShowMoreClickListener listener) {
        this.mOutOnShowMoreClickListener = listener;
    }

    public interface OnScreenBrightnessListener {
        void onScreenBrightness(int brightness);
    }

    public void setOnScreenBrightness(OnScreenBrightnessListener listener) {
        this.mOnScreenBrightnessListener = listener;
    }

    /**
     * 获取当前播放器正在播放的媒体信息
     */
    public MediaInfo getCurrentMediaInfo() {
        return mAliyunMediaInfo;
    }

    /**
     * 设置当前屏幕亮度
     */
    public void setScreenBrightness(int screenBrightness) {
        this.mScreenBrightness = screenBrightness;
    }

    public int getScreenBrightness() {
        return this.mScreenBrightness;
    }

    /** ------------------- 播放器回调 --------------------------- */

    /**
     * 广告视频播放器准备对外接口监听
     */
    public static class VideoPlayerPreparedListener implements IPlayer.OnPreparedListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerPreparedListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onPrepared() {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerPrepared();
            }
        }
    }

    /**
     * 原视频准备完成
     */
    private void sourceVideoPlayerPrepared() {
        //需要将mThumbnailPrepareSuccess重置,否则会出现缩略图错乱的问题
        mThumbnailPrepareSuccess = false;
        if(mThumbnailView != null){
            mThumbnailView.setThumbnailPicture(null);
        }

        if (mAliyunVodPlayer == null) {
            return;
        }
        mAliyunMediaInfo = mAliyunVodPlayer.getMediaInfo();

        if (mAliyunMediaInfo == null) {
            return;
        }
        List<Thumbnail> thumbnailList = mAliyunMediaInfo.getThumbnailList();
        if (thumbnailList != null && thumbnailList.size() > 0) {

            mThumbnailHelper = new ThumbnailHelper(thumbnailList.get(0).mURL);

            mThumbnailHelper.setOnPrepareListener(new ThumbnailHelper.OnPrepareListener() {
                @Override
                public void onPrepareSuccess() {
                    mThumbnailPrepareSuccess = true;
                }

                @Override
                public void onPrepareFail() {
                    mThumbnailPrepareSuccess = false;
                }
            });

            mThumbnailHelper.prepare();

            mThumbnailHelper.setOnThumbnailGetListener(new ThumbnailHelper.OnThumbnailGetListener() {
                @Override
                public void onThumbnailGetSuccess(long l, ThumbnailBitmapInfo thumbnailBitmapInfo) {
                    if (thumbnailBitmapInfo != null && thumbnailBitmapInfo.getThumbnailBitmap() != null) {
                        Bitmap thumbnailBitmap = thumbnailBitmapInfo.getThumbnailBitmap();
                        mThumbnailView.setTime(TimeFormater.formatMs(l));
                        mThumbnailView.setThumbnailPicture(thumbnailBitmap);
                    }
                }

                @Override
                public void onThumbnailGetFail(long l, String s) {

                }
            });
        }

        //防止服务器信息和实际不一致
        mSourceDuration = mAliyunVodPlayer.getDuration();
        mAliyunMediaInfo.setDuration((int) mSourceDuration);
        TrackInfo currentTrack = mAliyunVodPlayer.currentTrack(TrackInfo.Type.TYPE_VOD);
        String currentQuality = "FD";
        if (currentTrack != null){
            currentQuality = currentTrack.getVodDefinition();
        }

        mControlView.setMediaInfo(mAliyunMediaInfo, currentQuality,mAliyunLocalSource);
        mControlView.setHideType(ViewAction.HideType.Normal);
        mGestureView.setHideType(ViewAction.HideType.Normal);
        mGestureView.show();
        if (mTipsView != null) {
            mTipsView.hideNetLoadingTipView();
        }

        mSurfaceView.setVisibility(View.VISIBLE);

        //准备成功之后可以调用start方法开始播放
        if (mOutPreparedListener != null) {
            mOutPreparedListener.onPrepared();
        }
    }

    private static class VideoPlayerErrorListener implements IPlayer.OnErrorListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerErrorListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onError(ErrorInfo errorInfo) {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerError(errorInfo);
            }
        }
    }

    /**
     * 原视频错误监听
     */
    private void sourceVideoPlayerError(ErrorInfo errorInfo) {

        if (mTipsView != null) {
            mTipsView.hideAll();
        }
        //出错之后解锁屏幕，防止不能做其他操作，比如返回。
        lockScreen(false);

        //errorInfo.getExtra()展示为null,修改为显示errorInfo.getCode的十六进制的值
        showErrorTipView(errorInfo.getCode().getValue(), Integer.toHexString(errorInfo.getCode().getValue()), errorInfo.getMsg());

        if (mOutErrorListener != null) {
            mOutErrorListener.onError(errorInfo);
        }
    }

    /**
     * 播放器加载状态监听
     */
    private static class VideoPlayerLoadingStatusListener implements IPlayer.OnLoadingStatusListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerLoadingStatusListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onLoadingBegin() {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerLoadingBegin();
            }
        }

        @Override
        public void onLoadingProgress(int percent, float v) {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerLoadingProgress(percent);
            }
        }

        @Override
        public void onLoadingEnd() {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerLoadingEnd();
            }
        }
    }

    /**
     * 原视频开始加载
     */
    private void sourceVideoPlayerLoadingBegin() {
        if (mTipsView != null) {
            mTipsView.showBufferLoadingTipView();
        }
    }

    /**
     * 原视频开始加载进度
     */
    private void sourceVideoPlayerLoadingProgress(int percent) {

        if (mTipsView != null) {
            //视频广告,并且广告视频在播放状态,不要展示loading
            mTipsView.updateLoadingPercent(percent);

            if (percent == 100) {
                mTipsView.hideBufferLoadingTipView();
            }
        }
    }

    /**
     * 原视频加载结束
     */
    private void sourceVideoPlayerLoadingEnd() {

        if (mTipsView != null) {
            mTipsView.hideBufferLoadingTipView();
        }
        if (isPlaying()) {
            mTipsView.hideErrorTipView();
        }
        hasLoadEnd.put(mAliyunMediaInfo, true);
        vodPlayerLoadEndHandler.sendEmptyMessage(1);
    }

    /**
     * 播放器状态改变监听
     */
    private static class VideoPlayerStateChangedListener implements IPlayer.OnStateChangedListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerStateChangedListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }


        @Override
        public void onStateChanged(int newState) {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerStateChanged(newState);
            }
        }
    }


    /**
     * 原视频状态改变监听
     */
    private void sourceVideoPlayerStateChanged(int newState) {
        mPlayerState = newState;
        if (newState == IPlayer.stopped) {
            if (mOnStoppedListener != null) {
                mOnStoppedListener.onStop();
            }
        } else if (newState == IPlayer.started) {
            if (mControlView != null) {
                mControlView.setPlayState(ControlView.PlayState.Playing);
            }
        }
    }

    /**
     * 播放器播放完成监听
     */
    private static class VideoPlayerCompletionListener implements IPlayer.OnCompletionListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerCompletionListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onCompletion() {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerCompletion();
            }
        }
    }

    /**
     * 原视频播放完成
     */
    private void sourceVideoPlayerCompletion() {
        inSeek = false;
        //如果当前播放资源是本地资源时, 再显示replay
        if (mTipsView != null && isLocalSource()) {
            //隐藏其他的动作,防止点击界面去进行其他操作
            mGestureView.hide(ViewAction.HideType.End);
            mControlView.hide(ViewAction.HideType.End);
            mTipsView.showReplayTipView();
        }
        if (mOutCompletionListener != null) {
            mOutCompletionListener.onCompletion();
        }
    }

    /**
     * 播放器Info监听
     */
    private static class VideoPlayerInfoListener implements IPlayer.OnInfoListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerInfoListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onInfo(InfoBean infoBean) {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerInfo(infoBean);
            }
        }
    }

    /**
     * 原视频Info
     */
    private void sourceVideoPlayerInfo(InfoBean infoBean) {
        if (infoBean.getCode() == InfoCode.AutoPlayStart) {
            //自动播放开始,需要设置播放状态
            if (mControlView != null) {
                mControlView.setPlayState(ControlView.PlayState.Playing);
            }
            if (mOutAutoPlayListener != null) {
                mOutAutoPlayListener.onAutoPlayStarted();
            }
        } else if (infoBean.getCode() == InfoCode.BufferedPosition) {
            //更新bufferedPosition
            mVideoBufferedPosition = infoBean.getExtraValue();
            mControlView.setVideoBufferPosition((int) mVideoBufferedPosition);
        } else if (infoBean.getCode() == InfoCode.CurrentPosition) {
            //更新currentPosition
            mCurrentPosition = infoBean.getExtraValue();
            long min = mCurrentPosition/1000/60;
            long sec = mCurrentPosition/1000%60;
            if (mControlView != null && !inSeek && mPlayerState == IPlayer.started) {
                mControlView.setVideoPosition((int) mCurrentPosition);
            }
        } else if (infoBean.getCode() == InfoCode.AutoPlayStart) {
            //自动播放开始,需要设置播放状态
            if (mControlView != null) {
                mControlView.setPlayState(ControlView.PlayState.Playing);
            }
            if (mOutAutoPlayListener != null) {
                mOutAutoPlayListener.onAutoPlayStarted();
            }
        } else {
            if (mOutInfoListener != null) {
                mOutInfoListener.onInfo(infoBean);
            }
        }
    }

    /**
     * 播放器Render监听
     */
    private static class VideoPlayerRenderingStartListener implements IPlayer.OnRenderingStartListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerRenderingStartListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onRenderingStart() {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerOnVideoRenderingStart();
            }
        }
    }

    /**
     * 原视频onVideoRenderingStart
     */
    private void sourceVideoPlayerOnVideoRenderingStart() {
        mCoverView.setVisibility(GONE);
        if (mOutFirstFrameStartListener != null) {
            mOutFirstFrameStartListener.onRenderingStart();
        }
    }

    /**
     * 播放器TrackChanged监听
     */
    private static class VideoPlayerTrackChangedListener implements IPlayer.OnTrackChangedListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerTrackChangedListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onChangedSuccess(TrackInfo trackInfo) {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerTrackInfoChangedSuccess(trackInfo);
            }
        }

        @Override
        public void onChangedFail(TrackInfo trackInfo, ErrorInfo errorInfo) {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerTrackInfoChangedFail(trackInfo, errorInfo);
            }
        }
    }

    /**
     * 原视频 trackInfoChangedSuccess
     */
    private void sourceVideoPlayerTrackInfoChangedSuccess(TrackInfo trackInfo) {
        //清晰度切换监听
        if (trackInfo.getType() == TrackInfo.Type.TYPE_VOD) {
            //切换成功后就开始播放
            mControlView.setCurrentQuality(trackInfo.getVodDefinition());
            start();

            if (mTipsView != null) {
                mTipsView.hideNetLoadingTipView();
            }
            if (mOutChangeQualityListener != null) {
                mOutChangeQualityListener.onChangeQualitySuccess(TrackInfo.Type.TYPE_VOD.name());
            }
        }
    }

    public PlayerConfig getPlayerConfig() {
        if (mAliyunVodPlayer != null) {
            return mAliyunVodPlayer.getConfig();
        }
        return null;
    }

    public void setPlayerConfig(PlayerConfig playerConfig) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setConfig(playerConfig);
        }
    }

    /**
     * 原视频 trackInfochangedFail
     */
    private void sourceVideoPlayerTrackInfoChangedFail(TrackInfo trackInfo, ErrorInfo errorInfo) {
        //失败的话，停止播放，通知上层
        if (mTipsView != null) {
            mTipsView.hideNetLoadingTipView();
        }
        stop();
        if (mOutChangeQualityListener != null) {
            mOutChangeQualityListener.onChangeQualityFail(0, errorInfo.getMsg());
        }
    }

    /**
     * 播放器seek完成监听
     */
    private static class VideoPlayerOnSeekCompleteListener implements IPlayer.OnSeekCompleteListener {

        private WeakReference<AliyunVodPlayerView> weakReference;

        public VideoPlayerOnSeekCompleteListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }

        @Override
        public void onSeekComplete() {
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerSeekComplete();
            }
        }
    }

    /**
     * 原视频seek完成
     */
    private void sourceVideoPlayerSeekComplete() {
        inSeek = false;

        if (mOuterSeekCompleteListener != null) {
            mOuterSeekCompleteListener.onSeekComplete();
        }
    }
    /**
     * SEI监听
     */
    private static class VideoPlayerOnSeiDataListener implements IPlayer.OnSeiDataListener{
        private WeakReference<AliyunVodPlayerView> weakReference;
        public VideoPlayerOnSeiDataListener(AliyunVodPlayerView aliyunVodPlayerView) {
            weakReference = new WeakReference<>(aliyunVodPlayerView);
        }
        @Override
        public void onSeiData(int var1, byte[] var2){
            AliyunVodPlayerView aliyunVodPlayerView = weakReference.get();
            if (aliyunVodPlayerView != null) {
                aliyunVodPlayerView.sourceVideoPlayerSeiData(var1,var2);
            }
        }
    }
    /**
     * SEI事件出现
     * @param i 类型
     * @param s 内容
     */
    private void sourceVideoPlayerSeiData(int i, byte[] s) {
        if (mOutSeiDataListener != null){
            mOutSeiDataListener.onSeiData(i, s);
        }
    }

    /**
     * ------------------- 播放器回调 end---------------------------
     */
    private void hideSystemUI() {
        AliyunVodPlayerView.this.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
