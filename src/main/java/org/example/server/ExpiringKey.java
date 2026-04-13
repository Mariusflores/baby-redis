package org.example.server;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ExpiringKey implements Delayed {
    private final String key;
    private final Long expireAt;

    public ExpiringKey(String key, Long expireAt) {
        this.key = key;
        this.expireAt = expireAt;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert((expireAt - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ExpiringKey) {
            return (this.expireAt.compareTo(((ExpiringKey) o).expireAt));

        } else {
            return -1;
        }
    }

    public String getKey() {
        return key;
    }
}
