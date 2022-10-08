package org.nuntius35.wrongpinshutdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UserReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ShutdownManager sm = new ShutdownManager(context);
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            sm.resetTries();
        }
    }
}
