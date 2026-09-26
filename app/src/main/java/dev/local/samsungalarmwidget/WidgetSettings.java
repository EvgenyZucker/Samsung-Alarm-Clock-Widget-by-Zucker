package dev.local.samsungalarmwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class WidgetSettings {
    static final String PREFS = "digital_clock_settings";
    static final String KEY_ALIGNMENT = "alignment";
    static final String KEY_FONT = "font";
    static final String KEY_FONT_SIZE = "font_size";
    static final String KEY_TIME_FORMAT = "time_format";
    static final String KEY_SHOW_TIME = "show_time";
    static final String KEY_SHOW_ALARM = "show_alarm";
    static final String KEY_SHOW_DATE = "show_date";
    static final String KEY_SHADOW = "shadow";
    static final String KEY_TIME_COLOR = "time_color";
    static final String KEY_DATE_COLOR = "date_color";
    static final String KEY_BACKGROUND_COLOR = "background_color";
    static final String KEY_DATE_WEEKDAY = "date_weekday";
    static final String KEY_DATE_DAY = "date_day";
    static final String KEY_DATE_YEAR = "date_year";
    static final String KEY_SHORT_WEEKDAY = "short_weekday";
    static final String KEY_SHORT_MONTH = "short_month";
    static final String KEY_SHORT_YEAR = "short_year";
    static final String KEY_NUMERIC_DATE = "numeric_date";
    static final String KEY_CLICK_PACKAGE = "click_package";
    static final String KEY_TIMEZONE = "timezone";

    private final SharedPreferences p;
    WidgetSettings(Context context) { p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    boolean bool(String key, boolean fallback) { return p.getBoolean(key, fallback); }
    int integer(String key, int fallback) { return p.getInt(key, fallback); }
    String string(String key, String fallback) { return p.getString(key, fallback); }
    void putBoolean(String key, boolean value) { p.edit().putBoolean(key, value).apply(); }
    void putInt(String key, int value) { p.edit().putInt(key, value).apply(); }
    void putString(String key, String value) { p.edit().putString(key, value).apply(); }

    Snapshot snapshot() {
        return new Snapshot(
                integer(KEY_ALIGNMENT, 1),
                string(KEY_FONT, "Pro"),
                integer(KEY_FONT_SIZE, 100),
                integer(KEY_TIME_FORMAT, 2),
                bool(KEY_SHOW_TIME, true),
                bool(KEY_SHOW_ALARM, true),
                bool(KEY_SHOW_DATE, false),
                bool(KEY_SHADOW, false),
                timeColor(),
                dateColor(),
                backgroundColor(),
                bool(KEY_DATE_WEEKDAY, true),
                bool(KEY_DATE_DAY, true),
                bool(KEY_DATE_YEAR, false),
                bool(KEY_SHORT_WEEKDAY, false),
                bool(KEY_SHORT_MONTH, false),
                bool(KEY_SHORT_YEAR, false),
                bool(KEY_NUMERIC_DATE, false),
                string(KEY_TIMEZONE, "auto"));
    }

    static final class Snapshot {
        final int alignment;
        final String font;
        final int fontSize;
        final int timeFormat;
        final boolean showTime;
        final boolean showAlarm;
        final boolean showDate;
        final boolean shadow;
        final int timeColor;
        final int dateColor;
        final int backgroundColor;
        final boolean dateWeekday;
        final boolean dateDay;
        final boolean dateYear;
        final boolean shortWeekday;
        final boolean shortMonth;
        final boolean shortYear;
        final boolean numericDate;
        final String timezone;

        Snapshot(int alignment, String font, int fontSize, int timeFormat,
                 boolean showTime, boolean showAlarm, boolean showDate, boolean shadow,
                 int timeColor, int dateColor, int backgroundColor,
                 boolean dateWeekday, boolean dateDay, boolean dateYear,
                 boolean shortWeekday, boolean shortMonth, boolean shortYear,
                 boolean numericDate, String timezone) {
            this.alignment = alignment;
            this.font = font;
            this.fontSize = fontSize;
            this.timeFormat = timeFormat;
            this.showTime = showTime;
            this.showAlarm = showAlarm;
            this.showDate = showDate;
            this.shadow = shadow;
            this.timeColor = timeColor;
            this.dateColor = dateColor;
            this.backgroundColor = backgroundColor;
            this.dateWeekday = dateWeekday;
            this.dateDay = dateDay;
            this.dateYear = dateYear;
            this.shortWeekday = shortWeekday;
            this.shortMonth = shortMonth;
            this.shortYear = shortYear;
            this.numericDate = numericDate;
            this.timezone = timezone;
        }

        String renderKey() {
            return alignment + "|" + font + "|" + fontSize + "|" + timeFormat + "|"
                    + showTime + "|" + showAlarm + "|" + showDate + "|" + shadow + "|"
                    + timeColor + "|" + dateColor + "|" + backgroundColor + "|"
                    + dateWeekday + "|" + dateDay + "|" + dateYear + "|"
                    + shortWeekday + "|" + shortMonth + "|" + shortYear + "|"
                    + numericDate + "|" + timezone;
        }
    }

    int timeColor() { return integer(KEY_TIME_COLOR, Color.WHITE); }
    int dateColor() { return integer(KEY_DATE_COLOR, Color.WHITE); }
    int backgroundColor() { return integer(KEY_BACKGROUND_COLOR, Color.TRANSPARENT); }
}
