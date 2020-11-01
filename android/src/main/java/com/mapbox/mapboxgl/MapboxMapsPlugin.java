// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.mapbox.mapboxgl;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * Plugin for controlling a set of MapboxMap views to be shown as overlays on top of the Flutter
 * view. The overlay should be hidden during transformations or while Flutter is rendering on top of
 * the map. A Texture drawn using MapboxMap bitmap snapshots can then be shown instead of the
 * overlay.
 */
public class MapboxMapsPlugin implements Application.ActivityLifecycleCallbacks,
        FlutterPlugin, ActivityAware, DefaultLifecycleObserver {
  static final int CREATED = 1;
  static final int STARTED = 2;
  static final int RESUMED = 3;
  static final int PAUSED = 4;
  static final int STOPPED = 5;
  static final int DESTROYED = 6;
  private final AtomicInteger state = new AtomicInteger(0);
  private int registrarActivityHashCode;

  private FlutterPluginBinding pluginBinding;
  private Lifecycle lifecycle;

  private static final String VIEW_TYPE = "plugins.flutter.io/mapbox_gl";

  public static void registerWith(Registrar registrar) {
    if (registrar.activity() == null) {
      // When a background flutter view tries to register the plugin, the registrar has no activity.
      // We stop the registration process as this plugin is foreground only.
      return;
    }
    final MapboxMapsPlugin plugin = new MapboxMapsPlugin(registrar.activity());
    registrar.activity().getApplication().registerActivityLifecycleCallbacks(plugin);
    registrar
      .platformViewRegistry()
      .registerViewFactory(
              VIEW_TYPE,
              new MapboxMapFactory(plugin.state, registrar.messenger(), null, null, registrar, -1));

    MethodChannel methodChannel =
            new MethodChannel(registrar.messenger(), "plugins.flutter.io/mapbox_gl");
    methodChannel.setMethodCallHandler(new GlobalMethodHandler(registrar));
  }

  public MapboxMapsPlugin() {}

  // DefaultLifecycleObserver methods

  @Override
  public void onCreate(@NonNull LifecycleOwner owner) {
    state.set(CREATED);
  }

  @Override
  public void onStart(@NonNull LifecycleOwner owner) {
    state.set(STARTED);
  }

  @Override
  public void onResume(@NonNull LifecycleOwner owner) {
    state.set(RESUMED);
  }

  @Override
  public void onPause(@NonNull LifecycleOwner owner) {
    state.set(PAUSED);
  }

  @Override
  public void onStop(@NonNull LifecycleOwner owner) {
    state.set(STOPPED);
  }

  @Override
  public void onDestroy(@NonNull LifecycleOwner owner) {
    state.set(DESTROYED);
  }

  // Application.ActivityLifecycleCallbacks methods

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(CREATED);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(STARTED);
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(RESUMED);
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(PAUSED);
  }

  @Override
  public void onActivityStopped(Activity activity) {
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(STOPPED);
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(DESTROYED);
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = binding;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding);
    lifecycle.addObserver(this);
    pluginBinding
            .getPlatformViewRegistry()
            .registerViewFactory(
                    VIEW_TYPE,
                    new MapboxMapFactory(
                            state,
                            pluginBinding.getBinaryMessenger(),
                            binding.getActivity().getApplication(),
                            lifecycle,
                            null,
                            binding.getActivity().hashCode()));


    // TODO: we aren't using tile caching yet and I'm not sure yet how to get what we need to find
    // the assets path with embedding v2
//    MethodChannel methodChannel =
//            new MethodChannel(pluginBinding.getBinaryMessenger(), VIEW_TYPE);
//    methodChannel.setMethodCallHandler(new GlobalMethodHandler(registrar));
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding);
    lifecycle.addObserver(this);
  }

  @Override
  public void onDetachedFromActivity() {
    lifecycle.removeObserver(this);
  }

  private MapboxMapsPlugin(Activity activity) {
    this.registrarActivityHashCode = activity.hashCode();
  }
}
