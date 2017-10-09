package com.example.xyzreader.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
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
import com.example.xyzreader.ui.view.PageView;
import com.example.xyzreader.util.BookPageFactory;

import java.io.IOException;

/**
 * Created by YGL on 2017/10/4.
 */

public class ReaderActivity extends AppCompatActivity {
    private static final String TAG = "ReaderActivity";
    private PageView mPageView;
    private int screenHeight;
    private int readHeight; // 电子书显示高度
    private int screenWidth;
    private Bitmap mCurPageBitmap, mNextPageBitmap;
    private BookPageFactory pagefactory;
    public static Canvas mCurPageCanvas, mNextPageCanvas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        Log.i(TAG,"protected void onCreate");

        hideSystemUI();

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
        LinearLayout linearLayout=(LinearLayout)findViewById(R.id.reader_layout);
        linearLayout.addView(mPageView);

        pagefactory = new BookPageFactory(screenWidth, readHeight,this);// 书工厂
        //设置背景及文字颜色
        Bitmap bmp = Bitmap.createBitmap(screenWidth,screenHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(getResources().getColor(R.color.read_background_paperYellow));
        pagefactory.setM_textColor(getResources().getColor(R.color.read_textColor));
        pagefactory.setBgBitmap(bmp);
        // 从指定位置打开书籍，默认从开始打开
        try {
            pagefactory.openbook("/storage/emulated/0/perflog.txt",0);//0,书籍打开的位置
            pagefactory.setM_fontSize(30);//字体大小
            pagefactory.onDraw(mCurPageCanvas);
            //word = pagefactory.getFirstTwoLineText();// 获取当前阅读位置的前两行文字,用作书签
        } catch (IOException e) {
            Log.e(TAG, "打开电子书失败", e);
            Toast.makeText(this, "打开电子书失败", Toast.LENGTH_SHORT).show();
        }

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
                        pagefactory.onDraw(mCurPageCanvas);
                        int x = (int) e.getX();
                        int y = (int) e.getY();
                        if (x < screenWidth / 2) {// 从左翻
                            try {
                                pagefactory.prePage();
                                //begin = pagefactory.getM_mbBufBegin();// 获取当前阅读位置
                                //word = pagefactory.getFirstTwoLineText();// 获取当前阅读位置的首行文字
                            } catch (IOException e1) {
                                Log.e(TAG, "onTouch->prePage error", e1);
                            }
                            if (pagefactory.isfirstPage()) {
                                Toast.makeText(getBaseContext(), "当前是第一页", Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            pagefactory.onDraw(mNextPageCanvas);

                        } else if (x >= screenWidth / 2) {// 从右翻
                            try {
                                pagefactory.nextPage();
                                //begin = pagefactory.getM_mbBufBegin();// 获取当前阅读位置
                                //word = pagefactory.getFirstTwoLineText();// 获取当前阅读位置的首行文字
                            } catch (IOException e1) {
                                Log.e(TAG, "onTouch->nextPage error", e1);
                            }
                            if (pagefactory.islastPage()) {
                                Toast.makeText(getBaseContext(), "已经是最后一页了", Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            pagefactory.onDraw(mNextPageCanvas);
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
