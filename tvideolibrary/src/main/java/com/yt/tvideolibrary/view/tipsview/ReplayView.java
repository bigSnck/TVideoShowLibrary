package com.yt.tvideolibrary.view.tipsview;

import android.content.Context;
import android.content.res.Resources;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;


import com.yt.tvideolibrary.R;
import com.yt.tvideolibrary.theme.ITheme;
import com.yt.tvideolibrary.widget.AliyunVodPlayerView;

/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * 重播提示对话框。播放结束的时候会显示这个界面
 */
public class ReplayView extends RelativeLayout implements ITheme {
    //重播按钮
    private TextView mReplayBtn;
    //重播事件监听
    private OnReplayClickListener mOnReplayClickListener = null;

    public ReplayView(Context context) {
        super(context);
        init();
    }

    public ReplayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReplayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Resources resources = getContext().getResources();

        View view = inflater.inflate(R.layout.alivc_dialog_replay, null);
        int viewWidth = resources.getDimensionPixelSize(R.dimen.alivc_player_dialog_err_width);
        int viewHeight = resources.getDimensionPixelSize(R.dimen.alivc_player_dialog_err_height);


        LayoutParams params = new LayoutParams(viewWidth, viewHeight);
        addView(view, params);

        //设置监听
        mReplayBtn = (TextView) view.findViewById(R.id.replay);
        mReplayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnReplayClickListener != null) {
                    mOnReplayClickListener.onReplay();
                }
            }
        });

    }

    @Override
    public void setTheme(AliyunVodPlayerView.Theme theme) {
        //更新主题
        if (theme == AliyunVodPlayerView.Theme.Blue) {
            mReplayBtn.setBackgroundResource(R.drawable.alivc_rr_bg_blue);
            mReplayBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.alivc_player_theme_blue));
        } else if (theme == AliyunVodPlayerView.Theme.Green) {
            mReplayBtn.setBackgroundResource(R.drawable.alivc_rr_bg_green);
            mReplayBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.alivc_player_theme_green));
        } else if (theme == AliyunVodPlayerView.Theme.Orange) {
            mReplayBtn.setBackgroundResource(R.drawable.alivc_rr_bg_orange);
            mReplayBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.alivc_player_theme_orange));
        } else if (theme == AliyunVodPlayerView.Theme.Red) {
            mReplayBtn.setBackgroundResource(R.drawable.alivc_rr_bg_red);
            mReplayBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.alivc_player_theme_red));
        }
    }

    /**
     * 重播点击事件
     */
    public interface OnReplayClickListener {
        /**
         * 重播事件
         */
        void onReplay();
    }

    /**
     * 设置重播事件监听
     *
     * @param l 重播事件
     */
    public void setOnReplayClickListener(OnReplayClickListener l) {
        mOnReplayClickListener = l;
    }

}
