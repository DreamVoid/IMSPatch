package me.dreamvoid.imspatch;

import android.app.Application;

public class IMSApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        XposedStatus.register();
    }
}
