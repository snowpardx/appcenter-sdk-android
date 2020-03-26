package com.microsoft.appcenter.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.appcenter.AppCenterService;

import java.util.ArrayList;
import java.util.List;

public class AppLifecycleListener implements Application.ActivityLifecycleCallbacks {

    private static final long TIMEOUT_MS = 700;
    private static final AppLifecycleListener sInstance = new AppLifecycleListener();
    private int mStartedCounter = 0;
    private int mResumedCounter = 0;
    private boolean mPauseSent = true;
    private boolean mStopSent = true;
    private Handler mHandler = new Handler();
    private List<AppCenterService> services = new ArrayList<>();
    private Runnable mDelayedPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
        }
    };

    private boolean subscribed;

    private AppLifecycleListener() {
    }

    public static AppLifecycleListener getInstance() {
        return sInstance;
    }

    public static void attachToActivityLifecycleCallbacks(Application application, AppCenterService serviceInstance) {
        if (!sInstance.subscribed) {
            application.registerActivityLifecycleCallbacks(sInstance);
            sInstance.subscribed = true;
        }
        sInstance.services.add(serviceInstance);
    }

    private void started() {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {
            for (AppCenterService service : services) {
                if (service instanceof OnStartApplicationListener) {
                    ((OnStartApplicationListener) service).onApplicationStart();
                }
            }
            mStopSent = false;
        }
    }

    private void resumed() {
        mResumedCounter++;
        if (mResumedCounter == 1) {
            if (mPauseSent) {
                mPauseSent = false;
            } else {
                mHandler.removeCallbacks(mDelayedPauseRunnable);
            }
        }
    }

    private void paused() {
        mResumedCounter--;
        if (mResumedCounter == 0) {
            mHandler.postDelayed(mDelayedPauseRunnable, TIMEOUT_MS);
        }
    }

    private void stopped() {
        mStartedCounter--;
        dispatchStopIfNeeded();
    }

    private void dispatchPauseIfNeeded() {
        if (mResumedCounter == 0) {
            mPauseSent = true;
        }
    }

    private void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {
            mStopSent = true;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        for (AppCenterService service : services) {
            service.onActivityCreated(activity, savedInstanceState);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        started();
        for (AppCenterService service : services) {
            service.onActivityStarted(activity);
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        resumed();
        for (AppCenterService service : services) {
            service.onActivityResumed(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        paused();
        for (AppCenterService service : services) {
            service.onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        stopped();
        for (AppCenterService service : services) {
            service.onActivityStopped(activity);
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        for (AppCenterService service : services) {
            service.onActivitySaveInstanceState(activity, outState);
        }
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        for (AppCenterService service : services) {
            service.onActivityDestroyed(activity);
        }
    }

    public interface OnStartApplicationListener {
        void onApplicationStart();
    }
}
