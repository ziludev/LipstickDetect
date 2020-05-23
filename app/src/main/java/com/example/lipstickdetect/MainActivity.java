package com.example.lipstickdetect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int TAKE_PHOTO = 1;
    private static final int CHOOSE_PHOTO = 2;
    private static final String TAG = "opencv load";

    static {//加载so库
        System.loadLibrary("native-lib");
    }

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded！");
        }
    }

    Context context;
    private ImageView imageView;
    private Uri imageUri;
    private File outputImage;
    private Mat inputMat, outputMat;
    private Bitmap bitmap;
    AssetManager assetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        findViewById(R.id.camera).setOnClickListener(this);
        findViewById(R.id.album).setOnClickListener(this);
        findViewById(R.id.detect).setOnClickListener(this);
        context = getBaseContext();
        //获得assets资源管理器
        assetManager = context.getResources().getAssets();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.camera) {
//            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
//            imageView.setImageBitmap(bitmap);

            //创建File对象，用于存储拍照后的图片
            outputImage = new File(getExternalCacheDir(), "output_image.jpg");
            try {
                if (outputImage.exists()) {
                    outputImage.delete();
                }
                outputImage.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Build.VERSION.SDK_INT >= 24) {
                imageUri = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.lipstickdetect.fileprovider", outputImage);
            } else {
                imageUri = Uri.fromFile(outputImage);
            }
            //启动相机程序
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PHOTO);
        } else if (v.getId() == R.id.detect) {
            if (bitmap != null) {
                // 将Bitmap转化为Mat图片，并创建等大小的新的Bitmap
                inputMat = convertBitmap2Mat(bitmap);
                outputMat = new Mat(inputMat.rows(), inputMat.cols(), CvType.CV_8UC3);
                FaceDetect.loadShapePredictor(assetManager);
                String lipsColor = FaceDetect.LandmarkDetection(inputMat.getNativeObjAddr(), outputMat.getNativeObjAddr());
                // 将Mat转换为Bitmap，并进行显示
                Bitmap bitmapOutput = convertMat2Bitmap(outputMat);
                imageView.setImageBitmap(bitmapOutput);
                json2Bean(lipsColor);
            } else {
                Toast.makeText(MainActivity.this, "没有选择照片", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                openAlbum();
            }

        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        //使用应用本地native库
//        OpenCVLoader.initDebug();
//    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);//打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        //将拍摄的照片显示出来
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        imageView.setImageBitmap(rotateIfRequired(bitmap));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    //判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        //4.4及以上系统使用这个方法处理图片
                        handleImageOnKitKat(data);
                    } else {
                        //4.4以下系统使用这个方法处理图片
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型Uri，则通过document id 处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath);//根据图片路径显示图片
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if ((cursor != null)) {
            if ((cursor.moveToNext())) {
                path = cursor.getString((cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            bitmap = BitmapFactory.decodeFile(imagePath);
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "图片获取失败", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * 解决部分手机拍照方向的问题
     * */
    public Bitmap rotateIfRequired(Bitmap bitmap) {
        try {
            ExifInterface exif = new ExifInterface(outputImage.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotateBitmap(90, bitmap);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotateBitmap(180, bitmap);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotateBitmap(270, bitmap);
                    break;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public Bitmap rotateBitmap(int degree, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();   //回收不再需要的Bitmap
        return rotatedBitmap;
    }

    // Mat转换成Bitmap
    Bitmap convertMat2Bitmap(Mat img) {
        int width = img.width();
        int height = img.height();
        Bitmap bmp;
        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Mat tmp;
        tmp = img.channels() == 1 ? new Mat(width, height, CvType.CV_8UC1, new Scalar(1)) : new Mat(width, height, CvType.CV_8UC3, new Scalar(3));
        try {
            if (img.channels() == 3) {
                Imgproc.cvtColor(img, tmp, Imgproc.COLOR_RGB2BGRA);
            } else if (img.channels() == 1) {
                Imgproc.cvtColor(img, tmp, Imgproc.COLOR_GRAY2BGRA);
            }
            Utils.matToBitmap(tmp, bmp);
        } catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }
        return bmp;
    }

    //bitmap转换成Mat
    Mat convertBitmap2Mat(Bitmap rgbaImg) {
        Mat rgbaMat = new Mat(rgbaImg.getHeight(), rgbaImg.getWidth(), CvType.CV_8UC4);
        Bitmap bmp32 = rgbaImg.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, rgbaMat);
        Mat rgbNewMat = new Mat(rgbaImg.getHeight(), rgbaImg.getWidth(), CvType.CV_8UC3);
        Imgproc.cvtColor(rgbaMat, rgbNewMat, Imgproc.COLOR_RGB2BGR, 3);
        return rgbNewMat;
    }

    // 获取json转换成字符串
    public String getJson(Context context, String fileName) {

        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获得assets资源管理器
            AssetManager assetManager = context.getResources().getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName), "utf-8"));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    // 使用Gson将json字符串变成Bean类
    public void json2Bean(String lipsColor) {
        String result = "检测的颜色:"+lipsColor;
        // 将json变成字符串
        String jsonStr = getJson(context, "lipstick.json");
        // 使用Gson解析
        Gson gson = new Gson();
        Lipstick lipstick = gson.fromJson(jsonStr, Lipstick.class);

        ArrayList<Lipstick.BrandsInfo> brands = lipstick.brands;
        for (Lipstick.BrandsInfo brandsInfo : brands) {
            String brands_name = brandsInfo.name;
            ArrayList<Lipstick.BrandsInfo.SeriesInfo> series = brandsInfo.series;
            for (Lipstick.BrandsInfo.SeriesInfo seriesInfo : series) {
                String series_name = seriesInfo.name;
                ArrayList<Lipstick.BrandsInfo.SeriesInfo.LipstickInfo> lipsticks = seriesInfo.lipsticks;
                for (Lipstick.BrandsInfo.SeriesInfo.LipstickInfo lipstickInfo : lipsticks) {
                    String id = lipstickInfo.id;
                    String color = lipstickInfo.color;
                    String name = lipstickInfo.name;
                    if (SimilarColor(lipsColor,color)) {
                        result += "\n相似的口红:\n品牌:"+brands_name+",系列:"+series_name+",色号:"+id+",名字:"+name+",颜色:"+color;
                    }
                    else
                        result += "";
                }
            }
        }

        // 提示识别结果
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("识别结果");
        if (result.length() != 13) {
            builder.setMessage(result);
        }
        else {
            builder.setMessage(result+"\n没有相似的口红颜色");
        }
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    // 颜色相似度比较
    public boolean SimilarColor(String detectColor, String originColor) {
        boolean isSimilar = false;
        Integer decR = Integer.parseInt(detectColor.substring(1, 3),16);
        Integer decG = Integer.parseInt(detectColor.substring(3, 5),16);
        Integer decB = Integer.parseInt(detectColor.substring(5, 7),16);
        Integer orgR = Integer.parseInt(originColor.substring(1, 3),16);
        Integer orgG = Integer.parseInt(originColor.substring(3, 5),16);
        Integer orgB = Integer.parseInt(originColor.substring(5, 7),16);

        // 判断相似度
        isSimilar = -(decR - orgR) * (decR - orgR) - (decG - orgG) * (decG - orgG) - (decB - orgB) * (decB - orgB) > -73;

        return isSimilar;
    }
}
