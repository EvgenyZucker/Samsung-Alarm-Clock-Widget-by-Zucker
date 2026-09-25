package dev.local.samsungalarmwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class InternalRefreshReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync();
        AlarmWidgetProvider.handleInternalRefresh(
                context.getApplicationContext(), intent.getAction(), result);
    }
}
