package com.yt.tvideolibrary.constants;


import java.io.Serializable;

public class UrlSourceData implements Serializable {


    private String url;
    private String title;

    public UrlSourceData(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "UrlSourceData{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
