package com.yt.tvideolibrary.view.function;

import android.content.Context;

import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorInfo;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.source.UrlSource;
import com.aliyun.player.source.VidAuth;
import com.aliyun.player.source.VidMps;
import com.aliyun.player.source.VidSts;
import com.aliyun.utils.VcPlayerLog;

/**
 * 视频广告View
 * @author hanyu
 */
public class AdvVideoView extends RelativeLayout {

    private static final String TAG = AdvVideoView.class.getSimpleName();

    //广告播放器的surfaceView
    private SurfaceView mAdvSurfaceView;
    //用于播放视频广告的播放器
    private AliPlayer mAdvVideoAliyunVodPlayer;

    //对外info改变监听
    private IPlayer.OnInfoListener mOutOnInfoListener;
    //对外错误监听
    private IPlayer.OnErrorListener mOutOnErrorListener;
    //对外播放完成监听
    private IPlayer.OnCompletionListener mOutOnCompletionListener;
    //对外loading状态监听
    private IPlayer.OnLoadingStatusListener mOutOnLoadingStatusListener;
    //对外renderingStart监听
    private IPlayer.OnRenderingStartListener mOutOnRenderingStartListener;
    //状态改变监听
    private IPlayer.OnStateChangedListener mOutOnStateChangedListener;
    //对外准备完成监听
    private IPlayer.OnPreparedListener mOutPreparedListener;
    //播放器的状态
    private int mPlayerState = -1;


    public AdvVideoView(Context context) {
        super(context);
        init();
    }

    public AdvVideoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdvVideoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        initSurfaceView();
        initAdvPlayer();
    }

    private void initSurfaceView(){
        mAdvSurfaceView = new SurfaceView(getContext().getApplicationContext());
        addSubView(mAdvSurfaceView);
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

    public SurfaceView getAdvSurfaceView(){
        return mAdvSurfaceView;
    }

    public void setSurfaceViewVisibility(int visibility){
        mAdvSurfaceView.setVisibility(visibility);
    }

    /**
     * 初始化视频广告
     */
    private void initAdvPlayer() {
        SurfaceHolder holder = mAdvSurfaceView.getHolder();
        //增加surfaceView的监听
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                VcPlayerLog.d(TAG, " surfaceCreated = surfaceHolder = " + surfaceHolder);
                if (mAdvVideoAliyunVodPlayer != null) {
                    mAdvVideoAliyunVodPlayer.setDisplay(surfaceHolder);
                    //防止黑屏
                    mAdvVideoAliyunVodPlayer.redraw();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width,
                                       int height) {
                VcPlayerLog.d(TAG,
                        " surfaceChanged surfaceHolder = " + surfaceHolder + " ,  width = " + width + " , height = " + height);
                if (mAdvVideoAliyunVodPlayer != null) {
                    mAdvVideoAliyunVodPlayer.redraw();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                VcPlayerLog.d(TAG, " surfaceDestroyed = surfaceHolder = " + surfaceHolder);
                if (mAdvVideoAliyunVodPlayer != null) {
                    mAdvVideoAliyunVodPlayer.setDisplay(null);
                }
            }
        });

        //该播放器用于播放器视频广告
        mAdvVideoAliyunVodPlayer = AliPlayerFactory.createAliPlayer(getContext());
        mAdvVideoAliyunVodPlayer.setAutoPlay(true);
        //设置准备回调
        mAdvVideoAliyunVodPlayer.setOnPreparedListener(new IPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                if(mOutPreparedListener != null){
                    mOutPreparedListener.onPrepared();
                }
            }
        });

        //播放器出错监听
        mAdvVideoAliyunVodPlayer.setOnErrorListener(new IPlayer.OnErrorListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                if(mOutOnErrorListener != null){
                    mOutOnErrorListener.onError(errorInfo);
                }
            }
        });

        //播放器加载回调
        mAdvVideoAliyunVodPlayer.setOnLoadingStatusListener(new IPlayer.OnLoadingStatusListener() {
            @Override
            public void onLoadingBegin() {
                if(mOutOnLoadingStatusListener != null){
                    mOutOnLoadingStatusListener.onLoadingBegin();
                }
            }

            @Override
            public void onLoadingProgress(int percent, float speed) {
                if(mOutOnLoadingStatusListener != null){
                    mOutOnLoadingStatusListener.onLoadingProgress(percent,speed);
                }
            }

            @Override
            public void onLoadingEnd() {
                if(mOutOnLoadingStatusListener != null){
                    mOutOnLoadingStatusListener.onLoadingEnd();
                }
            }
        });

        //播放结束
        mAdvVideoAliyunVodPlayer.setOnCompletionListener(new IPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                if(mOutOnCompletionListener != null){
                    mOutOnCompletionListener.onCompletion();
                }
            }
        });

        //播放器状态监听
//        mAdvVideoAliyunVodPlayer.setOnStateChangedListener(new IPlayer.OnStateChangedListener() {
//            @Override
//            public void onStateChanged(int newState) {
//                //暂停状态
//                if (newState == IPlayer.stopped) {
//                    if (mOnStoppedListener != null) {
//                        mOnStoppedListener.onStop();
//                    }
//                }
//            }
//        });

        //播放信息监听
        mAdvVideoAliyunVodPlayer.setOnInfoListener(new IPlayer.OnInfoListener() {
            @Override
            public void onInfo(InfoBean infoBean) {
                if(mOutOnInfoListener != null){
                    mOutOnInfoListener.onInfo(infoBean);
                }
            }
        });

        //第一帧显示
        mAdvVideoAliyunVodPlayer.setOnRenderingStartListener(new IPlayer.OnRenderingStartListener() {
            @Override
            public void onRenderingStart() {
                if(mOutOnRenderingStartListener != null){
                    mOutOnRenderingStartListener.onRenderingStart();
                }
            }
        });

        //状态改变监听
        mAdvVideoAliyunVodPlayer.setOnStateChangedListener(new IPlayer.OnStateChangedListener() {
            @Override
            public void onStateChanged(int newState) {
                mPlayerState = newState;
                if(mOutOnStateChangedListener != null){
                    mOutOnStateChangedListener.onStateChanged(newState);
                }
            }
        });

        //url过期监听
