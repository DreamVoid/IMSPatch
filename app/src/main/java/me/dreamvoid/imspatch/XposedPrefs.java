package me.dreamvoid.imspatch;

import android.content.SharedPreferences;

public final class XposedPrefs {
    public static final String GROUP = "settings";

    public static final String KEY_MASTER = "master_enabled";
    public static final String KEY_CARRIER_CONFIG = "carrier_config_enabled";
    public static final String KEY_PLATFORM_CHECKS = "platform_checks_bypass";
    public static final String KEY_PROVISIONING_CHECKS = "provisioning_checks_bypass";
    public static final String KEY_USER_SETTINGS = "user_settings_force";

    private XposedPrefs() {
    }

    /**
     * 判断开关是否开启。
     *
     * @param prefs remote preferences，可为 {@code null}
     * @param key   分组开关键
     * @return 是否启用
     */
    public static boolean isEnabled(SharedPreferences prefs, String key) {
        return prefs != null && prefs.getBoolean(KEY_MASTER, false) && prefs.getBoolean(key, false);
    }
}
