package com.example.xyzreader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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
    private Paint percentPaint;
    private Paint coverPaint;

    private String book;
    private List<String> pageString;

    private int textColor;
    private int textSize;
    private Typeface textTypeface;

    private int marginWidth;
    private int marginLine;
    private int maxLine;
    private int lineWidth;
    private int percentSize;

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
        marginLine=(int) mContext.getResources().getDimension(R.dimen.line_margin);
        backColor = mContext.getResources().getColor(R.color.reader_back);
        textColor=mContext.getResources().getColor(R.color.reader_body);
        percentSize=(int)mContext.getResources().getDimension(R.dimen.percent_size);
        //设置画笔
        p=new Paint();
        p.setColor(textColor);
        textSize=(int) mContext.getResources().getDimension(R.dimen.reading_body_size);
        p.setTextSize(textSize);
        textTypeface=Typeface.createFromAsset(mContext.getAssets(), "9t.ttf");
        p.setTypeface(textTypeface);
        coverPaint=new Paint();
        percentPaint=new Paint();
        percentPaint.setTextSize(percentSize);
        percentPaint.setColor(textColor);
        //计算屏幕可以显示文字的行数
        maxLine=screenHeight/(marginLine+textSize)-1;//减去的一行用于显示阅读进度
        //计算一行的长度
        lineWidth=screenWidth-marginWidth*2;

        df = new DecimalFormat("#0.0");
    }

    public void onDraw(Canvas c) {
        c.drawColor(backColor);

        if(readAddress<0){
            //画封面
            //设置封面专用画笔
            coverPaint.setColor(mContext.getResources().getColor(R.color.Black));
            int titleSize=textSize*6/5;
            coverPaint.setTextSize(titleSize);
            coverPaint.setTypeface(textTypeface);
            int marginTop=(screenHeight-(screenWidth+titleSize*2+textSize*2+marginLine*4))/2;
            //画封面图片
            if(coverImage==null){
                //设置默认图片
                coverImage=BitmapFactory.decodeResource(mContext.getResources(),R.drawable.empty_detail);
            }
            int iWidth=coverImage.getWidth();
            int iHeight=coverImage.getHeight();
            Rect r=new Rect();
            Rect rr=new Rect();
            if(iWidth>=iHeight){
                //横向图片
                r.set((iWidth-iHeight)/2,0,iHeight+(iWidth-iHeight)/2,iHeight);
                rr.set(0,marginTop,screenWidth,screenWidth+marginTop);
            }else {
                //纵向图片
                if((iHeight/iWidth)>(screenHeight/screenWidth)){
                    int ih=iWidth*screenHeight/screenWidth;
                    r.set(0,(iHeight-ih)/2,iWidth,ih+(iHeight-ih)/2);
                    rr.set(0,0,screenWidth,screenHeight);
                }else {
                    int iw=iHeight*screenWidth/screenHeight;
                    r.set((iWidth-iw)/2,0,iw+(iWidth-iw)/2,iHeight);
                    rr.set(0,0,screenWidth,screenHeight);
                }
            }
            c.drawBitmap(coverImage,r,rr,coverPaint);
            //画标题
            Paint wordBackground =new Paint();
            wordBackground.setColor(mContext.getResources().getColor(R.color.Gray));
            Rect titleRect=new Rect();
            int lineX=marginWidth;
            int lineY=marginTop+screenWidth;
            if(!TextUtils.isEmpty(title)){
                List<String> list=getLineString(coverPaint,title);
                for (String line: list){
                    lineY+=(titleSize+marginLine);
                    titleRect.set(0,lineY-titleSize,screenWidth,lineY+marginLine);
                    c.drawRect(titleRect,wordBackground);
                    c.drawText(line,lineX,lineY,coverPaint);
                }
            }
            coverPaint.setColor(mContext.getResources().getColor(R.color.MinBlack));
            coverPaint.setTextSize(textSize*4/5);
            //画副标题
            if(!TextUtils.isEmpty(subTitle)){
                List<String> list=getLineString(coverPaint,subTitle);
                for (String line: list){
                    lineY+=(textSize+marginLine);
                    titleRect.set(0,lineY-textSize,screenWidth,lineY+marginLine);
                    c.drawRect(titleRect,wordBackground);
                    c.drawText(line,lineX,lineY,coverPaint);
                }
            }
        }else {
            //画文字
            pageString=getPageString(readAddress);
            int lineX=marginWidth;
            int lineY=0;
            for (String line: pageString){
                lineY+=marginLine+textSize;
                c.drawText(line,lineX,lineY,p);
            }

            //画进度
            float fPercent = (float) (readAddress * 1.0 / book.length());
            String strPercent = df.format(fPercent * 100) + "%";
            lineY+=marginLine+textSize;
            c.drawText(strPercent,marginWidth,lineY, percentPaint);
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
                        b+=1;
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
            while (address<string.length()){
                string=string.substring(address,string.length());
                int a=paint.breakText(string,true,lineWidth,null);//自动折行
                int b=string.indexOf("\n");
                if(b!=-1&&b<=a){
                    //如果有转行符
                    b+=1;
                }else {
                    b=string.lastIndexOf(" ",a);
                    if (b!=-1&&isMultipleString(string.substring(0,a))&&a<string.length()){
                        //如果有空格
                        b+=1;
                    }else {
                        //既没有转行符，也没有空格
                        b=a;
                    }
                }
                list.add(string.substring(0,b));
                address+=b;
            }
        }
        return list;
    }

    //翻到上一页
    public void prePage(){
        if (readAddress<=-1){
            showMessage(mContext.getResources().getString(R.string.first_page_toast));
            return;
        }
        readAddress=prePageAddress(readAddress);
    }

    //翻到下一页
    public void nextPage(){
        if (readAddress>=book.length()){
            showMessage(mContext.getResources().getString(R.string.last_page_toast));
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

    /*
    public void setMargin(int marginX){
        this.marginWidth=marginX;
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
    */

    public void setReadAddress(int readAddress) {
        this.readAddress = readAddress;
    }

    public void setCoverImage(Bitmap coverImage) {
        this.coverImage = coverImage;
    }
}
