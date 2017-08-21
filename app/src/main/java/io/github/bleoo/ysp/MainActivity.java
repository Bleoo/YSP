package io.github.bleoo.ysp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.blankj.utilcode.util.FileIOUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.view.OrientationEventListener.ORIENTATION_UNKNOWN;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    SurfaceView surfaceView;
    View iv_take_picture;

    Camera camera;
    SurfaceHolder surfaceHolder;
    boolean isPreview;
    MyOrientationEventListener orientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        iv_take_picture = findViewById(R.id.iv_take_picture);
        surfaceView.setOnClickListener(this);
        iv_take_picture.setOnClickListener(this);

        MainActivityPermissionsDispatcher.showCameraWithCheck(this);
        orientationEventListener = new MyOrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(orientationEventListener.canDetectOrientation()){
            orientationEventListener.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationEventListener.disable();
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showCamera() {
        Log.e("CAMERA", "用户允许该权限");
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setFixedSize(400, 300);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.e("surfaceCreated", "");
                camera = Camera.open(0);
                Camera.Parameters parameters = camera.getParameters();
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
                camera.setParameters(parameters);
                CameraUtil.setCameraDisplayOrientation(MainActivity.this, 0, camera);
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.startPreview();
                isPreview = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.e("surfaceChanged", "");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.e("surfaceDestroyed", "");
                if (camera != null) {
                    // 若摄像头正在工作，先停止它
                    camera.stopPreview();
                    //如果注册了此回调，在release之前调用，否则release之后还回调，crash
                    camera.setPreviewCallback(null);
                    camera.release();
                    camera = null;
                }
                isPreview = false;
            }
        });

    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton("允许", (dialog, which) -> request.proceed())
                .setNegativeButton("拒绝", (dialog, which) -> request.cancel())
                .setCancelable(false)
                .setMessage("是否允许相机权限？")
                .show();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showDeniedForCamera() {
        Log.e("permissions", "用户不授予某权限");
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showNeverAskForCamera() {
        Log.e("permissions", "不再询问");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        // 此处回调处理
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.surface_view:
                if (camera != null && isPreview) {
                    camera.autoFocus(null);
                }
                break;
            case R.id.iv_take_picture:
                if (camera != null && isPreview) {
                    camera.takePicture(null, null, (bytes, camera1) -> {
                        new Thread(() -> FileIOUtils.writeFileFromBytesByStream(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                                        + File.separator + SIMPLE_DATE_FORMAT.format(new Date()) + ".jpeg",
                                bytes)).start();
                        surfaceView.postDelayed(() -> camera.startPreview(), 1500);
                    });
                }
        }
    }

    private class MyOrientationEventListener extends OrientationEventListener {

        public MyOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN || camera == null) return;
            Camera.Parameters parameters = camera.getParameters();
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
            camera.setParameters(parameters);
        }
    }

}
