package me.dreamvoid.imspatch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import io.github.libxposed.service.HookedTarget;
import io.github.libxposed.service.XposedService;

import java.util.List;

public class SettingsActivity extends Activity implements XposedStatus.Listener {
    private static final String PHONE_PACKAGE = "com.android.phone";

    private TextView statusView;
    private Switch masterSwitch;
    private Switch carrierConfigSwitch;
    private Switch platformChecksSwitch;
    private Switch provisioningChecksSwitch;
    private Switch userSettingsSwitch;

    private SharedPreferences prefs;
    private boolean updating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());
        XposedStatus.addListener(this);
    }

    @Override
    protected void onDestroy() {
        XposedStatus.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onXposedStatusChanged(XposedService service) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                updateUi(service);
            }
        });
    }

    private View buildContentView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int base = dp(24);
        content.setPadding(base, base, base, base);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        getWindow().setDecorFitsSystemWindows(false);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            content.setPadding(base + bars.left, base + bars.top, base + bars.right, base + bars.bottom);
            return insets;
        });

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(title);

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setTypeface(Typeface.MONOSPACE);
        statusView.setTextIsSelectable(true);
        statusView.setPadding(0, dp(12), 0, dp(12));
        content.addView(statusView);

        addSection(content, "注入开关");
        masterSwitch = addSwitch(content, "启用 Xposed 注入", "总开关；关闭时所有分组均不生效。", XposedPrefs.KEY_MASTER);
        carrierConfigSwitch = addSwitch(content, "CarrierConfig 覆盖", "叠加 VoLTE、VT、Wi-Fi 通话、VoNR 与 5G 可用性配置。", XposedPrefs.KEY_CARRIER_CONFIG);
        platformChecksSwitch = addSwitch(content, "平台能力检查绕过", "强制 ImsManager 的平台 VoLTE/VT/WFC/VoNR 检查返回可用。", XposedPrefs.KEY_PLATFORM_CHECKS);
        provisioningChecksSwitch = addSwitch(content, "Provisioning 检查绕过", "强制运营商 provisioning 检查返回已开通，可能导致业务异常。", XposedPrefs.KEY_PROVISIONING_CHECKS);
        userSettingsSwitch = addSwitch(content, "用户开关强制", "强制 VoLTE、VT、Wi-Fi 通话及漫游 Wi-Fi 通话用户开关为开启。", XposedPrefs.KEY_USER_SETTINGS);

        TextView note = new TextView(this);
        note.setText("设置通常在下一次 CarrierConfig 读取时生效；如未生效，请重启设备或切换飞行模式。\n这些开关会改变电话进程行为，仅在了解风险时启用。");
        note.setTextSize(12);
        note.setTextColor(Color.rgb(211, 47, 47));
        note.setPadding(0, dp(16), 0, 0);
        content.addView(note);

        return scroll;
    }

    private void addSection(LinearLayout parent, String text) {
        TextView section = new TextView(this);
        section.setText(text);
        section.setTextSize(15);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setPadding(0, dp(16), 0, dp(4));
        parent.addView(section);
    }

    private Switch addSwitch(LinearLayout parent, String title, String summary, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        texts.addView(titleView);

        TextView summaryView = new TextView(this);
        summaryView.setText(summary);
        summaryView.setTextSize(12);
        summaryView.setAlpha(0.7f);
        texts.addView(summaryView);

        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> save(key, isChecked));
        row.addView(toggle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(32, 128, 128, 128));
        parent.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return toggle;
    }

    private void updateUi(XposedService service) {
        if (service == null) {
            prefs = null;
            statusView.setText("libxposed 服务未连接。\n请确认模块已在 LSPosed 中启用，并等待电话进程重新启动。");
            setSwitchesEnabled(false);
            return;
        }

        boolean remoteSupported = hasRemotePreferences(service);
        StringBuilder status = new StringBuilder();
        status.append("libxposed: 已连接\n");
        appendStatus(status, "API", () -> String.valueOf(service.getApiVersion()));
        appendStatus(status, "框架", () -> service.getFrameworkName() + " " + service.getFrameworkVersion());
        status.append("Remote preferences: ").append(remoteSupported ? "支持" : "不支持").append('\n');

        try {
            List<String> scope = service.getScope();
            status.append("Scope: ").append(scope).append('\n');
            if (!scope.contains(PHONE_PACKAGE)) {
                status.append("警告: Scope 未包含 ").append(PHONE_PACKAGE).append('\n');
            }
        } catch (RuntimeException e) {
            status.append("Scope: 读取失败\n");
        }

        try {
            List<HookedTarget> targets = service.getRunningTargets();
            status.append("运行中的目标: ").append(targets.size()).append('\n');
            boolean phoneInjected = false;
            for (HookedTarget target : targets) {
                if (PHONE_PACKAGE.equals(target.getProcessName())) {
                    phoneInjected = true;
                    status.append("电话进程: ").append(target.getState()).append('\n');
                }
            }
            if (!phoneInjected) {
                status.append("电话进程尚未注入，请重启设备或电话进程。\n");
            }
        } catch (UnsupportedOperationException e) {
            status.append("运行中的目标: 当前框架不支持\n");
        } catch (RuntimeException e) {
            status.append("运行中的目标: 读取失败\n");
        }

        statusView.setText(status.toString());

        if (!remoteSupported) {
            prefs = null;
            setSwitchesEnabled(false);
            return;
        }

        try {
            prefs = service.getRemotePreferences(XposedPrefs.GROUP);
        } catch (RuntimeException e) {
            prefs = null;
            statusView.append("\n无法读取 remote preferences: " + e.getMessage());
            setSwitchesEnabled(false);
            return;
        }

        updating = true;
        masterSwitch.setChecked(prefs.getBoolean(XposedPrefs.KEY_MASTER, false));
        carrierConfigSwitch.setChecked(prefs.getBoolean(XposedPrefs.KEY_CARRIER_CONFIG, false));
        platformChecksSwitch.setChecked(prefs.getBoolean(XposedPrefs.KEY_PLATFORM_CHECKS, false));
        provisioningChecksSwitch.setChecked(prefs.getBoolean(XposedPrefs.KEY_PROVISIONING_CHECKS, false));
        userSettingsSwitch.setChecked(prefs.getBoolean(XposedPrefs.KEY_USER_SETTINGS, false));
        updating = false;
        setSwitchesEnabled(true);
    }

    private void appendStatus(StringBuilder builder, String label, StatusValue value) {
        try {
            builder.append(label).append(": ").append(value.get()).append('\n');
        } catch (RuntimeException e) {
            builder.append(label).append(": 读取失败\n");
        }
    }

    private boolean hasRemotePreferences(XposedService service) {
        try {
            return (service.getFrameworkProperties() & XposedService.PROP_CAP_REMOTE) != 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void setSwitchesEnabled(boolean enabled) {
        masterSwitch.setEnabled(enabled);
        carrierConfigSwitch.setEnabled(enabled);
        platformChecksSwitch.setEnabled(enabled);
        provisioningChecksSwitch.setEnabled(enabled);
        userSettingsSwitch.setEnabled(enabled);
    }

    private void save(String key, boolean value) {
        if (updating) {
            return;
        }
        if (prefs == null) {
            Toast.makeText(this, "libxposed remote preferences 不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (!prefs.edit().putBoolean(key, value).commit()) {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        } catch (RuntimeException e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateUi(XposedStatus.getService());
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface StatusValue {
        String get();
    }
}
