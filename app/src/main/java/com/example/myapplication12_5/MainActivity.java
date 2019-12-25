package com.example.myapplication12_5;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ubtrobot.mini.properties.sdk.PropertiesApi;
import com.ubtrobot.mini.voice.VoiceListener;
import com.ubtrobot.mini.voice.VoicePool;
import com.ubtrobot.speech.SpeechApiExtra;
import com.ubtrobot.speech.asr.AsrRequest;

import org.json.*;

import static android.media.AudioRecord.getMinBufferSize;
import static com.ubtrobot.commons.Priority.HIGH;
import static com.ubtrobot.commons.Priority.NORMAL;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private Camera camera;
    private boolean isPreviewing;
    private Button switcherBtn;
    private TextView resultTv;

    String switchText1 = "开始录音识别";
    String switchText2 = "结束录音识别";

    String prefixResult = "识别结果为: \n";
    AudioHandler mAudioHandler;

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
        switcherBtn = (Button) findViewById(R.id.switch_btn);
        resultTv = (TextView) findViewById(R.id.result_tv);
        switcherBtn.setText(switchText1);
        mAudioHandler = new AudioHandler();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //deleteFile();
                openCamera();
            }
        });

        switcherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence text = switcherBtn.getText().toString().trim();
                boolean asrStart = TextUtils.equals(switchText1, text);
                mAudioHandler.setSwitch(asrStart);
                if (asrStart) {
                    switcherBtn.setText(switchText2);
                } else {
                    switcherBtn.setText(switchText1);
                }
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
     * <p>
     * 默认是先打开前置摄像头，如果没有前置摄像头的话就打开后置摄像头
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

    class AudioHandler implements Runnable {

        private final int minBufferSize;
        private AudioRecord audioRecord;
        private volatile File outputFile;
        private volatile FileOutputStream os;
        private volatile byte[] buffer;

        AudioHandler() {
            minBufferSize = getMinBufferSize(16000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = createRecorder();
        }

        private AudioRecord createRecorder() {
            return new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        }

        boolean switcher;

        synchronized void setSwitch(boolean switcher) {
            this.switcher = switcher;
            if (switcher) {
                outputFile = createOutputFile();
                attachOutput(outputFile);
                if (audioRecord == null) {
                    audioRecord = createRecorder();
                }
                new Thread(this).start();
            } else {
                //识别
                SpeechApiExtra speechApiExtra = SpeechApiExtra.get();
                if (outputFile != null && outputFile.length() > 0) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            speechApiExtra.beginAsrSession(AsrRequest.SampleRate.Rate16K, 2);
                            speechApiExtra.asr(readFile2Bytes(outputFile));
                            final String result = speechApiExtra.endAsrSession();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultTv.setText(prefixResult + result);
                                    if (result == "") {
                                        VoicePool.get().playTTs("没听清楚，请再说一次", HIGH, new VoiceListener() {
                                            @Override
                                            public void onCompleted() {

                                            }

                                            @Override
                                            public void onError(int i, String s) {

                                            }
                                        });
                                    } else {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    getByName(result);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }).start();
                                    }
                                }
                            });
                        }
                    }).start();

                }
            }
        }

        public byte[] readFile2Bytes(File file) {
            if (file == null) {
                return null;
            } else {
                try {
                    return inputStream2Bytes(new FileInputStream(file));
                } catch (FileNotFoundException var2) {
                    var2.printStackTrace();
                    return null;
                }
            }
        }

        private byte[] inputStream2Bytes(InputStream is) {
            return is == null ? null : input2OutputStream(is).toByteArray();
        }

        private ByteArrayOutputStream input2OutputStream(InputStream is) {
            if (is == null) {
                return null;
            } else {
                Object var2;
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] b = new byte[1024];

                    int len;
                    while ((len = is.read(b, 0, 1024)) != -1) {
                        os.write(b, 0, len);
                    }

                    ByteArrayOutputStream var4 = os;
                    return var4;
                } catch (IOException var8) {
                    var8.printStackTrace();
                    var2 = null;
                } finally {

                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return (ByteArrayOutputStream) var2;
            }
        }


        public synchronized void attachOutput(File output) {
            if (os != null) {
                closeQuiet(os);
            }
            try {
                this.os = new FileOutputStream(output, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("AudioHandler", "FileNotFoundException: " + e.getMessage());
            }
            if (this.os != null) {
                buffer = new byte[minBufferSize];
            }
        }

        @Override
        public void run() {
            while (switcher) {
                if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording();
                    if (readError()) {
                        Log.e("AudioHandler", "read data error!");
                        audioRecord.release();
                        audioRecord = null;
                        setSwitch(false);
                        closeQuiet(os);
                    } else {
                        writeSafe(os, buffer);
                    }
                }

            }
        }


        private boolean readError() {
            return audioRecord == null || audioRecord.read(buffer, 0, minBufferSize) < AudioRecord.SUCCESS;
        }

        private void writeSafe(FileOutputStream os, byte[] data) {
            if (os != null && data != null) {
                try {
                    os.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeQuiet(FileOutputStream os) {
            if (os != null) {
                try {
                    os.close();
                    os = null;
                } catch (
                        IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String buildRecordCachePath(String id) {
            return PropertiesApi.getCustomizeMusicDir() + File.separator + id + ".pcm";
        }


        private File createOutputFile() {
            String id = "demo_record_" + System.currentTimeMillis();
            final File file = new File(buildRecordCachePath(id));
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return file;
        }
    }

    public void sendPic(String picpath) throws IOException {
        Socket s = new Socket("10.128.63.126", 12345);//建立服务
        FileInputStream fis = new FileInputStream(picpath);//读取图片
        OutputStream out = s.getOutputStream();//读到的写入
        byte[] b = new byte[1024];
        int len = 0;
        out.write(1);
        while ((len = fis.read(b)) != -1) {
            out.write(b, 0, len);
        }
        s.shutdownOutput();//标记结束
        InputStream in = s.getInputStream();//读服务端返回数据
        byte[] bin = new byte[1024];
        int num = in.read(bin);
        final String read_str = new String(bin, 0, num);
        Log.i("main", read_str);
        fis.close();
        s.close();
    }

    public void getByName(String name) throws IOException {
        System.out.println("fffffffffffffffffffff" + name);
        Socket s = new Socket("10.128.63.126", 12345);//建立服务
        OutputStream out = s.getOutputStream();//读到的写入
        out.write(0);
        PrintWriter pw = new PrintWriter(out);
        pw.write(name);
        pw.flush();
        s.shutdownOutput();//标记结束
        InputStream in = s.getInputStream();//读服务端返回数据
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String reply = null;
        while (((reply = br.readLine()) == null)) {
            ;
        }
        String finalReply = reply;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(finalReply);
                    JSONObject jsonObj = new JSONObject(finalReply);
                    String position_x = (String) jsonObj.get("position_x");
                    String position_y = (String) jsonObj.get("position_y");
                    Log.i("ffffff", position_x + "___" + position_y);
                    sayWhere(name, 1, 2, 3, 4);
//                    VoicePool.get().playTTs(age,HIGH , new VoiceListener() {
//                        @Override
//                        public void onCompleted() {
//                            Log.i("Main","tts Success");
//                        }
//
//                        @Override
//                        public void onError(int i, String s) {
//                            Log.i("Main","tts Failed");
//                        }
//                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        s.close();
    }

    public void sayWhere(String name, int position1_x, int position1_y, int position2_x, int position2_y) {
        int position_x = (position1_x + position2_x) / 2;
        int position_y = (position1_y + position2_y) / 2;
        int x_one = 1, x_two = 2;
        int y_one = 3, y_two = 4;
        String location;
        if (position_x < x_one) {
            if (position_y < y_one) {
                location = "左上角";
            } else if (position_y > y_two) {
                location = "左下角";
            } else {
                location = "左侧";
            }
        } else if (position_x > x_two) {
            if (position_y < y_one) {
                location = "右上角";
            } else if (position_y > y_two) {
                location = "右下角";
            } else {
                location = "右侧";
            }
        } else {
            if (position_y < y_one) {
                location = "上方";
            } else if (position_y > y_two) {
                location = "下方";
            } else {
                location = "中间";
            }
        }
        String result1 = name + "在桌面" + location;
        VoicePool.get().playTTs(result1, HIGH, new VoiceListener() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(int i, String s) {

            }
        });
    }
}

