package dev.local.samsungalarmwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AlarmWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "SamsungAlarmWidget";
    private static final String ACTION_DATE_REFRESH =
            "dev.local.samsungalarmwidget.DATE_REFRESH";
    private static final String ACTION_ALARM_VERIFY =
            "dev.local.samsungalarmwidget.ALARM_VERIFY";
    private static final long DATE_REFRESH_WINDOW_MS = 15L * 60L * 1000L;
    private static final long ALARM_VERIFY_INTERVAL_MS = 60_000L;
    private static final long ALARM_CHANGE_INITIAL_DELAY_MS = 600L;
    private static final long ALARM_CHANGE_CONFIRM_DELAY_MS = 4_000L;
    private static final ThreadPoolExecutor EXECUTOR = createExecutor();
    private static final Object UPDATE_LOCK = new Object();
    private static boolean updateQueued;
    private static boolean updateDirty;
    private static boolean queuedAlarmRefresh;

    private static ThreadPoolExecutor createExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (!isRefreshAction(action)) return;

        PendingResult result = goAsync();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                boolean refreshAlarm = shouldRefreshAlarm(action);
                boolean hasWidgets;
                if (refreshAlarm && isAlarmChangeAction(action)) {
                    // Samsung Clock can publish NEXT_ALARM_CLOCK_CHANGED before its distant
                    // recurring alarm has appeared in AlarmManager. Render the quick result,
                    // then confirm once after Samsung has finished updating its alarm entries.
                    SystemClock.sleep(ALARM_CHANGE_INITIAL_DELAY_MS);
                    hasWidgets = updateAll(appContext, true);
                    SystemClock.sleep(ALARM_CHANGE_CONFIRM_DELAY_MS);
                    hasWidgets = updateAll(appContext, true) || hasWidgets;
                } else {
                    hasWidgets = updateAll(appContext, refreshAlarm);
                }
                if (hasWidgets) scheduleRefreshes(appContext);
                else cancelRefreshes(appContext);
            } finally {
                result.finish();
            }
        });
    }

    static void handleInternalRefresh(Context context, String action,
                                      BroadcastReceiver.PendingResult result) {
        EXECUTOR.execute(() -> {
            try {
                if (ACTION_ALARM_VERIFY.equals(action)) {
                    verifyAlarm(context);
                } else if (ACTION_DATE_REFRESH.equals(action)) {
                    if (updateAll(context, false)) scheduleRefreshes(context);
                    else cancelRefreshes(context);
                }
            } finally {
                result.finish();
            }
        });
    }

    @Override public void onDisabled(Context context) {
        cancelRefreshes(context.getApplicationContext());
    }

    static void requestUpdate(Context context, boolean refreshAlarm) {
        Context appContext = context.getApplicationContext();
        synchronized (UPDATE_LOCK) {
            updateDirty = true;
            queuedAlarmRefresh |= refreshAlarm;
            if (updateQueued) return;
            updateQueued = true;
        }
        EXECUTOR.execute(() -> drainUpdates(appContext));
    }

    private static void drainUpdates(Context context) {
        while (true) {
            boolean refreshAlarm;
            synchronized (UPDATE_LOCK) {
                if (!updateDirty) {
                    updateQueued = false;
                    return;
                }
                updateDirty = false;
                refreshAlarm = queuedAlarmRefresh;
                queuedAlarmRefresh = false;
            }
            if (updateAll(context, refreshAlarm)) scheduleRefreshes(context);
            else cancelRefreshes(context);
        }
    }

    static boolean updateAll(Context context, boolean refreshAlarm) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AlarmWidgetProvider.class));
        if (ids.length == 0) return false;

        WidgetSettings settings = new WidgetSettings(context);
        WidgetSettings.Snapshot snapshot = settings.snapshot();
        Long alarmMillis = alarmMillis(context, snapshot.showAlarm, refreshAlarm);
        Map<Long, WidgetRenderer.WidgetFrame> renderCache = new HashMap<>();
        Intent launchIntent = resolveLaunchIntent(context, settings);

        for (int id : ids) {
            Bundle options = manager.getAppWidgetOptions(id);
            int minWidth = positive(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), 260);
            int minHeight = positive(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT), 64);
            int maxWidth = positive(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH), minWidth);
            int maxHeight = positive(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT), minHeight);

            PendingIntent click = createClickIntent(context, id, launchIntent);
            RemoteViews portrait = buildViews(context, minWidth, maxHeight, alarmMillis,
                    snapshot, renderCache, click);
            RemoteViews landscape = buildViews(context, maxWidth, minHeight, alarmMillis,
                    snapshot, renderCache, click);
            manager.updateAppWidget(id, new RemoteViews(landscape, portrait));
        }
        return true;
    }

    private static Long alarmMillis(Context context, boolean showAlarm, boolean refreshAlarm) {
        if (!showAlarm) return null;
        AlarmStateCache cache = new AlarmStateCache(context);
        if (refreshAlarm) {
            AlarmReader.Result result = AlarmReader.read(context);
            storeAlarmResult(cache, result, true);
        }
        return cache.get();
    }

    private static RemoteViews buildViews(Context context, int width, int height,
                                          Long alarmMillis, WidgetSettings.Snapshot settings,
                                          Map<Long, WidgetRenderer.WidgetFrame> renderCache,
                                          PendingIntent click) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.alarm_widget);
        long key = ((long) width << 32) ^ (height & 0xffffffffL);
        WidgetRenderer.WidgetFrame frame = renderCache.get(key);
        if (frame == null) {
            frame = WidgetRenderer.renderWidget(context, width, height, alarmMillis, settings);
            renderCache.put(key, frame);
        }
        views.setImageViewBitmap(R.id.widget_image, frame.bitmap);
        views.setViewVisibility(R.id.widget_clock, frame.showTime ? View.VISIBLE : View.GONE);
        if (frame.showTime) {
            views.setTextViewTextSize(R.id.widget_clock, TypedValue.COMPLEX_UNIT_DIP,
                    frame.timeSizeDp);
            views.setTextColor(R.id.widget_clock, frame.timeColor);
            views.setCharSequence(R.id.widget_clock, "setFormat12Hour", frame.timeFormat);
            views.setCharSequence(R.id.widget_clock, "setFormat24Hour", frame.timeFormat);
            views.setString(R.id.widget_clock, "setTimeZone", frame.timeZone);
            int horizontal = frame.alignment % 3;
            int gravity = Gravity.TOP | (horizontal == 0 ? Gravity.START
                    : horizontal == 1 ? Gravity.CENTER_HORIZONTAL : Gravity.END);
            views.setInt(R.id.widget_clock, "setGravity", gravity);
            float density = context.getResources().getDisplayMetrics().density;
            int sidePadding = Math.round(8f * density);
            views.setViewPadding(R.id.widget_clock, sidePadding, 0, sidePadding, 0);
            views.setFloat(R.id.widget_clock, "setTranslationY",
                    frame.timeTranslationDp * density);
        }
        if (click != null) {
            views.setOnClickPendingIntent(R.id.widget_root, click);
            views.setOnClickPendingIntent(R.id.widget_image, click);
            views.setOnClickPendingIntent(R.id.widget_clock, click);
        }
        return views;
    }

    private static Intent resolveLaunchIntent(Context context, WidgetSettings settings) {
        String clickPackage = settings.string(WidgetSettings.KEY_CLICK_PACKAGE,
                "com.sec.android.app.clockpackage");
        if (clickPackage.isEmpty()) return null;
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(clickPackage);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        return intent;
    }

    private static PendingIntent createClickIntent(Context context, int id, Intent launchIntent) {
        if (launchIntent == null) return null;
        return PendingIntent.getActivity(context, 100 + id, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static boolean isRefreshAction(String action) {
        return AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
                || AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || isAlarmChangeAction(action);
    }

    private static boolean shouldRefreshAlarm(String action) {
        return AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || isAlarmChangeAction(action);
    }

    private static boolean isAlarmChangeAction(String action) {
        return AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED.equals(action);
    }

    private static void verifyAlarm(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AlarmWidgetProvider.class));
        if (ids.length == 0) {
            cancelRefreshes(context);
            return;
        }
        WidgetSettings settings = new WidgetSettings(context);
        if (!settings.bool(WidgetSettings.KEY_SHOW_ALARM, true)) {
            cancelAlarmVerification(context);
            return;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            // This is a non-wakeup alarm: while the device sleeps it does not wake the CPU.
            // When delivery is deferred until the device becomes interactive, the next pass
            // performs one real alarm read. If delivery happens during another background wake,
            // only this inexpensive state check runs and dumpsys remains disabled.
            scheduleAlarmVerification(context, ALARM_VERIFY_INTERVAL_MS);
            return;
        }
        AlarmStateCache cache = new AlarmStateCache(context);
        Long previous = cache.get();
        AlarmReader.Result result = AlarmReader.read(context);
        Long current = storeAlarmResult(cache, result, false);
        if (!same(previous, current)) updateAll(context, false);
        scheduleAlarmVerification(context, ALARM_VERIFY_INTERVAL_MS);
    }

    private static Long storeAlarmResult(AlarmStateCache cache, AlarmReader.Result result,
                                         boolean logFailure) {
        if (result.triggerMillis != null || result.noAlarm) {
            cache.put(result.triggerMillis);
        } else if (logFailure && result.error != null) {
            Log.w(TAG, "Unable to refresh alarm: " + result.error);
        }
        return cache.get();
    }

    private static boolean same(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    private static void scheduleRefreshes(Context context) {
        WidgetSettings.Snapshot settings = new WidgetSettings(context).snapshot();
        if (settings.showDate) {
            scheduleDateRefresh(context, settings.timezone);
        } else {
            cancelDateRefresh(context);
        }
        if (settings.showAlarm) {
            scheduleAlarmVerification(context, ALARM_VERIFY_INTERVAL_MS);
        } else {
            cancelAlarmVerification(context);
        }
    }

    private static void cancelRefreshes(Context context) {
        cancelDateRefresh(context);
        cancelAlarmVerification(context);
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static void scheduleDateRefresh(Context context, String timezone) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        java.util.TimeZone zone = "auto".equals(timezone)
                ? java.util.TimeZone.getDefault() : java.util.TimeZone.getTimeZone(timezone);
        Calendar nextDay = Calendar.getInstance(zone);
        nextDay.add(Calendar.DAY_OF_YEAR, 1);
        nextDay.set(Calendar.HOUR_OF_DAY, 0);
        nextDay.set(Calendar.MINUTE, 0);
        nextDay.set(Calendar.SECOND, 2);
        nextDay.set(Calendar.MILLISECOND, 0);
        manager.setWindow(AlarmManager.RTC, nextDay.getTimeInMillis(), DATE_REFRESH_WINDOW_MS,
                dateRefreshIntent(context));
    }

    private static void cancelDateRefresh(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(dateRefreshIntent(context));
    }

    private static void scheduleAlarmVerification(Context context, long delayMillis) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        manager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + delayMillis,
                alarmVerificationIntent(context));
    }

    private static void cancelAlarmVerification(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(alarmVerificationIntent(context));
    }

    private static PendingIntent dateRefreshIntent(Context context) {
        Intent intent = new Intent(context, InternalRefreshReceiver.class)
                .setAction(ACTION_DATE_REFRESH);
        return PendingIntent.getBroadcast(context, 3, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent alarmVerificationIntent(Context context) {
        Intent intent = new Intent(context, InternalRefreshReceiver.class)
                .setAction(ACTION_ALARM_VERIFY);
        return PendingIntent.getBroadcast(context, 4, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
