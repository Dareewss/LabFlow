package com.labflow.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AuditDiffUtil {
    private AuditDiffUtil() {
    }

    public static AuditDiffBuilder builder() {
        return new AuditDiffBuilder();
    }

    public static final class AuditDiffBuilder {
        private final List<Change> changes = new ArrayList<>();

        public AuditDiffBuilder add(String field, Object oldValue, Object newValue) {
            String oldText = stringify(oldValue);
            String newText = stringify(newValue);
            if (!Objects.equals(oldText, newText)) {
                changes.add(new Change(field, oldText, newText));
            }
            return this;
        }

        public String toJsonOrNull() {
            if (changes.isEmpty()) {
                return null;
            }
            StringBuilder json = new StringBuilder("{\"changes\":[");
            for (int i = 0; i < changes.size(); i++) {
                Change change = changes.get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append("{\"field\":\"").append(escape(change.field())).append("\",")
                        .append("\"oldValue\":\"").append(escape(change.oldValue())).append("\",")
                        .append("\"newValue\":\"").append(escape(change.newValue())).append("\"}");
            }
            json.append("]}");
            return json.toString();
        }

        private String stringify(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private String escape(String value) {
            return value == null ? "" : value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }

    private record Change(String field, String oldValue, String newValue) {
    }
}
