package com.example.xyzreader.ui;

import android.content.Context;
import android.icu.util.Measure;
import android.util.AttributeSet;
import android.util.Log;

import com.android.volley.toolbox.NetworkImageView;

public class DynamicHeightNetworkImageView extends NetworkImageView {
    //private float mAspectRatio = 1.5f;

    public DynamicHeightNetworkImageView(Context context) {
        super(context);
    }

    public DynamicHeightNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicHeightNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //public void setAspectRatio(float aspectRatio) {
    //    mAspectRatio = aspectRatio;
    //    requestLayout();
    //}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int measuredWidth=getMeasuredWidth();
        int measuredHeight;
        if(getDrawable()!=null){
            int drawableWidth = getDrawable().getIntrinsicWidth();
            int drawableHeight=getDrawable().getIntrinsicHeight();
            measuredHeight=(measuredWidth*drawableHeight)/drawableWidth;
        }else {
            //3:2
            measuredHeight=measuredWidth*2/3;
        }
        setMeasuredDimension(measuredWidth,measuredHeight);
    }
}
