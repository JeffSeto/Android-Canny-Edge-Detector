package com.cardscanner;

/**
 * Created by jeffreyseto on 2016-05-16.
 */
class DetectionInfo {

    private boolean mTopDetected;
    private boolean mBotDetected;
    private boolean mRightDetected;
    private boolean mLeftDetected;

    private boolean mLocked;

    public DetectionInfo(){
        mTopDetected = false;
        mBotDetected = false;
        mRightDetected = false;
        mLeftDetected = false;
        mLocked = false;
    }

    public boolean isTopDetected() {
        return mTopDetected || mLocked;
    }

    public boolean isBotDetected() {
        return mBotDetected || mLocked;
    }

    public boolean isRightDetected() {
        return mRightDetected || mLocked;
    }

    public boolean isLeftDetected() {
        return mLeftDetected || mLocked;
    }



    public void setTopDetected(boolean detected) {
        mTopDetected = detected;
    }

    public void setBotDetected(boolean detected) {
        mBotDetected = detected;
    }

    public void setRightDetected(boolean detected) {
        mRightDetected = detected;
    }

    public void setLeftDetected(boolean detected) {
        mLeftDetected = detected;
    }


    public boolean cardDetected(){
        return mLocked || (mTopDetected && mBotDetected && mLeftDetected && mRightDetected);
    }

    public boolean isLocked(){
        return mLocked;
    }

    public void setLocked(boolean locked){
        mLocked = locked;
    }
}
