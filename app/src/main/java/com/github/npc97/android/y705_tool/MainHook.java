package com.github.npc97.android.y705_tool;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    public static final String PREFS_NAME = "Y705_Tool";

    public static final String APP_LIST_CONFIG = "apps";

    public static final String OTA_CONFIG = "ota";

    public static final String TARGET_PACKAGE_LAUNCHER = "com.zui.launcher";

    public static final String TARGET_PACKAGE_OTA = "com.lenovo.ota";

    private static final String TAG = "Y705_Tool_Module";

    private volatile ArrayList<String> appList = new ArrayList<>();

    private volatile boolean otaSwitch = false;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        Log.i(TAG, "模块已加载");
        readApps();
        readOtaSwitch();
    }

    private void readApps() {
        try {
            var prefs = getRemotePreferences(PREFS_NAME);
            var apps = prefs.getStringSet(APP_LIST_CONFIG, new HashSet<>());
            appList = new ArrayList<>(apps);
            Log.i(TAG, "读取APP列表 " + appList.size() + " 个");
        } catch (Throwable e) {
            Log.e(TAG, "读取配置失败", e);
        }
    }

    private void readOtaSwitch() {
        try {
            var prefs = getRemotePreferences(PREFS_NAME);
            otaSwitch = prefs.getBoolean(MainHook.OTA_CONFIG, false);
            Log.i(TAG, "读取OTA Switch：" + otaSwitch);
        } catch (Throwable e) {
            Log.e(TAG, "读取配置失败", e);
            otaSwitch = false;
        }
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        // 只处理目标包
        if (TARGET_PACKAGE_LAUNCHER.equals(param.getPackageName())) {
            Log.i(TAG, "目标包 " + TARGET_PACKAGE_LAUNCHER + " 已加载，注入中......");
            try {
                // 全部清除
                hookAllClean(param.getDefaultClassLoader());
                // 滑动清除
                hookSingleClean(param.getDefaultClassLoader());
            } catch (Throwable t) {
                Log.e(TAG, TARGET_PACKAGE_LAUNCHER + " 注入异常", t);
            }
        } else if (otaSwitch && TARGET_PACKAGE_OTA.equals(param.getPackageName())) {
            Log.i(TAG, "目标包 " + TARGET_PACKAGE_OTA + " 已加载，注入中......");
            try {
                // 系统更新提醒
                hookOtaNotification(param.getDefaultClassLoader());
            } catch (Throwable t) {
                Log.e(TAG, TARGET_PACKAGE_OTA + " 注入异常", t);
            }
        }
    }

    private void hookAllClean(ClassLoader classLoader) throws Exception {
        var targetClassPkg = "com.zui.launcher.util.ForceStopTask";

        Class<?> targetClass = classLoader.loadClass(targetClassPkg);

        Class<?> activityClass  = classLoader.loadClass("android.app.Activity");

        // 目标方法有 6 个参数：1 个 Activity + 5 个 List
        Method targetMethod = targetClass.getDeclaredMethod(
                "f",
                activityClass,  // activity
                List.class,     // recentsPackages
                List.class,     // hiddenPackages
                List.class,     // protectedPackages
                List.class,     // filterPackages
                List.class      // overseasHiddenPackages
        );

        // 注册前置 Hook（BeforeInvocation 在方法执行前触发）
        hook(targetMethod).intercept(chain -> {
            try {
                Log.i(TAG, "[Launcher][拦截]全局清理");
                var args = chain.getArgs();
                var hiddenPackages = (List<String>) args.get(2);
                hiddenPackages.addAll(appList);

                var protectedPackages = (List<String>) args.get(3);
                protectedPackages.addAll(appList);
            } catch (Throwable t) {
                Log.e(TAG, "拦截异常", t);
            }
            return chain.proceed();
        });

        Log.i(TAG, targetClassPkg + " 注入成功");
    }

    private void hookSingleClean(ClassLoader classLoader) throws Exception {
        var targetClassPkg = "com.zui.launcher.util.ForceStopTask$b";

        Class<?> targetClass = classLoader.loadClass(targetClassPkg);

        Method targetMethod = targetClass.getDeclaredMethod(
                "invokeSuspend",
                Object.class    // obj
        );

        hook(targetMethod).intercept(chain -> {
            try {
                var thisObj = chain.getThisObject();
                var pkgField = thisObj.getClass().getDeclaredField("c");
                pkgField.setAccessible(true);
                var pkgName = (String) pkgField.get(thisObj);

                if (appList.contains(pkgName)) {
                    Log.i(TAG, "[Launcher][阻止清理]目标包 " + pkgName);
                    return kotlin.Unit.INSTANCE;
                }
                Log.i(TAG, "[Launcher][允许清理]目标包 " + pkgName);
            } catch (Throwable t) {
                Log.e(TAG, "拦截异常", t);
            }
            return chain.proceed();
        });

        Log.i(TAG, targetClassPkg + " 注入成功");
    }

    private void hookOtaNotification(ClassLoader classLoader) throws Exception {
        var targetClassPkg = "com.lenovo.row.ota.core.d.notification.NotificationCenter";

        Class<?> targetClass = classLoader.loadClass(targetClassPkg);

        Method targetMethod = targetClass.getDeclaredMethod(
                "showNewVersionNotification"
        );

        hook(targetMethod).intercept(chain -> {
            Log.i(TAG, "[OTA][阻止]系统更新提醒");
            return null;
        });

        Log.i(TAG, targetClassPkg + " 注入成功");
    }
}
