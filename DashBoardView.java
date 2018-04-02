package com.fenggood.myview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.text.DecimalFormat;

/**
 * Created by linfeng on 2018/3/30.
 */

public class DashBoardView extends View {
    private static final String TAG=DashBoardView.class.getSimpleName();

    private float width, height;//父布局高度和宽度

    private float outR;//最外层弧 相对于其他R是固定的
    private float shortLineR;//绘制短线的弧
    private float innerR;//最内层弧
    private float arcR;//弧的半径
    private float scaleValueR;//弧刻度值半径
    private float pointerR;//指针弧的半径
    private float legendHigh;
    private float legendWidth;

    private Path outPath;//最外层弧
    private Path shortLinePath;//绘制短线的弧
    private Path arcPath;//弧的半径
    private Path innerPath;//最内层弧
    private Path scaleValuePath;//弧刻度值
    private Path pointerPath;//指针弧的路径
    private Path pointerLongPath;//长一点指针弧的路径

    private float outScale = 0.40f;//最外层半径弧占父布局宽度（或者是高度）的比例  决定了圆的大小
    private float shortScale = 0.95f;//绘制短线的弧半径占最外层弧半径的比例
    private float innerScale = 0.80f;//最内层弧半径占最外层弧半径的比例 影响弧的宽度 半径越大弧的宽度越小
    private float scaleValueScale = 0.67f;//刻度值弧占最外层弧半径的比例
    private float pointerScale=0.1f;//指针弧占最外层弧半径的比例
    private float legendScale=0.46f;//图例占占父布局宽度（或者是高度）的比例
    private float arcWidth;//弧的宽度

    private float scaleValueCount = 50;//刻度的数量
    private float startscaleValue = -50;//开始刻度值
    private float endscaleValue = 50;//停止刻度值
    private float scaleValueCountBig=10;//大刻度值的数量 分成多少段
    private float scaleValueSweep=scaleValueCount/scaleValueCountBig;//隔多少个刻度画一个长线


    private Paint arcPaint;//弧的画笔
    private Paint shortLinePaint;//短线的画笔
    private Paint pointerPaint;//指针的画笔
    private Paint currentValuePaint;//值的画笔
    private float currentValueWidth=6f;//值的宽度
    private float currentValueSize=60f;
    private float pointerWidth=15f;//指针的线的宽度
    private float shortLineWidth = 6f;//短线的宽度
    private Paint scaleValuePaint;//刻度值的画笔
    private float scaleValueWidth = 3f;//刻度值的宽度
    private float scaleValueSize = 35f;//刻度值的宽度
    private Paint legendValuePaint;
    private float legendNameValueWidth=40f;//图例名称字体大小


    private float pointAngle=30;//指针偏移的角度

    //弧分成三段颜色的颜色值
    private int[] arcColors = {Color.parseColor("#91C7AE"), Color.parseColor("#63869E"),
            Color.parseColor("#C23531")};

    //弧分成三段所占的比例
    private float[] arcScales = {0.2f, 0.6f, 0.2f};//三个数的和必须是1

    private float[][] scaleValue=new float[arcScales.length][2];//每个颜色范围的 起始值

    private int startAngle;//开始的角度
    private int sweepAngle;//扫过的角度
    private int shortageAngle = 80;//缺失的部分的角度

    private float currentValue=10;

    private String legendName="温度(℃)";

    public DashBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(width / 2, height / 2);
        drawVisibleArc(canvas);
        drawLines(canvas);
        drawScaleValues(canvas);
        drawPointer(canvas);
        drawCurrentValue(canvas);
        drawLegend(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        outR = Math.min(w, h) * outScale;
        shortLineR = outR * shortScale;
        innerR = outR * innerScale;
        scaleValueR = outR * scaleValueScale;
        pointerR=outR*pointerScale;
        legendHigh=Math.min(w, h)*legendScale;
        legendWidth=outR*(1-legendScale*2);
        arcWidth = outR * (1 - innerScale); //弧的宽度等于最外层弧的半径减去最内存弧的半径
        arcR = innerR + arcWidth / 2;  //弧的半径等于弧的宽度除以2加上最内弧的半径
        intPaint();
        initAngle();
        calcScaleValue();
        drawInvisibleArc();
    }

