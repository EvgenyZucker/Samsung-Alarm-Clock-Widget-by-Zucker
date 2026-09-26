package dev.local.samsungalarmwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class WidgetRenderer {
    private static final int MAX_FIT_CACHE_ENTRIES = 48;
    private static final int MAX_FRAME_CACHE_ENTRIES = 12;
    private static final Map<String, Float> FIT_CACHE =
            new LinkedHashMap<String, Float>(MAX_FIT_CACHE_ENTRIES, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Float> eldest) {
                    return size() > MAX_FIT_CACHE_ENTRIES;
                }
            };
    private static final Map<String, Typeface> TYPEFACE_CACHE = new LinkedHashMap<>();
    private static final Map<String, WidgetFrame> FRAME_CACHE =
            new LinkedHashMap<String, WidgetFrame>(MAX_FRAME_CACHE_ENTRIES, .75f, true) {
                @Override protected boolean removeEldestEntry(
                        Map.Entry<String, WidgetFrame> eldest) {
                    return size() > MAX_FRAME_CACHE_ENTRIES;
                }
            };

    private WidgetRenderer() {}

    static final class WidgetFrame {
        final Bitmap bitmap;
        final float timeSizeDp;
        final float timeTranslationDp;
        final int timeColor;
        final int alignment;
        final boolean showTime;
        final String timeFormat;
        final String timeZone;
        final float secondarySizeDp;
        final String date;
        final String alarm;

        WidgetFrame(Bitmap bitmap, float timeSizeDp, float timeTranslationDp,
                    int timeColor, int alignment,
                    boolean showTime, String timeFormat, String timeZone,
                    float secondarySizeDp, String date, String alarm) {
            this.bitmap = bitmap;
            this.timeSizeDp = timeSizeDp;
            this.timeTranslationDp = timeTranslationDp;
            this.timeColor = timeColor;
            this.alignment = alignment;
            this.showTime = showTime;
            this.timeFormat = timeFormat;
            this.timeZone = timeZone;
            this.secondarySizeDp = secondarySizeDp;
            this.date = date;
            this.alarm = alarm;
        }
    }

    static Bitmap render(Context context, int widthDp, int heightDp, Long alarmMillis) {
        WidgetSettings.Snapshot settings = new WidgetSettings(context).snapshot();
        return renderInternal(context, widthDp, heightDp, alarmMillis, true, settings).bitmap;
    }

    static WidgetFrame renderWidget(Context context, int widthDp, int heightDp, Long alarmMillis) {
        WidgetSettings.Snapshot settings = new WidgetSettings(context).snapshot();
        return renderInternal(context, widthDp, heightDp, alarmMillis, false, settings);
    }

    static WidgetFrame renderWidget(Context context, int widthDp, int heightDp, Long alarmMillis,
                                    WidgetSettings.Snapshot settings) {
        TimeZone zone = "auto".equals(settings.timezone)
                ? TimeZone.getDefault() : TimeZone.getTimeZone(settings.timezone);
        Date now = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        dayFormat.setTimeZone(zone);
        String key = widthDp + "x" + heightDp + '|'
                + Float.floatToIntBits(Math.min(2f,
                context.getResources().getDisplayMetrics().density)) + '|'
                + settings.renderKey() + '|' + (alarmMillis == null ? "none" : alarmMillis)
                + '|' + dayFormat.format(now) + '|' + zone.getOffset(now.getTime())
                + '|' + Locale.getDefault().toLanguageTag();
        synchronized (FRAME_CACHE) {
            WidgetFrame cached = FRAME_CACHE.get(key);
            if (cached != null && !cached.bitmap.isRecycled()) return cached;
        }
        WidgetFrame rendered = renderInternal(
                context, widthDp, heightDp, alarmMillis, false, settings);
        synchronized (FRAME_CACHE) {
            FRAME_CACHE.put(key, rendered);
        }
        return rendered;
    }

    private static WidgetFrame renderInternal(Context context, int widthDp, int heightDp,
                                              Long alarmMillis, boolean drawTime,
                                              WidgetSettings.Snapshot settings) {
        // RemoteViews travels through Binder; cap raster density to stay below its payload limit.
        float density = Math.min(2f, context.getResources().getDisplayMetrics().density);
        int width = Math.max(1, Math.round(widthDp * density));
        int height = Math.max(1, Math.round(heightDp * density));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(settings.backgroundColor);

        TimeZone zone = "auto".equals(settings.timezone)
                ? TimeZone.getDefault() : TimeZone.getTimeZone(settings.timezone);
        Date now = new Date();
        int tf = settings.timeFormat;
        String timePattern = tf == 0 ? "h:mm" : tf == 1 ? "h:mm a" : "HH:mm";
        SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern, Locale.getDefault());
        timeFormat.setTimeZone(zone);

        Typeface face = typeface(settings.font);
        Paint timePaint = paint(settings.timeColor, density, face);
        Paint secondaryPaint = paint(settings.dateColor, density, face);
        if (settings.shadow) {
            timePaint.setShadowLayer(4f * density, .4f * density, 2f * density, 0x80000000);
            secondaryPaint.setShadowLayer(4f * density, .4f * density, 2f * density, 0x80000000);
        }

        String time = settings.showTime ? timeFormat.format(now) : "";
        String date = settings.showDate ? formatDate(now, settings, zone) : "";
        String alarm = "";
        if (alarmMillis != null && settings.showAlarm) {
            SimpleDateFormat af = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());
            af.setTimeZone(zone);
            alarm = af.format(new Date(alarmMillis));
        }

        String zoneLabel = timeZoneLabel(settings.timezone, zone, now.getTime());
        String leading = date + (!date.isEmpty() && !zoneLabel.isEmpty() ? " " : "") + zoneLabel;
        String separator = !leading.isEmpty() && !alarm.isEmpty()
                ? (zoneLabel.isEmpty() ? "   " : " ") : "";
        // The original uses U+E855 from its bundled alarm icon font. Keep the
        // same character for measurement; it is painted with that font below.
        String alarmIcon = alarm.isEmpty() ? "" : "\uE855";
        String secondary = leading + separator + (alarm.isEmpty() ? "" : alarmIcon + " " + alarm);

        // The original APK measures the widget and binary-searches for the largest size
        // that fits, capped at 168 dp and scaled by the user's percentage. Its secondary
        // line is one quarter of the clock size (SizesKt.b in the decompiled APK).
        // The original Clock.Background has only start/end padding (8 dp).
        // Its top and bottom padding are explicitly 0 dp.
        float horizontalPadding = 8f * density;
        // Clock.Container.Date uses layout_marginTop=-4 dp, so the second line
        // overlaps the clock line's layout box slightly.
        float secondaryTopMargin = secondary.isEmpty() ? 0f : -4f * density;
        float maxTimeSize = 168f * density * settings.fontSize / 100f;
        String fitKey = width + "x" + height + '|' + Float.floatToIntBits(density) + '|'
                + settings.font + '|' + settings.fontSize
                + '|' + timePattern + '|' + time + '|' + secondary + '|'
                + Locale.getDefault().toLanguageTag();
        Float cachedTimeSize = cachedFit(fitKey);
        float timeSize;
        if (cachedTimeSize != null) {
            timeSize = cachedTimeSize;
        } else {
            timeSize = fittedTimeSize(timePaint, secondaryPaint, timeFormat,
                    time, secondary, width - 2f * horizontalPadding, height,
                    secondaryTopMargin, maxTimeSize);
            cacheFit(fitKey, timeSize);
        }
        timePaint.setTextSize(timeSize);
        secondaryPaint.setTextSize(Math.max(1f, timeSize / 4f));

        float gap = secondaryTopMargin;
        int alignment = settings.alignment;
        int vertical = alignment / 3;
        int horizontal = alignment % 3;
        float x = horizontal == 0 ? horizontalPadding : horizontal == 1 ? width / 2f : width - horizontalPadding;
        Paint.Align align = horizontal == 0 ? Paint.Align.LEFT : horizontal == 1 ? Paint.Align.CENTER : Paint.Align.RIGHT;
        timePaint.setTextAlign(align);
        secondaryPaint.setTextAlign(align);

        // Align using the glyph pixels that are actually visible. Font ascent/descent includes
        // generous invisible padding, which previously made top and centre look identical.
        float timeBaseline = time.isEmpty() ? 0f : -timePaint.ascent();
        float secondaryBaseline;
        if (!time.isEmpty()) {
            secondaryBaseline = timeBaseline + timePaint.descent()
                    + gap - secondaryPaint.ascent();
        }
        else secondaryBaseline = -secondaryPaint.ascent();

        if (!secondary.isEmpty()) {
            secondaryBaseline -= (drawTime ? 4.0f : 14.6f) * density;
        }

        Rect bounds = new Rect();
        float visibleTop = Float.MAX_VALUE;
        float visibleBottom = -Float.MAX_VALUE;
        if (!time.isEmpty()) {
            timePaint.getTextBounds(time, 0, time.length(), bounds);
            visibleTop = Math.min(visibleTop, timeBaseline + bounds.top);
            visibleBottom = Math.max(visibleBottom, timeBaseline + bounds.bottom);
        }
        if (!secondary.isEmpty()) {
            secondaryPaint.getTextBounds(secondary, 0, secondary.length(), bounds);
            visibleTop = Math.min(visibleTop, secondaryBaseline + bounds.top);
            visibleBottom = Math.max(visibleBottom, secondaryBaseline + bounds.bottom);
        }
        if (visibleTop == Float.MAX_VALUE) {
            visibleTop = 0f;
            visibleBottom = 0f;
        }
        float shadowInset = settings.shadow ? 4f * density : 0f;
        float visibleHeight = visibleBottom - visibleTop;
        float targetTop = vertical == 0 ? shadowInset
                : vertical == 1 ? (height - visibleHeight) / 2f
                : height - visibleHeight - shadowInset;
        float shift = targetTop - visibleTop;
        timeBaseline += shift;
        secondaryBaseline += shift;

        if (!time.isEmpty() && drawTime) {
            canvas.drawText(time, x, timeBaseline, timePaint);
        }

        if (!secondary.isEmpty()) {
            // Calibrated from One UI's actual 235.37778 x 97.77778 widget area
            // and 2.8125 display density, rather than the nominal grid size.
            // The settings preview in the original APK uses the layout's natural
            // line spacing. The installed widget needs the calibrated overlap.
            drawSecondary(context, canvas, leading, separator, alarmIcon, alarm,
                    x + 1.4f * density, secondaryBaseline, secondaryPaint, align);
        }
        float textClockTopBaseline = -timePaint.ascent();
        float timeTranslation = timeBaseline - textClockTopBaseline;
        return new WidgetFrame(bitmap, timeSize / density, timeTranslation / density,
                settings.timeColor, alignment,
                !time.isEmpty(), timePattern, zone.getID(), timeSize / (4f * density), date, alarm);
    }

    private static float fittedTimeSize(Paint timePaint, Paint secondaryPaint,
                                        SimpleDateFormat format, String time, String secondary,
                                        float availableWidth, float availableHeight,
                                        float secondaryTopMargin, float maximum) {
        String widestTime = time;
        Date probe = new Date();
        for (int hour = 0; hour < 24; hour++) {
            probe.setHours(hour);
            probe.setMinutes(59);
            String candidate = format.format(probe);
            timePaint.setTextSize(maximum);
            if (timePaint.measureText(candidate) > timePaint.measureText(widestTime)) widestTime = candidate;
        }
        float low = 1f, high = Math.max(1f, maximum);
        for (int i = 0; i < 18; i++) {
            float mid = (low + high) / 2f;
            timePaint.setTextSize(mid);
            secondaryPaint.setTextSize(Math.max(1f, mid / 4f));
            float width = Math.max(time.isEmpty() ? 0 : timePaint.measureText(widestTime),
                    secondary.isEmpty() ? 0 : secondaryPaint.measureText(secondary));
            float height = (time.isEmpty() ? 0 : -timePaint.ascent() + timePaint.descent())
                    + (secondary.isEmpty() ? 0 : secondaryTopMargin
                    - secondaryPaint.ascent() + secondaryPaint.descent());
            if (width <= availableWidth && height <= availableHeight) low = mid;
            else high = mid;
        }
        return low;
    }

    private static Paint paint(int color, float size, Typeface face) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(color); p.setTextSize(size); p.setTypeface(face);
        return p;
    }

    private static void drawSecondary(Context context, Canvas canvas, String date,
                                      String separator, String alarmIcon, String alarm,
                                      float anchorX, float baseline, Paint textPaint,
                                      Paint.Align align) {
        if (alarm.isEmpty()) {
            canvas.drawText(date, anchorX, baseline, textPaint);
            return;
        }

        Paint iconPaint = new Paint(textPaint);
        iconPaint.setTypeface(context.getResources().getFont(R.font.alarm));
        float renderDensity = Math.min(2f, context.getResources().getDisplayMetrics().density);
        String beforeIcon = date + separator;
        float beforeWidth = textPaint.measureText(beforeIcon);
        float iconWidth = iconPaint.measureText(alarmIcon);
        float spaceWidth = textPaint.measureText(" ");
        float alarmWidth = textPaint.measureText(alarm);
        float totalWidth = beforeWidth + iconWidth + spaceWidth + alarmWidth;
        float start = align == Paint.Align.LEFT ? anchorX
                : align == Paint.Align.CENTER ? anchorX - totalWidth / 2f
                : anchorX - totalWidth;

        textPaint.setTextAlign(Paint.Align.LEFT);
        iconPaint.setTextAlign(Paint.Align.LEFT);
        if (!beforeIcon.isEmpty()) canvas.drawText(beforeIcon, start, baseline, textPaint);
        start += beforeWidth;
        // The original renders the glyph into a dedicated ImageView. A 6%
        // reduction reproduces its 17x16 px visible bounds; the small x offset
        // preserves the original image cell alignment.
        iconPaint.setTextSize(iconPaint.getTextSize() * 0.94f);
        canvas.drawText(alarmIcon, start + 0.7f * renderDensity,
                baseline + 2.15f * renderDensity, iconPaint);
        start += iconWidth + spaceWidth;
        // Original ImageView-to-text gap is two screenshot pixels wider.
        canvas.drawText(alarm, start + 1.4f * renderDensity, baseline, textPaint);
        textPaint.setTextAlign(align);
    }

    private static Typeface typeface(String name) {
        synchronized (TYPEFACE_CACHE) {
            Typeface cached = TYPEFACE_CACHE.get(name);
            if (cached != null) return cached;
            Typeface created;
            switch (name) {
                case "Regular": created = Typeface.create("sans-serif", Typeface.NORMAL); break;
                case "Light": created = Typeface.create("sans-serif-light", Typeface.NORMAL); break;
                case "Medium": created = Typeface.create("sans-serif-medium", Typeface.NORMAL); break;
                case "Condensed": created = Typeface.create("sans-serif-condensed", Typeface.NORMAL); break;
                case "Classic": created = Typeface.create("serif", Typeface.NORMAL); break;
                default: created = Typeface.create("source-sans-pro", Typeface.NORMAL); break;
            }
            TYPEFACE_CACHE.put(name, created);
            return created;
        }
    }

    private static Float cachedFit(String key) {
        synchronized (FIT_CACHE) {
            return FIT_CACHE.get(key);
        }
    }

    private static void cacheFit(String key, float value) {
        synchronized (FIT_CACHE) {
            FIT_CACHE.put(key, value);
        }
    }

    private static String timeZoneLabel(String configuredId, TimeZone selected,
                                        long timestamp) {
        if ("auto".equals(configuredId)) return "";
        TimeZone automatic = TimeZone.getDefault();
        int selectedOffset = selected.getOffset(timestamp);
        if (selectedOffset == automatic.getOffset(timestamp)) return "";
        int totalMinutes = Math.abs(selectedOffset / 60_000);
        return String.format(Locale.US, "GMT%s%02d:%02d", selectedOffset < 0 ? "-" : "+",
                totalMinutes / 60, totalMinutes % 60);
    }

    static String formatDate(Date date, WidgetSettings s, TimeZone zone) {
        return formatDate(date, s.snapshot(), zone);
    }

    private static String formatDate(Date date, WidgetSettings.Snapshot settings, TimeZone zone) {
        List<String> parts = new ArrayList<>();
        if (settings.dateWeekday)
            parts.add(format(date, settings.shortWeekday ? "EEE" : "EEEE", zone));
        if (settings.dateDay) {
            String pattern = settings.numericDate ? "dd.MM"
                    : "d " + (settings.shortMonth ? "MMM" : "MMMM");
            parts.add(format(date, pattern, zone));
        }
        if (settings.dateYear)
            parts.add(format(date, settings.shortYear ? "yy" : "yyyy", zone));
        return String.join(", ", parts);
    }

    private static String format(Date d, String pattern, TimeZone zone) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.getDefault()); f.setTimeZone(zone); return f.format(d);
    }
}
