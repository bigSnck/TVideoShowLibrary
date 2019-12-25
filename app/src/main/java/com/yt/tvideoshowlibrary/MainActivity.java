package com.yt.tvideoshowlibrary;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.yt.tvideolibrary.constants.TPlayer;
import com.yt.tvideolibrary.constants.UrlSourceData;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStartPaly(View view){
        String mUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

        TPlayer.player(this, new UrlSourceData(mUrl, "哈哈哈哈"));
    }
}
