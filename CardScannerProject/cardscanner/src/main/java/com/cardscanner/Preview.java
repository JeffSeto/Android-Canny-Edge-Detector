package com.cardscanner;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by jeffreyseto on 2016-05-16.
 */

//Based of Card.IO's Preview class
class Preview extends ViewGroup implements SurfaceHolder.Callback{

    //Desired preview width and height
    protected static int PREVIEW_WIDTH = 480;
    protected static int PREVIEW_HEIGHT = 640;
    //Camera object
    private Camera mCamera;
    //SurfaceView and it's holder
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    //Constructor
    protected Preview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        //Create a SurfaceView object
        mSurfaceView = new SurfaceView(context);
//        mSurfaceView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mCamera != null) {
//                    Camera.Area focusArea = new Camera.Area(new Rect(-500,-200,500,200), 1000);
//                    mCamera.cancelAutoFocus();
//                    Camera.Parameters cameraParams = mCamera.getParameters();
//                    ArrayList<Camera.Area> focusList = new ArrayList();
//                    focusList.add(focusArea);
//                    cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                    cameraParams.setFocusAreas(focusList);
//                    mCamera.setParameters(cameraParams);
//                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
//                        @Override
//                        public void onAutoFocus(boolean success, Camera camera) {
//                            System.out.println("Attempting to auto focus");
//                        }
//                    });
//                }
//            }
//        });

        //Gets the SurfaceView holder
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        //Add the SurfaceView to this preview's view
        addView(mSurfaceView);
    }

    //Sets the camera object
    public void setCamera(Camera camera){
        mCamera = camera;
    }

    //Sets up the SurfaceHolder object attached to this preview
    public void setupHolder(){
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    //Taken from Card.IO.  Not sure what this does, but it's required by ViewGroup
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);
    }


    //Taken from Card.IO
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bot) {
        if(changed && getChildCount() > 0){
            int height = bot-top;
            int width = right-left;

            int previewWidth = PREVIEW_WIDTH;
            int previewHeight = PREVIEW_HEIGHT;

            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                mSurfaceView.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                mSurfaceView.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }
        }
    }

    //Called when view is created
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Start the preview
        makePreviewGo();
    }

    //Called when view is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Stop camera preview
        if(mCamera != null){
            mCamera.stopPreview();
        }
    }

    //Our surface should never change, so no implementation code is provided
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    //Attempt to start the camera preview
    public void makePreviewGo(){
        try {
            //Set this preview's surface view as the camera preview display surface
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            System.out.println("Can't add holder");
        }
        try {
            //Attempt to start the preview
            mCamera.startPreview();
//            mCamera.autoFocus(new Camera.AutoFocusCallback() {
//                @Override
//                public void onAutoFocus(boolean success, Camera camera) {
//                    System.out.println("Attempting to auto focus");
//                }
//            });
        } catch (RuntimeException e) {
            System.out.println("Runtime exception");
        }

    }

}

