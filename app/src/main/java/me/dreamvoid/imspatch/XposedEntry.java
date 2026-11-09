package me.dreamvoid.imspatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.telephony.SubscriptionManager;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("PrivateApi")
public final class XposedEntry extends XposedModule {
    private static final String TAG = "IMSPatch";
    private static final Class<?>[] NO_PARAMS = new Class<?>[0];

    private final AtomicBoolean carrierHookLogged = new AtomicBoolean();
    private volatile SharedPreferences prefs;
    private volatile boolean prefsLoaded;

    public XposedEntry() {
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!"com.android.phone".equals(param.getPackageName())) {
            return;
        }
        log(Log.INFO, TAG, "Package ready: " + param.getPackageName());
        ClassLoader classLoader = param.getClassLoader();
        installCarrierConfigHook(classLoader);
        installImsManagerHooks(classLoader);
    }

    private void installCarrierConfigHook(ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName("com.android.phone.CarrierConfigLoader", true, classLoader);
            Method method = clazz.getDeclaredMethod("getConfigForSubIdWithFeature", int.class, String.class, String.class);
            hook(method).setId("carrier-config").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain -> {
                Object result = chain.proceed();
                if (!(result instanceof PersistableBundle config)) {
                    return result;
                }
                if (!XposedPrefs.isEnabled(prefs(), XposedPrefs.KEY_CARRIER_CONFIG)) {
                    return result;
                }
                int subId = (Integer) chain.getArg(0);
                if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                    return result;
                }
                if (config.isEmpty()) {
                    return result;
                }
                config.putAll(CarrierConfigOverrides.newBundle());
                if (carrierHookLogged.compareAndSet(false, true)) {
                    log(Log.INFO, TAG, "CarrierConfig override applied, subId=" + subId);
                }
                return config;
            });
            log(Log.INFO, TAG, "Hooked CarrierConfigLoader.getConfigForSubIdWithFeature");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook CarrierConfigLoader", t);
        }
    }

    private void installImsManagerHooks(ClassLoader classLoader) {
        Class<?> imsManager;
        try {
            imsManager = Class.forName("com.android.ims.ImsManager", true, classLoader);
        } catch (Throwable t) {
            log(Log.WARN, TAG, "com.android.ims.ImsManager not found, skipped");
            return;
        }

        hookPlatformChecks(imsManager);
        hookProvisioningChecks(imsManager);
        hookUserSettings(imsManager);
        deoptimizeCallers(imsManager, classLoader);
    }

    private void hookPlatformChecks(Class<?> clazz) {
        String group = XposedPrefs.KEY_PLATFORM_CHECKS;
        hookBoolean(clazz, "isVolteEnabledByPlatform", NO_PARAMS, false, group, "ims-platform-volte");
        hookBoolean(clazz, "isVtEnabledByPlatform", NO_PARAMS, false, group, "ims-platform-vt");
        hookBoolean(clazz, "isWfcEnabledByPlatform", NO_PARAMS, false, group, "ims-platform-wfc");
        hookBoolean(clazz, "isImsOverNrEnabledByPlatform", NO_PARAMS, false, group, "ims-platform-vonr");
        hookBoolean(clazz, "isSuppServicesOverUtEnabledByPlatform", NO_PARAMS, false, group, "ims-platform-ut");

        Class<?>[] contextParam = new Class<?>[]{Context.class};
        hookBoolean(clazz, "isVolteEnabledByPlatform", contextParam, true, group, "ims-platform-volte-static");
        hookBoolean(clazz, "isVtEnabledByPlatform", contextParam, true, group, "ims-platform-vt-static");
        hookBoolean(clazz, "isWfcEnabledByPlatform", contextParam, true, group, "ims-platform-wfc-static");
    }

    private void hookProvisioningChecks(Class<?> clazz) {
        String group = XposedPrefs.KEY_PROVISIONING_CHECKS;
        hookBoolean(clazz, "isVolteProvisionedOnDevice", NO_PARAMS, false, group, "ims-provisioning-volte");
        hookBoolean(clazz, "isVtProvisionedOnDevice", NO_PARAMS, false, group, "ims-provisioning-vt");
        hookBoolean(clazz, "isWfcProvisionedOnDevice", NO_PARAMS, false, group, "ims-provisioning-wfc");

        Class<?>[] contextParam = new Class<?>[]{Context.class};
        hookBoolean(clazz, "isVolteProvisionedOnDevice", contextParam, true, group, "ims-provisioning-volte-static");
        hookBoolean(clazz, "isVtProvisionedOnDevice", contextParam, true, group, "ims-provisioning-vt-static");
        hookBoolean(clazz, "isWfcProvisionedOnDevice", contextParam, true, group, "ims-provisioning-wfc-static");
    }

    private void hookUserSettings(Class<?> clazz) {
        String group = XposedPrefs.KEY_USER_SETTINGS;
        hookBoolean(clazz, "isEnhanced4gLteModeSettingEnabledByUser", NO_PARAMS, false, group, "ims-user-volte");
        hookBoolean(clazz, "isVtEnabledByUser", NO_PARAMS, false, group, "ims-user-vt");
        hookBoolean(clazz, "isWfcEnabledByUser", NO_PARAMS, false, group, "ims-user-wfc");
        hookBoolean(clazz, "isWfcRoamingEnabledByUser", NO_PARAMS, false, group, "ims-user-wfc-roaming");

        Class<?>[] contextParam = new Class<?>[]{Context.class};
        hookBoolean(clazz, "isEnhanced4gLteModeSettingEnabledByUser", contextParam, true, group, "ims-user-volte-static");
        hookBoolean(clazz, "isVtEnabledByUser", contextParam, true, group, "ims-user-vt-static");
        hookBoolean(clazz, "isWfcEnabledByUser", contextParam, true, group, "ims-user-wfc-static");
        hookBoolean(clazz, "isWfcRoamingEnabledByUser", contextParam, true, group, "ims-user-wfc-roaming-static");
    }

    private void hookBoolean(Class<?> clazz, String name, Class<?>[] params, boolean staticMethod, String group, String id) {
        try {
            Method method = clazz.getDeclaredMethod(name, params);
            if (method.getReturnType() != boolean.class) {
                log(Log.DEBUG, TAG, "Skip " + name + ": return type is not boolean");
                return;
            }
            if (staticMethod != Modifier.isStatic(method.getModifiers())) {
                log(Log.DEBUG, TAG, "Skip " + name + ": static mismatch");
                return;
            }
            AtomicBoolean firstHit = new AtomicBoolean();
            hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain -> {
                if (XposedPrefs.isEnabled(prefs(), group)) {
                    if (firstHit.compareAndSet(false, true)) {
                        log(Log.INFO, TAG, "Hook hit: " + id);
                    }
                    return true;
                }
                return chain.proceed();
            });
            log(Log.INFO, TAG, "Hooked ImsManager." + name + " (" + id + ")");
        } catch (NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "ImsManager." + name + " not found, skipped");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook ImsManager." + name, t);
        }
    }

    private void deoptimizeCallers(Class<?> clazz, ClassLoader classLoader) {
        Class<?> capabilityRequest;
        try {
            capabilityRequest = Class.forName("android.telephony.ims.feature.CapabilityChangeRequest", false, classLoader);
        } catch (Throwable t) {
            log(Log.WARN, TAG, "CapabilityChangeRequest not found, skip deoptimize");
            return;
        }

        deoptimize(clazz, "updateVoiceCellFeatureValue", capabilityRequest, boolean.class);
        deoptimize(clazz, "updateVideoCallFeatureValue", capabilityRequest, boolean.class);
        deoptimize(clazz, "updateVoiceWifiFeatureAndProvisionedValues", capabilityRequest, boolean.class);
        deoptimize(clazz, "updateCrossSimFeatureAndProvisionedValues", capabilityRequest);
        deoptimize(clazz, "updateUtFeatureValue", capabilityRequest);
        deoptimize(clazz, "updateCallComposerFeatureValue", capabilityRequest);
        deoptimize(clazz, "updateCallComposerFeatureValueLegacy", capabilityRequest);
        deoptimize(clazz, "setEnhanced4gLteModeSetting", boolean.class);
        deoptimize(clazz, "setVtSetting", boolean.class);
        deoptimize(clazz, "setWfcSetting", boolean.class);
        deoptimize(clazz, "reevaluateCapabilities");
        deoptimize(clazz, "updateImsServiceConfig");
    }

    private void deoptimize(Class<?> clazz, String name, Class<?>... params) {
        try {
            Method method = clazz.getDeclaredMethod(name, params);
            method.setAccessible(true);
            boolean result = deoptimize(method);
            log(Log.INFO, TAG, "deoptimize " + name + ": " + result);
        } catch (NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "deoptimize " + name + ": not found");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "deoptimize " + name + " failed", t);
        }
    }

    private SharedPreferences prefs() {
        if (!prefsLoaded) {
            synchronized (this) {
                if (!prefsLoaded) {
                    try {
                        prefs = getRemotePreferences(XposedPrefs.GROUP);
                    } catch (Throwable t) {
                        log(Log.WARN, TAG, "Remote preferences unavailable", t);
                        prefs = null;
                    }
                    prefsLoaded = true;
                }
            }
        }
        return prefs;
    }
}
