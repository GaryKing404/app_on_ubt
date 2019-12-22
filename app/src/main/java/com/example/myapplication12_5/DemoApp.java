package com.example.myapplication12_5;

import android.app.Application;

import com.ubtrobot.mini.SDKInit;
import com.ubtrobot.mini.properties.sdk.Path;
import com.ubtrobot.mini.properties.sdk.PropertiesApi;


public class DemoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PropertiesApi.setRootPath(Path.DIR_MINI_FILES_SDCARD_ROOT);
        SDKInit.initialize(this);
    }

}
