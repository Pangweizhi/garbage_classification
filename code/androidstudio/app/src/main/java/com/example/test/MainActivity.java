package com.example.test;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

public class MainActivity extends AppCompatActivity {

    // 定义日志标签，统一使用类名作为标签，方便日志查看和定位问题
    private static final String TAG = "CameraActivity";
    // 定义权限请求码常量，方便后续扩展和维护
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_ACTIVITY_RESULT_CODE = 1002;

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageButton button1;
    private ImageButton button2;
    private static final int AUTO_SWITCH_DELAY = 3000;
    private ImageView selectedImageView;
    private TextView textView;
    private ViewPager carouselViewPager;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        carouselViewPager = findViewById(R.id.carouselViewPager);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        selectedImageView = findViewById(R.id.selectedImageView);
        textView = findViewById(R.id.textView);

        int[] imageIds = {R.drawable.image1, R.drawable.image2, R.drawable.image3};
        CarouselAdapter adapter = new CarouselAdapter(this, imageIds);
        carouselViewPager.setAdapter(adapter);
        carouselViewPager.setCurrentItem(Integer.MAX_VALUE / 2);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 0) {
                    int currentItem = carouselViewPager.getCurrentItem();
                    carouselViewPager.setCurrentItem(currentItem + 1);
                    handler.sendEmptyMessageDelayed(0, AUTO_SWITCH_DELAY);
                }
                return false;
            }
        });

        handler.sendEmptyMessageDelayed(0, AUTO_SWITCH_DELAY);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermission();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    // 请求相机权限的方法
    private void requestCameraPermission() {
        // 检查相机权限是否已授予
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "相机权限未授予，请求权限");
            // 显示权限请求解释对话框，向用户说明为什么需要相机权限（这里可根据实际情况完善解释内容）
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);

            // 请求相机权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            takePhoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "处理权限请求结果");
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，执行拍照操作
                takePhoto();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "拍照需要相机权限，请授予权限。", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 启动相机并拍照的方法
    private void takePhoto() {
        Log.e(TAG, "启动相机拍照");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, CAMERA_ACTIVITY_RESULT_CODE);
        } else {
            Log.e(TAG, "没有找到可处理拍照意图的应用程序");
            Toast.makeText(this, "无法启动相机，请检查设备设置。", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            // 获取图片的实际路径
            String imagePath = getPathFromUri(selectedImageUri);
            try {
                Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImageUri));
                Bitmap scaledBitmap = getScaledBitmap(originalBitmap, selectedImageView.getWidth(), selectedImageView.getHeight());
                selectedImageView.setImageBitmap(scaledBitmap);
                // 调用getOnnx方法并传递图片路径
                getOnnx(imagePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (requestCode == CAMERA_ACTIVITY_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                // 获取图片的实际路径
//                String imagePath = getPathFromUri(selectedImageUri);

                Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey("data")) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    selectedImageView.setImageBitmap(imageBitmap);
                    // 调用getOnnx方法并传递图片路径
//                    getOnnx(imagePath);
                } else {
                    try {
                        Uri photoUri = data.getData();// 获取照片的URI
                        if (photoUri != null) {
                            Bitmap capturedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                            selectedImageView.setImageBitmap(capturedBitmap);// 显示图片
                            // 调用getOnnx方法并传递图片路径
//                            getOnnx(imagePath);
                        } else {
                            Log.e(TAG, "照片的URI为空，无法显示图片");
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                // 用户取消了拍照
                Log.i(TAG, "用户取消了拍照操作");
            } else {
                // 其他错误处理
                Log.e(TAG, "意外的结果码：" + resultCode);
            }
        }
    }

    private Bitmap getScaledBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        Matrix matrix = new Matrix();
        float scaleX = (float) targetWidth / bitmap.getWidth();
        float scaleY = (float) targetHeight / bitmap.getHeight();
        matrix.postScale(scaleX, scaleY);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeMessages(0); // 当Activity暂停时，停止自动轮播
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.sendEmptyMessageDelayed(0, AUTO_SWITCH_DELAY); // 当Activity恢复时，重新开始自动轮播
    }

    private void getOnnx(String imagePath) {
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        AssetManager assetManager = getAssets();
        try {
            // 创建会话
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // 读取模型
            InputStream stream = assetManager.open("best.onnx");
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                byteStream.write(buffer, 0, bytesRead);
            }

            byteStream.flush();
            byte[] bytes = byteStream.toByteArray();
            OrtSession session = environment.createSession(bytes, options);

            // 读取图片文件并转换为模型输入格式
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);
            float[] inputArray = new float[640 * 640 * 3]; // 假设是RGB三通道
            for (int i = 0; i < resizedBitmap.getWidth(); i++) {
                for (int j = 0; j < resizedBitmap.getHeight(); j++) {
                    int pixel = resizedBitmap.getPixel(i, j);
                    inputArray[(i * resizedBitmap.getWidth() + j) * 3] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                    inputArray[(i * resizedBitmap.getWidth() + j) * 3 + 1] = ((pixel >> 8) & 0xFF) / 255.0f; // G
                    inputArray[(i * resizedBitmap.getWidth() + j) * 3 + 2] = (pixel & 0xFF) / 255.0f; // B
                }
            }
            resizedBitmap.recycle();

            // 准备输入数据
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputArray), new long[]{1, 3, 640, 640});

            // 准备数据
            Map<String, OnnxTensor> map = new HashMap<>();
            map.put("images", inputTensor); // 使用模型的实际输入节点名

            // 运行推理
            OrtSession.Result result = session.run(map);

            // 获取输出结果
            float[][] outputArray = null;
            for (Map.Entry<String, OnnxValue> entry : result) {
                OnnxValue value = entry.getValue();
                if (value instanceof OnnxTensor) {
                    OnnxTensor tensor = (OnnxTensor) value;
                    outputArray = (float[][]) tensor.getValue();
                    break;
                }
            }
            // 打印原始输出结果
            if (outputArray != null) {
                for (float[] row : outputArray) {
                    Log.i("ew", Arrays.toString(row));
                }
            }
            // 处理输出结果，找到概率最高的类别
            String[]垃圾分类 = {"厨余垃圾", "可回收物", "其他垃圾", "有害垃圾"};
            float maxProb = 0;
            String resultClass = "";
            for (int i = 0; i < outputArray.length; i++) {
                if (outputArray[i][0] > maxProb) {
                    maxProb = outputArray[i][0];
                    resultClass = 垃圾分类[i];
                }
            }

            session.close();
            textView.setText(resultClass);
        } catch (IOException | OrtException e) {
            throw new RuntimeException(e);
        }
    }

    // 辅助方法，用于从Uri获取图片的实际路径
    private String getPathFromUri(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to get path from URI", e);
        }
        return null;
    }
}