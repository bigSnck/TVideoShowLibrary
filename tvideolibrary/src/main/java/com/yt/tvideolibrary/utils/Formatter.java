package com.yt.tvideolibrary.utils;

import android.text.TextUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * @Author: lifujun@alibaba-inc.com
 * @Date: 2016/12/29.
 * @Description:
 */

public class Formatter {

    public static String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60 % 60;
        int hours = totalSeconds / 60 / 60;
        String timeStr = "";
        if (hours > 9) {
            timeStr += hours + ":";
        } else if (hours > 0) {
            timeStr += "0" + hours + ":";
        }
        if (minutes > 9) {
            timeStr += minutes + ":";
        } else if (minutes > 0) {
            timeStr += "0" + minutes + ":";
        } else {
            timeStr += "00:";
        }
        if (seconds > 9) {
            timeStr += seconds;
        } else if (seconds > 0) {
            timeStr += "0" + seconds;
        } else {
            timeStr += "00";
        }

        return timeStr;
    }


    public static String formatDate(long seconds) {
        String finalStr = "";
        long mills = seconds * 1000;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mills);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        finalStr += (hour < 10 ? "0" + hour : hour) + ":";
        int minute = calendar.get(Calendar.MINUTE);
        finalStr += (minute < 10 ? "0" + minute : minute) + ":";
        int second = calendar.get(Calendar.SECOND);
        finalStr += (second < 10 ? "0" + second : second);

        return finalStr;

    }
    /**
     * double类型转换为日期  mm:ss
     * @param time
     * @return
     */
    public static String double2Date(double time) {
        long lTime = new Double(time).longValue();
        lTime = lTime - 28800;
        String s = formatDate(lTime);
        String substring = s.substring(3);
        return  substring;

    }

    /**
     * 把 00:00:00 格式转成时间戳
     * @param formatTime    00:00:00 时间格式
     * @return 时间戳(毫秒)
     */
    public static int getIntTime(String formatTime) {
        if (TextUtils.isEmpty(formatTime)) {
            return 0;
        }

        String[] tmp = formatTime.split(":");
        if (tmp.length < 3) {
            return 0;
        }
        int second = Integer.valueOf(tmp[0]) * 3600 + Integer.valueOf(tmp[1]) * 60 + Integer.valueOf(tmp[2]);

        return second * 1000;
    }

    /**
     * 把时间戳转换成 00:00:00 格式
     * @param timeMs    时间戳
     * @return 00:00:00 时间格式
     */
    public static String getStringTime(int timeMs) {
        StringBuilder formatBuilder = new StringBuilder();
        java.util.Formatter formatter = new java.util.Formatter(formatBuilder, Locale.getDefault());

        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        formatBuilder.setLength(0);
        return formatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString();
    }
}
