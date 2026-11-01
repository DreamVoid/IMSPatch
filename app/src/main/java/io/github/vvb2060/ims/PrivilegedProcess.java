package io.github.vvb2060.ims;

import android.annotation.SuppressLint;
import android.app.IActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.ServiceManager;
import android.system.Os;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.ICarrierConfigLoader;
import me.dreamvoid.imspatch.BuildConfig;
import rikka.shizuku.ShizukuBinderWrapper;

public class PrivilegedProcess extends Instrumentation {
    private static final String KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL =
            "show_wifi_calling_icon_in_status_bar_bool";
    private static final String KEY_WFC_SPN_FORMAT_IDX_INT = "wfc_spn_format_idx_int";

    @Override
    public void onCreate(Bundle arguments) {
        try {
            overrideConfig();
        } catch (Exception e) {
            Log.e(PrivilegedProcess.class.getSimpleName(), Log.getStackTraceString(e));
        }
        finish(0, new Bundle());
    }

    private void overrideConfig() throws Exception {
        var binder = ServiceManager.getService(Context.ACTIVITY_SERVICE);
        var am = IActivityManager.Stub.asInterface(new ShizukuBinderWrapper(binder));
        am.startDelegateShellPermissionIdentity(Os.getuid(), null);
        try {
            var cm = getContext().getSystemService(CarrierConfigManager.class);
            var sm = getContext().getSystemService(SubscriptionManager.class);
            var ccb = ServiceManager.getService(Context.CARRIER_CONFIG_SERVICE);
            var ccl = ICarrierConfigLoader.Stub.asInterface(new ShizukuBinderWrapper(ccb));
            var values = getConfig();
            var activeSubscriptions = sm.getActiveSubscriptionInfoList();
            if (activeSubscriptions == null) {
                return;
            }
            for (SubscriptionInfo info : activeSubscriptions) {
                var subId = info.getSubscriptionId();
                @SuppressLint("MissingPermission") PersistableBundle bundle = cm.getConfigForSubId(subId, "vvb2060_config_version");
                if (bundle.getInt("vvb2060_config_version", 0) != BuildConfig.VERSION_CODE) {
                    values.putInt("vvb2060_config_version", BuildConfig.VERSION_CODE);
                    ccl.overrideConfig(subId, values, false);
                }
            }
        } finally {
            am.stopDelegateShellPermissionIdentity();
        }
    }

    private static PersistableBundle getConfig() {
        var bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true);

        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL, true);

        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);
        bundle.putBoolean(KEY_SHOW_WIFI_CALLING_ICON_IN_STATUS_BAR_BOOL, true);
        bundle.putInt(KEY_WFC_SPN_FORMAT_IDX_INT, 6);

        bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false);
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false);

        bundle.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true);
        bundle.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                new int[]{CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA});
        bundle.putIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                // Boundaries: [-140 dBm, -44 dBm]
                new int[]{
                        -128, /* SIGNAL_STRENGTH_POOR */
                        -118, /* SIGNAL_STRENGTH_MODERATE */
                        -108, /* SIGNAL_STRENGTH_GOOD */
                        -98,  /* SIGNAL_STRENGTH_GREAT */
                });
        return bundle;
    }
}
