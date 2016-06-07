// Copyright (c) 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium;

import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.util.ArrayMap;

import java.lang.System;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ChromeProxy extends CordovaPlugin {
    private static final String LOG_TAG = "ChromeProxy";

    private JSONObject config = new JSONObject();

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("get".equals(action)) {
            get(args, callbackContext);
            return true;
        } else if ("set".equals(action)) {
            set(args, callbackContext);
            return true;
        } else if ("clear".equals(action)) {
            clear(args, callbackContext);
            return true;
        }
        return false;
    }

    private void get(final CordovaArgs args, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                // Ignoring scope.
                // Arguably, this should use System.getProperty to see if somehow
                // the proxy settings were already set...
                callbackContext.success(config);
            }
        });
    }

    private void setHttpProxy(JSONObject rule, String prefix, String nonProxyHosts) throws JSONException {
        String scheme = rule.getString("scheme");
        if (!"http".equals(scheme)) {
            throw new JSONException("Scheme must be http");
        }
        String host = rule.getString("host");
        int port = rule.has("port") ? rule.getInt("port") : 80;
        String portString = Integer.toString(port);
        System.setProperty(prefix + ".proxyHost", host);
        System.setProperty(prefix + ".proxyPort", portString);
        if (nonProxyHosts != null) {
            System.setProperty(prefix + ".nonProxyHosts", nonProxyHosts);
        }
    }

    private void set(final CordovaArgs args, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                // Ignoring scope.
                JSONObject details;
                try {
                    details = args.getJSONObject(0);
                } catch (JSONException e) {
                    callbackContext.error("Failed to parse details as JSON");
                    return;
                }
                try {
                    if (details.has("value")) {
                        config = details.getJSONObject("value");
                        String mode = config.getString("mode");
                        if (!"fixed_servers".equals(mode) &&
                            !"direct".equals(mode)) {
                            callbackContext.error("Mode must be fixed_servers or direct");
                            return;
                        }
                        if ("direct".equals(mode)) {
                            clear(args, callbackContext);
                            return;
                        }
                    } else {
                        callbackContext.error("Missing value");
                    }
                } catch (JSONException e) {
                    callbackContext.error("Failed to parse value as JSON");
                }

                try {
                    JSONObject rules = config.getJSONObject("rules");
                    String nonProxyHosts = null;
                    if (rules.has("bypassList")) {
                        JSONArray bypassList = rules.getJSONArray("bypassList");
                        nonProxyHosts = bypassList.join("|");
                    }
                    if (rules.has("singleProxy")) {
                        JSONObject rule = rules.getJSONObject("singleProxy");
                        String scheme = rule.getString("scheme");
                        if ("socks5".equals(scheme)) {
                            if (nonProxyHosts != null) {
                                callbackContext.error("nonProxyHosts not supported with socks");
                                return;
                            }
                            String host = rule.getString("host");
                            int port = rule.has("port") ? rule.getInt("port") : 1080;
                            String portString = Integer.toString(port);
                            System.setProperty("socksProxyHost", host);
                            System.setProperty("socksProxyPort", portString);
                        } else if ("http".equals(scheme)) {
                            setHttpProxy(rule, "http", nonProxyHosts);
                            setHttpProxy(rule, "https", nonProxyHosts);
                            setHttpProxy(rule, "ftp", nonProxyHosts);
                        } else {
                            callbackContext.error("Scheme must be socks5 or http");
                            return;
                        }
                    } else {
                        if (rules.has("proxyForHttp")) {
                            JSONObject rule = rules.getJSONObject("proxyForHttp");
                            setHttpProxy(rule, "http", nonProxyHosts);
                        }
                        if (rules.has("proxyForHttps")) {
                            JSONObject rule = rules.getJSONObject("proxyForHttps");
                            setHttpProxy(rule, "https", nonProxyHosts);
                        }
                        if (rules.has("proxyForFtp")) {
                            JSONObject rule = rules.getJSONObject("proxyForFtp");
                            setHttpProxy(rule, "ftp", nonProxyHosts);
                        }
                    }
                    onSettingsChanged();
                    callbackContext.success();
                } catch (JSONException e) {
                    callbackContext.error("Failed to parse rule(s) as JSON");
                } catch (Exception e) {
                    callbackContext.error("Other error");
                }
            }
        });
    }

    private void clear(final CordovaArgs args, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("http.nonProxyHosts");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
                System.clearProperty("https.nonProxyHosts");
                System.clearProperty("ftp.proxyHost");
                System.clearProperty("ftp.proxyPort");
                System.clearProperty("ftp.nonProxyHosts");
                onSettingsChanged();
            }
        });
    }

    private void onSettingsChanged() {
        Context appContext = this.cordova.getActivity().getApplicationContext();
        // See http://stackoverflow.com/questions/32245972/android-webview-non-fqdn-urls-not-routing-through-proxy-on-lollipop
        // and https://crbug.com/525945 for the source of this pattern.
        try {
            Class<?> applicationClass = Class.forName("android.app.Application");
            Field mLoadedApkField = applicationClass.getDeclaredField("mLoadedApk");
            mLoadedApkField.setAccessible(true);
            Object mloadedApk = mLoadedApkField.get(appContext);
            Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
            Field mReceiversField = loadedApkClass.getDeclaredField("mReceivers");
            mReceiversField.setAccessible(true);
            ArrayMap<?, ?> receivers = (ArrayMap<?, ?>) mReceiversField.get(mloadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object receiver : ((ArrayMap<?, ?>) receiverMap).keySet()) {
                    Class<?> clazz = receiver.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive",
                                Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
                        onReceiveMethod.invoke(receiver, appContext, intent);
                    }
                }
            }
        } catch (Exception e) {
            // TODO
        }
    }

}
