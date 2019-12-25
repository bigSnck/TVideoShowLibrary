package com.yt.tvideolibrary.view.guide;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


import com.yt.tvideolibrary.R;
import com.yt.tvideolibrary.theme.ITheme;
import com.yt.tvideolibrary.listener.AliyunScreenMode;
import com.yt.tvideolibrary.widget.AliyunVodPlayerView;

/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * 手势指导页面。
 * 主要在使用。
 */

public class GuideView extends LinearLayout implements ITheme {

    //三个文字显示
    private TextView mBrightText, mProgressText, mVolumeText;

    public GuideView(Context context) {
        super(context);
        init();
    }

    public GuideView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GuideView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //设置页面布局
        setBackgroundColor(Color.parseColor("#88000000"));
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        LayoutInflater.from(getContext()).inflate(R.layout.alivc_view_guide, this, true);

        //这几个文字有颜色的变化
        mBrightText = (TextView) findViewById(R.id.bright_text);
        mProgressText = (TextView) findViewById(R.id.progress_text);
        mVolumeText = (TextView) findViewById(R.id.volume_text);

        //默认是隐藏的
        hide();
    }

    /**
     * 设置当前屏幕的模式
     *
     * @param mode 全屏，小屏
     */
    public void setScreenMode(AliyunScreenMode mode) {
        if (mode == AliyunScreenMode.Small) {
            //小屏时隐藏
            hide();
            return;
        }
//只有第一次进入全屏的时候显示。通过SharedPreferences记录这个值。
        SharedPreferences spf = getContext().getSharedPreferences("alivc_guide_record", Context.MODE_PRIVATE);
        boolean hasShown = spf.getBoolean("has_shown", false);
        //如果已经显示过了，就不接着走了
        if (hasShown) {
            return;
        } else {
            setVisibility(VISIBLE);
        }
        //记录下来
        SharedPreferences.Editor editor = spf.edit();
        editor.putBoolean("has_shown", true);
        editor.apply();
    }

    /**
     * 隐藏不显示
     */
    public void hide() {
        setVisibility(GONE);
    }

    /**
     * 手势点击到了就隐藏
     *
     * @param event 触摸事件
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hide();
        return true;
    }

    /**
     * 设置主题色
     *
     * @param theme 支持的主题
     */
    @Override
    public void setTheme(AliyunVodPlayerView.Theme theme) {
        int colorRes = R.color.alivc_player_theme_blue;

        if (theme == AliyunVodPlayerView.Theme.Blue) {
            colorRes = R.color.alivc_player_theme_blue;
        } else if (theme == AliyunVodPlayerView.Theme.Green) {
            colorRes = R.color.alivc_player_theme_green;
        } else if (theme == AliyunVodPlayerView.Theme.Orange) {
            colorRes = R.color.alivc_player_theme_orange;
        } else if (theme == AliyunVodPlayerView.Theme.Red) {
            colorRes = R.color.alivc_player_theme_red;
        }

        int color = ContextCompat.getColor(getContext(), colorRes);
        //这三个text的颜色会改变
        mBrightText.setTextColor(color);
        mProgressText.setTextColor(color);
        mVolumeText.setTextColor(color);
    }
}
