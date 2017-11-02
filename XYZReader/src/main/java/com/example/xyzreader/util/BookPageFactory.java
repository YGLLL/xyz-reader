package com.example.xyzreader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Region;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xyzreader.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YGL on 2017/10/12.
 */
//用于生产给PageWidget显示的Bitmap
public class BookPageFactory {
    private static final String TAG = "ReaderActivity";
    private Context mContext;

    private int screenWidth;
    private int screenHeight;

    private Paint p;

    private String book;
    private List<String> pageString;

    private int textColor;
    private int textSize;
    private Typeface textTypeface;

    private int marginWidth;
    private int marginHeight;
    private int marginLine;
    private int maxLine;
    private int lineWidth;

    private int backColor;

    private int readAddress;

    private DecimalFormat df;

    private Bitmap coverImage;
    private String title;
    private String subTitle;

    public BookPageFactory(Context context,int screenWidth, int screenHeight){
        this.mContext=context;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;
        book="";
        readAddress=0;

        //读取默认配置
        marginWidth=(int) mContext.getResources().getDimension(R.dimen.width_margin);
        marginHeight=(int) mContext.getResources().getDimension(R.dimen.heigth_margin);
        marginLine=(int) mContext.getResources().getDimension(R.dimen.line_margin);
        backColor = mContext.getResources().getColor(R.color.white);
        textColor=mContext.getResources().getColor(R.color.DimGray);
        //设置画笔
        p=new Paint();
        p.setColor(textColor);
        textSize=(int) mContext.getResources().getDimension(R.dimen.reading_default_text_size);
        p.setTextSize(textSize);
        textTypeface=Typeface.createFromAsset(mContext.getAssets(), "font/QH.ttf");
        p.setTypeface(textTypeface);
        //计算屏幕可以容纳文字的行数
        maxLine=(screenHeight-marginHeight*2)/(textSize+marginLine)-1;//使用一行显示进度
        //计算一行的长度
        lineWidth=screenWidth-marginWidth*2;
        df = new DecimalFormat("#0.0");
    }

