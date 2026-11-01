package me.dreamvoid.imspatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPackageManager().setComponentEnabledSetting(
                ComponentName.createRelative(getPackageName(), getPackageName() + ".LauncherActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, // 彻底禁用别名
                PackageManager.DONT_KILL_APP // 确保当前正在运行的进程不被系统强制杀死
        );
        finish();
    }
}