package com.yt.tvideolibrary.constants;

import android.content.Context;
import android.content.Intent;

import com.aliyun.player.source.UrlSource;
import com.yt.tvideolibrary.activity.AliyunPlayerSkinActivity;


/**
 * 对外暴露的接口
 */
public class TPlayer {

    /**
     * 跳转到播放器界面
     *
     * @param context   context
     * @param urlSource 跳转到播放器提供的参数配置
     */
    public static void player(Context context, UrlSourceData urlSource) {
        Intent intent = new Intent(context, AliyunPlayerSkinActivity.class);

        intent.putExtra("urlSource",urlSource);
        context.startActivity(intent);
    }
}
