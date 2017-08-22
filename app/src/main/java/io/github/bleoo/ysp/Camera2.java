package io.github.bleoo.ysp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;

/**
 * Created by bleoo on 2017/8/22.
 */

@TargetApi(Build.VERSION_CODES.M)
public class Camera2 {

    public static final String TAG = "Camera2";

    Activity mActivity;
    CameraManager mCameraManager;
    CameraDevice mCamera;
    CaptureRequest.Builder mPreviewRequestBuilder;
    CameraCaptureSession mCaptureSession;
    TextureView mTextureView;
    Surface mSurface;

    public Camera2(Activity activity) {
        mActivity = activity;
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] idlist = mCameraManager.getCameraIdList();
            if (idlist.length == 0) {
                return;
            }
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                mCameraManager.openCamera(idlist[0], mCameraDeviceCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            try {
                mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mSurface = new Surface(mTextureView.getSurfaceTexture());
                mPreviewRequestBuilder.addTarget(mSurface);
                mCamera.createCaptureSession(Arrays.asList(mSurface),
                        mSessionCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    private final CameraCaptureSession.StateCallback mSessionCallback
            = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (mCamera == null) {
                return;
            }
            mCaptureSession = session;
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to start camera preview.", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure capture session.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            if (mCaptureSession != null && mCaptureSession.equals(session)) {
                mCaptureSession = null;
            }
        }

    };
}
