package dev.local.samsungalarmwidget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

final class PickerAdapters {
    private static final int TEXT = 0xFFF2F0F4;
    private static final int SUB = 0xFFC8C5CD;
    private static final int BLUE = 0xFF5289FA;

    private PickerAdapters() {}

    static final class TimezoneAdapter extends BaseAdapter {
        private final Context context;
        private final List<MainActivity.Zone> zones;
        private final String selectedId;

        TimezoneAdapter(Context context, List<MainActivity.Zone> zones, String selectedId) {
            this.context = context;
            this.zones = zones;
            this.selectedId = selectedId;
        }

        @Override public int getCount() { return zones.size(); }
        @Override public MainActivity.Zone getItem(int position) { return zones.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            TimezoneHolder holder;
            if (convertView == null) {
                holder = createTimezoneHolder();
                convertView = holder.root;
                convertView.setTag(holder);
            } else {
                holder = (TimezoneHolder) convertView.getTag();
            }
            MainActivity.Zone zone = getItem(position);
            holder.name.setText(zone.name);
            holder.offset.setText(zone.offset);
            holder.offset.setVisibility(zone.offset.isEmpty() ? View.GONE : View.VISIBLE);
            holder.check.setVisibility(selectedId.equals(zone.id) ? View.VISIBLE : View.INVISIBLE);
            return convertView;
        }

        private TimezoneHolder createTimezoneHolder() {
            LinearLayout root = new LinearLayout(context);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(20), dp(8), dp(20), dp(8));
            root.setMinimumHeight(dp(64));
            TextView dot = text("●", 18, SUB);
            dot.setGravity(Gravity.CENTER);
            dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            root.addView(dot, new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout labels = new LinearLayout(context);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setBackgroundColor(Color.TRANSPARENT);
            labels.setPadding(dp(12), 0, 0, 0);
            TextView name = text("", 16, TEXT);
            TextView offset = text("", 14, TEXT);
            labels.addView(name);
            labels.addView(offset);
            root.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView check = text("✓", 24, BLUE);
            check.setContentDescription(context.getString(R.string.selected));
            root.addView(check);
            return new TimezoneHolder(root, name, offset, check);
        }

        private final class TimezoneHolder {
            final LinearLayout root;
            final TextView name;
            final TextView offset;
            final TextView check;

            TimezoneHolder(LinearLayout root, TextView name, TextView offset, TextView check) {
                this.root = root;
                this.name = name;
                this.offset = offset;
                this.check = check;
            }
        }

        private TextView text(String value, float size, int color) {
            TextView view = new TextView(context);
            view.setText(value);
            view.setTextSize(size);
            view.setTextColor(color);
            return view;
        }

        private int dp(int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }

    static final class AppAdapter extends BaseAdapter {
        private final Context context;
        private final PackageManager packageManager;
        private final List<ResolveInfo> items;
        private final boolean shortcuts;
        private final LruCache<Integer, Drawable.ConstantState> iconCache = new LruCache<>(48);
        private final LruCache<Integer, CharSequence> labelCache = new LruCache<>(96);

        AppAdapter(Context context, PackageManager packageManager, List<ResolveInfo> items,
                   boolean shortcuts) {
            this.context = context;
            this.packageManager = packageManager;
            this.items = items;
            this.shortcuts = shortcuts;
        }

        @Override public int getCount() { return items.size() + (shortcuts ? 0 : 2); }
        @Override public ResolveInfo getItem(int position) {
            int itemPosition = position - (shortcuts ? 0 : 2);
            return itemPosition < 0 ? null : items.get(itemPosition);
        }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            AppHolder holder;
            if (convertView == null) {
                holder = createAppHolder();
                convertView = holder.root;
                convertView.setTag(holder);
            } else {
                holder = (AppHolder) convertView.getTag();
            }
            holder.icon.clearColorFilter();
            if (!shortcuts && position == 0) {
                holder.icon.setImageResource(R.drawable.ic_outline_block);
                holder.icon.setColorFilter(SUB);
                holder.label.setText(R.string.do_nothing);
            } else if (!shortcuts && position == 1) {
                Drawable clockIcon = cachedClockIcon(position);
                if (clockIcon != null) holder.icon.setImageDrawable(clockIcon);
                else holder.icon.setImageResource(R.drawable.ic_outline_alarm);
                holder.label.setText(R.string.open_clock);
            } else {
                ResolveInfo info = getItem(position);
                holder.icon.setImageDrawable(cachedIcon(position, info));
                CharSequence label = labelCache.get(position);
                if (label == null) {
                    label = info.loadLabel(packageManager);
                    labelCache.put(position, label);
                }
                holder.label.setText(label);
            }
            return convertView;
        }

        private Drawable cachedClockIcon(int position) {
            Drawable.ConstantState state = iconCache.get(position);
            if (state != null) return state.newDrawable(context.getResources());
            try {
                Drawable icon = packageManager.getApplicationIcon(
                        "com.sec.android.app.clockpackage");
                cacheIcon(position, icon);
                return icon;
            } catch (PackageManager.NameNotFoundException ignored) {
                return null;
            }
        }

        private Drawable cachedIcon(int position, ResolveInfo info) {
            Drawable.ConstantState state = iconCache.get(position);
            if (state != null) return state.newDrawable(context.getResources());
            Drawable icon = info.loadIcon(packageManager);
            cacheIcon(position, icon);
            return icon;
        }

        private void cacheIcon(int position, Drawable icon) {
            if (icon != null && icon.getConstantState() != null) {
                iconCache.put(position, icon.getConstantState());
            }
        }

        private AppHolder createAppHolder() {
            LinearLayout root = new LinearLayout(context);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(16), 0, dp(16), 0);
            root.setMinimumHeight(dp(64));
            ImageView icon = new ImageView(context);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconParams.setMarginEnd(dp(16));
            root.addView(icon, iconParams);
            TextView label = new TextView(context);
            label.setTextSize(16);
            label.setTextColor(TEXT);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setPadding(0, dp(8), 0, dp(8));
            root.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            return new AppHolder(root, icon, label);
        }

        private static final class AppHolder {
            final LinearLayout root;
            final ImageView icon;
            final TextView label;

            AppHolder(LinearLayout root, ImageView icon, TextView label) {
                this.root = root;
                this.icon = icon;
                this.label = label;
            }
        }

        private int dp(int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