//        mAdvVideoAliyunVodPlayer.setOnUrlTimeExpiredListener(new IAliyunVodPlayer.OnUrlTimeExpiredListener() {
//            @Override
//            public void onUrlTimeExpired(String vid, String quality) {
//                if (mOutUrlTimeExpiredListener != null) {
//                    mOutUrlTimeExpiredListener.onUrlTimeExpired(vid, quality);
//                }
//            }
//        });
//
//        //请求源过期信息
//        mAdvVideoAliyunVodPlayer.setOnTimeExpiredErrorListener(new IAliyunVodPlayer.OnTimeExpiredErrorListener() {
//            @Override
//            public void onTimeExpiredError() {
//                if (mTipsView != null) {
//                    mTipsView.hideAll();
//                    mTipsView.showErrorTipViewWithoutCode("鉴权过期");
//                    mTipsView.setOnTipClickListener(new TipsView.OnTipClickListener() {
//                        @Override
//                        public void onContinuePlay() {
//                        }
//
//                        @Override
//                        public void onStopPlay() {
//                        }
//
//                        @Override
//                        public void onRetryPlay() {
//                            if (mOutTimeExpiredErrorListener != null) {
//                                mOutTimeExpiredErrorListener.onTimeExpiredError();
//                            }
//                        }
//
//                        @Override
//                        public void onReplay() {
//                        }
//                    });
//                }
//            }
//        });

        mAdvVideoAliyunVodPlayer.setDisplay(mAdvSurfaceView.getHolder());
    }

    /**
     * 设置vidSts
     */
    public void optionSetVidSts(VidSts vidSts){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.setDataSource(vidSts);
        }
    }

    /**
     * 设置vidSuth
     */
    public void optionSetVidAuth(VidAuth vidAuth){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.setDataSource(vidAuth);
        }
    }

    /**
     * 设置urlSource
     */
    public void optionSetUrlSource(UrlSource urlSource){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.setDataSource(urlSource);
        }
    }

    /**
     * 设置vidMps
     */
    public void optionSetVidMps(VidMps vidMps){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.setDataSource(vidMps);
        }
    }

    /**
     * prepared操作
     */
    public void optionPrepare(){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.prepare();
        }
    }

    /**
     * 开始操作
     */
    public void optionStart(){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.start();
        }
    }

    /**
     * 暂停操作
     */
    public void optionPause(){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.pause();
        }
    }

    /**
     * 停止操作
     */
    public void optionStop(){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.stop();
        }
    }

    //获取视频广告的播放器
    public AliPlayer getAdvVideoAliyunVodPlayer(){
        return mAdvVideoAliyunVodPlayer;
    }

    /**
     * 设置prepared监听
     */
    public void setOutPreparedListener(IPlayer.OnPreparedListener outPreparedListener){
        this.mOutPreparedListener = outPreparedListener;
    }

    /**
     * 设置onLoading状态监听
     */
    public void setOutOnLoadingStatusListener(IPlayer.OnLoadingStatusListener onLoadingStatusListener){
        this.mOutOnLoadingStatusListener = onLoadingStatusListener;
    }

    /**
     * 设置播放完成监听
     */
    public void setOutOnCompletionListener(IPlayer.OnCompletionListener onCompletionListener){
        this.mOutOnCompletionListener = onCompletionListener;
    }

    /**
     * 设置对外info改变监听
     */
    public void setOutOnInfoListener(IPlayer.OnInfoListener onInfoListener){
        this.mOutOnInfoListener = onInfoListener;
    }

    /**
     * 设置对外 renderingStart 监听
     */
    public void setOutOnRenderingStartListener(IPlayer.OnRenderingStartListener onRenderingStartListener){
        this.mOutOnRenderingStartListener = onRenderingStartListener;
    }

    /**
     * 设置对外错误监听
     */
    public void setOutOnErrorListener(IPlayer.OnErrorListener onErrorListener){
        this.mOutOnErrorListener = onErrorListener;
    }

    //状态改变监听
    public void setOutOnStateChangedListener(IPlayer.OnStateChangedListener listener){
        this.mOutOnStateChangedListener = listener;
    }

    public void setAutoPlay(boolean autoPlay){
        if(mAdvVideoAliyunVodPlayer != null){
            mAdvVideoAliyunVodPlayer.setAutoPlay(autoPlay);
        }
    }

    /**
     * 获取视频广告播放器的状态
     */
    public int getAdvPlayerState(){
        return mPlayerState;
    }

    public enum VideoState{
        /**
         * 广告
         */
        VIDEO_ADV,
        /**
         * 原视频
         */
        VIDEO_SOURCE;
    }

    /**
     * 将要播放的视频
     */
    public enum IntentPlayVideo{
        /**
         * 先播放中间广告,播放完成后再播放最后一条广告
         */
        MIDDLE_END_ADV_SEEK,
        /**
         * 播放中间广告,并且播放完成需要seek
         */
        MIDDLE_ADV_SEEK,
        /**
         * 播放开始广告
         */
        START_ADV,
        /**
         * 播放中间广告
         */
        MIDDLE_ADV,
        /**
         * 播放结束广告
         */
        END_ADV,
        /**
         * 原视频左seek
         */
        REVERSE_SOURCE,
        /**
         * 正常seek
         */
        NORMAL;
    }
}
