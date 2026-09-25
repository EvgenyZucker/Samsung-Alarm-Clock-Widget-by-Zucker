package dev.local.samsungalarmwidget;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AlarmReader {
    private static final Pattern ALARM_HEADER = Pattern.compile(
            "^\\s*(?:RTC_WAKEUP|RTC|ELAPSED_WAKEUP|ELAPSED) #\\d+:.*$");
    private static final Pattern ORIG_WHEN = Pattern.compile("\\borigWhen[ =](\\d{13})\\b");

    static final class Result {
        final Long triggerMillis;
        final String error;
        Result(Long triggerMillis, String error) { this.triggerMillis = triggerMillis; this.error = error; }
    }

    private AlarmReader() {}

    static Result read() {
        Process process = null;
        try {
            // Keep the command and timestamp parsing identical to the proven 0.6 build.
            process = new ProcessBuilder("/system/bin/dumpsys", "alarm")
                    .redirectErrorStream(false).start();
            long now = System.currentTimeMillis();
            long best = Long.MAX_VALUE;
            boolean inAlarmBlock = false;
            boolean samsung = false;
            boolean explicit = false;
            Long blockTime = null;
            StringBuilder diagnostics = new StringBuilder();
            StringBuilder clockDiagnostics = new StringBuilder();
            Deque<String> recentLines = new ArrayDeque<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (diagnostics.length() < 1200) diagnostics.append(line).append('\n');
                    if (line.contains("clockpackage") || line.contains("EXPLICIT_ALARM_ALERT")
                            || line.contains("UPCOMING_ALERT") || line.contains("Permission Denial")) {
                        if (clockDiagnostics.length() < 12000) {
                            for (String recent : recentLines) clockDiagnostics.append(recent).append('\n');
                            clockDiagnostics.append(line).append("\n---\n");
                        }
                    }
                    recentLines.addLast(line);
                    while (recentLines.size() > 3) recentLines.removeFirst();
                    if (ALARM_HEADER.matcher(line).matches()) {
                        if (inAlarmBlock && samsung && explicit && blockTime != null
                                && blockTime > now && blockTime < best) best = blockTime;
                        inAlarmBlock = true;
                        samsung = line.contains("com.sec.android.app.clockpackage");
                        explicit = false;
                        // In Samsung's dumpsys output the only numeric epoch origWhen is
                        // on this header line. This is also how the working 0.6 reader behaved.
                        blockTime = extractTime(line);
                        continue;
                    }
                    if (inAlarmBlock) {
                        if (line.contains("com.sec.android.app.clockpackage")) samsung = true;
                        if (line.contains("EXPLICIT_ALARM_ALERT")) explicit = true;
                        Long found = extractTime(line);
                        if (found != null) blockTime = found;
                    }
                }
            }
            if (inAlarmBlock && samsung && explicit && blockTime != null
                    && blockTime > now && blockTime < best) best = blockTime;
            int exit = process.waitFor();
            if (exit != 0) return new Result(null, "dumpsys alarm завершился с кодом " + exit + "\n" + diagnostics);
            if (best == Long.MAX_VALUE) {
                String detail = clockDiagnostics.length() == 0
                        ? "Совпадений clockpackage в выводе нет. Начало вывода:\n" + diagnostics
                        : "Найдены связанные строки:\n" + clockDiagnostics;
                return new Result(null, "Активный Samsung Alarm не найден\n\n" + detail);
            }
            return new Result(best, null);
        } catch (Exception e) {
            return new Result(null, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (process != null) process.destroy();
        }
    }

    private static Long extractTime(String line) {
        Matcher matcher = ORIG_WHEN.matcher(line);
        if (!matcher.find()) return null;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
