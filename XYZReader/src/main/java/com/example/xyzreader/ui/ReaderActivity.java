package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.view.PageView;
import com.example.xyzreader.util.BookPageFactory;

import java.io.IOException;

/**
 * Created by YGL on 2017/10/4.
 */

public class ReaderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ReaderActivity";
    private PageView mPageView;
    private int screenHeight;
    private int readHeight; // 电子书显示高度
    private int screenWidth;
    private Bitmap mCurPageBitmap, mNextPageBitmap;
    public static Canvas mCurPageCanvas, mNextPageCanvas;
    private long mItemId;//文章ID
    private Cursor mCursor;
    private BookPageFactory bookPageFactory;
    private LinearLayout control;
    private SeekBar seekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_content);
        hideSystemUI();

        if (savedInstanceState == null) {//为什么要判断这个
            if (getIntent() != null && getIntent().getData() != null) {
                mItemId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }
        getLoaderManager().initLoader(0, null, this);

        //获取屏幕宽高
        WindowManager manage = getWindowManager();
        Display display = manage.getDefaultDisplay();
        Point displaysize = new Point();
        display.getSize(displaysize);
        screenWidth = displaysize.x;
        screenHeight = displaysize.y;
        readHeight = screenHeight - screenWidth / 320;

        mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);      //android:LargeHeap=true  use in  manifest application
        mNextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
        mCurPageCanvas = new Canvas(mCurPageBitmap);
        mNextPageCanvas = new Canvas(mNextPageBitmap);

        mPageView = new PageView(this, screenWidth, readHeight);
        FrameLayout contentLayout=(FrameLayout) findViewById(R.id.content_layout);
        seekBar=(SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        control=(LinearLayout)findViewById(R.id.control);
        control.setVisibility(View.GONE);
        contentLayout.addView(mPageView);

        bookPageFactory=new BookPageFactory(getBaseContext(),screenWidth,screenHeight);
        bookPageFactory.onDraw(mCurPageCanvas);
        //设置翻页过场动画
        mPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);
        mPageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                boolean ret = false;
                if (v == mPageView) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        mPageView.abortAnimation();
                        mPageView.calcCornerXY(e.getX(), e.getY());
                        bookPageFactory.onDraw(mCurPageCanvas);

                        int x = (int) e.getX();
                        int y = (int) e.getY();
                        if(x>screenWidth/3&&x<screenWidth*2/3){//显示控制菜单
                            if(control.getVisibility()==View.GONE){
                                control.setVisibility(View.VISIBLE);
                                seekBar.setProgress((int)((bookPageFactory.getReadAddress()*100.0)/bookPageFactory.getBookLength()));
                            }else {
                                control.setVisibility(View.GONE);
                            }
                            return false;
                        }
                        if (x < screenWidth / 2) {// 向前翻
                            bookPageFactory.prePage();
                        } else if (x >= screenWidth / 2) {// 向后翻
                            bookPageFactory.nextPage();
                        }
                        bookPageFactory.onDraw(mNextPageCanvas);
                        //设置翻页过场动画
                        mPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);
                    }
                    //输入触控坐标，开始绘制过场动画
                    ret = mPageView.doTouchEvent(e);
                    return ret;
                }
                return false;
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getBaseContext(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        if(mCursor!=null){
            Log.i(TAG,"mCursor:"+mCursor.getString(ArticleLoader.Query.BODY));
            bookPageFactory.setBook(mCursor.getString(ArticleLoader.Query.BODY),getReadAddress(mCursor.getString(ArticleLoader.Query.TITLE)));
            bookPageFactory.onDraw(mCurPageCanvas);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }

    private int getReadAddress(String title){
        SharedPreferences sharedPreferences=getSharedPreferences("ReadAddress",MODE_PRIVATE);
        return sharedPreferences.getInt(title,0);
    }
    private void setReadAddress(String title,int readAddress){
        SharedPreferences.Editor editor=getSharedPreferences("ReadAddress",MODE_PRIVATE).edit();
        editor.putInt(title,readAddress);
        editor.commit();
    }

    @Override
    protected void onResume(){
        super.onResume();
        hideSystemUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor!=null){
            setReadAddress(mCursor.getString(ArticleLoader.Query.TITLE),bookPageFactory.getReadAddress());
        }
    }

    /**
     * 隐藏菜单。沉浸式阅读
     */
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void flipPage(int address){
        mPageView.abortAnimation();
        MotionEvent e;
        if(address<bookPageFactory.getReadAddress()){
            //向前翻
            mPageView.calcCornerXY(10, screenHeight-10);
            e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, screenWidth-20, screenHeight-20, 1);
        }else {
            //向后翻
            mPageView.calcCornerXY(screenWidth-10, screenHeight-10);
            e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 20, screenHeight-20, 1);
        }
        bookPageFactory.setReadAddress(address);
        bookPageFactory.onDraw(mNextPageCanvas);
        mPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);
        mPageView.doTouchEvent(e);
    }

    int address=0;
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.i(TAG,"onProgressChanged progress"+progress);
        address=bookPageFactory.getBookLength()*progress/100;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG,"onStopTrackingTouch");
        flipPage(address);
    }
}
