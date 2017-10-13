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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.view.PageView;
import com.example.xyzreader.util.BookPageFactory;
import com.example.xyzreader.util.NewBookPageFactory;

import java.io.IOException;

/**
 * Created by YGL on 2017/10/4.
 */

public class ReaderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ReaderActivity";
    private PageView mPageView;
    private int screenHeight;
    private int readHeight; // 电子书显示高度
    private int screenWidth;
    private Bitmap mCurPageBitmap, mNextPageBitmap;
    public static Canvas mCurPageCanvas, mNextPageCanvas;
    public static SharedPreferences.Editor editor;
    private static int begin = 0;// 记录的书籍开始位置
    private long mItemId;//文章ID
    private Cursor mCursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        Log.i(TAG,"protected void onCreate");

        hideSystemUI();

        editor=getSharedPreferences("config", MODE_PRIVATE).edit();
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
        Log.i(TAG,"screenWidth:"+screenWidth+"       "+"screenHeight:"+screenHeight);
        readHeight = screenHeight - screenWidth / 320;

        mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);      //android:LargeHeap=true  use in  manifest application
        mNextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
        mCurPageCanvas = new Canvas(mCurPageBitmap);
        mNextPageCanvas = new Canvas(mNextPageBitmap);

        mPageView = new PageView(this, screenWidth, readHeight);
        LinearLayout linearLayout=(LinearLayout)findViewById(R.id.reader_layout);
        linearLayout.addView(mPageView);

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
                        int x = (int) e.getX();
                        int y = (int) e.getY();
                        if (x < screenWidth / 2) {// 向前翻
                        } else if (x >= screenWidth / 2) {// 向后翻
                        }
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
            NewBookPageFactory n=new NewBookPageFactory(getBaseContext(),mCursor.getString(ArticleLoader.Query.BODY),screenWidth,screenHeight,0);
            n.onDraw(mCurPageCanvas);
            mPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);
        }else {
            Log.i(TAG,"mCursor is null");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
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
}
