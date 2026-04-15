package org.example.server;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Represents a key that is set to expire after a certain amount of time.
 * This class implements the Delayed interface, allowing it to be used in a DelayQueue for managing expiring keys in the 
 * Baby Redis server. Each ExpiringKey instance contains the key that is set to expire and the timestamp at which it will expire. 
 */
public class ExpiringKey implements Delayed {
    private final String key;
    private final Long expireAt;

    /**
     * Constructs a new ExpiringKey with the specified key and expiration timestamp. 
     * @param key the key that is set to expire
     * @param expireAt the timestamp (in milliseconds since the epoch) at which the key will expire
     */
    public ExpiringKey(String key, Long expireAt) {
        this.key = key;
        this.expireAt = expireAt;
    } 

    /**
     * Returns the delay of this ExpiringKey in the specified time unit.
     * @param unit the time unit for the return value
     * @return the delay of this ExpiringKey in the specified time unit
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert((expireAt - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }

    /**
     * Compares this ExpiringKey with another Delayed object based on their expiration times.
     * @param o the other Delayed object to compare with
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object
     */
    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ExpiringKey) {
            return (this.expireAt.compareTo(((ExpiringKey) o).expireAt));

        } else {
            return -1;
        }
    }

    /**
     * Returns the key associated with this ExpiringKey.
     * @return the key that is set to expire
     */
    public String getKey() {
        return key;
    }
}
