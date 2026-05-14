package io.babyredis.server.persistence;

import java.util.Map;
import java.util.Set;

/**
 * Represents the data to be snapshotted for the Baby Redis server.
 */
public record SnapshotData(
        Map<String, String> stringSnapshot,
        Map<String, Set<String>> setSnapshot,
        Map<String, Long> expiryQueueSnapshot
) {
}