    public void onDraw(Canvas c) {
        c.drawColor(backColor);

        if(readAddress<0&&(coverImage!=null||title!=null||subTitle!=null)){
            //画封面
            int marginTop=(screenHeight-screenWidth-textSize*3-marginLine*2)/2;
            //画封面图片
            if(coverImage!=null){
                c.drawBitmap(coverImage,0,marginTop,p);
            }else {
                c.drawBitmap(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.empty_detail),0,marginTop,p);
            }
            //画标题
            int lineX=marginWidth;
            int lineY=marginTop+screenWidth+marginLine;
            if(!TextUtils.isEmpty(title)){
                //标题专用画笔
                Paint titlePaint=new Paint();
                titlePaint.setColor(mContext.getResources().getColor(R.color.Black));
                int titleSize=textSize*6/5;
                titlePaint.setTextSize(titleSize);
                titlePaint.setTypeface(textTypeface);
                List<String> list=getLineString(titlePaint,title);
                for (String line: list){
                    c.drawText(line,lineX,lineY,titlePaint);
                    lineY+=titleSize+marginLine;
                }
            }
            //画副标题
            if(!TextUtils.isEmpty(subTitle)){
                List<String> list=getLineString(p,subTitle);
                for (String line: list){
                    c.drawText(line,lineX,lineY,p);
                    lineY+=textSize+marginLine;
                }
            }
        }else {
            //画文字
            pageString=getPageString(readAddress);
            int lineX=marginWidth;
            int lineY=marginHeight+textSize;
            for (String line: pageString){
                c.drawText(line,lineX,lineY,p);
                lineY+=textSize+marginLine;
            }

            //画进度
            float fPercent = (float) (readAddress * 1.0 / book.length());
            String strPercent = df.format(fPercent * 100) + "%";
            c.drawText(strPercent,marginWidth,marginHeight+textSize*(maxLine+1)+marginLine*maxLine, p);
        }
    }

    //读取指定位置一页内容
    private List<String> getPageString(int address){
        List<String> list=new ArrayList<>();
        if(!TextUtils.isEmpty(book)&&address>=0){
            for (int i=0;i<maxLine;i++){
                String word=book.substring(address,
                        (address+oneLineCharacterQuantity(p)*2)>book.length()?book.length():(address+oneLineCharacterQuantity(p)*2));
                int a=p.breakText(word,true,lineWidth,null);//自动折行
                int b=word.indexOf("\n");
                if(b!=-1&&b<=a){
                    //如果有转行符
                    b+=1;
                }else {
                    b=word.lastIndexOf(" ",a);
                    if (b!=-1&&isMultipleString(word.substring(0,a))){
                        //如果有空格
                    }else {
                        //既没有转行符，也没有空格
                        b=a;
                    }
                }
                list.add(word.substring(0,b));
                address+=b;
            }
        }
        return list;
    }
    //将一段字符串分成多行
    private List<String> getLineString(Paint paint,String string){
        List<String> list=new ArrayList<>();
        if(!TextUtils.isEmpty(string)){
            int address=0;
            while (address!=string.length()){
                String word=string.substring(address,
                        (address+oneLineCharacterQuantity(paint)*2)>string.length()?string.length():(address+oneLineCharacterQuantity(paint)*2));
                int a=paint.breakText(word,true,lineWidth,null);//自动折行
                int b=word.indexOf("\n");
                if(b!=-1&&b<=a){
                    //如果有转行符
                    b+=1;
                }else {
                    b=word.lastIndexOf(" ",a);
                    if (b!=-1&&isMultipleString(word.substring(0,a))){
                        //如果有空格
                    }else {
                        //既没有转行符，也没有空格
                        b=a;
                    }
                }
                list.add(word.substring(0,b));
                address+=b;
            }
        }
        return list;
    }

    //翻到上一页
    public void prePage(){
        if (readAddress<=-1){
            showMessage("已经是第一页");
            return;
        }
        readAddress=prePageAddress(readAddress);
    }

    //翻到下一页
    public void nextPage(){
        if (readAddress>=book.length()){
            showMessage("已经是最后一页");
            return;
        }
        readAddress=nextPageAddress(readAddress);
    }

    //得到上一页大概的起始位置
    private int prePageAddress(int address){
        if(address<=0){
            return -1;
        }
        int myAddress=address;
        if (!TextUtils.isEmpty(book)){
            //以下为风骚时刻，请勿轻易模仿
            //使用穷举法求解
            int z=nextPageAddress(myAddress);
            int accuracy=(oneLineCharacterQuantity(p)*maxLine)/4;//设置开始精度
            while (z!=address||z==book.length()){
                if(z<address){
                    myAddress+=accuracy;
                    z=nextPageAddress(myAddress);
                    if(z>address){//增加精度
                        if (accuracy==1){
                            break;
                        }
                        accuracy=accuracy/2;
                    }
                }else {
                    myAddress-=accuracy;
                    z=nextPageAddress(myAddress);
                    if(z<address){//增加精度
                        if (accuracy==1){
                            myAddress+=1;
                            break;
                        }
                        accuracy=accuracy/2;
                    }
                }
                Log.i("performanceLog","myAddress:"+myAddress);
            }
            Log.i("performanceLog","最终myAddress="+myAddress);
        }
        if(myAddress<0){
            return 0;
        }else {
            return myAddress;
        }
    }

    //得到下一页的起始位置
    private int nextPageAddress(int address){
        if(address<0){
            return 0;
        }
        if(!TextUtils.isEmpty(book)){
            for (int i=0;i<maxLine;i++){
                //计算每一行的长度
                String word=book.substring(address,
                        (address+oneLineCharacterQuantity(p)*2)>book.length()?book.length():(address+oneLineCharacterQuantity(p)*2));
                int a=p.breakText(word,true,lineWidth,null);
                int b=word.indexOf("\n");
                if(b!=-1&&b<=a){
                    //如果有转行符
                    b=b+1;
                }else {
                    b=word.lastIndexOf(" ",a);
                    if (b!=-1&&isMultipleString(word.substring(0,a))){
                        //如果有空格
                    }else {
                        //既没有转行符，也没有空格
                        b=a;
                    }
                }
                //加上每一行的长度
                address+=b;
            }
        }
        return address;
    }

    //验证此行是否至少包涵两段字
    private Boolean isMultipleString(String string){
        Boolean firstString=false;
        Boolean intermediateSpace=false;
        Boolean secondString=false;
        for (int i=0;i<string.length();i++){
            if(!string.substring(i,i+1).equals(" ")&&!intermediateSpace&&!secondString){
                firstString=true;
            }
            if (firstString&&string.substring(i,i+1).equals(" ")&&!secondString){
                intermediateSpace=true;
            }
            if (firstString&&intermediateSpace&&!string.substring(i,i+1).equals(" ")){
                secondString=true;
            }
        }
        return firstString&&intermediateSpace&&secondString;
    }

    //计算一行大概能容纳的字符串数
    private int oneLineCharacterQuantity(Paint paint){
        String testString="aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ";
        return paint.breakText(testString,true,lineWidth,null);
    }

    private void showMessage(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public int getReadAddress() {
        return readAddress;
    }
    public int getBookLength(){return book.length();}

    //以下为设置方法
    public void setBook(String book){
        setBook(book,-1);
    }
    public void setBook(String book,int readAddress){
        setBook(book,readAddress,null,null,null);
    }
    public void setBook(String book,int readAddress,Bitmap coverImage,String title,String subTitle){
        this.book=book;
        this.readAddress=readAddress;
        this.coverImage=coverImage;
        this.title=title;
        this.subTitle=subTitle;
    }

    public void setMargin(int marginX,int marginY){
        this.marginWidth=marginX;
        this.marginHeight=marginY;
    }

    public void setBackColor(int backColor){
        this.backColor=backColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setTextTypeface(Typeface textTypeface) {
        this.textTypeface = textTypeface;
    }

    public void setReadAddress(int readAddress) {
        this.readAddress = readAddress;
    }

    public void setCoverImage(Bitmap coverImage) {
        this.coverImage = coverImage;
    }
}
