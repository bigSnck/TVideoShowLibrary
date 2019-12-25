package com.yt.tvideolibrary.view.tipsview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yt.tvideolibrary.R;


/**
 * 试看
 * @author hanyu
 */
public class TrailersView extends RelativeLayout {

    private TextView mTipsTextView;
    private View view;
    private FrameLayout mTrailersMask;

    public TrailersView(Context context) {
        super(context);
        init();
    }

    public TrailersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrailersView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        LayoutInflater inflater = (LayoutInflater) getContext().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.alivc_trailers_view_layout,this,true);


        initView();
    }

    private void initView(){
        mTipsTextView = view.findViewById(R.id.tv_tips);
        mTrailersMask = view.findViewById(R.id.trailers_mask);
    }

    public void setContentText(String text){
        if(mTipsTextView != null){
            mTipsTextView.setText(text);
        }
    }

    /**
     * 超过了试看时长
     */
    public void setCurrentProgress(boolean overTrailerTime) {
        if(overTrailerTime){
            setVisibility(View.VISIBLE);
            mTrailersMask.setVisibility(View.VISIBLE);
            mTipsTextView.setVisibility(View.VISIBLE);
            mTipsTextView.setText(getContext().getString(R.string.alivc_tips_trailer_end));
        }else{
            if(mTrailersMask.isShown()){
                mTrailersMask.setVisibility(View.INVISIBLE);
            }
            if(mTipsTextView.isShown()){
                mTipsTextView.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     *  隐藏所有
     */
    public void hideAll(){
        mTrailersMask.setVisibility(View.INVISIBLE);
        mTipsTextView.setVisibility(View.GONE);
    }

    /**
     * 开始试看
     */
    public void startTrailer() {
        if(mTrailersMask != null){
            mTrailersMask.setVisibility(View.INVISIBLE);
        }
        if(mTipsTextView != null){
            mTipsTextView.setVisibility(View.VISIBLE);
            mTipsTextView.setText(getContext().getString(R.string.alivc_tips_trailer));
        }
    }

    /**
     * 试看结束
     */
    public void endTrailer(){
        if(mTrailersMask != null){
            mTrailersMask.setVisibility(View.VISIBLE);
        }
        if(mTipsTextView != null){
            mTipsTextView.setVisibility(View.VISIBLE);
            mTipsTextView.setText(getContext().getString(R.string.alivc_tips_trailer_end));
        }
    }

}
