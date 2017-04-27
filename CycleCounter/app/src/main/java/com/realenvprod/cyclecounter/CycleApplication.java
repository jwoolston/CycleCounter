package com.realenvprod.cyclecounter;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Intent;
import android.os.Bundle;
import com.realenvprod.cyclecounter.service.BLEScanService;

/**
 * @author Jared Woolston (jwoolston@keywcorp.com)
 */

public class CycleApplication extends Application implements ActivityLifecycleCallbacks {

    private boolean mainActivityVisibile = false;

    public boolean isMainActivityVisibile() {
        return mainActivityVisibile;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        launchScanService();
    }

    private void launchScanService() {
        final Intent intent = new Intent(this, BLEScanService.class);
        startService(intent);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof MainActivity) {
            mainActivityVisibile = true;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity instanceof MainActivity) {
            mainActivityVisibile = false;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
