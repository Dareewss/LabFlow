package com.labflow.util;

import com.labflow.model.RecentLoginEntry;
import com.labflow.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

public final class RecentLoginService {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(RecentLoginService.class);
    private static final String KEY = "recentLogins";
    private static final int MAX_ENTRIES = 6;
    private static final long REVERIFY_WINDOW_MS = 24L * 60L * 60L * 1000L;

    private RecentLoginService() {
    }

    public static void recordSuccessfulLogin(User user) {
        if (user == null || user.getId() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        List<RecentLoginEntry> entries = getRecentLogins();
        entries.removeIf(entry -> entry.getUserId() == user.getId());
        entries.add(0, new RecentLoginEntry(
                user.getId(),
                safe(user.getUsername()),
                safe(user.getFullName()),
                user.getRole() == null ? "" : user.getRole().getDisplayName(),
                now,
                now
        ));
        save(trim(entries));
    }

    public static void recordQuickAccess(RecentLoginEntry entry) {
        if (entry == null || entry.getUserId() <= 0) {
            return;
        }
        List<RecentLoginEntry> entries = getRecentLogins();
        long now = System.currentTimeMillis();
        entries.removeIf(existing -> existing.getUserId() == entry.getUserId());
        entries.add(0, new RecentLoginEntry(
                entry.getUserId(),
                safe(entry.getUsername()),
                safe(entry.getFullName()),
                safe(entry.getRole()),
                now,
                entry.getLastVerifiedAt()
        ));
        save(trim(entries));
    }

    public static List<RecentLoginEntry> getRecentLogins() {
        String raw = PREFERENCES.get(KEY, "");
        List<RecentLoginEntry> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return entries;
        }
        for (String line : raw.split("\n")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 6) {
                continue;
            }
            try {
                int userId = Integer.parseInt(parts[0]);
                long lastUsed = Long.parseLong(parts[4]);
                long lastVerified = Long.parseLong(parts[5]);
                entries.add(new RecentLoginEntry(
                        userId,
                        unescape(parts[1]),
                        unescape(parts[2]),
                        unescape(parts[3]),
                        lastUsed,
                        lastVerified
                ));
            } catch (Exception ignored) {
            }
        }
        entries.sort(Comparator.comparingLong(RecentLoginEntry::getLastUsedAt).reversed());
        return trim(entries);
    }

    public static void removeUser(int userId) {
        List<RecentLoginEntry> entries = getRecentLogins();
        entries.removeIf(entry -> entry.getUserId() == userId);
        save(entries);
    }

    public static boolean requiresPassword(RecentLoginEntry entry) {
        return entry == null || System.currentTimeMillis() - entry.getLastVerifiedAt() > REVERIFY_WINDOW_MS;
    }

    public static String relativeAccessText(RecentLoginEntry entry) {
        if (entry == null || entry.getLastUsedAt() <= 0) {
            return "No recent access";
        }
        long delta = Math.max(0L, System.currentTimeMillis() - entry.getLastUsedAt());
        long hours = delta / (60L * 60L * 1000L);
        if (hours < 1) {
            return "Active recently";
        }
        if (hours < 24) {
            return "Used " + hours + "h ago";
        }
        long days = hours / 24;
        return "Used " + days + "d ago";
    }

    private static List<RecentLoginEntry> trim(List<RecentLoginEntry> entries) {
        return new ArrayList<>(entries.stream().limit(MAX_ENTRIES).toList());
    }

    private static void save(List<RecentLoginEntry> entries) {
        StringBuilder builder = new StringBuilder();
        for (RecentLoginEntry entry : entries) {
            if (entry.getUserId() <= 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(entry.getUserId()).append('|')
                    .append(escape(entry.getUsername())).append('|')
                    .append(escape(entry.getFullName())).append('|')
                    .append(escape(entry.getRole())).append('|')
                    .append(entry.getLastUsedAt()).append('|')
                    .append(entry.getLastVerifiedAt());
        }
        PREFERENCES.put(KEY, builder.toString());
    }

    private static String escape(String value) {
        return safe(value).replace("\\", "\\\\").replace("|", "\\p").replace("\n", " ");
    }

    private static String unescape(String value) {
        return safe(value).replace("\\p", "|").replace("\\\\", "\\");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
