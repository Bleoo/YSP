package io.github.bleoo.ysp;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    View iv_take_picture;
    FrameLayout surfaceLayout;
    Camera1 camera1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceLayout = (FrameLayout) findViewById(R.id.surface_view);
        iv_take_picture = findViewById(R.id.iv_take_picture);
        surfaceLayout.setOnClickListener(this);
        iv_take_picture.setOnClickListener(this);

        camera1 = new Camera1(this);
        surfaceLayout.addView(camera1.mSurfaceView);
        MainActivityPermissionsDispatcher.showCameraWithCheck(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera1.open(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera1.release();
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showCamera() {
        Log.e("CAMERA", "用户允许该权限");
        camera1.open(0);
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
                camera1.focus();
                break;
            case R.id.iv_take_picture:
                camera1.takePicture();
                break;
        }
    }

}
