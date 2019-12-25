package com.yt.tvideolibrary.theme;


import com.yt.tvideolibrary.widget.AliyunVodPlayerView;

/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * 主题的接口。用于变换UI的主题。
 * 实现类有{@link com.yt.tvideolibrary.view.tipsview.ErrorView}，{@link com.yt.tvideolibrary.view.tipsview.NetChangeView} , {@link com.yt.tvideolibrary.view.tipsview.ReplayView} ,{@link com.yt.tvideolibrary.view.control.ControlView},
 * {@link com.yt.tvideolibrary.view.guide.GuideView} , {@link com.yt.tvideolibrary.view.quality.QualityView}, {@link com.yt.tvideolibrary.view.speed.SpeedView} , {@link com.yt.tvideolibrary.view.tipsview.TipsView},
 * {@link AliyunVodPlayerView}
 */

public interface ITheme {
    /**
     * 设置主题
     * @param theme 支持的主题
     */
    void setTheme(AliyunVodPlayerView.Theme theme);
}
