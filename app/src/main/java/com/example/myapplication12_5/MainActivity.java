package com.example.myapplication12_5;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ubtrobot.mini.voice.VoiceListener;
import com.ubtrobot.mini.voice.VoicePool;

import org.json.*;

import static com.ubtrobot.commons.Priority.HIGH;
import static com.ubtrobot.commons.Priority.NORMAL;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private Camera camera;
    private boolean isPreviewing;
    private Button button;
    private Button button1;
    private ImageView picture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        button = findViewById(R.id.getpic);
        button1 = findViewById(R.id.playtts);
        picture = findViewById(R.id.picture);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //deleteFile();
                openCamera();
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoicePool.get().playTTs("你是猪", NORMAL, new VoiceListener() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(int i, String s) {

                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        //openCamera();
    }

    public void deleteFile() {
        File file = new File("/storage/emulated/0/ubtrobot/camera/");//获取SD卡指定路径
        File[] files = file.listFiles();//获取SD卡指定路径下的文件或者文件夹
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {//如果是文件直接删除
                File photoFile = new File(files[i].getPath());
                //Log.d("photoPath -->> ", photoFile.getPath());
                photoFile.delete();
            }
        }
    }

    public List<String> getPictures(final String strPath) {
        List<String> list = new ArrayList<String>();
        File file = new File(strPath);
        File[] allfiles = file.listFiles();
        if (allfiles == null) {
            return null;
        }
        for (int k = 0; k < allfiles.length; k++) {
            final File fi = allfiles[k];
            if (fi.isFile()) {
                int idx = fi.getPath().lastIndexOf(".");
                if (idx <= 0) {
                    continue;
                }
                String suffix = fi.getPath().substring(idx);
                if (suffix.toLowerCase().equals(".jpg") ||
                        suffix.toLowerCase().equals(".jpeg") ||
                        suffix.toLowerCase().equals(".bmp") ||
                        suffix.toLowerCase().equals(".png") ||
                        suffix.toLowerCase().equals(".gif")) {
                    list.add(fi.getPath());
                }
            }
        }
        return list;
    }


    /**
     * 初始化设置Camera.打开摄像头
     *
     * 默认是先打开前置摄像头，如果没有前置摄像头的话就打开后置摄像头
     *
     */
    @SuppressLint("NewApi")
    private void openCamera() {

        Camera.CameraInfo cameraInfo = new CameraInfo();
        //获得设备上的硬件camera数量
        int count = Camera.getNumberOfCameras();

        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    camera = Camera.open(i);    //尝试打开前置摄像头
                } catch (Exception e) {
                    System.out.println("打开qian摄像头异常" + e.toString());
                    e.printStackTrace();
                }
            }
        }

        if (camera == null) {
            for (int i = 0; i < count; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        camera = Camera.open(i);    //尝试打开后置摄像头
                    } catch (Exception e) {
                        System.out.println("打开后置摄像头异常" + e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            //			camera.setPreviewDisplay(surfaceHolder);
            if (camera != null) {
                camera.startPreview(); // 打开预览画面
                isPreviewing = true;
                camera.autoFocus(autoFocusCallback);    //自动聚焦
            } else {
                Toast.makeText(getApplicationContext(), "没有前置摄像头", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 自动对焦后拍照
     **/
    AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // TODO Auto-generated method stub

            System.out.println("自动对焦完成");
            System.out.println(success && camera != null);

            if (camera != null) {
                System.out.println("拍照");

                //设置回调，参数（快门，源数据，JPEG数据）
                camera.takePicture(null, null, new PictureCallback() {

                    @SuppressLint("SdCardPath")
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        // TODO Auto-generated method stub

                        //使用当前的时间拼凑图片的名称
                        String name = DateFormat.format("yyyy_MM_dd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".jpg";

                        File file = new File("/storage/emulated/0/ubtrobot/camera/");
                        //file.mkdirs(); //创建文件夹保存照片
                        String filename = file.getPath() + File.separator + name;
                        //System.out.println(filename);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        try {

                            FileOutputStream fileOutputStream = new FileOutputStream(filename);
                            boolean b = bitmap.compress(CompressFormat.JPEG, 100, fileOutputStream);
                            fileOutputStream.flush();
                            fileOutputStream.close();

                            if (b) {
                                Toast.makeText(getApplicationContext(), "照片保存成功", Toast.LENGTH_LONG).show();
                                final List<String> list = getPictures("/storage/emulated/0/ubtrobot/camera/");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            sendPic(list.get(list.size() - 1));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                                Canvas canvas = new Canvas();
                                Bitmap bm = BitmapFactory.decodeFile(list.get(list.size() - 1));
                                int top = 30;
                                canvas.drawBitmap(bm, 0, top, null);
                                picture.setImageBitmap(bm);
                            } else {
                                Toast.makeText(getApplicationContext(), "照片保存失败", Toast.LENGTH_LONG).show();
                            }

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            releaseCamera();//释放Camera
                        }
                    }
                });
            }
        }
    };

    /**
     * 释放摄像头资源
     */
    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(null);
                camera.stopPreview();
                camera.release();
                camera = null;
                isPreviewing = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendPic(String picpath) throws IOException {
        Socket s = new Socket("10.135.188.31", 12345);//建立服务
        FileInputStream fis = new FileInputStream(picpath);//读取图片
        OutputStream out = s.getOutputStream();//读到的写入
        byte[] b = new byte[1024];
        int len = 0;
        while ((len = fis.read(b)) != -1) {
            out.write(b, 0, len);
        }
        s.shutdownOutput();//标记结束
        InputStream in = s.getInputStream();//读服务端返回数据
        byte[] bin = new byte[1024];
        int num = in.read(bin);
        final String read_str =new String(bin, 0, num);
        Log.i("main",read_str);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObj = new JSONObject(read_str);
                    String age = (String) jsonObj.get("age");
                    VoicePool.get().playTTs(age,HIGH , new VoiceListener() {
                        @Override
                        public void onCompleted() {
                            Log.i("Main","tts Success");
                        }

                        @Override
                        public void onError(int i, String s) {
                            Log.i("Main","tts Failed");
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        fis.close();
        s.close();
    }
}
