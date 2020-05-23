package com.example.lipstickdetect;

import android.content.res.AssetManager;

import org.opencv.core.Mat;

public class FaceDetect {
    public native static String LandmarkDetection(long addrInput, long addrOutput);
    public static native void loadShapePredictor(AssetManager assetManager);
}