    private void intPaint() {
        arcPaint = new Paint();//弧的画笔
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(arcWidth);
        arcPaint.setAntiAlias(true);
        shortLinePaint = new Paint();
        shortLinePaint.setStyle(Paint.Style.STROKE);
        shortLinePaint.setStrokeWidth(shortLineWidth);
        shortLinePaint.setColor(Color.WHITE);
        shortLinePaint.setAntiAlias(true);
        scaleValuePaint = new Paint();
        scaleValuePaint.setStyle(Paint.Style.STROKE);
        scaleValuePaint.setStrokeWidth(scaleValueWidth);
        scaleValuePaint.setTextSize(scaleValueSize);
        scaleValuePaint.setTextAlign(Paint.Align.CENTER);
        scaleValuePaint.setAntiAlias(true);
        pointerPaint=new Paint();
        pointerPaint.setStyle(Paint.Style.FILL);
        pointerPaint.setStrokeWidth(pointerWidth);
        pointerPaint.setAntiAlias(true);
        currentValuePaint=new Paint();
        currentValuePaint.setStyle(Paint.Style.STROKE);
        currentValuePaint.setStrokeWidth(currentValueWidth);
        currentValuePaint.setTextSize(currentValueSize);
        currentValuePaint.setTextAlign(Paint.Align.CENTER);
        currentValuePaint.setAntiAlias(true);
        legendValuePaint=new Paint();
        legendValuePaint.setAntiAlias(true);
        legendValuePaint.setTextAlign(Paint.Align.CENTER);
        legendValuePaint.setStrokeWidth(2f);
        legendValuePaint.setTextSize(legendNameValueWidth);
    }

    //绘制看不见的弧，用来计算位置的
    private void drawInvisibleArc() {
        //绘制最外层的path
        outPath = consPath(outR);
        shortLinePath = consPath(shortLineR);
        innerPath = consPath(innerR);
        scaleValuePath = consPath(scaleValueR);
        pointerPath=new Path();
        pointerPath.addArc(getArcRecf(pointerR),startAngle,sweepAngle);
        pointerLongPath=new Path();
        pointerLongPath.addArc(getArcRecf(pointerR),startAngle-pointAngle,sweepAngle+2*pointAngle);//因为有可能指针指到边界
    }

    //绘制看的见的弧
    private void drawVisibleArc(Canvas canvas) {
        float currentAngle = startAngle;
        for (int i = 0; i < arcScales.length; i++) {
            arcPath = new Path();
            float intervalAngle = sweepAngle * arcScales[i];//绘制的弧度
            arcPath.addArc(getArcRecf(arcR), currentAngle, intervalAngle);
            currentAngle = currentAngle + intervalAngle;
            arcPaint.setColor(arcColors[i]);
            canvas.drawPath(arcPath, arcPaint);
        }

    }

    //绘制所有线条
    private void drawLines(Canvas canvas) {
        PathMeasure outMeasure = new PathMeasure(outPath, false);
        float outLength = outMeasure.getLength();//获取外弧的总长度
        PathMeasure shortLineMeasure = new PathMeasure(shortLinePath, false);
        float shortLineLength = shortLineMeasure.getLength();//获取短线外弧的总长度
        PathMeasure innerMeasure = new PathMeasure(innerPath, false);
        float innerLineLength = innerMeasure.getLength();//获取最内弧的总长度
        Path shortPath = new Path();
        for (int i = 0; i < scaleValueCount; i++) {
            float[] outPosition = new float[2];//最外弧上的点位置
            float[] shortLinePosition = new float[2];//短线弧的点位置
            if (i != 0 && i % scaleValueSweep != 0) {
                outMeasure.getPosTan(outLength / scaleValueCount * i, outPosition, null);
                shortLineMeasure.getPosTan(shortLineLength / scaleValueCount * i, shortLinePosition, null);
                shortPath.moveTo(outPosition[0], outPosition[1]);
                shortPath.lineTo(shortLinePosition[0], shortLinePosition[1]);
            } else if (i % scaleValueSweep == 0&&i!=0) {
                float[] innerLinePosition = new float[2];//最内弧的点的位置
                outMeasure.getPosTan(outLength / scaleValueCount * i, outPosition, null);
                innerMeasure.getPosTan(innerLineLength / scaleValueCount * i, innerLinePosition, null);
                shortPath.moveTo(outPosition[0], outPosition[1]);
                shortPath.lineTo(innerLinePosition[0], innerLinePosition[1]);
            }
        }
        canvas.drawPath(shortPath, shortLinePaint);
    }


    //绘制刻度值
    private void drawScaleValues(Canvas canvas) {
        PathMeasure scaleValueMeasure = new PathMeasure(scaleValuePath, false);
        float scaleValueLength = scaleValueMeasure.getLength();//获取刻度值弧的总长度
        for (int i = 0; i < scaleValueCount + 1; i++) {
            if (i % scaleValueSweep == 0) {
                int scaleValueColor = 0;
                float[] scaleValuePosition = new float[2];//点位置
                float scale = i / scaleValueCount;
                float distance = scaleValueLength * scale;
                scaleValueMeasure.getPosTan(distance, scaleValuePosition, null);
                float value = (( endscaleValue-startscaleValue) * scale)+startscaleValue;
                //根据比例给不同范围的刻度值设置颜色
                if (scale <= arcScales[0]) {
                    scaleValueColor = arcColors[0];
                } else if (scale > arcScales[0] && scale <= arcScales[1]+arcScales[0]) {
                    scaleValueColor = arcColors[1];
                } else if (scale > arcScales[1]+arcScales[0] && scale <= arcScales[2]+arcScales[1]+arcScales[0]) {
                    scaleValueColor = arcColors[2];
                }
                scaleValuePaint.setColor(scaleValueColor);
                canvas.drawText( new DecimalFormat("#.##").format(value) + "", scaleValuePosition[0], scaleValuePosition[1], scaleValuePaint);
            }
        }
    }


