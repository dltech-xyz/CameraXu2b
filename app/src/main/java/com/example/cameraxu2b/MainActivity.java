package com.example.cameraxu2b;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 101;//arbitrary number, can be changed accordingly
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    TextureView  textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();//hide action bar

        textureView = (TextureView) findViewById(R.id.view_finder);

        if(allPermissionGranted()){

            startCamera();//start camera if permission has been granted by user
        }
        else {

            ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        //make sure there isn't another camera instance running before starting
        CameraX.unbindAll();

        /* start preview */
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());//aspect ratio
        Size screen = new Size(textureView.getWidth(), textureView.getHeight());//size of the screen

        //config obj for preview/viewfinder thingy.
        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);//lets build it

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we have to destroy it first, then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {

                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                }
        );

        /* image capture */

        //config obj, selected capture mode
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).
                setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imageCapture).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                File file = new File( Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".jpg");//"sdcard/photos/DCIM(0)/Camera/CameraX "
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
//                        super.onCaptureSuccess(image, rotationDegrees);
                        String msg = "Pic captured at "+ file.getAbsolutePath();
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
//                        super.onError(imageCaptureError, message, cause);
                        String msg = "Pic captured failed:" + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();

                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });
        /* image analyser */

//        ImageAnalysisConfig imgAConfig = new ImageAnalysisConfig.Builder().setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE).build();
//        ImageAnalysis analysis = new ImageAnalysis(imgAConfig);

//        analysis.setAnalyzer(
//                new ImageAnalysis.Analyzer(){
//                    @Override
//                    public void analyze(ImageProxy image, int rotationDegrees){
//                        //y'all can add code to analyse stuff here idek go wild.
//                    }
//                });
        //bind to lifecycle:
        CameraX.bindToLifecycle(this, preview, imgCap);
    }

    private void updateTransform() {
        /*
         * compensates the changes in orientation for the viewfinder, bc the rest of the layout stays in portrait mode.
         * methinks :thonk:
         * imgCap does this already, this class can be commented out or be used to optimise the preview
         */

        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;//calc centre of the viewfinder
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();//cast to int bc switches don't like floats

        switch(rotation){//correct output to account for display rotation
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = -90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = -180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = -270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);//apply transformations to textureview


    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        //start camera when permissions have been granted otherwise exit app
//        if(requestCode == REQUEST_CODE_PERMISSIONS){
//            if(allPermissionsGranted()){
//                startCamera();
//            } else{
//                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        }
//    }

    private boolean allPermissionGranted() {
        //check if req permissions have been granted
        for(String permission : REQUIRED_PERMISSIONS){

            if(ContextCompat.checkSelfPermission( this, permission) != PackageManager.PERMISSION_GRANTED){

                return false;
            }
        }
        return true;
    }

//    @Override
//    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//
//    }
//
//    @Override
//    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//
//    }
//
//    @Override
//    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        return false;
//    }
//
//    @Override
//    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//    }
//    //array w/ permissions from manifest

}
