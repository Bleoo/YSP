package io.github.bleoo.ysp;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.blankj.utilcode.util.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by bleoo on 2017/8/22.
 */

public class Camera1 implements SurfaceHolder.Callback {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    Activity mActivity;
    Camera mCamera;
    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    boolean isPreview;
    MyOrientationEventListener mOrientationEventListener;
    MyFaceDetectionListener mFaceDetectionListener;

    public Camera1(Activity activity){
        mActivity = activity;
        mOrientationEventListener = new MyOrientationEventListener(mActivity, SensorManager.SENSOR_DELAY_NORMAL);
        mSurfaceView = new SurfaceView(mActivity);
        mSurfaceView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mSurfaceHolder = mSurfaceView.getHolder();
    }

    public void open(int CameraNO){
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
        if (mCamera == null) {
            mCamera = Camera.open(CameraNO);
            mSurfaceHolder.addCallback(this);
        }
        if (!isPreview) {
            mCamera.startPreview();
//            startFaceDetection();
            isPreview = true;
        }
    }

    public void release() {
        mOrientationEventListener.disable();
        if (mCamera != null) {
            // 若摄像头正在工作，先停止它
            mCamera.stopPreview();
            //如果注册了此回调，在release之前调用，否则release之后还回调，crash
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        isPreview = false;
    }

    public void focus(){
        if (mCamera != null && isPreview) {
            mCamera.autoFocus(null);
        }
    }

    public void takePicture(){
        if (mCamera != null && isPreview) {
            mCamera.takePicture(null, null, (bytes, camera1) -> {
                new Thread(() -> FileIOUtils.writeFileFromBytesByStream(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                                + File.separator + SIMPLE_DATE_FORMAT.format(new Date()) + ".jpeg",
                        bytes)).start();
                mSurfaceView.postDelayed(() -> mCamera.startPreview(), 1500);
            });
        }
    }

    public void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            if (mFaceDetectionListener == null) {
                mFaceDetectionListener = new MyFaceDetectionListener();
            }
            mCamera.setFaceDetectionListener(mFaceDetectionListener);

            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Camera.Parameters parameters = mCamera.getParameters();
        // 选择合适的预览尺寸
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        int previewWidth = sizeList.get(0).width;
        int previewHeight = sizeList.get(0).height;
        // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
        if (sizeList.size() > 1) {
            Iterator<Camera.Size> itor = sizeList.iterator();
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();
                if (cur.width >= previewWidth
                        && cur.height >= previewHeight) {
                    previewWidth = cur.width;
                    previewHeight = cur.height;
                }
            }
        }
        parameters.setPreviewSize(previewWidth, previewHeight); //获得摄像区域的大小
        parameters.setPictureSize(previewWidth, previewHeight);
        // 每秒从摄像头捕获帧画面
        parameters.setPreviewFrameRate(30);
        // 设置照片的输出格式 jpg  照片质量
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setJpegQuality(85);
        parameters.setRotation(270);
        mCamera.setParameters(parameters);
        CameraUtil.setCameraDisplayOrientation(mActivity, 0, mCamera);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mCamera.startFaceDetection();
        isPreview = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        release();
    }

    /**
     * 方向传感器事件监听器
     */
    private class MyOrientationEventListener extends OrientationEventListener {

        public MyOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN || mCamera == null) return;
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(0, info);
            orientation = (orientation + 45) / 90 * 90;
            int rotation = 0;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + orientation) % 360;
            }
            parameters.setRotation(rotation);
            mCamera.setParameters(parameters);
        }
    }

    /**
     * 人脸识别监听器
     */
    private class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Log.d("FaceDetection", "face detected: " + faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY());
            }
        }
    }
}
