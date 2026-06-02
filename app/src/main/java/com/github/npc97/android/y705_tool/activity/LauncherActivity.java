package com.github.npc97.android.y705_tool.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.npc97.android.y705_tool.MainHook;
import com.github.npc97.android.y705_tool.R;
import com.github.npc97.android.y705_tool.Y705ToolApp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LauncherActivity extends AppCompatActivity {

    private MaterialSwitch switchOta;

    private final Y705ToolApp.ServiceReadyListener serviceListener =
            new Y705ToolApp.ServiceReadyListener() {
                @Override
                public void onServiceReady() {
                    runOnUiThread(() -> {
                        switchOta.setChecked(readOtaSwitch());
                    });
                }

                @Override
                public void onServiceLost() {}
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        var rowPickApp = (LinearLayout) findViewById(R.id.row_pick_app);
        rowPickApp.setOnClickListener(v -> {
            var selectedPackages = readApps();
            Intent intent = new Intent(this, AppPickerActivity.class);
            intent.putStringArrayListExtra(
                    AppPickerActivity.EXTRA_SELECTED_PACKAGES,
                    selectedPackages
            );
            appPickerLauncher.launch(intent);
        });

        switchOta = findViewById(R.id.switch_ota);
        switchOta.setChecked(false);
        switchOta.setOnClickListener(v -> confirmOtaSwitchChanges(switchOta.isChecked()));
        var rowOta = (LinearLayout) findViewById(R.id.row_ota);
        rowOta.setOnClickListener(v -> confirmOtaSwitchChanges(switchOta.isChecked()));
        Y705ToolApp.addServiceReadyListener(serviceListener);

        getRootOnStart();
        getCompatibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Y705ToolApp.removeServiceReadyListener(serviceListener);
    }

    private final ActivityResultLauncher<Intent> appPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            if (result.getData() != null) {
                                var selectedPackages = result.getData().getStringArrayListExtra(AppPickerActivity.RESULT_SELECTED_PACKAGES);
                                if (selectedPackages != null) {
                                    Log.i(Y705ToolApp.TAG, "已选择: " + selectedPackages.size());
                                    saveApps(selectedPackages);
                                    return;
                                }
                            }
                            Log.i(Y705ToolApp.TAG, "用户舍弃变更");
                        }
                    }
            );

    private void saveApps(List<String> apps) {
        var xposedService = Y705ToolApp.currentService();
        if (xposedService == null) {
            Log.e(Y705ToolApp.TAG, "保存失败：Xposed未加载");
            Snackbar.make(findViewById(android.R.id.content), "保存失败：Xposed未加载", Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            var prefs = xposedService.getRemotePreferences(MainHook.PREFS_NAME);
            prefs.edit().putStringSet(MainHook.APP_LIST_CONFIG, new HashSet<>(apps)).apply();
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, "保存失败", e);
            Snackbar.make(findViewById(android.R.id.content), "保存失败，请确认已在LSPosed管理器中启用了本模块", Snackbar.LENGTH_SHORT).show();
        }

        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + MainHook.TARGET_PACKAGE_LAUNCHER});
            Log.i(Y705ToolApp.TAG, "应用成功");
            Snackbar.make(findViewById(android.R.id.content), "应用成功", Snackbar.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, MainHook.TARGET_PACKAGE_LAUNCHER + " 结束失败", e);
            Snackbar.make(findViewById(android.R.id.content), "保存成功。请前往应用管理，手动结束「ZUX桌面」", Snackbar.LENGTH_LONG).show();
        }
    }

    private ArrayList<String> readApps() {
        var xposedService = Y705ToolApp.currentService();
        if (xposedService == null) {
            Log.e(Y705ToolApp.TAG, "读取配置失败：Xposed未加载");
            Snackbar.make(findViewById(android.R.id.content), "读取配置失败,请确认已在LSPosed管理器中启用了本模块", Snackbar.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
        try {
            var prefs = xposedService.getRemotePreferences(MainHook.PREFS_NAME);
            var apps = prefs.getStringSet(MainHook.APP_LIST_CONFIG, new HashSet<>());
            Log.i(Y705ToolApp.TAG, "读取APP列表：" + apps.size());
            return new ArrayList<>(apps);
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, "读取配置失败", e);
            Snackbar.make(findViewById(android.R.id.content), "读取配置失败，请确认已在LSPosed管理器中启用了本模块", Snackbar.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    private void confirmOtaSwitchChanges(boolean otaSwitch) {
        new MaterialAlertDialogBuilder(this)
                .setMessage("警告：本操作需要结束「系统更新」应用，请确保当前没有正在进行更新!")
                .setNegativeButton("取消", (dialog, which) -> {
                    switchOta.setChecked(!otaSwitch);
                    dialog.dismiss();
                })
                .setPositiveButton("执行", (dialog, which) -> {
                    saveOtaSwitch(otaSwitch);
                    dialog.dismiss();
                })
                .show();
    }

    private void saveOtaSwitch(boolean otaSwitch) {
        var xposedService = Y705ToolApp.currentService();
        if (xposedService == null) {
            Log.e(Y705ToolApp.TAG, "保存失败：Xposed未加载");
            Snackbar.make(findViewById(android.R.id.content), "保存失败：Xposed未加载", Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            var prefs = xposedService.getRemotePreferences(MainHook.PREFS_NAME);
            prefs.edit().putBoolean(MainHook.OTA_CONFIG, otaSwitch).apply();
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, "保存失败", e);
            Snackbar.make(findViewById(android.R.id.content), "保存失败，请确认已在LSPosed管理器中启用了本模块", Snackbar.LENGTH_SHORT).show();
        }

        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + MainHook.TARGET_PACKAGE_OTA});
            Log.i(Y705ToolApp.TAG, "应用成功");
            Snackbar.make(findViewById(android.R.id.content), "应用成功", Snackbar.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, MainHook.TARGET_PACKAGE_OTA + " 结束失败", e);
            Snackbar.make(findViewById(android.R.id.content), "保存成功。请前往应用管理，手动结束「系统更新」", Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean readOtaSwitch() {
        var xposedService = Y705ToolApp.currentService();
        if (xposedService == null) {
            Log.e(Y705ToolApp.TAG, "读取配置失败：Xposed未加载");
            Snackbar.make(findViewById(android.R.id.content), "读取配置失败,请确认已在LSPosed管理器中启用了本模块", Snackbar.LENGTH_SHORT).show();
            return false;
        }
        try {
            var prefs = xposedService.getRemotePreferences(MainHook.PREFS_NAME);
            var otaSwitch = prefs.getBoolean(MainHook.OTA_CONFIG, false);
            Log.i(Y705ToolApp.TAG, "读取OTA Switch：" + otaSwitch);
            return otaSwitch;
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, "读取配置失败", e);
            Snackbar.make(findViewById(android.R.id.content), "读取配置失败，请确认已在LSPosed管理器中启用了本模块", Snackbar.LENGTH_SHORT).show();
            return false;
        }
    }

    private void getRootOnStart() {
        var banner = (TextView) findViewById(R.id.tv_title);
        var banner_sub = (TextView) findViewById(R.id.tv_subtitle);
        try {
            Runtime.getRuntime().exec(new String[]{"su"});
            banner.setText("✅已获得ROOT权限");
        } catch (Throwable e) {
            Log.e(Y705ToolApp.TAG, "无法获得ROOT权限", e);
            banner.setText("❌未获得ROOT权限");
            banner_sub.setText("你仍然可以正常使用模块，但一些操作需要手动完成");
        }
    }

    private void getCompatibility() {
        var banner_sub2 = (TextView) findViewById(R.id.tv_subtitle2);
        var c1 = "";
        try {
            var launcherPkgInfo = this.getApplicationContext().getPackageManager().getPackageInfo(MainHook.TARGET_PACKAGE_LAUNCHER, 0);
            var launcherVersion = launcherPkgInfo.versionName;
            if ("18.2.0.0375".equals(launcherVersion)) {
                c1 = "✅ZUX桌面：" + launcherVersion;
            } else {
                c1 = "❌ZUX桌面：" + launcherVersion;
            }
        } catch (Exception e) {
            c1 = "❌ZUX桌面：未知版本";
        }

        var c2 = "";
        try {
            var otaPkgInfo = this.getApplicationContext().getPackageManager().getPackageInfo(MainHook.TARGET_PACKAGE_OTA, 0);
            var otaVersion = otaPkgInfo.versionName;
            if ("V9.2.1.260114".equals(otaVersion)) {
                c2 = "✅系统更新：" + otaVersion;
            } else {
                c2 = "❌系统更新：" + otaVersion;
            }
        } catch (Exception e) {
            c2 = "❌系统更新：未知版本";
        }
        banner_sub2.setText(c1 + "  " + c2);
    }
}