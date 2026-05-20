package io.github.vvb2060.ims;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.UiAutomationConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.util.Log;
import me.dreamvoid.imspatch.BuildConfig;
import org.lsposed.hiddenapibypass.LSPass;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.sui.Sui;

public class ShizukuProvider extends rikka.shizuku.ShizukuProvider {
    @Override
    public boolean onCreate() {
        LSPass.setHiddenApiExemptions("");
        Sui.init(BuildConfig.APPLICATION_ID);
        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                startInstrument(ShizukuProvider.this.getContext());
            }
        });
        Shizuku.requestPermission(0);
        return super.onCreate();
    }

    private static void startInstrument(Context context) {
        try {
            var binder = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            var am = IActivityManager.Stub.asInterface(new ShizukuBinderWrapper(binder));
            var name = new ComponentName(context, PrivilegedProcess.class);
            var flags = ActivityManager.INSTR_FLAG_NO_RESTART;
            var connection = new UiAutomationConnection();
            am.startInstrumentation(name, null, flags, new Bundle(), null, connection, 0, null);
        } catch (Exception e) {
            Log.e(ShizukuProvider.class.getSimpleName(), Log.getStackTraceString(e));
        }
    }
}
