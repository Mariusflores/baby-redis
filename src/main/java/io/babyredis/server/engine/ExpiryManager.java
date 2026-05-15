package io.babyredis.server.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.function.Consumer;

import io.babyredis.error.BabyRedisException;
import io.babyredis.server.store.ExpiringKey;

public class ExpiryManager {

    private final DelayQueue<ExpiringKey> expireQueue = new DelayQueue<>();
    private final Map<String, Long> expireQueueState = new ConcurrentHashMap<>();

    public ExpiryManager(Consumer<String> purgeCallback) {

        // Daemon thread, tracks and removes items from expireQueue
        Runnable expireTrack = () -> {
            while (true) {
                try {
                    ExpiringKey key = expireQueue.take();

                    purgeCallback.accept(key.getKey());
                    expireQueueState.remove(key.getKey());

                } catch (InterruptedException e) {
                    throw new BabyRedisException(
                            "Error occurred while tracking expired keys",
                            e);
                }
            }
        };

        Thread.ofVirtual().start(expireTrack);
    }

    public void setQueueState(Map<String, Long> state) {
        expireQueueState.putAll(state);
    }

    public void loadQueue() {
        expireQueueState.forEach((k, v) -> expireQueue.add(new ExpiringKey(k, v)));
    }

    public Map<String, Long> getQueueState() {
        return Map.copyOf(expireQueueState);
    }

    public void addKey(String key, long expireAt) {

        ExpiringKey expiringKey = new ExpiringKey(key, expireAt);
        expireQueue.add(expiringKey);
        expireQueueState.put(key, expireAt);
    }

    public void removeKey(String key) {
        expireQueue.removeIf(k -> k.getKey().equals(key));
        expireQueueState.remove(key);
    }

}
