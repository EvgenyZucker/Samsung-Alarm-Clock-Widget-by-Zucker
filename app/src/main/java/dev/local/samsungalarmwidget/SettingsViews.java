package dev.local.samsungalarmwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

final class SettingsViews {
    private SettingsViews() {}

    static final class MaterialToggle extends View {
        private boolean checked;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        MaterialToggle(Context context, boolean checked) {
            super(context);
            this.checked = checked;
            setClickable(true);
        }

        void toggle() {
            checked = !checked;
            invalidate();
        }

        boolean isChecked() {
            return checked;
        }

        @Override protected void onDraw(Canvas canvas) {
            float height = getHeight();
            float centerY = height / 2f;
            paint.setColor(checked ? 0xFF3F78DD : 0xFF77777F);
            canvas.drawRoundRect(0, 0, getWidth(), height, height, height, paint);
            paint.setColor(checked ? 0xFFD0D1CA : 0xFFBDBDC2);
            canvas.drawCircle(checked ? getWidth() - dp(16) : dp(16), centerY, dp(12), paint);
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }

    static final class DiscreteSizeSlider extends View {
        private static final int MIN = 20;
        private static final int MAX = 100;
        private static final int STEP = 10;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int selected;

        DiscreteSizeSlider(Context context, int value) {
            super(context);
            selected = Math.max(0, Math.min((MAX - MIN) / STEP,
                    Math.round((value - MIN) / (float) STEP)));
            setClickable(true);
        }

        @Override protected void onDraw(Canvas canvas) {
            float left = dp(4);
            float right = getWidth() - dp(4);
            float centerY = getHeight() / 2f;
            float x = left + (right - left) * selected / ((MAX - MIN) / (float) STEP);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(4));
            paint.setColor(0xFF66666C);
            canvas.drawLine(left, centerY, right, centerY, paint);
            paint.setColor(0xFF4F86F7);
            canvas.drawLine(left, centerY, x, centerY, paint);
            for (int i = 0; i <= (MAX - MIN) / STEP; i++) {
                float tickX = left + (right - left) * i / ((MAX - MIN) / (float) STEP);
                paint.setColor(0xFFF2F0F4);
                canvas.drawCircle(tickX, centerY, dp(2), paint);
            }
            paint.setColor(0xFF4F86F7);
            canvas.drawRoundRect(x - dp(2), centerY - dp(23), x + dp(2), centerY + dp(23),
                    dp(2), dp(2), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE
                    && action != MotionEvent.ACTION_UP) {
                return false;
            }
            float left = dp(4);
            float right = getWidth() - dp(4);
            selected = Math.max(0, Math.min((MAX - MIN) / STEP,
                    Math.round((event.getX() - left) / (right - left) * ((MAX - MIN) / STEP))));
            invalidate();
            return true;
        }

        int value() {
            return MIN + selected * STEP;
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }

    static final class ColorSwatch extends View {
        private final int color;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorSwatch(Context context, int color) {
            super(context);
            this.color = color;
        }

        @Override protected void onDraw(Canvas canvas) {
            float square = dp(6);
            float radius = getWidth() / 2f;
            canvas.save();
            Path clip = new Path();
            clip.addCircle(radius, radius, radius - 1, Path.Direction.CW);
            canvas.clipPath(clip);
            if (Color.alpha(color) < 255) {
                for (int y = 0; y < getHeight(); y += square) {
                    for (int x = 0; x < getWidth(); x += square) {
                        paint.setColor(((x / (int) square + y / (int) square) & 1) == 0
                                ? Color.WHITE : 0xFFC8C8C8);
                        canvas.drawRect(x, y, x + square, y + square, paint);
                    }
                }
            }
            paint.setColor(color);
            canvas.drawCircle(radius, radius, radius, paint);
            canvas.restore();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0xFFB9B6BE);
            canvas.drawCircle(radius, radius, radius - dp(1), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }
}
