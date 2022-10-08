package org.nuntius35.wrongpinshutdown;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class LogInReceiver extends DeviceAdminReceiver {

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        ShutdownManager sm = new ShutdownManager(context);
        sm.incTries();
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        ShutdownManager sm = new ShutdownManager(context);
        sm.resetTries();
    }
}
