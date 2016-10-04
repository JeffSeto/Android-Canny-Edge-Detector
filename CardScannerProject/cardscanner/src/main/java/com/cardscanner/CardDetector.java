package com.cardscanner;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by jeffreyseto on 2016-05-17.
 */
class CardDetector implements Camera.PreviewCallback {

    //Is true if a CardDetector object is processing an image
    public static boolean processingInProgress;

    private static final int DETECTION_WIDTH = 30;

    //Delay (in milliseconds) before a detected card is captured
    private static final long DETECTION_TIME_DELAY = 300;

    //Edge detection thresholds (Low resolution cameras)
    private static final int LR_TOP_LOW_THRESHOLD = 300;
    private static final int LR_BOT_LOW_THRESHOLD = 300;
    private static final int LR_SIDE_LOW_THRESHOLD= 150;

    private static final int LR_TOP_HIGH_THRESHOLD = 800;
    private static final int LR_BOT_HIGH_THRESHOLD = 1200;
    private static final int LR_SIDE_HIGH_THRESHOLD = 800;

    //Edge detection thresholds (High resolution cameras)
    private static final int HR_TOP_LOW_THRESHOLD = 500;
    private static final int HR_BOT_LOW_THRESHOLD = 600;
    private static final int HR_SIDE_LOW_THRESHOLD= 400;

    private static final int HR_TOP_HIGH_THRESHOLD = 2500;
    private static final int HR_BOT_HIGH_THRESHOLD = 3500;
    private static final int HR_SIDE_HIGH_THRESHOLD = 2000;

    //Holds the Canny processed image
    private Bitmap mEdgeImage;
    //Holds the raw stream image
    private Bitmap mRawImage;
    //Holds the captured card image
    private Bitmap mCardImage;

    //Height and width of the preview screen
    private int mWidth;
    private int mHeight;

    private boolean mHighResolution;

    private CannyEdgeDetector mEdgeDetector;

    private DetectionInfo mDetectionInfo;
    private GuideOverlay mOverlay;
    private CardScannerActivity mActivity;

    private long mStartTime;

    private boolean mFocusing;

    //Constructor that can only be accessed in this package
    protected CardDetector(CardScannerActivity activity, DetectionInfo info, GuideOverlay overlay, boolean highRes){
        mEdgeDetector = new CannyEdgeDetector();
        mEdgeDetector.setLowThreshold(1f);
        mEdgeDetector.setHighThreshold(1.5f);
        mDetectionInfo = info;
        mOverlay = overlay;
        mStartTime = -1;
        mActivity = activity;
        mHighResolution = highRes;
        mFocusing = false;
        processingInProgress = false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(camera != null) {
            if (processingInProgress) {
                // return frame buffer to pool
                if(camera != null) {
                    camera.addCallbackBuffer(data);
                }
                return;
            }

            //If a card is detected...
            if(mDetectionInfo.cardDetected()){
                //Start the countdown timer if timer hasn't been started
                if(mStartTime == -1){
                    mStartTime = System.currentTimeMillis();
                //If timer has been started, check if the timer has reached the DETECTION_TIME_DELAY value
                } else if(System.currentTimeMillis() - mStartTime >= DETECTION_TIME_DELAY){
                    mStartTime = -1;
                    //Lock guide bars
                    mDetectionInfo.setLocked(true);
                    //Generate the card image
                    generateCardImage(camera);
                    //Finish this activity
                    mActivity.finishAndSendResult(CardScannerActivity.RESULT_CODE_SUCCESS);


                }
            } else {
                //Reset timer if card is no longer detected
                mStartTime = -1;
            }

            //Start processing an image
            processingInProgress = true;

            if(mRawImage != null) {
                mRawImage.recycle();
            }
            //Get the camera preview parameters
            Camera.Size s = camera.getParameters().getPreviewSize();
            mWidth = s.width;
            mHeight = s.height;


            //Create a bitmap of the raw image
            int[] previewPixels = convertYUV420_NV21toRGB8888(data,mWidth, mHeight);
            mRawImage = Bitmap.createBitmap(previewPixels, mWidth, mHeight, Bitmap.Config.ARGB_8888);

            //Generates a bitmap that only displays edges found in the raw image
            mEdgeDetector.setSourceImage(mRawImage);
            mEdgeDetector.process();
            mEdgeImage = mEdgeDetector.getEdgesImage();

            //Creates bitmaps that are associated with the expected location of card edges
            Bitmap topEdge = Bitmap.createBitmap(mEdgeImage, 180,30,DETECTION_WIDTH, 420);
            Bitmap rightEdge = Bitmap.createBitmap(mEdgeImage, 180,20,280,DETECTION_WIDTH);
            Bitmap leftEdge = Bitmap.createBitmap(mEdgeImage, 180,440,280,DETECTION_WIDTH);
            Bitmap bottomEdge = Bitmap.createBitmap(mEdgeImage, 445,30,DETECTION_WIDTH, 420);


            //If a card edge is detected, set the appropriate boolean in DetectionInfo object
            System.out.println("High res");
            System.out.print("TOP: ");
            mDetectionInfo.setTopDetected(meetsThreshold(HR_TOP_LOW_THRESHOLD, HR_TOP_HIGH_THRESHOLD, topEdge));
            System.out.print("BOT: ");
            mDetectionInfo.setBotDetected(meetsThreshold(HR_BOT_LOW_THRESHOLD, HR_BOT_HIGH_THRESHOLD, bottomEdge));
            System.out.print("LEFT: ");
            mDetectionInfo.setLeftDetected(meetsThreshold(HR_SIDE_LOW_THRESHOLD, HR_SIDE_HIGH_THRESHOLD, leftEdge));
            System.out.println("RIGHT: ");
            mDetectionInfo.setRightDetected(meetsThreshold(HR_SIDE_LOW_THRESHOLD, HR_SIDE_HIGH_THRESHOLD, rightEdge));

            //Force Overlay to redraw itself
            mOverlay.invalidate();
            //Return buffer to camera
            camera.addCallbackBuffer(data);
            //No longer processing an image
            processingInProgress = false;
        }
    }

