package com.cardscanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by jeffreyseto on 2016-05-16.
 */
class GuideOverlay extends View{

    private static final float GUIDE_FONT_SIZE = 26.0f;
    private static final float GUIDE_LINE_PADDING = 8.0f;
    private static final float GUIDE_LINE_HEIGHT = GUIDE_FONT_SIZE + GUIDE_LINE_PADDING;

    //Taken from Card.IO
    private final static int GUIDE_STROKE_WIDTH = 17;

    //Holds the rectangle that represents the guide
    private Rect mGuide;

    //Holds the rectangle that represents the preview
    private Rect mPreviewRectangle;

    //Saves the colour code of the guide colour
    private int mGuideColour;

    //Paint object used to paint the guide lines
    private Paint mGuidePaint;

    private float mScale = 1;

    private DetectionInfo mInfo;

    private Paint mLockedBackgroundPaint;

    private Path mLockedBackgroundPath;

    public String scanInstructions;

    private WeakReference<CardScannerActivity> mParentActivity;

    //Constructor that can only be accessed in this package
    protected GuideOverlay(CardScannerActivity context, AttributeSet attrs, DetectionInfo info, Rect guide, String scanInstructions) {
        super(context, attrs);

        //Set the detection info object
        mInfo = info;
        //Set the guide rectangle
        mGuide = guide;

        //Determine scale of device
        mScale = getResources().getDisplayMetrics().density / 1.5f;

        //Initialize the guide Paint
        mGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        //Set colour and opacity of the locked background
        mLockedBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLockedBackgroundPaint.clearShadowLayer();
        mLockedBackgroundPaint.setStyle(Paint.Style.FILL);
        mLockedBackgroundPaint.setColor(0xbb000000);

        //Sets the scan instructions
        if(scanInstructions == null){
            this.scanInstructions = "";
        } else {
            this.scanInstructions = scanInstructions;
        }

        mParentActivity = new WeakReference<CardScannerActivity>(context);
    }

    //Set the guide colour
    protected void setColor(int colour){
        mGuideColour = colour;
    }

    @Override
    protected void onDraw(Canvas canvas){
        if(mGuide == null){
            return;
        }

        if(mPreviewRectangle == null ||
                (mParentActivity.get().getPreviewRectangle()) != null && !mPreviewRectangle.equals(mParentActivity.get().getPreviewRectangle())){
            mPreviewRectangle = mParentActivity.get().getPreviewRectangle();
            mLockedBackgroundPath = new Path();
            mLockedBackgroundPath.addRect(new RectF(mPreviewRectangle), Path.Direction.CW);
            mLockedBackgroundPath.addRect(new RectF(mGuide), Path.Direction.CCW);
        }
        //Determine corner tick length
        int tickLength = (mGuide.bottom - mGuide.top) / 4;
        //Save this view
        canvas.save();

        //Show locked background if a card is detected
        if(mInfo.cardDetected() && mLockedBackgroundPath != null){
            canvas.drawPath(mLockedBackgroundPath, mLockedBackgroundPaint);
        }


        // Draw guide lines
        mGuidePaint.clearShadowLayer();
        mGuidePaint.setStyle(Paint.Style.FILL);
        mGuidePaint.setColor(mGuideColour);



        //Draw top left corner
        canvas.drawRect(
                tick(mGuide.left, mGuide.top, mGuide.left + tickLength, mGuide.top),
                mGuidePaint);
        canvas.drawRect(
                tick(mGuide.left, mGuide.top, mGuide.left, mGuide.top + tickLength),
                mGuidePaint);

        //Draw top right corner
        canvas.drawRect(
                tick(mGuide.right, mGuide.top, mGuide.right - tickLength, mGuide.top),
                mGuidePaint);
        canvas.drawRect(
                tick(mGuide.right, mGuide.top, mGuide.right, mGuide.top + tickLength),
                mGuidePaint);

        //Draw bottom left corner
        canvas.drawRect(
                tick(mGuide.left, mGuide.bottom, mGuide.left + tickLength, mGuide.bottom),
                mGuidePaint);
        canvas.drawRect(
                tick(mGuide.left, mGuide.bottom, mGuide.left, mGuide.bottom - tickLength),
                mGuidePaint);

        //Draw bottom right corner
        canvas.drawRect(
                tick(mGuide.right, mGuide.bottom, mGuide.right - tickLength,
                        mGuide.bottom), mGuidePaint);
        canvas.drawRect(
                tick(mGuide.right, mGuide.bottom, mGuide.right, mGuide.bottom
                        - tickLength), mGuidePaint);


        if(mInfo != null){
            int edgesDetected = 0;
            //Draw the top bar if a card's top edge is detected
            if(mInfo.isTopDetected()){
                edgesDetected++;
                canvas.drawRect(
                        tick(mGuide.left, mGuide.top, mGuide.right, mGuide.top), mGuidePaint);
            }
            //Draw the bottom bar if a card's bottom edge is detected
            if(mInfo.isBotDetected()){
                edgesDetected++;
                canvas.drawRect(
                        tick(mGuide.left, mGuide.bottom, mGuide.right, mGuide.bottom), mGuidePaint);
            }
            //Draw the right bar if a card's right edge is detected
            if(mInfo.isRightDetected()){
                edgesDetected++;
                canvas.drawRect(
                        tick(mGuide.right, mGuide.top, mGuide.right, mGuide.bottom), mGuidePaint);
            }
            //Draw the left bar if a card's left edge is detected
            if(mInfo.isLeftDetected()){
                edgesDetected++;
                canvas.drawRect(
                        tick(mGuide.left, mGuide.top, mGuide.left, mGuide.bottom), mGuidePaint);
            }


            if(edgesDetected < 3){
                // Draw guide text
                // Set up paint attributes
                float guideHeight = GUIDE_LINE_HEIGHT * mScale;
                float guideFontSize = GUIDE_FONT_SIZE * mScale;

                mGuidePaint.setColor(Color.WHITE);
                mGuidePaint.setStyle(Paint.Style.FILL);
                mGuidePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                mGuidePaint.setAntiAlias(true);
                float[] black = { 0f, 0f, 0f };
                mGuidePaint.setShadowLayer(1.5f, 0.5f, 0f, Color.HSVToColor(200, black));

                mGuidePaint.setTextAlign(Paint.Align.CENTER);
                mGuidePaint.setTextSize(guideFontSize);

                // Translate and rotate text
                canvas.translate(mGuide.left + mGuide.width() / 2, mGuide.top + mGuide.height() / 2);
                canvas.rotate(0);

                if (scanInstructions != null && scanInstructions != "") {
                    String[] lines = scanInstructions.split("\n");
                    float y = -(((guideHeight * (lines.length - 1)) - guideFontSize) / 2) - 3;

                    for (int i = 0; i < lines.length; i++) {
                        canvas.drawText(lines[i], 0, y, mGuidePaint);
                        y += guideHeight;
                    }
                }
            }
        }
        //Re-draw this view
        canvas.restore();
    }

    //Returns a rectangle that represents a tick
    private Rect tick(int x1, int y1, int x2, int y2){

        Rect r = new Rect();
        int t2 = (int) (GUIDE_STROKE_WIDTH / 2 * mScale);

        r.left = Math.min(x1, x2) - t2;
        r.right = Math.max(x1, x2) + t2;

        r.top = Math.min(y1, y2) - t2;
        r.bottom = Math.max(y1, y2) + t2;

        return r;
    }


}
