package org.opencv.samples.facedetect;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

@SuppressWarnings("deprecation")
public class ExtendedJavaCamera extends JavaCameraView {

	public ExtendedJavaCamera(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	public ExtendedJavaCamera(Context context, int cameraId) {
		super(context, cameraId);
		// TODO Auto-generated constructor stub
	}
	
	public Camera getCurrentCamera(){
		return mCamera;
	}
}