    //绘制指针
    private void drawPointer(Canvas canvas){
        float[] scaleValuePosition=getValuePosition(scaleValuePath,currentValue,false,0);//刻度值上的点
        float offScale=pointAngle/sweepAngle;//偏移角度占总角度的比例
        float[] upPosition=getValuePosition(pointerPath,currentValue,true,offScale);//靠右点的位置
        float[] downPosition=getValuePosition(pointerPath,currentValue,true,-offScale);//靠左点的位置
        Log.i(TAG,"scaleValuePosition:["+scaleValuePosition[0]+","+scaleValuePosition[1]+"]");
        Log.i(TAG,"upPosition:["+upPosition[0]+","+upPosition[1]+"]");
        Log.i(TAG,"downPosition:["+downPosition[0]+","+downPosition[1]+"]");
        Path pointPath=new Path();
        pointPath.lineTo(upPosition[0],upPosition[1]);
        pointPath.lineTo(scaleValuePosition[0],scaleValuePosition[1]);
        pointPath.lineTo(downPosition[0],downPosition[1]);
        pointPath.close();
        int color=getValueColor(currentValue);
        pointerPaint.setColor(color);
//        canvas.drawPoint(upPosition[0],upPosition[1],pointerPaint);
//        canvas.drawPoint(downPosition[0],downPosition[1],pointerPaint);
        canvas.drawPath(pointPath,pointerPaint);//FILL 要转弯
    }


    //绘制值
    private void drawCurrentValue(Canvas canvas){
        currentValuePaint.setColor(getValueColor(currentValue));
        canvas.drawText(currentValue+"",0,scaleValueR,currentValuePaint);
    }


    //绘制图例
    private void drawLegend(Canvas canvas){
        RectF rectF=new RectF(-legendWidth,-legendHigh,0,-legendHigh+legendWidth);
        legendValuePaint.setColor(getValueColor(currentValue));
        canvas.drawText(legendName,0,-legendHigh+legendWidth,legendValuePaint);
    }

    //根据半径获取矩形
    private RectF getArcRecf(float R) {
        return new RectF(-R, -R, R, R);//矩形左上角和右下角两个点的坐标
    }

    /**
     * 根据shortageAngle来调整圆弧的角度
     */
    private void initAngle() {
        sweepAngle = 360 - shortageAngle;
        startAngle = 90 + shortageAngle / 2;
    }

    //根据半径构造 有弧度的path
    private Path consPath(float R) {
        Path path = new Path();
        path.addArc(getArcRecf(R), startAngle, sweepAngle);
        return path;
    }

//计算每个刻度范围 起始值
    private void calcScaleValue(){
        float lastValue=0f;
        for (int i=0;i<scaleValue.length;i++){
            float startValue=i==0?startscaleValue:lastValue;
            float endValue = (( endscaleValue-startscaleValue) * arcScales[i])+startValue;
            scaleValue[i][0]=startValue;
            scaleValue[i][1]=endValue;
            lastValue=endValue;
        }
    }
    //获取当前值是什么颜色
    private int getValueColor(float value){
        for (int i=0;i<scaleValue.length;i++){
            if(value>=scaleValue[i][0] && value<=scaleValue[i][1]){
                return arcColors[i];
            }
        }
        return 0;
    }
    //获取当前值在刻度弧上的坐标
    private float[] getValuePosition(Path path,float value,boolean isOffset,float offScale){
        float[] position=new float[2];
        PathMeasure measure = new PathMeasure(path, false);
        PathMeasure measureLong=new PathMeasure(pointerLongPath,false);
        float longPointerLength=measureLong.getLength();//长一点的刻度弧的长度
        float pointLength = measure.getLength();//获取弧的总长度
        float pointAnglelength=Math.abs(offScale*pointLength);//偏移角度的弧长 占短一点的指针弧的长度
        float scale=(value-startscaleValue)/(endscaleValue-startscaleValue);//弧总长度 不一样
        float distance=scale*pointLength;//点在短指针弧上的直接位置
        float longDistance=pointAnglelength+distance+offScale*pointLength;//等于偏移角度的弧长加上
        if(isOffset){
            measureLong.getPosTan(longDistance,position,null);//使用长一点的弧获取位置 距离要加上一个偏移角度弧长
        }else{
            measure.getPosTan(scale*pointLength,position,null);
        }
        return position;
    }

    public void setCurrentValue(float value){
        currentValue= Float.parseFloat(new DecimalFormat("#.##").format(value));
        invalidate();
    }
}
