package com.github.npc97.android.y705_tool;

import android.app.Application;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Y705ToolApp extends Application {

    public static final String TAG = "Y705_Tool_APP";

    private static volatile XposedService sService = null;
    private static volatile boolean sFrameworkActive = false;
    private static volatile String sFrameworkDesc = "";

    private static final List<ServiceReadyListener> listeners = new ArrayList<>();

    public static void addServiceReadyListener(ServiceReadyListener l) {
        listeners.add(l);
        if (sService != null) {
            l.onServiceReady();
        }
    }

    public static void removeServiceReadyListener(ServiceReadyListener l) {
        listeners.remove(l);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                sService = service;
                sFrameworkActive = true;
                int apiVersion = 0;
                try {
                    apiVersion = service.getApiVersion();
                } catch (Throwable ignored) {
                }
                sFrameworkDesc = "Framework: " + service.getFrameworkName()
                        + "\nAPI: " + apiVersion
                        + "  Version: " + service.getFrameworkVersionCode();

                for (var l : listeners) {
                    l.onServiceReady();
                }
            }

            @Override
            public void onServiceDied(XposedService service) {
                sService = null;
                sFrameworkActive = false;
                sFrameworkDesc = "";

                for (var l : listeners) {
                    l.onServiceLost();
                }
            }
        });
    }

    @Nullable
    public static XposedService currentService() {
        return sService;
    }

    public static boolean isFrameworkActive() {
        return sFrameworkActive;
    }

    public static String frameworkDesc() {
        return sFrameworkDesc;
    }

    public interface ServiceReadyListener {
        void onServiceReady();
        void onServiceLost();
    }
}
