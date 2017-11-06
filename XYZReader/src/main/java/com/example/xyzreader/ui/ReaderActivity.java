package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Intent;
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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.remote.Config;
import com.example.xyzreader.ui.view.PageView;
import com.example.xyzreader.util.BookPageFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

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
    private FrameLayout control;
    //private ImageButton exit;
    private SeekBar seekBar;
    private FloatingActionButton share;
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_content);
        hideSystemUI();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕常亮

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
        contentLayout.addView(mPageView);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        seekBar=(SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        control=(FrameLayout)findViewById(R.id.control);
        control.setVisibility(View.GONE);
        share =(FloatingActionButton)findViewById(R.id.share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, Config.APP_LOCATION);
                Intent chooserIntent=Intent.createChooser(intent,getResources().getString(R.string.share_title));
                if (chooserIntent == null) {
                    return;
                }
                try {
                    startActivity(chooserIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getBaseContext(), "Can't find share component to share", Toast.LENGTH_SHORT).show();
                }
            }
        });
        /*
        exit=(ImageButton)findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        */

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
                        if(showOrHideControl(x,y)){//控制控制菜单
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

    private Boolean showOrHideControl(int x,int y){
        if(control.getVisibility()==View.GONE){
            if((x>screenWidth/3&&x<screenWidth*2/3)&&(y>screenHeight/3&&y<screenHeight*2/3)){
                showSystemUI();
                control.setVisibility(View.VISIBLE);
                seekBar.setProgress((int)((bookPageFactory.getReadAddress()*100.0)/bookPageFactory.getBookLength()));
            }else {
                return false;
            }
        }else {
            hideSystemUI();
            control.setVisibility(View.GONE);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
            String publishedDate=getPublishedDate(mCursor);
            bookPageFactory.setBook(
                    mCursor.getString(ArticleLoader.Query.BODY),
                    getReadAddress(mCursor.getString(ArticleLoader.Query.TITLE)),
                    null,
                    mCursor.getString(ArticleLoader.Query.TITLE),
                    publishedDate);
            ImageLoaderHelper.getInstance(getBaseContext()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                bookPageFactory.setCoverImage(bitmap);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
            bookPageFactory.onDraw(mCurPageCanvas);
        }
    }

    private String getPublishedDate(Cursor cursor){
        Date publishedDate = parsePublishedDate(cursor);
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            return DateUtils.getRelativeTimeSpanString(publishedDate.getTime(), System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString()
                    + " by "
                    + mCursor.getString(ArticleLoader.Query.AUTHOR);
        } else {
            // If date is before 1902, just show the string
            return new SimpleDateFormat().format(publishedDate)
                    + " by "
                    + mCursor.getString(ArticleLoader.Query.AUTHOR);
        }
    }
    private Date parsePublishedDate(Cursor c) {
        try {
            String date = c.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss").parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }

    private int getReadAddress(String title){
        SharedPreferences sharedPreferences=getSharedPreferences("ReadAddress",MODE_PRIVATE);
        return sharedPreferences.getInt(title,-1);
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
    protected void onStart(){
        super.onStart();
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
            e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 20, screenHeight-20, 1);
        }else {
            //向后翻
            mPageView.calcCornerXY(screenWidth-10, screenHeight-10);
            e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, screenWidth-20, screenHeight-20, 1);
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
        address=(bookPageFactory.getBookLength()*progress/100)-1;//-1是为了取得-1，-1是封面位置
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
