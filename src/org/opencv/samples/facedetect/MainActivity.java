package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.pedestriandetectdemo.R;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements CvCameraViewListener {
    private static final String TAG = "test";

    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("detection_based_tracker");

    }

    /*
     * private Preview mPreview; private Camera mCamera;
     */
    private int numberOfCameras;
    private int DEFAULT_CAMERA_ID;
    protected boolean isOpenFaceDetection = true;
    private ExtendedJavaCamera mCamera;
    private Mat mGray;
    private Mat mRgba;
    private int mAbsoluteFaceSize = 0;
    private float mRelativeFaceSize;
    private boolean isOrientationLandscope = true;
    //行人数量
    private int pedestrian_count = 0;

    private TextView tv_pedestrian_count;
    //text to sound
    TextToSpeech tts;
    //表示左中右三个方向是否有人的数组，1代表有人，0代表没人
    private int directionPedestrian[] = new int[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        this.setContentView(R.layout.activity_main);

        //每一秒更新一次人数的显示
        //start
        tv_pedestrian_count = (TextView) findViewById(R.id.tv_pedestrian_count);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Your logic here...

                // When you need to modify a UI element, do so on the UI thread.
                // 'getActivity()' is required as this is being ran from a Fragment.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
                        tv_pedestrian_count.setText("cout: " + pedestrian_count);
                    }
                });
            }
        }, 0, 1000); // End of your timer code.
        //end


        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                DEFAULT_CAMERA_ID = i;
            }
        }
        mCamera = new ExtendedJavaCamera(this, DEFAULT_CAMERA_ID);

        ((RelativeLayout) findViewById(R.id.camera_layout)).addView(mCamera);

        // 这里设定的是最小的识别大小,原本为0.2f，。。。数据越小需要识别的图像越多，速度越慢，误差也会提高
        // this is the minimal parameter of the detect object. The smaller, the
        // slower.
        setMinFaceSize(0.2f);// 0.2f

        //新开一个线程，用于每3秒检测一次行人，做语音提示
        new Thread(new ThreadShow()).start();

    }

    //新线程用于语音，每三秒检测一次
    class ThreadShow implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (pedestrian_count > 0) {

                    //分布有八种情况，不同的情况给予不同的语音提示
                    if (directionPedestrian[0] == 1) {
                        if (directionPedestrian[1] == 1) {
                            if (directionPedestrian[2] == 1) {
                                textToSpeech("both sides find people");
                            } else {
                                textToSpeech("left and center find people");
                            }
                        } else {
                            if (directionPedestrian[2] == 1) {
                                textToSpeech("left and right find people");
                            } else {
                                textToSpeech("left find people");
                            }
                        }
                    } else {
                        if (directionPedestrian[1] == 1) {
                            if (directionPedestrian[2] == 1) {
                                textToSpeech("center and right find people");
                            } else {
                                textToSpeech("center find people");
                            }
                        } else {
                            if (directionPedestrian[2] == 1) {
                                textToSpeech("right find people");
                            } else {
                                textToSpeech("no people find");
                            }
                        }
                    }

                }
                try {
                    Thread.currentThread().sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //语音函数
    private void textToSpeech(String text) {
        final String speechText = text;
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
//                    int result = tts.setLanguage(Locale.US);
//                    if (result == TextToSpeech.LANG_MISSING_DATA ||
//                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                        Log.e("error", "This Language is not supported");
//                    } else {
                        //ConvertTextToSpeech("left test");
                        tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null);
//                    }
                } else
                    Log.e("error", "Initialization Failed!");
            }
        });
    }


    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    boolean replaceView(ViewGroup vg, View view, View replace_view) {
        vg.removeAllViews();
        vg.addView(replace_view);
        vg.requestLayout();
        replace_view.forceLayout();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Open the default i.e. the first rear facing camera.

        // set requested orientation
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isOrientationLandscope = false;
        } else {
            isOrientationLandscope = true;
        }
        mCamera.enableView();
        mCamera.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mCamera.setCvCameraViewListener(null);
            mCamera.disableView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        if (isOpenFaceDetection) {
            // 转置的算法将导致内存溢出。这里需要释放内存
            Runtime.getRuntime().gc();
            inputFrame.copyTo(mRgba);

            //Imgproc.cvtColor(inputFrame, mGray, Imgproc.COLOR_RGBA2GRAY);
            //边缘化处理，提取图像边缘，机器学习是通过非边缘化的图像学习的，所以，无用，而且会提高运算量
            //Imgproc.Canny(mGray, mGray, 300, 600, 5, true);

            //图像预处理操作，包括 调整为灰度图，直方图均衡化, 图像锐化
            mGray = Preprocess.main(inputFrame);
            // start, if the orientation is not landscape, change the
            // orientation of the image, too.
//			Imgproc.Canny(mGray, mGray, 80, 100);
            //二值化图像测试
            //Imgproc.threshold(mGray, mGray, 128, 255, Imgproc.THRESH_BINARY);
//			return mGray;
            if (isOrientationLandscope) {

                // matrix's translate will out of memory, so we need to
                // reallocate memory
                mGray = mGray.t();
            }

            // end
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);

            MatOfRect faces = new MatOfRect();
            if (mJavaDetector != null) {
                // detectMultiScale函数中smallImg表示的是要检测的输入图像为smallImg，faces表示检测到的人脸目标序列，1.1表示
                // 每次图像尺寸减小的比例为1.1，2表示每一个目标至少要被检测到3次才算是真的目标(因为周围的像素和不同的窗口大
                // 小都可以检测到人脸)，之后的两个Size()为目标的最小最大尺寸

                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
                        new Size());
            }
            //mGray.release();
            Rect[] facesArray = faces.toArray();

            //输出行人数量
            pedestrian_count = faces.height();
            Log.d(TAG, " main acitivity  face number = " + faces.height());

            for (int i = 0; i < directionPedestrian.length; i++) {
                directionPedestrian[i] = 0;
            }

            for (int i = 0; i < facesArray.length; i++) {
                // if the orientation is not landscape, change the
                // orientation of the image, too. Also, we need to change the
                // coordinate data.
                Point tl = new Point();
                Point br = new Point();
                if (isOrientationLandscope) {
                    tl.x = facesArray[i].tl().y;
                    tl.y = facesArray[i].tl().x;

                    br.x = facesArray[i].br().y;
                    br.y = facesArray[i].br().x;
                } else {
                    tl = facesArray[i].tl();
                    br = facesArray[i].br();
                }

                //判断哪些区域有人
                if (isOrientationLandscope) {
                    //竖屏，手机分辨率
                    WindowManager windowManager = getWindowManager();
                    Display display = windowManager.getDefaultDisplay();
                    int screenWidth = display.getHeight();
                    int targetPositionX = screenWidth - (int) (tl.y + br.y) / 2;
                    Log.e("screenwidth", "portrait screenWidth: " + screenWidth + "  " + "targetPositionX: " + targetPositionX);
                    if (targetPositionX < screenWidth / 3) {
                        directionPedestrian[0] = 1;
                    } else if (targetPositionX < 2 * screenWidth / 3) {
                        directionPedestrian[1] = 1;
                    } else {
                        directionPedestrian[2] = 1;
                    }
                } else {
                    //手机分辨率，横屏时，对换屏幕宽高值
                    WindowManager windowManager = getWindowManager();
                    Display display = windowManager.getDefaultDisplay();
                    int screenWidth = display.getWidth();
                    int targetPositionX = (int) (tl.x + br.x) / 2;
                    Log.e("screenwidth", "landscope screenWidth: " + screenWidth + "  " + "targetPositionX: " + targetPositionX);
                    if (targetPositionX < screenWidth / 3) {
                        directionPedestrian[0] = 1;
                    } else if (targetPositionX < 2 * screenWidth / 3) {
                        directionPedestrian[1] = 1;
                    } else {
                        directionPedestrian[2] = 1;
                    }
                }


                Core.rectangle(mRgba, tl, br, FACE_RECT_COLOR, 3);
                // TIPS: facesArray[i].tl() is the top left of the
                // rectangle, facesArray[i].br()is the bottom right of the
                // rectangle.


                //change start
                //处理已框出的行人
                //process the selected pedestrians
                Rect newRect = pedestrianDetectOpt(tl, br, mRgba);
                //change end

                if (newRect != null && newRect.height > (br.y - tl.y) / 2) {
                    Core.rectangle(mRgba, new Point(tl.x + newRect.x, tl.y + newRect.y), new Point(tl.x + newRect.x + newRect.width, tl.y + newRect.y + newRect.height), new Scalar(0, 0, 0), 3);
                }
                Core.circle(mRgba, tl, 20, FACE_RECT_COLOR);
                Core.circle(mRgba, br, 20, FACE_RECT_COLOR);
                Log.d(TAG, " faces details: " + tl + "  " + br);

            }
            return mRgba;

        } else {
            return inputFrame;
        }
    }


    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    @Override
    public void onCameraViewStarted(int arg0, int arg1) {
        mGray = new Mat();
        mRgba = new Mat();

        initFacedetector();
    }

    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker mNativeDetector;

    private void initFacedetector() {
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(
                    R.raw.haarcascade_fullbody);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_fullbody.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(
                    mCascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                Log.i(TAG,
                        "Loaded cascade classifier from "
                                + mCascadeFile.getAbsolutePath());

            mNativeDetector = new DetectionBasedTracker(
                    mCascadeFile.getAbsolutePath(), 0);

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }


    //处理已框出的行人，优化
    //process the selected pedestrians, optimization
    public Rect pedestrianDetectOpt(Point tl, Point br, Mat source) {
        Mat rgbaInnerWindow = source.submat((int) (tl.y), (int) (br.y), (int) (tl.x), (int) (br.x));
        Mat mIntermediateMat = new Mat();
        //process function, rgbaInnerWindow处理后传递给mIntermediateMat即可
        mIntermediateMat = Preprocess.main(rgbaInnerWindow);
        //高斯滤波
        Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(3, 3), 10);
        //OSTU方法变为二值图
        Imgproc.threshold(mIntermediateMat, mIntermediateMat, 0, 255, Imgproc.THRESH_OTSU);
        //canny边缘算子
        Imgproc.Canny(mIntermediateMat, mIntermediateMat, 100, 180);
        //中值滤波方法，数字参数越大，滤波强度越大，只能取奇数
        Imgproc.medianBlur(mIntermediateMat, mIntermediateMat, 3);
        //寻找最大边界框的算法，即最小外接矩形
        Rect newRect = findContourProcess(mIntermediateMat);
        //Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
        //rgbaInnerWindow.release();
        return newRect;
    }

    @Override
    public void onBackPressed() {
        Thread.currentThread().interrupt();
        tts.shutdown();
        finish();
        super.onBackPressed();
    }

    //寻找最大边界框的算法，及最小外接矩形
    //优化考虑：当rect的面积小于一定值时，不考虑计入
    public Rect findContourProcess(Mat src) {

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.e("contours", contours.size() + "");

        src.setTo(new Scalar(0, 0, 0));

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Rect finalRect = null;

        for (int i = 0; i < contours.size(); i++) {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());


            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            if (finalRect == null) {
                finalRect = rect.clone();
            } else {
                int tl_x = Math.min(finalRect.x, rect.x);
                int tl_y = Math.min(finalRect.y, rect.y);
                int br_x = Math.max(finalRect.x + finalRect.width, rect.x + rect.width);
                int br_y = Math.max(finalRect.y + finalRect.height, rect.y + rect.height);

                finalRect = new Rect(tl_x, tl_y, br_x - tl_x, br_y - tl_y);
            }


            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            //Core.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 180, 0, 255), 3);

        }

        //Core.rectangle(src, new Point(finalRect.x, finalRect.y), new Point(finalRect.x + finalRect.width, finalRect.y + finalRect.height), new Scalar(255, 180, 0, 255), 3);

        Log.e("finalRect", finalRect.toString());
        return finalRect;

    }

    @Override
    public void finish() {
        super.finish();
        Thread.currentThread().interrupt();
        //Thread.currentThread().stop();
        //tts.stop();
        //tts.shutdown();
    }
}
