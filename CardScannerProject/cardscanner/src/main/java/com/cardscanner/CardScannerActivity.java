package com.cardscanner;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.carddetector.R;

import java.io.IOException;
import java.security.Policy;


public class CardScannerActivity extends Activity {

    /**
     * String Extra. Optional. Will display the provided phrase inside the card guide.
     * Enter "\n" to start a new line.
     */
    public static final String EXTRA_CARD_IMAGE = "CardScanner.cardImage";

    /**
     * Bitmap (Parceable) Extra. Call this extra in your onActivityResult method to receive
     * the card bitmap.
     */
    public static final String EXTRA_SCAN_INSTRUCTIONS = "CardScanner.mScanInstructions";

    /**
     * Boolean Extra. Optional. Will suppress the cancel button if set to <code>true</code>.
     */
    public static final String EXTRA_SUPPRESS_CANCEL = "CardScanner.suppressCancel";

    /**
     * Boolean Extra. Optional. Will suppress the manual button if set to <code>true</code>.
     */
    public static final String EXTRA_SUPPRESS_MANUAL = "CardScanner.suppressManual";

    /**
     * String Extra. Optional. Will replace default cancel button text if not <code>null</code>.
     */
    public static final String EXTRA_CANCEL_TEXT = "CardScanner.cancelText";

    /**
     * String Extra. Optional. Will replace default manual button text if not <code>null</code>.
     */
    public static final String EXTRA_MANUAL_TEXT = "CardScanner.manualText";


    //Arbitrarily chosen, hopefully it does not conflict with other result codes
    private static int result = 1303841;
    /**
     * Result code indicating that the scan was succesfull
     */
    public static final int RESULT_CODE_SUCCESS = result++;

    /**
     * Result code indicating that the user cancelled the scan
     */
    public static final int RESULT_CODE_CANCELLED =  result++;

    /**
     * Result code indicating that the camera failed to be open
     */
    public static final int RESULT_CODE_CAMERA_FAILED = result++;

    /**
     * Result code indicating that the scanner hit an error
     */
    public static final int RESULT_CODE_ERROR = result++;


    //Taken from Card.IO and used as a reference
    private final int CARD_TARGET_WIDTH = 428;
    private final int CARD_TARGET_HEIGHT = 270;

    private DetectionInfo mInfo;
    private CardDetector mDetector;
    private Rect mGuide;

    private GuideOverlay mOverlay;
    private FrameLayout mMainLayout;
    private Preview mPreview;

    private Camera mCamera;
    private String mScanInstructions;

    private boolean mSuppressCancel;
    private boolean mSuppressManual;

    private String mCancelText;
    private String mManualText;

