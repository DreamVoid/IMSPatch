package me.dreamvoid.imspatch;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class XposedStatus {
    public interface Listener {
        void onXposedStatusChanged(XposedService service);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private static volatile XposedService service;

    private XposedStatus() {
    }

    /**
     * 注册一次 libxposed 服务监听。
     */
    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService xposedService) {
                service = xposedService;
                notifyListeners();
            }

            @Override
            public void onServiceDied(XposedService xposedService) {
                if (service == xposedService) {
                    service = null;
                }
                notifyListeners();
            }
        });
    }

    /**
     * @return 当前服务，未连接时为 {@code null}
     */
    public static XposedService getService() {
        return service;
    }

    /**
     * @return libxposed 服务是否已连接
     */
    public static boolean isActive() {
        return service != null;
    }

    /**
     * 添加状态监听。
     *
     * @param listener 监听器
     */
    public static void addListener(Listener listener) {
        LISTENERS.addIfAbsent(listener);
        listener.onXposedStatusChanged(service);
    }

    /**
     * 移除状态监听。
     *
     * @param listener 监听器
     */
    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) {
                listener.onXposedStatusChanged(service);
            }
        });
    }
}
