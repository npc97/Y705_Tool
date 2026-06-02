package com.github.npc97.android.y705_tool.activity;

import android.graphics.drawable.Drawable;

public class AppInfo {

    private String appName;
    private String packageName;
    private Drawable icon;

    public AppInfo(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }
}