    private boolean mHighResolution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_detector);

        //Create a new DetectionInfo object
        mInfo = new DetectionInfo();

        //Get the main layout of this activity and give it a black background
        mMainLayout = (FrameLayout) findViewById(R.id.card_detector_main_layout);
        mMainLayout.setBackgroundColor(Color.BLACK);

        //If provided, get the scan instruction text
        mScanInstructions = getIntent().getStringExtra(EXTRA_SCAN_INSTRUCTIONS);

        //If provided, get the manual button text
        mManualText = getIntent().getStringExtra(EXTRA_MANUAL_TEXT);

        //If provided, get the cancel button text
        mCancelText = getIntent().getStringExtra(EXTRA_CANCEL_TEXT);

        //Check if parent activity wants to suppress the cancel button
        mSuppressCancel = getIntent().getBooleanExtra(EXTRA_SUPPRESS_CANCEL, false);

        //Check if parent activity wants to suppress the manual button
        mSuppressManual = getIntent().getBooleanExtra(EXTRA_SUPPRESS_MANUAL, false);

        try {
            //Initialise fullsreen mode
            initFullScreen();
            //Initialise activity layout and Preview view
            initPreviewScreen();
        } catch (Exception e){
            e.printStackTrace();
            finishAndSendResult(RESULT_CODE_ERROR);
        }

        //Create a new CardDetector
        mDetector = new CardDetector(this, mInfo, mOverlay, mHighResolution);


    }

    //Set fullscreen settings
    private void initFullScreen(){
        //Force activity to full screen
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        //Remove action bar
        ActionBar actionBar = getActionBar();
        if(actionBar!= null){
            actionBar.hide();
        }
    }

    //Attempt to initialize camera
    private void prepareCamera(){
        if(mCamera == null){
            try {
                mCamera = Camera.open();
            } catch (RuntimeException e){
                e.printStackTrace();
                //Finish this activity and tell parent activity camera failed to open
                finishAndSendResult(RESULT_CODE_ERROR);
            }
            if(mCamera == null){
                //Finish this activity and tell parent activity camera failed to open
                finishAndSendResult(RESULT_CODE_CAMERA_FAILED);
            } else {
                //Rotate camera display by 90 degrees
                mCamera.setDisplayOrientation(90);
                //Get camera supported camera preview sizes
                Camera.Parameters parameters = mCamera.getParameters();
                mHighResolution = true;
//                for(Camera.Size s: parameters.getSupportedPreviewSizes()){
//                    //System.out.println(s.width + "," + s.height + ";");
//                    if(s.width == 1280 && s.height == 720){
//                        mHighResolution = false;
//                        break;
//                    }
//                }
                parameters.setPreviewSize(Preview.PREVIEW_HEIGHT,Preview.PREVIEW_WIDTH);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(parameters);
            }
        }
    }

    //Set the activity layout and initialize the preview view
    private void initPreviewScreen(){
        //Start the camera
        prepareCamera();
        //Create a Preview view
        mPreview = new Preview(this, null);
        //mPreview.setCamera(mCamera);
        mPreview.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP));
        //Add Preview view to the layout
        mMainLayout.addView(mPreview);
        //Create the guide frame rectangle
        mGuide = generateGuideFrame();

        //Create the GuideOverlay
        mOverlay = new GuideOverlay(this, null, mInfo ,mGuide, mScanInstructions);
        mOverlay.setColor(Color.rgb(129,98,201));
        mOverlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        //Add GuideOverlay to the layout
        mMainLayout.addView(mOverlay);

        //Create a RelativeLayout that will force the button layout to be at the bottom of the screen
        RelativeLayout anchor = new RelativeLayout(this);

        //Set the layout params for the anchor layout
        RelativeLayout.LayoutParams anchorParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        anchorParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        anchor.setLayoutParams(anchorParams);
        anchor.setGravity(Gravity.BOTTOM);

        //Add the anchor layout to the main layout
        mMainLayout.addView(anchor);

        LinearLayout buttonLayout = new LinearLayout(this);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonLayout.setLayoutParams(buttonLayoutParams);

        anchor.addView(buttonLayout);

        //Add the cancel button to the button layout if it is not suppressed
        if(!mSuppressCancel){
            //Create the cancel button
            Button cancelButton = new Button(this);
            if(mCancelText == null){
                cancelButton.setText("Cancel");
            } else {
                cancelButton.setText(mCancelText);
            }
            //Add cancel button to button layout
            buttonLayout.addView(cancelButton);
            //If the cancel button was pressed, return to parent activity and send RESULT_CODE_CANCELLED
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAndSendResult(RESULT_CODE_CANCELLED);
                }
            });

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cancelButton.getLayoutParams();
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            params.width = 0;
            params.weight = 1;
            params.gravity = Gravity.BOTTOM;

            params.setMargins(0,0,5,10);

            cancelButton.setLayoutParams(params);
        }

        //Adds the manual button to the button layout if it is not suppressed
        if(!mSuppressManual) {
            //Create the manual camera button
            Button manualButton = new Button(this);
            if(mManualText == null){
                manualButton.setText("Take Picture");
            } else {
                manualButton.setText(mManualText);
            }
            //Add manual button to button layout
            buttonLayout.addView(manualButton);
            //If manual button is clicked, instantly generate a card image and then finish this aci
            manualButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDetector.generateCardImage(mCamera);
                    finishAndSendResult(RESULT_CODE_SUCCESS);

                }
            });

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) manualButton.getLayoutParams();
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            params.width = 0;
            params.weight = 1;
            params.gravity = Gravity.BOTTOM;
            params.setMargins(5,0,0,10);
            manualButton.setLayoutParams(params);
        }

//        RelativeLayout testLayout = new RelativeLayout(this);
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
//        testLayout.setLayoutParams(params);
//        final ImageView image = new ImageView(this);
//        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        image.setLayoutParams(imageParams);
//
//        Button button = new Button(this);
//        testLayout.addView(button);
//        testLayout.addView(image);
//        mMainLayout.addView(testLayout);
//
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                image.setImageBitmap(mDetector.getEdgeImage());
//            }
//        });
    }

    //Generates guide frame
    private Rect generateGuideFrame(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) (size.x * 0.90);
        int height = (int) (width * CARD_TARGET_HEIGHT/CARD_TARGET_WIDTH);
        int xc = size.x/2;
        int yc = size.y/2;
        int x1 = xc - width/2;
        int x2 = xc + width/2;
        int y1 = yc - height/2;
        int y2 = yc + height/2;

        return new Rect(x1,y1,x2,y2);
    }

    protected Rect getPreviewRectangle(){
        SurfaceView sv = mPreview.getSurfaceView();
        return new Rect(new Rect(sv.getLeft(), sv.getTop(), sv.getRight(), sv
                .getBottom()));
    }

    //Finish this activity and send result to parent activity
    public void finishAndSendResult(int result){
        Intent data = null;
        if(result == RESULT_CODE_SUCCESS) {
            data = new Intent();
            Bitmap cardImage = mDetector.getCardImage();
            data.putExtra(EXTRA_CARD_IMAGE, cardImage);
        }
        setResult(result, data);
        finish();
    }

    public void onBackPressed(){
        setResult(RESULT_CODE_CANCELLED, null);
        super.onBackPressed();
    }

    @Override
    public void onPause(){
        if(mCamera != null){
            //Remove camera from Preview object
            mPreview.setCamera(null);
            try {
                //Stop the camera mPreview
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
            }
            mDetector.processingInProgress = false;
            //Release camera
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;

        }
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        //Open the camera
        if(mCamera == null) {
            prepareCamera();
        }
        //Restart the mPreview
        if(mPreview != null && mCamera != null){
            mPreview.setCamera(mCamera);
            try {
                mCamera.setPreviewDisplay(mPreview.getSurfaceView().getHolder());
            } catch (IOException e) {
                e.printStackTrace();
                finishAndSendResult(RESULT_CODE_ERROR);
            }
            //Set up SurfaceHolder in the preview
            mPreview.setupHolder();
            //Set up camera buffer
            mDetector.setupBuffer(mCamera);
            mCamera.setPreviewCallbackWithBuffer(mDetector);
            //Start preview
            mPreview.makePreviewGo();
        }
    }
}