    //Checks if the given image (assumed to be a image processed using CANNY Edge Detection) contains a card edge.
    //Determines if there is a credit card edge by counting the number of white pixels in the image.  If the number of
    //white pixels falls within a specific range, then we assume that there is a card edge in the image.
    public boolean meetsThreshold(int lowerThreshold, int higherThreshold, Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[height * width];
        bitmap.getPixels(pixels,0,width,0,0,width,height);
        //Counter for number of white pixels in bitmap
        int counter = 0;
        //Counts the number of white pixels in bitmap
        for(int i = 0; i < pixels.length; i++){
            if(pixels[i] == -1){
                counter++;
            }
        }
        System.out.println(counter);
        //If the number of white pixels detected is above the given threshold, return true
        if(counter >= lowerThreshold && counter <= higherThreshold){
            return true;
        }
        return false;
    }


    //Sets up the camera buffer
    protected void setupBuffer(Camera camera){
        if(camera != null) {
            //Gets the size of the buffer array
            Camera.Parameters parameters = camera.getParameters();
            int previewFormat = parameters.getPreviewFormat();
            int bytesPerPixel = ImageFormat.getBitsPerPixel(previewFormat) / 8;
            int bufferSize = parameters.getPreviewSize().width * parameters.getPreviewSize().height * bytesPerPixel * 3;
            //Add a blank buffer to camera preview
            byte[] previewBuffer = new byte[bufferSize];
            camera.addCallbackBuffer(previewBuffer);
        }
    }

    //Generates the card image bitmap
    protected void generateCardImage(Camera camera){
        camera.stopPreview();
        //Rotate bitmap by 90 degrees
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(mRawImage,mRawImage.getWidth(),mRawImage.getHeight(),true);
        mRawImage = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        //Only save the area of the bitmap the card is in.
        mCardImage = Bitmap.createBitmap(mRawImage,30,180,415,275);
    }

    //Returns the generated card image
    protected Bitmap getCardImage(){
        return mCardImage;
    }

    //Converts a given YUV array into its RGB equivalent
    private static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    //Converts a YUV pixel to a RGB pixel
    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;
        r = y + (int)1.402f*v;
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)1.772f*u;
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }

    public Bitmap getEdgeImage(){ return mEdgeImage;}
}
