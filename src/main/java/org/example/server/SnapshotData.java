package org.example.server;

import java.util.Map;
import java.util.Set;

public record SnapshotData(
        Map<String, String> stringSnapshot,
        Map<String, Set<String>> setSnapshot,
        Map<String, Long> expiryQueueSnapshot
) {
}
