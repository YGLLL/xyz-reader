package com.example.xyzreader.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

import com.example.xyzreader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YGL on 2017/10/12.
 */

public class NewBookPageFactory {
    //用于生产给PageWidget显示的Bitmap
    private static final String TAG = "ReaderActivity";
    private Context mContext;

    private int screenWidth;
    private int screenHeight;

    private Paint p;

    private String book;
    private List<String> pageString;

    private int textSize;

    private int marginWidth;
    private int marginHeight;
    private int marginLine;

    private int backColor = 0xffff9e85; // 背景颜色

    private int readAddress;

    public NewBookPageFactory(Context context,String book,int screenWidth, int screenHeight,int readAddress){
        this.mContext=context;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;
        this.book=book;
        this.readAddress=readAddress;

        //设置画笔
        p=new Paint();
        p.setColor(Color.rgb(255,255,255));
        textSize=(int) mContext.getResources().getDimension(R.dimen.reading_default_text_size);
        p.setTextSize(textSize);
        p.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "font/QH.ttf"));

        //读取默认配置
        marginWidth=(int) mContext.getResources().getDimension(R.dimen.width_margin);
        marginHeight=(int) mContext.getResources().getDimension(R.dimen.heigth_margin);
        marginLine=(int) mContext.getResources().getDimension(R.dimen.line_margin);
    }

    public void onDraw(Canvas c) {
        c.drawColor(backColor);
        pageString=getPageString(readAddress);
        Log.i(TAG,"pageString Size:"+pageString.size());
        int lineX=marginWidth;
        int lineY=marginHeight+textSize;
        for (String line: pageString){
            Log.i(TAG,"the line:"+line);
            c.drawText(line,lineX,lineY,p);
            lineY+=textSize+marginLine;
        }
    }

    //读取指定位置一页内容
    private List<String> getPageString(int address){
        List<String> list=new ArrayList<>();
        //计算屏幕可以容纳的行数
        int maxLine=(screenHeight-marginHeight*2)/(textSize+marginLine);
        //计算一行的长度
        int lineWidth=screenWidth-marginWidth*2;

        if(book!=null){
            String word;
            if((book.length()-address)>=2000){
                word=book.substring(address,address+2000);//只截取2001个字符，以提高性能
            }else {
                word=book.substring(address);
            }
            int wordAddress=0;
            for (int i=0;i<maxLine;i++){
                word=word.substring(wordAddress);
                int a=p.breakText(word,true,lineWidth,null);
                String line;
                int b=word.indexOf("\n");
                if(b!=-1&&b<=a){
                    //如果有转行符
                    b=b+1;
                    line=word.substring(0,b);
                    Log.i(TAG,"有转行符");
                }else {
                    b=word.lastIndexOf(" ",a);
                    if (b!=-1){
                        //如果有空格
                        line=word.substring(0,b);
                        Log.i(TAG,"有空格");
                    }else {
                        //既没有转行符，也没有空格
                        b=a;
                        line=word.substring(0,a);
                        Log.i(TAG,"既没有转行符，也没有空格");
                    }
                }
                Log.i(TAG,"a:"+a+"    b:"+b+"   wordAddress"+wordAddress);
                list.add(line);
                wordAddress=b;
            }
        }
        return list;
    }

    //翻到上一页
    public void prePage(){}

    //翻到下一页
    public void nextPage(){}

    public void setMargin(int marginX,int marginY){
        this.marginWidth=marginX;
        this.marginHeight=marginY;
    }

    public void setBackColor(int backColor){
        this.backColor=backColor;
    }
}
